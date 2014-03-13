#!/bin/sh -e

if [ ! -f /etc/init.d/spushnik ]; then
	ln -s /etc/spushnik/spushnik.sh /etc/init.d/spushnik 
fi
