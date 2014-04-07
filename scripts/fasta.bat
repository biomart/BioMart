REM Note: If you get Java "Out of memory" errors, try increasing the numbers
REM in the -Xmx and -Xms parameters in the java command below. For performance
REM sake it is best if they are both the same value.
java -Xmx1024m -Xms1024m -cp ..\build\classes;..\lib\activation.jar;..\lib\dsn.jar;..\lib\imap.jar;..\lib\mailapi.jar;..\lib\pop3.jar;..\lib\smtp.jar;..\lib\martconfigurator.jar;..\lib\log4j-1.2.15.jar;..\lib\ecp1_0beta.jar;..\lib\mysql-connector-java-5.1.7-bin.jar;..\lib\postgresql-8.3-604.jdbc3.jar;..\lib\ensj-util.jar;..\lib\jdom-1.0.jar;..\lib\ojdbc14.jar;..\lib\xerces.jar;..\lib\xalan.jar;..\lib\idw-gpl.jar;..\lib\martServiceTransformation.jar org.biomart.processors.Fasta
 
