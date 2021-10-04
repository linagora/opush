#!/bin/sh -e
#
### BEGIN INIT INFO
# Provides:          spushnik
# Required-Start:    $remote_fs $syslog $network
# Required-Stop:     $remote_fs $syslog $network
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: OBM ActiveSync component
# Description:       Spushnik is a webserver offering a http endpoint 
#                    to check Opush availability using ActiveSync protocol
### END INIT INFO


PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
NAME=spushnik
SPUSHNIK_HOME=/usr/share/$NAME
SPUSHNIK_USER=spushnik
SPUSHNIK_PORT=8083
LOGDIR="/var/log/spushnik"
LOGFILE="$LOGDIR/out.log"
START_JAR="$SPUSHNIK_HOME/spushnik.jar"
PIDFILE="/var/run/$NAME.pid"
HOSTNAME=$(uname -n)

if [ `id -u` -ne 0 ]; then
        echo "You need root privileges to run this script"
        exit 1
fi

# Make sure spushnik is started with system locale
if [ -r /etc/default/locale ]; then
        . /etc/default/locale
        export LANG
fi

. /lib/lsb/init-functions

if [ -r /etc/default/rcS ]; then
        . /etc/default/rcS
fi

if [ -z "$JAVA_HOME" ]; then
	test -d "/usr/lib/jvm/java-8-openjdk-"`dpkg --print-architecture` && {
        JAVA_HOME="/usr/lib/jvm/java-8-openjdk-"`dpkg --print-architecture`
    }
fi
if [ -z "$JAVA_HOME" ]; then
	test -d "/usr/lib/jvm/java-7-openjdk-"`dpkg --print-architecture` && {
        JAVA_HOME="/usr/lib/jvm/java-7-openjdk-"`dpkg --print-architecture`
    }
fi

if [ -z "$SHUTDOWN_TIMEOUT" ]; then
	SHUTDOWN_TIMEOUT=10
fi

JAVA_OPTIONS="$JAVA_OPTIONS -Dfile.encoding=UTF-8 -XX:+UseG1GC -Djava.io.tmpdir=$TMP_DIR -DspushnikPort=$SPUSHNIK_PORT"

JAVA="$JAVA_HOME/bin/java"
SPUSHNIK_COMMAND="$JAVA -- $JAVA_OPTIONS -jar $START_JAR"

##################################################
# SPUSHNIK FUNCTIONS
##################################################
isStopped () {
        start-stop-daemon --quiet --test --start --pidfile "$PIDFILE" \
                          --user "$SPUSHNIK_USER" --startas $JAVA > /dev/null
}

start () {
        start-stop-daemon --start --pidfile "$PIDFILE" --make-pidfile \
                          --chuid "$SPUSHNIK_USER" --startas $SPUSHNIK_COMMAND > $LOGFILE 2>&1 &
}

stop () {
        stopWithSigTERM
        while ! isStopped ; do
                sleep 1
                log_progress_msg "."
                SHUTDOWN_TIMEOUT=`expr $SHUTDOWN_TIMEOUT - 1` || true
                if [ $SHUTDOWN_TIMEOUT -ge 0 ]; then
                        stopWithSigTERM
                else
                        log_progress_msg " (killing) "
                        stopWithSigKILL
                fi
        done

        rm -f "$PIDFILE"
}
stopWithSigTERM () {
        start-stop-daemon --quiet --stop --signal 15 --pidfile "$PIDFILE" --oknodo \
                          --user "$SPUSHNIK_USER" --startas $JAVA >> $LOGFILE 2>&1
}
stopWithSigKILL () {
        start-stop-daemon --quiet --stop --signal 9 --pidfile "$PIDFILE" --oknodo \
                          --user "$SPUSHNIK_USER" --startas $JAVA >> $LOGFILE 2>&1
}

##################################################
# Do the action
##################################################
case "$1" in
  start)
        log_daemon_msg "Starting $NAME"
        if isStopped ; then

                if [ -f $PIDFILE ] ; then
                        log_warning_msg "$PIDFILE exists, but $NAME was not running. Ignoring $PIDFILE"
                fi

                if start ; then
	                log_daemon_msg "$NAME started, reachable on http://$HOSTNAME:$SPUSHNIK_PORT/." "$NAME"
                        log_end_msg 0
                else
                        log_end_msg 1
                fi

        else
                log_warning_msg "(already running)."
                log_end_msg 0
                exit 1
        fi
        ;;

  stop)
        log_daemon_msg "Stopping $NAME (was reachable on http://$HOSTNAME:$SPUSHNIK_PORT/)." "$NAME"

        if isStopped ; then
                if [ -x "$PIDFILE" ]; then
                        log_warning_msg "(not running but $PIDFILE exists)."
                else
                        log_warning_msg "(not running)."
                fi
        else
                stop
                log_daemon_msg "$NAME stopped."
                log_end_msg 0
        fi
        ;;

  status)
        if isStopped ; then
                if [ -f "$PIDFILE" ]; then
                    log_success_msg "$NAME is not running, but pid file exists."
                        exit 1
                else
                    log_success_msg "$NAME is not running."
                        exit 3
                fi
        else
                log_success_msg "$NAME is running with pid `cat $PIDFILE`, and is reachable on http://$HOSTNAME:$SPUSHNIK_PORT/"
        fi
        ;;

  restart)
        if ! isStopped ; then
                $0 stop $*
                sleep 1
        fi
        $0 start $*
        ;;

  check)
        log_success_msg "Checking arguments for $NAME: "
        log_success_msg ""
        log_success_msg "PIDFILE        =  $PIDFILE"
        log_success_msg "JAVA_OPTIONS   =  $JAVA_OPTIONS"
        log_success_msg "JAVA           =  $JAVA"
        log_success_msg "SPUSHNIK_USER     =  $SPUSHNIK_USER"
        log_success_msg "ARGUMENTS      =  $ARGUMENTS"

        if [ -f $PIDFILE ]
        then
                log_success_msg "$NAME is running with pid `cat $PIDFILE`, and is reachable on http://$HOSTNAME:$SPUSHNIK_PORT/"
                exit 0
        fi
        exit 1
        ;;

  *)
        log_success_msg "Usage: $0 {start|stop|restart|status|check}"
        exit 1
        ;;
esac

exit 0
