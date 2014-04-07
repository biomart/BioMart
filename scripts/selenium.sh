#!/bin/bash -xe
# DCCTEST-992 - [cBM1102091700]
# author: Anthony Cros (anthony.cros@oicr.on.ca)
# usage: ./selenium.sh host port browsers(whitespace-separated) selenium-jar 
#
# tests each browsers on a (hudson) slave
#

echo $*
TIMESTAMP=`date '+%y%m%d%H%M'` # [cBM1102021507]
echo "TIMESTAMP=${TIMESTAMP?}"

# ==============================================================================================================
# initializations

# checking necessary variables are set
: ${TARGET_DIR_REMOTE?}

# constants
SELENIUM=selenium
QUNIT=qunit
CHANGE_DISPLAY=0

# arguments
BIOMART_HOST=$1
BIOMART_PORT=$2
BROWSERS=$3
SELENIUM_SERVER_JAR=$4

BIOMART_HOST=${BIOMART_HOST:=${DEFAULT_BIOMART_HOST?}}
BIOMART_PORT=${BIOMART_PORT:=${DEFAULT_BIOMART_PORT?}}
BROWSERS=(${BROWSERS:=${DEFAULT_BROWSERS_STRING?}}) # array!
SELENIUM_SERVER_JAR=${SELENIUM_SERVER_JAR:=${DEFAULT_SELENIUM_SERVER_JAR?}}

echo "BIOMART_HOST=|${BIOMART_HOST?}|"
echo "BIOMART_PORT=|${BIOMART_PORT?}|"
echo "BROWSERS=|${BROWSERS[*]?}|"
echo "SELENIUM_SERVER_JAR=|${SELENIUM_SERVER_JAR?}|"

# computed constants
BIOMART_SERVER="http://${BIOMART_HOST?}:${BIOMART_PORT?}"
FILES=(${SELENIUM?} ${QUNIT?})
HTML_SUITE=${SELENIUM?}.html
OS=`uname -a`
if [[ "$OS" =~ ^CYGWIN ]]; then
  CYGWIN=1
else
  CYGWIN=0
fi

# ==============================================================================================================
# download necessary html files if not present
for FILE in ${FILES[*]?}; do
  OUTPUT_FILE_NAME=${FILE?}.html
  OUTPUT_FILE=${TARGET_DIR_REMOTE?}/${OUTPUT_FILE_NAME?}
  if [ ! -f ${OUTPUT_FILE?} ]; then
    echo "downloading ${OUTPUT_FILE_NAME?}"
    wget -O ${OUTPUT_FILE?} ${BIOMART_SERVER?}/tests/${OUTPUT_FILE_NAME?}
    echo
    cat ${TARGET_DIR_REMOTE?}/${FILE?}.html
    echo
  else
    echo "reusing ${OUTPUT_FILE_NAME?}"
  fi
done

# ==============================================================================================================
# handle additional display if required
echo $DISPLAY
if [ ${CHANGE_DISPLAY?} != 0 ]; then # FIXME does not work with selenium for now (not sure why)
  DISPLAY_NUMBER=4

  set +e # emulate java finally
  Xvfb :${DISPLAY_NUMBER?} -ac -screen 0 1024x768x8 > ${TARGET_DIR_REMOTE?}/Xvfb.log 2>&1 & XVFB_PID=$!
  export DISPLAY"=:${DISPLAY_NUMBER?}.0"

  # test with xclock
  sleep 2
  xclock & XCLOCK_PID=$!
  sleep 2
  xwd -display :${DISPLAY_NUMBER?} -root > ${TARGET_DIR_REMOTE?}/xclock.xwd # works
  sleep 2
  kill ${XCLOCK_PID?}
fi

# ==============================================================================================================
# define function for daemon runner: easier than try to access Windows Display Manager from cygwin; TODO JNLP in hudson may be an alternative

if [ $CYGWIN != 0 ]; then
	function windows_daemon() { # [cBM1102151350]
	  JAVA_OPTIONS=$*
	  DAEMON_RUNNER_NAME=daemon_runner.py
	  DAEMON_RUNNER="${TARGET_DIR_REMOTE?}/${DAEMON_RUNNER_NAME?}"
	  INPUT_FILE="${DAEMON_RUNNER?}.in"
	  OUTPUT_FILE="${DAEMON_RUNNER?}.out"
	  END_OF_FILE='_EOF_'
	  /bin/rm ${INPUT_FILE?} || :
	  /bin/rm ${OUTPUT_FILE?} || :
	  echo "java ${JAVA_OPTIONS?}${END_OF_FILE?}" > ${INPUT_FILE?}
	  set +x
	  echo "awaiting daemon" && while [ -f ${INPUT_FILE?} ]; do sleep 0.5; done
	  echo "awaiting result" && while [ ! -f ${OUTPUT_FILE?} ]; do sleep 0.5; done
	  set -x
	  EXIT_STATUS=`cat ${OUTPUT_FILE?} | awk '{gsub(/'"${END_OF_FILE?}"'$/,"")}1' | tr -d "\n"`
	  /bin/rm ${OUTPUT_FILE?}
	  return $EXIT_STATUS
	}
fi

# ==============================================================================================================
# test each browsers
EXIT_STATUS=0
for BROWSER in ${BROWSERS[*]?}; do
  echo "testing ${BROWSER?}"

  # test browser
  JAVA_OPTIONS="-jar ${SELENIUM_SERVER_JAR?} -log ${TARGET_DIR_REMOTE?}/${BROWSER?}.log -htmlSuite *${BROWSER?} ${BIOMART_SERVER?}/ ${TARGET_DIR_REMOTE?}/${HTML_SUITE?} ${TARGET_DIR_REMOTE?}/${BROWSER?}.html"

  if [ $CYGWIN == 0 ]; then
    java ${JAVA_OPTIONS?}
  else
    JAVA_OPTIONS=`echo "${JAVA_OPTIONS?}" | awk '{gsub(/\/cygdrive\/g/,"G:")}1'` # adapt cygwin path to windows path
    windows_daemon ${JAVA_OPTIONS?} # use daemon to execute
  fi
  EXIT_STATUS=$?

  # don't bother going further if one failed
  if [ ${EXIT_STATUS?} != 0 ]; then
    break
  fi

  echo
done
sleep 2

# ==============================================================================================================
# handle display (part 2)
if [ ${CHANGE_DISPLAY?} != 0 ]; then
  kill ${XVFB_PID?}
  set -e
fi

# ==============================================================================================================
# finalization

echo "TIMESTAMP=${TIMESTAMP?}"
echo OK
exit ${EXIT_STATUS?}

