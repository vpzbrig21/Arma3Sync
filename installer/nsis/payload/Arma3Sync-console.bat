@echo off
setlocal
call "%~dp0Arma3Sync.exe" -console %*
exit /b %ERRORLEVEL%
