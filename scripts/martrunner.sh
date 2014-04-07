#!/bin/sh

# Note: /bin/sh doesn't work on Alphas (need to use bash thexre) but
# works everywhere else.

# Starts the MartRunner CLI application.

# Usage:
#
# prompt> bin/martrunner.sh <PORT>

TMP_ROOT=`dirname $0`/..

cd $TMP_ROOT

for i in `ls lib/*.jar`
do
  TMP_CLASSPATH=${TMP_CLASSPATH}:${i}
done

echo "Starting MartRunner please wait .... " 

# Note: If you get Java "Out of memory" errors, try increasing the numbers
# in the -Xmx and -Xms parameters in the java command below. For performance
# sake it is best if they are both the same value.
java -Xmx256m -Xms256m -cp $TMP_CLASSPATH org.biomart.runner.view.cli.MartRunner $@

cd -




