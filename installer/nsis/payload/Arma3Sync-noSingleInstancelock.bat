@echo off
setlocal
call "%~dp0Arma3Sync.exe" -unlock %*
exit /b %ERRORLEVEL%
