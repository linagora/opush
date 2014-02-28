#!/bin/sh -e

# Stop spushnik if that is a remove
if [ "$1" -eq "0" ]; then
	/sbin/service spushnik stop >/dev/null 2>&1 || :
fi
