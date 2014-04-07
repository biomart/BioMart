#!/bin/bash
# anthony.cros@oicr.on.ca
SCRIPT_DIR=`dirname $0`
[ -d ${SCRIPT_DIR?} ] || { echo -e "ERROR\tcan not find script dir: ${SCRIPT_DIR?}"; exit 1; }
export QUICK_TEST=true
${SCRIPT_DIR?}/build.sh
