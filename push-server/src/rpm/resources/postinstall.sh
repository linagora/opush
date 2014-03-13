#!/bin/sh -e

# When upgrading old OPush, init script is deleted
if [ ! -f /etc/init.d/opush ]; then
	cp /etc/opush/opush.sh /etc/init.d/opush 
fi

# When upgrading old OPush, give valide rights to OPush folders
rights() {
	local GROUP=`stat -c "%G" $1`
	local USER=`stat -c "%U" $1`
	if [ "$GROUP" != 'opush' ] || [ "$USER" != 'opush' ]; then
	        chown -R opush:opush $1
	fi
}

rights "/etc/opush"
rights "/var/lib/opush"
rights "/var/log/opush"
rights "/var/run/opush"
rights "/usr/share/opush"