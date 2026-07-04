@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul 2>nul

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "PS_SCRIPT=%SCRIPT_DIR%check-dependencies.ps1"
set "NO_PAUSE="

for %%A in (%*) do (
  if /I "%%~A"=="--no-pause" set "NO_PAUSE=1"
)

if not exist "%PS_SCRIPT%" (
  echo [ERROR] PowerShell helper not found: %PS_SCRIPT%
  exit /b 2
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%" -ProjectRoot "%PROJECT_ROOT%" %*
set "CHECK_DEPS_EXIT=%ERRORLEVEL%"

echo.
if defined NO_PAUSE goto :end
echo Press any key to exit...
pause >nul

:end
exit /b %CHECK_DEPS_EXIT%
