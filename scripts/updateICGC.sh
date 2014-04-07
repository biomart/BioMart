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

for i in `ls scripts/*.jar`
do
  TMP_CLASSPATH=${TMP_CLASSPATH}:${i}
done
 
echo "Starting icgc.update please wait .... " 

# Note: If you get Java "Out of memory" errors, try increasing the numbers
# in the -Xmx and -Xms parameters in the java command below. For performance
# sake it is best if they are both the same value.
##################################################
# Check for JAVA_HOME
##################################################
if [[ ! "$JAVA_HOME" ]]
then
  # If a java runtime is not defined, search the following
  # directories for a JVM and sort by version. Use the highest
  # version number.

  # Java search path
  JAVA_LOCATIONS=(
      "/usr/java"
      "/usr/local/java"
      "/usr/local/jdk"
      "/usr/local/jre"
      "/usr/lib/jvm"
      "/opt/java"
      "/opt/jdk"
      "/opt/jre"
      )

  JAVA_HOME=${JAVA%/*}
  while [[ "$JAVA_HOME" && ! -f "$JAVA_HOME/lib/tools.jar" ]]; do
    JAVA_HOME=${JAVA_HOME%/*}
  done

  (( DEBUG )) && echo "Found java '$JAVA' at '$JAVA_HOME'"
fi


##################################################
# Determine which JVM of version >1.6
# Try to use JAVA_HOME
##################################################
if [[ -z "$JAVA" && "$JAVA_HOME" ]]
then
  if [[ "$JAVACMD" ]]
  then
    JAVA="$JAVACMD" 
  else
    [[ -x "$JAVA_HOME/bin/jre" && ! -d "$JAVA_HOME/bin/jre" ]] && JAVA=$JAVA_HOME/bin/jre
    [[ -x "$JAVA_HOME/bin/java" && ! -d "$JAVA_HOME/bin/java" ]] && JAVA=$JAVA_HOME/bin/java
  fi
fi

if [[ ! "$JAVA" ]]
then
  JAVA=`which java`
fi

if [[ ! "$JAVA" ]]
then
  echo "Cannot find a JRE or JDK. Please set JAVA_HOME" 2>&2
  exit 1
fi


$JAVA -Xmx2048m -Xms1024m -cp $TMP_CLASSPATH org.biomart.configurator.test.UpdateCLI $@

cd -


