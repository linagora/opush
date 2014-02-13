#!/bin/sh -e

# Stop opush if that is a removal
if [ "$1" -eq "0" ]; then
	/sbin/service opush stop >/dev/null 2>&1 || :
fi
