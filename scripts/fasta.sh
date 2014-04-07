#!/bin/sh

# Note: /bin/sh doesn't work on Alphas (need to use bash thexre) but
# works everywhere else.

# Starts the MartBuilder GUI application.

# Usage:
#
# prompt> bin/martbuilder.sh

TMP_ROOT=`dirname $0`/..
 
TMP_CLASSPATH=${TMP_ROOT}
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/build/classes
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mysql-connector-java-5.1.7-bin.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/log4j-1.2.15.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jython.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ensj-util.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/jdom-1.0.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/biomart-java.jar 
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ojdbc14.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/ecp1_0beta.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/postgresql-8.3-604.jdbc3.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/activation.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/dsn.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/imap.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/mailapi.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/pop3.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/smtp.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/xerces.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/xalan.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/idw-gpl.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${TMP_ROOT}/lib/martServiceTransformation.jar
TMP_CLASSPATH=${TMP_CLASSPATH}:${CLASSPATH}

TMP_JYTHON_LIB=${TMP_ROOT}/lib

echo "Starting MartConfigurator please wait .... " 

#java -ea -cp $TMP_CLASSPATH org.ensembl.mart.builder.MartBuilder $@

# Note: If you get Java "Out of memory" errors, try increasing the numbers
# in the -Xmx and -Xms parameters in the java command below. For performance
# sake it is best if they are both the same value.
java -Xmx1024m -Xms1024m -cp $TMP_CLASSPATH org.biomart.processors.Fasta $@




