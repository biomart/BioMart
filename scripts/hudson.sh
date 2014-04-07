#!/bin/bash -xe
# DCCTEST-992 - [cBM1102171357]
# author: Anthony Cros (anthony.cros@oicr.on.ca)
# usage: ./dist/scripts/hudson.sh
#
# this script is there to provide a placeholder for hudson to run command, this way it keeps the hudson configuration minimal by simply refering to this script
# the only steps necessary in hudson are therefore: svn update, stop biomart server (if running), ant clean+build, call to this script
#

TIMESTAMP=`date '+%y%m%d%H%M'` # [cBM1102021507]
echo "TIMESTAMP=${TIMESTAMP?}"

# initialization
export BIOMART_HOST=10.0.3.226 # for windows; = odl-acros (temporarily, see DCCTEST-1044); though biomart server is actually *started* on 0.0.0.0 (so it's accessible from outside too)
export BIOMART_PORT=9001

export LINUX=linux
export WINDOWS=windows
#TODO: mac?

export LINUX_BROWSERS="firefox googlechrome"
#export WINDOWS_BROWSERS="${LINUX_BROWSERS?}"
export WINDOWS_BROWSERS="iexplore ${LINUX_BROWSERS?}"
#export WINDOWS_BROWSERS="iexplore"

# test linux
./dist/scripts/slave.sh ${BIOMART_HOST?} ${BIOMART_PORT?} "${LINUX_BROWSERS?}" ${LINUX?}
echo "LINUX WAS SUCCESSFUL ON: '${LINUX_BROWSERS?}'"

# test windows
./dist/scripts/slave.sh ${BIOMART_HOST?} ${BIOMART_PORT?} "${WINDOWS_BROWSERS?}" ${WINDOWS?}
echo "WINDOWS WAS SUCCESSFUL ON: '${WINDOWS_BROWSERS?}'"

# finalization
echo OK
echo "TIMESTAMP=${TIMESTAMP?}"
exit 0
