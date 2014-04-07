@echo off
setlocal

SET BASE_DIR=%~dp0..
SET CLASSPATH=.

subst z: %BASE_DIR%
for %%j in (z:\lib\*.jar) do (call :append_classpath "%%j")
goto :run

:append_classpath
set CLASSPATH=%CLASSPATH%;%1
goto :eof

:run
cd %BASE_DIR%
REM java -Xmx512M -cp %CLASSPATH% -Dcom.sun.management.jmxremote voldemort.server.VoldemortServer %1
java -Xmx2048m -Xms1024m -cp %CLASSPATH% org.biomart.configurator.test.MartConfigurator %*

endlocal