#!/bin/sh

JAVA_BIN=`which java`
BIOMART_PATH=/Users/jhsu/code/biomart-java/trunk
REGISTRY_NAME=IcgcPortalUpdate2j

CLASSPATH="$BIOMART_PATH/lib/activation.jar:$BIOMART_PATH/lib/ant-launcher.jar:\
$BIOMART_PATH/lib/ant.jar:$BIOMART_PATH/lib/asm-3.1.jar:$BIOMART_PATH/lib/commons-beanutils-1.7.jar:$BIOMART_PATH/lib/commons-codec-1.3.jar:\
$BIOMART_PATH/lib/commons-collections.jar:$BIOMART_PATH/lib/commons-httpclient-3.0.1.jar:$BIOMART_PATH/lib/commons-lang.jar:\
$BIOMART_PATH/lib/commons-logging-1.1.1.jar:$BIOMART_PATH/lib/dna-common-0.6.jar:$BIOMART_PATH/lib/dsn.jar:$BIOMART_PATH/lib/ecp1_0beta.jar:\
$BIOMART_PATH/lib/ensj-util.jar:$BIOMART_PATH/lib/ensj.jar:$BIOMART_PATH/lib/ezmorph.jar:$BIOMART_PATH/lib/idw-gpl.jar:$BIOMART_PATH/lib/imap.jar:\
$BIOMART_PATH/lib/java-getopt-1.0.9.jar:$BIOMART_PATH/lib/jdbc2_0-stdext.jar:$BIOMART_PATH/lib/jdom-1.0.jar:$BIOMART_PATH/lib/jersey-core-1.1.5.jar:\
$BIOMART_PATH/lib/jersey-server-1.1.5.jar:$BIOMART_PATH/lib/jline.jar:$BIOMART_PATH/lib/json-lib-2.3-jdk15.jar:$BIOMART_PATH/lib/json.jar:\
$BIOMART_PATH/lib/jsr311-api-1.1.1.jar:$BIOMART_PATH/lib/junit-4.5.jar:$BIOMART_PATH/lib/junit.jar:$BIOMART_PATH/lib/jython.jar:\
$BIOMART_PATH/lib/libreadline-java.jar:$BIOMART_PATH/lib/lingpipe-3.8.2.jar:$BIOMART_PATH/lib/log4j-1.2.15.jar:$BIOMART_PATH/lib/mailapi.jar:\
$BIOMART_PATH/lib/mysql-connector-java-5.1.7-bin.jar:$BIOMART_PATH/lib/nekohtml-1.9.7.jar:$BIOMART_PATH/lib/opencsv-2.1.jar:\
$BIOMART_PATH/lib/openid4java-0.9.5.jar:$BIOMART_PATH/lib/optional.jar:$BIOMART_PATH/lib/p6spy.jar:$BIOMART_PATH/lib/pop3.jar:\
$BIOMART_PATH/lib/postgresql-8.3-604.jdbc3.jar:$BIOMART_PATH/lib/servlet-api.jar:$BIOMART_PATH/lib/smtp.jar:$BIOMART_PATH/lib/start.jar:\
$BIOMART_PATH/lib/xalan.jar:$BIOMART_PATH/lib/xercesImpl.jar:$BIOMART_PATH/lib/xml-apis.jar:$BIOMART_PATH/lib/core-3.1.1.jar:\
$BIOMART_PATH/lib/jsp-2.1-glassfish-9.1.1.B60.25.p2.jar:$BIOMART_PATH/lib/jsp-api-2.1-glassfish-9.1.1.B60.25.p2.jar:\
$BIOMART_PATH/lib/commons-fileupload-1.2.1.jar:$BIOMART_PATH/lib/commons-io-1.4.jar:$BIOMART_PATH/lib/jstl-api-1.2.jar:\
$BIOMART_PATH/lib/jstl-impl-1.2.jar:$BIOMART_PATH/lib/ojdbc5.jar:$BIOMART_PATH/lib/jetty-6.1.23.jar:$BIOMART_PATH/lib/jetty-util-6.1.23.jar:\
$BIOMART_PATH/lib/jsp-2.1-jetty-6.1.23.jar"

if [ -w /dev/console ]
then
  CONSOLE=/dev/console
else
  CONSOLE=/dev/tty
fi

echo Building java sources
ant compile

echo Building SoakTest class
mkdir -p $BIOMART_PATH/build/classes
javac -classpath $CLASSPATH -d $BIOMART_PATH/build/classes $BIOMART_PATH/test/org/biomart/soak/SoakTest.java


echo Starting tests

RUN_CMD="$JAVA_BIN -Dfile.encoding=UTF-8 -Xmx2048m -DWebroot=$BIOMART_PATH/web/apps -Dbiomart.registry.file=$BIOMART_PATH/conf/xml/$REGISTRY_NAME.xml \
-Dbiomart.registry.key.file=$BIOMART_PATH/conf/xml/.$REGISTRY_NAME"

if [ "$1" != "" ]; then
	RUN_CMD="$RUN_CMD -Dtest.soak.url=$1"
fi

RUN_CMD="$RUN_CMD -classpath $CLASSPATH:$BIOMART_PATH/src:$BIOMART_PATH/build/classes:$BIOMART_PATH/test org.biomart.soak.SoakTest"

exec $RUN_CMD > $CONSOLE

