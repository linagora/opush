#!/bin/sh
#
### BEGIN INIT INFO
# Provides:          opush
# Required-Start:    $remote_fs $syslog $network
# Required-Stop:     $remote_fs $syslog $network
# Should-Start:      $named
# Should-Stop:       $named
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: OBM ActiveSync component
# Description:       Opush is a webserver offering a http endpoint 
#                    to synchronize emails, contacts and calendar of OBM users
### END INIT INFO

NAME=opush
OPUSH_USER=opush
OPUSH_HOME=/usr/share/$NAME
LOGDIR="/var/log/opush"
LOGFILE="$LOGDIR/out.log"
OPUSH_JAR="$OPUSH_HOME/opush.jar"
OPUSH_RUNNABLE="$OPUSH_HOME/opush-start.sh"
PIDFILE="/var/run/$NAME/$NAME.pid"

# Source function library.
. /etc/rc.d/init.d/functions
# Source /etc/default/opush if file exists
if [ -r /etc/default/opush ]; then
	. /etc/default/opush
fi

if [ -z "$OPUSH_PORT" ]; then
	OPUSH_PORT=8082
fi
if [ -z "$JAVA_HOME" ]; then
	JAVA_HOME="/usr/lib/jvm/jre-1.7.0"
fi
if [ ! -d "$TMP_DIR" ]; then
	TMP_DIR="/tmp"
fi
if [ -z "$SHUTDOWN_TIMEOUT" ]; then
	SHUTDOWN_TIMEOUT=10
fi

JAVA_OPTIONS="$JAVA_OPTIONS -Dfile.encoding=UTF-8 -XX:+UseG1GC -Djava.io.tmpdir=$TMP_DIR -DopushPort=$OPUSH_PORT"
JAVA="$JAVA_HOME/bin/java"

start() {

        if [ -f $PIDFILE ] ; then
                read kpid < $PIDFILE
                if checkpid $kpid 2>&1; then
                        echo "process already running"
                        return 0
                else
                        echo "pid file found but no process running for pid $kpid, continuing"
                fi
        fi

        echo -n $"Starting $NAME: "

        if [ -r /etc/rc.d/init.d/functions ]; then
                daemon --pidfile $PIDFILE --user $OPUSH_USER OPUSH_JAR=$OPUSH_JAR PIDFILE=$PIDFILE LOGFILE=$LOGFILE JAVA=$JAVA JAVA_OPTIONS=\"$JAVA_OPTIONS\" $OPUSH_RUNNABLE
        else
                echo "Cannot found functions library at /etc/rc.d/init.d/functions"
        fi

        echo
        return 0
}

stop() {

        echo -n $"Stopping $NAME: "
        count=0;

        if [ -f $PIDFILE ]; then

            read kpid < $PIDFILE
            let kwait=$SHUTDOWN_TIMEOUT

#           Try issuing SIGTERM

            kill -15 $kpid
            until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
            do
                echo -n ".";
                sleep 1
                let count=$count+1;
            done

            if [ $count -gt $kwait ]; then
                echo "killing processes which didn't stop after $SHUTDOWN_TIMEOUT seconds"
                kill -9 $kpid
            fi
            rm -f $PIDFILE
        fi
        success
        echo
}

getStatus() {
        status -p $PIDFILE opush
}

case "$1" in
  start)
        start
        ;;
  stop)
        stop
        ;;
  status)
        status -p $PIDFILE opush
        exit $?
        ;;
  restart)
        stop
        sleep 2
        start
        ;;
  condrestart|try-restart)
        getStatus
        if [ "$?" -eq 0 ] ; then
		stop
		sleep 2
		start
        fi
        ;;
  *)
        echo "Usage: $0 {start|stop|status|restart|condrestart}"
        exit 1
esac


exit 0

#
#
# end
