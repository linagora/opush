#!/bin/sh -e

# Stop opush if that is a removal
if [ "$1" -eq "0" ]; then
	if [ -f /etc/init.d/opush ]; then
		service opush stop >/dev/null 2>&1 || :
	fi
fi
