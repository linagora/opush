#!/bin/sh -e

# condrestart spushnik if that is an update
if [ "$1" -gt "0" ]; then
	if [ -f /etc/init.d/spushnik ]; then
		/sbin/service spushnik condrestart >/dev/null 2>&1 || :
	fi 	
fi
