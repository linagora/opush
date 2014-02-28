#!/bin/sh -e

exec $JAVA $JAVA_OPTIONS -jar $SPUSHNIK_JAR > $LOGFILE 2>&1 &
echo $! >$PIDFILE
