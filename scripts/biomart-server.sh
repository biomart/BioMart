#!/usr/bin/env bash 

DEFAULT_MAX_HEAP="6144m"

usage() {
    echo "Usage: $0 {start|stop|run|restart|check|supervise} [ JAVA_OPTIONS ]"
    exit 1
}


(( $# )) || usage


##################################################
# Get the action & configs
##################################################

TMPJ=/tmp/j$$

# Get the action & configs
ACTION=$1 ; shift
EXTRA_ARGS=$*

BIOMART_INSTALL_TRACE_FILE="scripts/biomart-start.jar"

# Try to determine BIOMART_HOME if not set
if [ -z "$BIOMART_HOME" ] ; then
  BIOMART_HOME_1=$(dirname $0)
  BIOMART_HOME_1="${BIOMART_HOME_1}/.."
  if [ -f "${BIOMART_HOME_1}/${BIOMART_INSTALL_TRACE_FILE}" ] ; then
     BIOMART_HOME=${BIOMART_HOME_1}
  fi
fi

# if no BIOMART_HOME, search likely locations.
if [ "$BIOMART_HOME" = "" ] ; then
  STANDARD_LOCATIONS="           \
        .                        \
        ./dist                   \
        ../dist                  \
        "

  for L in $STANDARD_LOCATIONS
  do
     if [ -d $L ] && [ -f "$L/${BIOMART_INSTALL_TRACE_FILE}" ] ;
     then
        BIOMART_HOME="$L"
        break
     fi
  done
fi


if [ -z "$BIOMART_HOME" ] ; then
    echo "** ERROR: BIOMART_HOME not set, you need to set it or install in a standard location"
    exit 1
fi

for i in `ls $BIOMART_HOME/lib/*.jar`
do
  TMP_CLASSPATH=${TMP_CLASSPATH}:${i}
done

# Check that biomart is where we think it is
if [ ! -r $BIOMART_HOME/$BIOMART_INSTALL_TRACE_FILE ] ; then
   echo "** ERROR: Oops! BioMart doesn't appear to be installed in $BIOMART_HOME"
   echo "** ERROR:  $BIOMART_HOME/$BIOMART_INSTALL_TRACE_FILE is not readable!"
   exit 1
fi


# Location for the pid file
if [ -z "$BIOMART_RUN" ] ; then
  BIOMART_RUN=$BIOMART_HOME
fi

# Find a PID for the pid file
if [ -z "$BIOMART_PID" ] ; then
  BIOMART_PID="$BIOMART_RUN/biomart.pid"
fi

# Find a location for the biomart console
if [ -z "$BIOMART_CONSOLE" ] ; then
  if [ -w /dev/console ] ; then
    BIOMART_CONSOLE=/dev/console
  else
    if [ -w /dev/console ] ; then
      BIOMART_CONSOLE=/dev/tty
    else
      BIOMART_CONSOLE=/dev/null
    fi  
  fi
fi



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
  IFS=: read JVERSION JAVA < <(
    for N in java jdk jre
    do
      for L in "${JAVA_LOCATIONS[@]}"
      do
        [[ -d "$L" ]] || continue 
        find "$L" -name "$N" ! -type d ! -path '*threads*' | while read JAVA; do
          [[ -x "$JAVA" ]] || continue

          JAVA_VERSION=$("$JAVA" -version 2>&1) || continue
          IFS='"_' read _ JAVA_VERSION _ <<< "$JAVA_VERSION"

          [[ "$JAVA_VERSION" ]] || continue
          expr "$JAVA_VERSION" '<' '1.6' >/dev/null && continue
        done
      done
    done | sort)

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

JAVA_VERSION=$("$JAVA" -version 2>&1) || continue
IFS='"_' read _ JAVA_VERSION _ <<< "$JAVA_VERSION"

if [ "$JAVA_VERSION" '<' '1.6' ] ; then
    echo "Java version is too old. (1.6.x required but $JAVA_VERSION was found)"
    exit 1;
fi

##################################################
# Determine path of biomart.properties file
##################################################

if [ "$BIOMART_PROPERTIES" ] ; then
    if [ -e "$BIOMART_PROPERTIES" ] ; then
        JAVA_OPTIONS+=("-Dbiomart.properties=$BIOMART_PROPERTIES")
    else
        echo "Cannot find biomart.properties file: $BIOMART_PROPERTIES"
        exit 1
    fi
else
    BIOMART_PROPERTIES_EXIST=`echo $EXTRA_ARGS | grep "\-Dbiomart.properties"`
    if [ "$BIOMART_PROPERTIES_EXIST" = "" ]; then
        JAVA_OPTIONS="$JAVA_OPTIONS -Dbiomart.properties=$BIOMART_HOME/biomart.properties"
    fi
fi

##################################################
# Set JVM max heap size
##################################################

MAX_HEAP_EXISTS=`echo $EXTRA_ARGS | grep "\-Xmx"`

if [ "$MAX_HEAP_EXISTS" = "" ]; then
    JAVA_OPTIONS="$JAVA_OPTIONS -Xmx$DEFAULT_MAX_HEAP"
fi

##################################################
# Run command for BioMart server
##################################################
RUN_CMD="$JAVA -cp \"$BIOMART_HOME/$BIOMART_INSTALL_TRACE_FILE$TMP_CLASSPATH\" $JAVA_OPTIONS -Dbiomart.dir=$BIOMART_HOME $EXTRA_ARGS org.biomart.start.Main"

# Run BioMart!
case "$ACTION" in
  start)
        echo "Starting BioMart: "

        if [ -f $BIOMART_PID ] ; then
          echo "Already Running!"
          exit 1
        fi

        echo "STARTED BioMart `date`" >> $BIOMART_CONSOLE

        nohup sh -c "exec $RUN_CMD >>$BIOMART_CONSOLE" &
        echo $! > $BIOMART_PID
        echo "BioMart running pid="`cat $BIOMART_PID`

        ;;

  stop)
        PID=`cat $BIOMART_PID 2>/dev/null`
        echo "Shutting down BioMart: $PID"
        kill $PID 2>/dev/null
        sleep 2
        kill -9 $PID 2>/dev/null
        rm -f $BIOMART_PID
        echo "STOPPED `date`" >>$BIOMART_CONSOLE
        ;;

  restart)
        $0 stop $*
        sleep 5
        $0 start $*
        ;;

  supervise)
       #
       # Under control of daemontools supervise monitor which
       # handles restarts and shutdowns via the svc program.
       #
         exec $RUN_CMD
         ;;

  run|demo)
        echo "Running BioMart: "

        if [ -f $BIOMART_PID ] ; then
            echo "Already Running!"
            exit 1
        fi

        exec $RUN_CMD
        ;;

  check|status)
        echo "Checking arguments to BioMart: "
        echo "BIOMART_HOME     =  $BIOMART_HOME"
        echo "BIOMART_RUN      =  $BIOMART_RUN"
        echo "BIOMART_PID      =  $BIOMART_PID"
        echo "BIOMART_CONSOLE  =  $BIOMART_CONSOLE"
        echo "BIOMART_PORT     =  $BIOMART_PORT"
        echo "JAVA_OPTIONS   =  $JAVA_OPTIONS"
        echo "JAVA           =  $JAVA"
        echo "CLASSPATH      =  $CLASSPATH"
        echo "RUN_CMD        =  $RUN_CMD"
        echo

        if [ -f $BIOMART_RUN/biomart.pid ] ; then
            echo "BioMart running pid="`cat $BIOMART_RUN/biomart.pid`
            exit 0
        fi
        exit 1
        ;;

  cron)
        PID_FILE=$BIOMART_RUN/biomart.pid
        if [ -f $PID_FILE ] ; then
          PID=$(cat $PID_FILE)
          NAME=$(ps -o comm= -p $PID)
          if [ "java" = "$NAME" ] ; then
                echo `date` "BioMart running"
                exit 0
          fi
        fi
        echo `date` "BioMart is not working now, trying to start server"
        $0 start $*
        exit 1
        ;;



*)
        usage
        ;;
esac

exit 0

