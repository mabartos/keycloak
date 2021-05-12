set NOPAUSE=true

start "JBoss Server" /b cmd /c %JBOSS_HOME%\bin\standalone.bat

set ERROR=0
set TIMEOUT=10
set I=0

ping 127.0.0.1 -n 3 > nul


:wait_for_jboss
call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":read-attribute(name=server-state)" | findstr "running"
if %ERRORLEVEL% equ 0 goto configure_web_connector
ping 127.0.0.1 -n 1 > nul
set /a I=%I%+1
if %I% gtr %TIMEOUT% (
    set ERROR=1
    goto shutdown_jboss
)
goto wait_for_jboss


:configure_web_connector
call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%CLI_PATH%\configure-web-connector.cli"
set ERROR=%ERRORLEVEL%
echo Error code: "%ERROR%"
if %ERROR% neq 0 (
    goto shutdown_jboss
)

:shutdown_jboss
echo Shutting down with error code: "%ERROR%"
call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":shutdown"
exit /b %ERROR%
