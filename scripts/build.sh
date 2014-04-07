#!/bin/sh

# Note: /bin/sh doesn't work on Alphas (need to use bash thexre) but
# works everywhere else.

# Starts the MartConfigurator GUI application.

# Usage:
#
# prompt> martconfigurator.sh

TMP_ROOT=`dirname $0`/..

cd $TMP_ROOT

for i in `ls lib/*.jar`
do
  TMP_CLASSPATH=${TMP_CLASSPATH}:${i}
done
 
echo "Starting testing please wait .... " 

# Note: If you get Java "Out of memory" errors, try increasing the numbers
# in the -Xmx and -Xms parameters in the java command below. For performance
# sake it is best if they are both the same value.

#KEY_FILES_DIR=../biomart_metadata
#[ -d "${KEY_FILES_DIR?}" ] || { echo "must create directory ${KEY_FILES_DIR?} with appropriate key files (see DCCTEST-1677)"; exit 1; }  
#cp ${KEY_FILES_DIR?}/.icgc4_reference ${KEY_FILES_DIR?}/.restapi ${KEY_FILES_DIR?}/.sequence2 ${KEY_FILES_DIR?}/.test_source_link ./testdata/
#if [ $? != 0 ]; then
#  ls -la
#  echo "missing a keyfile?"
#  exit 2
#fi

export ANT_OPTS="$ANT_OPTS -Xmx1g -Xms1g"
QUICK_TEST=${QUICK_TEST:=false}
ant -Dtest.subset=${QUICK_TEST?} -buildfile build_test.xml $@

cd -
