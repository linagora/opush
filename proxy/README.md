Active Sync Proxy Apache2 Module using mod perl
=================================================

CPAN archive build
----------------------

It uses [Dist::Zilla](http://dzil.org) to build a release tarball.

To build it, just do :

    cd ProxyAS-Handler/
    dzil build

The CPAN archive is then created

Debian package build
-----------------------------

    cd ProxyAS-Handler/
    cd ProxyAS-Handler-VERSION/
    dpkg-buildpackage -b -us -uc  

The Docker way
--------------

    docker build -t=proxyasbuild .
    docker run --rm --env version=VERSION -v $PWD/ProxyAS-Handler:/ProxyAS-Handler proxyasbuild

Sample configuration
--------------------

A sample configuration is located in conf directory.
