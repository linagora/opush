#!/bin/sh -e

if ! id spushnik > /dev/null 2>&1 ; then
	useradd --system --user-group --create-home --home-dir /usr/share/spushnik spushnik
fi
