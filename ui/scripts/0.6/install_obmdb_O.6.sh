#!/bin/sh
###############################################################################
# OBM - File : install_obmdb_0.6.sh                                           #
#     - Desc : MySQL Database 0.6 installation script                         #
# 2002-12-17 ALIACOM                                                          #
###############################################################################
# $Id$ #
###############################################################################

# Mysql User and Password var definition
U=root
P=""


# We search for the PHP interpreter (different name on Debian, RedHat)
PHP=`which php4 2> /dev/null`
if [ $? != 0 ]; then
  PHP=`which php 2> /dev/null`
  if [ $? != 0 ]; then
    echo "Can't find php interpreter"
    exit
  fi
fi
echo $PHP : PHP interpreter found


# Database creation
echo "Database creation"
mysql -u $U -p$P < create_obmdb_0.6.mysql.sql

# Dictionnary data insertion
echo "Dictionnary data insertion"
mysql -u $U -p$P obm < create_obmdb_0.6_fr.mysql.sql

# Test data insertion
echo "Test data insertion"
mysql -u $U -p$P obm < obmdb_test_values_0.6.sql

# Default preferences data insertion
echo "Default preferences data insertion"
mysql -u $U -p$P obm < obmdb_default_values_0.6.sql

# Default preferences propagation on created users
echo "Default preferences propagation on created users"
$PHP ../../php/admin_pref/admin_pref_index.php -a user_pref_update

# Update calculated values
echo "Update calculated values"
$PHP ../../php/admin_data/admin_data_index.php -a data_update
