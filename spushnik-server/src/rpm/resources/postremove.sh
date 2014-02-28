#!/bin/sh -e

# condrestart spushnik if that is an update
if [ "$1" -gt "0" ]; then
	/sbin/service spushnik condrestart >/dev/null 2>&1 || :
fi
