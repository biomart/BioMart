@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

SET BASE_DIR=%~dp0..
cd %BASE_DIR%

set path=scripts\biomart-start.jar
for %%i in (lib\*jar) do (
	set path=!path!;%%i
)



if "%1"=="" (
    echo Usage: biomart-server.bat start^|stop^|remove^|install
)
if "%1"=="start" (
    if "%ERRORLEVEL%"=="1" (
        echo Stopping Biomart...
        scripts\prunsrv.exe //SS//Biomart
	) else (
		echo Starting Biomart...
		scripts\prunmgr.exe //MR//Biomart
	)
) else if "%1"=="stop" (
    if "%ERRORLEVEL%"=="1" (
        echo Biomart is not running
    ) else (
        scripts\prunsrv.exe //SS//Biomart
		scripts\prunmgr.exe //MQ//Biomart
    )
) else if "%1"=="remove" (
    scripts\prunsrv.exe //DS//Biomart
) else if "%1"=="install" (
    scripts\prunsrv.exe //IS//Biomart --Install=prunsrv.exe --Description="Biomart Service" --Jvm=auto --JvmMx=1500 ++JvmOptions=-Dbiomart.properties=%BASE_DIR%\biomart.properties --Classpath=%path% --Startup=auto --StartMode=jvm --StopMode=jvm --StartClass=org.biomart.start.Main --StartMethod=windowsService --StartParams=start --StartPath=%BASE_DIR% --StopClass=org.biomart.start.Main --StopMethod=windowsService --StopParams=stop --StopPath=%BASE_DIR% --LogPath=logs --LogLevel=Debug --StdOutput=auto --StdError=auto
) else if "%1"=="debug" (
	scripts\prunsrv.exe //TS//Biomart
)