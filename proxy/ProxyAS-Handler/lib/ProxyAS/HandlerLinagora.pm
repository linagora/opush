#==============================================================================
# ABSTRACT: Apache2 mod_perl Proxy for Active Sync requests
#==============================================================================

package ProxyAS::HandlerLinagora;

our $VERSION = '0.1';

use Apache2::Log;
use Apache2::RequestUtil ();
use Apache2::RequestRec  ();
use Apache2::RequestIO   ();
use Apache2::Const       ( "-compile", qw(:common :log) );
use Apache2::URI         ();
use Apache2::ServerUtil  ();
use APR::Table           ();
use MIME::Base64 qw(decode_base64);
use LWP::UserAgent;
use File::Cache;

use strict;

our $r;
our $email;
our $activeSyncServer;
our $baseHTTP;
our $cache;
our $pTTL = 86400;
our $nTTL = 3600;

## @rmethod void pLog(string mess, string level)
# Wrapper for Apache log system
# @param $mess message to log
# @param $level string (emerg, alert, crit, error, warn, notice, info or debug)
sub pLog {
    my ( $mess, $level ) = splice @_;

    die("Level is required") unless ($level);

    Apache2::ServerRec->log->$level($mess);

    return 1;
}

## @rmethod string getActiveSyncServer()
# @param $r request which contains configurations for the ldap request
# @param $email email to look for association with active sync server
# @return uri of the active sync server, null if not found
sub getActiveSyncServer {
    my $server = [
        $r->dir_config('ProxyASPrimaryServer'),
        $r->dir_config('ProxyASPrimaryServerPort')
    ];
    my @users = $r->dir_config->get('ProxyASSecondaryServerUser');
    if ( grep( /$email/, @users ) ) {
        pLog( "User $email will use the secondary server", "debug" );
        $server = [
            $r->dir_config('ProxyASSecondaryServer'),
            $r->dir_config('ProxyASSecondaryServerPort')
        ];
    }

    return $server;
}

## @rmethod string isConfigurationValid()
# Check configuration integrity
sub isConfigurationValid {
    my $isValid = "true";

    $isValid = '' if ( $r->dir_config('ProxyASPrimaryServer') eq "" );

    return $isValid;
}

## @rmethod void initCache()
# Cache initialization
sub initCache {
    $pTTL = $r->dir_config('PositiveTTL')
      if ( $r->dir_config('PositiveTTL') ne "" );
    $nTTL = $r->dir_config('NegativeTTL')
      if ( $r->dir_config('NegativeTTL') ne "" );
    pLog( "PositiveTTL : " . $pTTL, 'debug' );
    pLog( "NegativeTTL : " . $nTTL, 'debug' );

    $cache =
      new File::Cache( { namespace => 'ProxyASCache', filemode => 0600 } );
}

## @cmethod void headers(HTTP::Request response)
# Send headers received from remote server to the client.
# Replace "Location" header.
# @param $response current HTTP response
sub headers {
    my $response = shift;
    my $tmp      = $response->header('Content-Type');
    $r->content_type($tmp) if ($tmp);
    $r->status( $response->code );
    $r->status_line( join ' ', $response->code, $response->message );

    pLog( "Response status line: " . $r->status_line, 'debug' );

    # Scan LWP response headers to generate Apache response headers
    my ( $server, $port ) = @$activeSyncServer;
    my $location_old = $port ? "$server:$port" : $server;
    my $location_new = $r->hostname;
    my $removepath   = $r->dir_config('ProxyASPathToRemove');
    my $addpath      = $r->dir_config('ProxyASPathToAdd');

    pLog( "Header pushed to the client:", 'debug' );
    $response->scan(
        sub {

            # Replace Location headers
            $_[1] =~ s#$location_old#$location_new#o
              if ( $location_old and $location_new and $_[0] =~ /Location/i );
            $_[1] =~ s#$removepath#$addpath#o
              if ( $removepath and $addpath and $_[0] =~ /Location/i );
            $r->err_headers_out->set( $_[0] => $_[1] );
            pLog( "   $_[0] => $_[1]", 'debug' );
            return 1;
        }
    );
}

## @rmethod protected int handler()
# Contains all the processing engine
# @return Apache constant
sub handler ($$) {

    # Get the request object
    $r = shift;

    # Check and read configuration
    if ( !isConfigurationValid() ) {
        pLog( "Configurations are not set correctly", 'emerg' );
        return Apache2::Const::SERVER_ERROR;
    }

    # Choose HTTP or HTTPS
    $baseHTTP = $r->dir_config('ProxyASHTTPS') ? "https://" : "http://";

    initCache();

    # This section is only to print debug informations
    pLog( "Request headers : ", 'debug' );
    $r->headers_in->do(
        sub {
            my ( $key, $value ) = @_;
            pLog( "   $key => $value", 'debug' );
            return 1;
        }
    );

    ## Get the user and domain to create the email
    my $domain = $r->dir_config('EmailDomain');
    $r->headers_in->{'Authorization'} =~ /^Basic (.+)$/;
    my ( $user, $password ) = split( /:/, decode_base64($1), 2 );

    # Reject unauthenticated request
    if ( !$user and !$password ) {
        pLog( "No user and password found in the request", 'error' );
        return Apache2::Const::AUTH_REQUIRED;
    }

    # Remove domain prefix
    $user =~ s/(.*)\\//;

    # Create email if necessary
    $email = $user;
    $email = $email . "@" . $domain if ( $domain ne "" );
    pLog( "User's email to look for: <$email>", 'debug' );

    # No \ in cache key
    $user =~ s/\\//g;

    ## Get the Active Sync server for this user
    # # If the email/server are not in the cache
    if ( !defined $cache->get($user) ) {
        pLog( "<$user> is not set in the cache", 'debug' );

        # # # Look for the right server to relay the request to
        $activeSyncServer = getActiveSyncServer();

        # # # Manage the cache accordingly to the LDAP search
        if ($activeSyncServer) {
            $cache->set( $user, $activeSyncServer, $pTTL );
        }
        else {
            $cache->set( $user, "", $nTTL );
        }

        # # Else set the server from the cache
    }
    else {
        $activeSyncServer = $cache->get($user);
        pLog( "<$user> is set in the cache, server is: $activeSyncServer",
            'debug' );
    }

    ## Relay the request or reply with an HTTP Error
    if ( !$activeSyncServer ) {
        pLog( "No ActiveSync server found", 'error' );
        return Apache2::Const::AUTH_REQUIRED;
    }

    # Add user in the logs
    $r->user($email);

    my $UA = new LWP::UserAgent;
    $UA->requests_redirectable( [] );

    # If args are set into the requested uri
    my $uri = $r->uri();
    $uri .= "?" . $r->args if ( $r->args );

    # Remove/Add path
    my $removepath = $r->dir_config('ProxyASPathToRemove');
    my $addpath    = $r->dir_config('ProxyASPathToAdd');
    if ( $removepath && $addpath ) {
        $uri =~ s#$removepath#$addpath#o;
    }

    my ( $server, $port ) = @$activeSyncServer;
    my $fullserver = $port ? "$server:$port" : $server;

    # Create the request to the ActiveSync Server
    my $request =
      new HTTP::Request( $r->method, $baseHTTP . $fullserver . $uri );

    pLog( "Request from device: " . $request->as_string, 'debug' );

    # Scan Apache request headers to generate LWP request headers
    # Host|Referer header are not set on the LWP request headers
    pLog( "Header pushed to the active sync server:", 'debug' );
    $r->headers_in->do(
        sub {
            return 1 if ( $_[1] =~ /^$/ );
            $request->header(@_) unless ( $_[0] =~ /^(Host|Referer)$/i );
            if ( $_[0] =~ /^(Host|Referer)$/i ) {
                pLog( "   $_[0] => $server", 'debug' );
            }
            else { pLog( "   $_[0] => $_[1]", 'debug' ); }
            return 1;
        }
    );
    $request->header( Host => $server );

    # Copy POST data, if any
    if ( $r->method eq "POST" ) {
        my $len = $r->headers_in->{'Content-Length'};

        #  The LENGTH argument can't be negative
        if ( $len > 0 ) {
            my $buf;
            $r->read( $buf, $len );
            pLog( "Add POST content: $buf", 'debug' );
            $request->content($buf);
        }
    }

    # For performance, we use a callback. See LWP::UserAgent for more
    my $response = $UA->request($request);

    pLog( "Response from Active Sync Server " . $response->as_string, 'debug' );

    # Transform headers
    headers($response);

    my $response_content = $response->content;

    if ($response_content) {

        pLog( "Response content " . $response->content, 'debug' );
        $r->print( $response->content );
    }
    else {
        pLog( "No content in response", 'debug' );
    }

    return Apache2::Const::OK if $response->is_success();

    return Apache2::Const::DONE;
}

1;
