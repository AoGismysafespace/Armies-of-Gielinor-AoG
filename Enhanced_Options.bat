@echo off
setlocal enabledelayedexpansion
title Armies of Gielinor - Enhanced Options Selector

:: ====================================================================================================
:: Armies of Gielinor - Enhanced Options Selector (English)
:: Interactive configuration tool for Skirmish AI pantheons and heraldic crests.
:: ====================================================================================================

set "CONFIG_FILE=enhanced_options.txt"
set "USER_CONFIG=%USERPROFILE%\.armiesofgielinor\enhanced_options.txt"

:MENU
cls
:: Read current values from enhanced_options.txt
set "AI_GODS=random"
set "AI_COATS=random"

if exist "%CONFIG_FILE%" (
    for /f "usebackq tokens=1,2 delims==" %%A in ("%CONFIG_FILE%") do (
        set "KEY=%%A"
        set "VAL=%%B"
        for /f "tokens=* delims= " %%K in ("!KEY!") do set "KEY=%%K"
        for /f "tokens=* delims= " %%V in ("!VAL!") do set "VAL=%%V"
        if /i "!KEY!"=="ai_gods_mode" set "AI_GODS=!VAL!"
        if /i "!KEY!"=="ai_coats_mode" set "AI_COATS=!VAL!"
    )
)

echo ==========================================================================
echo               Armies of Gielinor - Enhanced Options
echo ==========================================================================
echo.
echo  Current Skirmish AI Settings:
echo.
if /i "%AI_GODS%"=="mirror" (
    echo    1. AI Gods Selection  : [ MIRRORED ] (Original 2009 - copies player)
) else (
    echo    1. AI Gods Selection  : [ RANDOM   ] (Diverse armies across all 6 gods)
)

if /i "%AI_COATS%"=="mirror" (
    echo    2. AI Heraldry / Coat : [ MIRRORED ] (Original 2009 - copies player crest)
) else (
    echo    2. AI Heraldry / Coat : [ RANDOM   ] (Unique crest and flags per bot)
)

echo.
echo --------------------------------------------------------------------------
echo  Available Actions:
echo.
echo    [1] Toggle AI Gods Selection      (Random ^<--^> Mirrored)
echo    [2] Toggle AI Heraldry / Crests   (Random ^<--^> Mirrored)
echo    [3] Enable ALL Enhanced Features  (Set Both to RANDOM - Recommended)
echo    [4] Restore Original 2009 Rules   (Set Both to MIRRORED)
echo    [0] Exit
echo --------------------------------------------------------------------------
echo.

set /p "CHOICE=Enter your choice [1, 2, 3, 4, or 0]: "

if "%CHOICE%"=="1" (
    if /i "%AI_GODS%"=="mirror" (
        set "NEW_GODS=random"
    ) else (
        set "NEW_GODS=mirror"
    )
    call :SAVE_OPTIONS "!NEW_GODS!" "%AI_COATS%"
    goto :MENU
)

if "%CHOICE%"=="2" (
    if /i "%AI_COATS%"=="mirror" (
        set "NEW_COATS=random"
    ) else (
        set "NEW_COATS=mirror"
    )
    call :SAVE_OPTIONS "%AI_GODS%" "!NEW_COATS!"
    goto :MENU
)

if "%CHOICE%"=="3" (
    call :SAVE_OPTIONS "random" "random"
    goto :MENU
)

if "%CHOICE%"=="4" (
    call :SAVE_OPTIONS "mirror" "mirror"
    goto :MENU
)

if "%CHOICE%"=="0" goto :QUIT
goto :MENU

:: ============================================================================
:: Helper Routine to Write Options
:: ============================================================================
:SAVE_OPTIONS
set "G_VAL=%~1"
set "C_VAL=%~2"

powershell -NoProfile -Command ^
    "$paths = @('enhanced_options.txt', [System.IO.Path]::Combine($env:USERPROFILE, '.armiesofgielinor', 'enhanced_options.txt'));" ^
    "$content = @(" ^
    "  '# =============================================================================='," ^
    "  '# Armies of Gielinor - Enhanced Options Configuration', " ^
    "  '# =============================================================================='," ^
    "  '#', " ^
    "  '# 1. AI Pantheon Selection in Skirmish (random | mirror)'," ^
    "  'ai_gods_mode=' + '%G_VAL%', " ^
    "  '#', " ^
    "  '# 2. AI Coat of Arms / Heraldry in Skirmish (random | mirror)'," ^
    "  'ai_coats_mode=' + '%C_VAL%'" ^
    ");" ^
    "foreach ($p in $paths) {" ^
    "  $dir = [System.IO.Path]::GetDirectoryName($p);" ^
    "  if ($dir -and -not (Test-Path $dir)) { [System.IO.Directory]::CreateDirectory($dir) | Out-Null; }" ^
    "  [System.IO.File]::WriteAllLines($p, $content);" ^
    "}"
exit /b 0

:QUIT
echo.
echo  Configuration saved successfully to enhanced_options.txt.
echo  Exiting...
timeout /t 1 >nul
exit /b 0
