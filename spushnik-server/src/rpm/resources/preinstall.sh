#!/bin/sh

if ! id spushnik > /dev/null 2>&1 ; then
	useradd --system --user-group spushnik
fi
