#!/bin/sh -e
#
### BEGIN INIT INFO
# Provides:          opush
# Required-Start:    $remote_fs $syslog $network
# Required-Stop:     $remote_fs $syslog $network
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: OBM ActiveSync component
# Description:       Opush is a webserver offering a http endpoint 
#                    to synchronize emails, contacts and calendar of OBM users
### END INIT INFO


PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
NAME=opush
OPUSH_HOME=/usr/share/$NAME
OPUSH_USER=opush
LOGDIR="/var/log/opush"
LOGFILE="$LOGDIR/out.log"
START_JAR="$OPUSH_HOME/opush.jar"
PIDFILE="/var/run/$NAME.pid"
HOSTNAME=$(uname -n)

if [ `id -u` -ne 0 ]; then
        echo "You need root privileges to run this script"
        exit 1
fi

# Make sure opush is started with system locale
if [ -r /etc/default/locale ]; then
        . /etc/default/locale
        export LANG
fi

. /lib/lsb/init-functions

if [ -r /etc/default/rcS ]; then
        . /etc/default/rcS
fi


# Source /etc/default/opush if file exists
if [ -r /etc/default/opush ]; then
	. /etc/default/opush
fi
if [ -z "$OPUSH_PORT" ]; then
	OPUSH_PORT=8082
fi
if [ -z "$JAVA_HOME" ]; then
	JAVA_HOME="/usr"
fi

if [ ! -d "$TMP_DIR" ]; then
	TMP_DIR="/tmp"
fi
if [ -z "$SHUTDOWN_TIMEOUT" ]; then
	SHUTDOWN_TIMEOUT=10
fi

JAVA_OPTIONS="$JAVA_OPTIONS -Dfile.encoding=UTF-8 -XX:+UseG1GC -Djava.io.tmpdir=$TMP_DIR -DopushPort=$OPUSH_PORT"

JAVA="$JAVA_HOME/bin/java"
OPUSH_COMMAND="$JAVA -- $JAVA_OPTIONS -jar $START_JAR"

##################################################
# OPUSH FUNCTIONS
##################################################
opushIsStopped () {
        start-stop-daemon --quiet --test --start --pidfile "$PIDFILE" \
                          --user "$OPUSH_USER" --startas $JAVA > /dev/null
}

startOpush () {
        start-stop-daemon --start --pidfile "$PIDFILE" --make-pidfile \
                          --chuid "$OPUSH_USER" --startas $OPUSH_COMMAND > $LOGFILE 2>&1 &
}

stopOpush () {
        stopOpushWithSigTERM
        while ! opushIsStopped ; do
                sleep 1
                log_progress_msg "."
                SHUTDOWN_TIMEOUT=`expr $SHUTDOWN_TIMEOUT - 1` || true
                if [ $SHUTDOWN_TIMEOUT -ge 0 ]; then
                        stopOpushWithSigTERM
                else
                        log_progress_msg " (killing) "
                        stopOpushWithSigKILL
                fi
        done

        rm -f "$PIDFILE"
}
stopOpushWithSigTERM () {
        start-stop-daemon --quiet --stop --signal 15 --pidfile "$PIDFILE" --oknodo \
                          --user "$OPUSH_USER" --startas $JAVA >> $LOGFILE 2>&1
}
stopOpushWithSigKILL () {
        start-stop-daemon --quiet --stop --signal 9 --pidfile "$PIDFILE" --oknodo \
                          --user "$OPUSH_USER" --startas $JAVA >> $LOGFILE 2>&1
}

##################################################
# Do the action
##################################################
case "$1" in
  start)
        log_daemon_msg "Starting $NAME"
        if opushIsStopped ; then

                if [ -f $PIDFILE ] ; then
                        log_warning_msg "$PIDFILE exists, but $NAME was not running. Ignoring $PIDFILE"
                fi

                if startOpush ; then
	                log_daemon_msg "$NAME started, reachable on http://$HOSTNAME:$OPUSH_PORT/." "$NAME"
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
        log_daemon_msg "Stopping $NAME (was reachable on http://$HOSTNAME:$OPUSH_PORT/)." "$NAME"

        if opushIsStopped ; then
                if [ -x "$PIDFILE" ]; then
                        log_warning_msg "(not running but $PIDFILE exists)."
                else
                        log_warning_msg "(not running)."
                fi
        else
                stopOpush
                log_daemon_msg "$NAME stopped."
                log_end_msg 0
        fi
        ;;

  status)
        if opushIsStopped ; then
                if [ -f "$PIDFILE" ]; then
                    log_success_msg "$NAME is not running, but pid file exists."
                        exit 1
                else
                    log_success_msg "$NAME is not running."
                        exit 3
                fi
        else
                log_success_msg "$NAME is running with pid `cat $PIDFILE`, and is reachable on http://$HOSTNAME:$OPUSH_PORT/"
        fi
        ;;

  restart)
        if ! opushIsStopped ; then
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
        log_success_msg "OPUSH_USER     =  $OPUSH_USER"
        log_success_msg "ARGUMENTS      =  $ARGUMENTS"

        if [ -f $PIDFILE ]
        then
                log_success_msg "$NAME is running with pid `cat $PIDFILE`, and is reachable on http://$HOSTNAME:$OPUSH_PORT/"
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
