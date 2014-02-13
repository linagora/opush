#!/bin/sh -e

if ! id opush > /dev/null 2>&1 ; then
	useradd --system --user-group --create-home --home-dir /usr/share/opush opush
fi
