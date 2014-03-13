#!/bin/sh -e

# condrestart opush if that is an update
if [ "$1" -gt "0" ]; then
	service opush condrestart >/dev/null 2>&1 || :
fi
