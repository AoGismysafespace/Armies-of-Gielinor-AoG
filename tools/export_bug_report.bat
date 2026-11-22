@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0export_bug_report.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo An error occurred while generating the bug report.
    pause
)
