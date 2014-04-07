@echo off

Jetty-Service.exe --install jetty-service.conf > nul

tasklist /FI "IMAGENAME eq Jetty-Service.exe" 2>NUL | find /I /N "Jetty-Service.exe">NUL

if "%1"=="" (
    echo Usage: jetty.bat start^|stop^|remove
)
if "%1"=="start" (
    if "%ERRORLEVEL%"=="0" (
        echo Stopping Jetty
        Jetty-Service.exe --stop jetty-service.conf
	)
	Jetty-Service.exe --start jetty-service.conf
) else if "%1"=="stop" (
    if "%ERRORLEVEL%"=="1" (
        echo Jetty not running
    ) else (
        Jetty-Service.exe --stop jetty-service.conf
    )
) else if "%1"=="remove" (
    Jetty-Service.exe --remove jetty-service.conf
)
