#!/bin/sh

if ! id opush > /dev/null 2>&1 ; then
	useradd --system --user-group opush
fi
