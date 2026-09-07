@echo off
setlocal
call "%~dp0Arma3Sync.exe" %*
exit /b %ERRORLEVEL%
