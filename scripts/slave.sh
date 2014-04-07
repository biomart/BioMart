#!/bin/bash -xe
# DCCTEST-992 - [cBM1102171357]
# author: Anthony Cros (anthony.cros@oicr.on.ca)
# usage: ./slave.sh host port browsers(whitespace-separated) os
#
# runs the selenium tests for the given slave os (linux or windows only for now)
#

echo $*
TIMESTAMP=`date '+%y%m%d%H%M'` # [cBM1102021507]
echo "TIMESTAMP=${TIMESTAMP?}"

# ==============================================================================================================
# initializations

# checking necessary variables are set
: ${LINUX?}
: ${WINDOWS?}

# constants
SELENIUM=selenium
QUNIT=qunit
SELENIUM_SERVER_JAR_NAME=selenium-server-standalone-2.0b1.jar # current version
BIOMART_START_HOST=0.0.0.0 # so that biomart server is accessible as both localhost and on the network
BUILD_DIR=../builds/${BUILD_ID} # by design
REMOTE=1 # always remote for now
DAEMON_RUNNER=daemon_runner
DAEMON_RUNNER_PYTHON_SCRIPT=${DAEMON_RUNNER?}.py

# arguments
BIOMART_HOST=$1
BIOMART_PORT=$2
BROWSERS_STRING=$3
OS=$4

BIOMART_HOST=${BIOMART_HOST:=${DEFAULT_BIOMART_HOST?}}
BIOMART_PORT=${BIOMART_PORT:=${DEFAULT_BIOMART_PORT?}}
BROWSERS_STRING=${BROWSERS_STRING:=${DEFAULT_BROWSERS_STRING?}}
OS=${OS:=${DEFAULT_OS?}}

# computed constants
SELENIUM_HTML_FILE=${SELENIUM?}.html
QUNIT_HTML_FILE=${QUNIT?}.html
SELENIUM_SCRIPT_NAME=${SELENIUM?}.sh
TARGET_DIR_NAME=${SELENIUM?}
CYGWIN=0 # unless changed further down
if [ ${REMOTE?} == 0 ]; then
    TARGET_DIR=${BUILD_DIR?}/${TARGET_DIR_NAME?}
else
  if [ "${OS?}" == "${LINUX?}" ]; then
    TARGET_USER=test
    TARGET_HOST=${BIOMART_HOST?} # temporarily
    TARGET_DIR=/home/${TARGET_USER?}/${TARGET_DIR_NAME?}
  else
    CYGWIN=1
    TARGET_USER=Administrator # case matters
    TARGET_HOST=arektest.res.oicr.on.ca
    TARGET_DIR=/cygdrive/g/${TARGET_DIR_NAME?}
  fi
fi

# ==============================================================================================================
# start biomart server
./dist/scripts/biomart-server.sh check -Xmx1024m -Dhttp.host=${BIOMART_START_HOST?} -Dhttp.port=${BIOMART_PORT?} || :
./dist/scripts/biomart-server.sh restart -Xmx1024m -Dhttp.host=${BIOMART_START_HOST?} -Dhttp.port=${BIOMART_PORT?}
sleep 5 # will need at least that much time anyway
ATTEMPT=0
while true; do
  echo "attempt ${ATTEMPT?}"
  set +e ; wget -O /dev/null http://${BIOMART_HOST?}:${BIOMART_PORT?}/ ; EXIT_STATUS=$? ; set -e
  if [ ${EXIT_STATUS?} == 0 ]; then
    break
  fi
  if [ ${ATTEMPT?} -gt 5 ]; then
    exit 99 # "timeout" for server to start
  else
    ATTEMPT=$[ATTEMPT+1]
    sleep 5
  fi
done

# ==============================================================================================================
# define functions to be used as "runner"

# basic bash run; test with: bash_runner 'echo $USER@`hostname`'
function bash_runner() { /bin/bash -c "$*"; }

# basic ssh run; test with: ssh_runner 'echo $USER@`hostname`'
function ssh_runner() { ssh ${TARGET_USER?}@${TARGET_HOST?} "$*"; }

# [BM1102161120] special ssh run (accounts for selenium not closing its resources properly)
#	- for now using a workaround (flag files), as even the "shopt -s huponexit" recommended by OpenSsh (http://openssh.com/faq.html#3.10) does not seem to work ([cBM1102111856])
#	- TARGET_USER, TARGET_HOST, FLAG must be set prior to using it
#	- test with:
#		set -x && TARGET_USER=test && TARGET_HOST=localhost && FLAG=/tmp/flag && ARGS='echo $USER@'"\`"'hostname'"\`" TMP=`ssh_hang_on_exit_flag_runner "${ARGS?}"` && echo "TMP=${TMP?}"
#	
# caution: may not work if used with ``s in arguments (needs investigating: [cBM1102161234])
function ssh_hang_on_exit_flag_runner() {
  DELETE_FLAG_COMMAND='/bin/rm '"${FLAG?}"
  WAIT_FLAG_COMMAND='while [ ! -f '"${FLAG?}"' ]; do sleep 0.5; done; cat '"${FLAG?}"'; '"$DELETE_FLAG_COMMAND" # only returns flag value
  INFINITE_LOOP='echo "infinite loop" ; while true; do sleep 0.5; done'
  ssh ${TARGET_USER?}@${TARGET_HOST?} "${DELETE_FLAG_COMMAND?}"' || :' # may not exist
  ssh -X ${TARGET_USER?}@${TARGET_HOST?} "$*"' ; echo -n $? > '"${FLAG?} ; ${INFINITE_LOOP?}" & SSH_PID=$!
  EXIT_STATUS=`ssh ${TARGET_USER?}@${TARGET_HOST?} "${WAIT_FLAG_COMMAND?}"`
  kill ${SSH_PID?}
  return ${EXIT_STATUS?}
}

# ==============================================================================================================
# running command placeholder
if [ ${REMOTE?} == 0 ]; then
  SCRIPT_DIR=./dist/scripts
  LIB_DIR=./dist/lib
  function runner() { bash_runner "$*"; }
else
  LIB_DIR=${TARGET_DIR?}
  SCRIPT_DIR=${TARGET_DIR?}

  SELENIUM_SERVER_JAR="${LIB_DIR?}/${SELENIUM_SERVER_JAR_NAME?}"
  # copy necessary files
  ssh ${TARGET_USER?}@${TARGET_HOST?} "[ -f ${SELENIUM_SERVER_JAR?} ]" # assumption is that the selenium server has already been copied (a bit big); make sure of it
  scp ./dist/scripts/${DAEMON_RUNNER_PYTHON_SCRIPT?} ./dist/scripts/${SELENIUM_SCRIPT_NAME?} ./dist/web/webapps/martapps/tests/${SELENIUM_HTML_FILE?} ./dist/web/webapps/martapps/tests/${QUNIT_HTML_FILE?} ${TARGET_USER?}@${TARGET_HOST?}:${TARGET_DIR?}
  
  if [ ${CYGWIN?} == 0 ]; then
    # use special ssh runner (see description)
    FLAG="${SCRIPT_DIR?}/${SELENIUM_SCRIPT_NAME?}.flag"
    function runner() {  ssh_hang_on_exit_flag_runner "$*"; } # [cBM1102111520]
  else
    # ensure daemon runner is running
    # 	TODO  ps not working on cygwin anymore...	ssh ${TARGET_USER?}@${TARGET_HOST?} '[ -n "`ps -W | grep '"${DAEMON_RUNNER?}"'`" ]' # assumption is that daemon is already running
    # for now assume is running (should anyway)

    # use basic ssh runner (sufficient for cygwin)
    function runner() {  ssh_runner "$*"; }
  fi
fi
export -f runner

# run it (will use appropriate runner as set above)
runner 'export TARGET_DIR_REMOTE='"${TARGET_DIR?}"' && mkdir -p ${TARGET_DIR_REMOTE?} && '\
"  ${SCRIPT_DIR?}/${SELENIUM_SCRIPT_NAME?}"' '"${BIOMART_HOST?}"' '"${BIOMART_PORT?}"' "'"${BROWSERS_STRING?}"'" '"${SELENIUM_SERVER_JAR?}"\
'  > ${TARGET_DIR_REMOTE?}/'"${SELENIUM?}"'.'"${OS?}"'.oe 2>&1' ; \
  EXIT_STATUS=$? ; echo ${EXIT_STATUS?} ; exit ${EXIT_STATUS?} # [cBM1102111605]

# ==============================================================================================================
# finalization

echo "TIMESTAMP=${TIMESTAMP?}"
echo OK
exit 0
