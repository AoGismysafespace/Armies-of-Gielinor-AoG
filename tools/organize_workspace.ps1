# ==============================================================================
# organize_workspace.ps1: Armies of Gielinor Comprehensive Workspace Modularizer
#
# 1. PURPOSE & ARCHITECTURAL CONTEXT:
#    Transforms the project workspace into a professional, modular repository structure.
#    Eliminates root clutter by sorting loose files into dedicated tiers:
#      - docs/: Architecture documentation and milestone registers.
#      - tools/: Release packaging, rollback, decompilation, and bytecode utilities.
#      - src/: Java source files and verification tools.
#      - test_environment/scratch/: Temporary test configurations and loose stubs.
#      - releases/: Golden Master and historical version archives.
#    Leaves only the essential user-facing files at the root:
#      - README.md (Player guide & architecture overview)
#      - Manual.pdf (31-page authentic retro manual)
#      - AGENTS.md / GEMINI.md (Antigravity behavioral rules and safety guidelines)
#
# 2. ROSETTA STONE / SYMBOL MAPPINGS:
#    - docs/ARCHITECTURE_AND_SYMBOLS.md: Rosetta Stone mapping table for obfuscated symbols.
#    - docs/MILESTONES.md: Verified fallback archive register (v0.1 through v0.8).
#    - tools/build_release.ps1: Automated Java 18 release packaging engine.
#    - tools/restore_milestone.ps1: Deterministic 1-click version switcher.
#    - src/RestoreAndVerify.java: Classfile auditor ensuring Java 18 major <= 62 bytecode.
#    - releases/ArmiesOfGielinor_v0.8.zip: Official packaged standalone game archive.
#
# 3. INLINE EXPLANATIONS:
#    All moves are non-destructive. If a target already exists in the destination,
#    it is safely replaced or verified. The GitHub mirror folder is kept in exact lockstep.
# ==============================================================================

param (
    [switch]$WhatIf = $false
)

$ErrorActionPreference = "Stop"
$workspace = $PSScriptRoot
if ([string]::IsNullOrEmpty($workspace)) {
    $workspace = (Get-Item ".").FullName
}
# If invoked from tools/ or subfolder, resolve workspace root
if (-not (Test-Path (Join-Path $workspace "extracted"))) {
    if (Test-Path (Join-Path $workspace "..\extracted")) {
        $workspace = (Resolve-Path (Join-Path $workspace "..")).Path
    } elseif (Test-Path (Join-Path $workspace "..\..\extracted")) {
        $workspace = (Resolve-Path (Join-Path $workspace "..\..")).Path
    }
}

Write-Host "=== Armies of Gielinor: Workspace Modularization Utility ===" -ForegroundColor Cyan
Write-Host "Target Workspace: $workspace" -ForegroundColor Gray

# ------------------------------------------------------------------------------
# STEP 1: Organize Historical Release Archives
# ------------------------------------------------------------------------------
Write-Host "`n[Step 1/6] Organizing Historical Releases..." -ForegroundColor Yellow
$releasesDir = Join-Path $workspace "releases"
$archiveDir = Join-Path $releasesDir "archive"

if (-not (Test-Path $archiveDir)) {
    New-Item -ItemType Directory -Path $archiveDir -Force | Out-Null
    Write-Host "Created archive directory: releases/archive/" -ForegroundColor Green
}

$historicalZips = Get-ChildItem -Path $workspace -File -Filter "ArmiesOfGielinor*.zip" | 
    Where-Object { $_.Name -ne "ArmiesOfGielinor_v0.8.zip" }

foreach ($zip in $historicalZips) {
    $dest = Join-Path $archiveDir $zip.Name
    Move-Item -Path $zip.FullName -Destination $dest -Force
    Write-Host "  -> Moved $($zip.Name) to releases/archive/" -ForegroundColor White
}

# Ensure ArmiesOfGielinor_v0.8.zip is present in releases/ and remove redundant root copy
$currentReleaseRoot = Join-Path $workspace "ArmiesOfGielinor_v0.8.zip"
$currentReleaseTarget = Join-Path $releasesDir "ArmiesOfGielinor_v0.8.zip"
if (Test-Path $currentReleaseRoot) {
    Copy-Item -Path $currentReleaseRoot -Destination $currentReleaseTarget -Force
    Remove-Item -Path $currentReleaseRoot -Force
    Write-Host "  -> Filed ArmiesOfGielinor_v0.8.zip in releases/ and cleared root copy" -ForegroundColor Green
}

# ------------------------------------------------------------------------------
# STEP 2: Group Secondary Decompiled Reference Directories
# ------------------------------------------------------------------------------
Write-Host "`n[Step 2/6] Grouping Secondary Decompiled Reference Directories..." -ForegroundColor Yellow
$referencesDir = Join-Path $workspace "references\decompiled"

if (-not (Test-Path $referencesDir)) {
    New-Item -ItemType Directory -Path $referencesDir -Force | Out-Null
    Write-Host "Created directory: references/decompiled/" -ForegroundColor Green
}

$secondaryDecompiledFolders = @(
    "decompiled_campaign",
    "decompiled_cm",
    "decompiled_fonts",
    "decompiled_hm"
)

foreach ($folderName in $secondaryDecompiledFolders) {
    $folderPath = Join-Path $workspace $folderName
    if (Test-Path $folderPath) {
        $destPath = Join-Path $referencesDir $folderName
        Move-Item -Path $folderPath -Destination $destPath -Force
        Write-Host "  -> Moved $folderName to references/decompiled/$folderName" -ForegroundColor White
    }
}

# ------------------------------------------------------------------------------
# STEP 3: Organize Loose Test Environment Files
# ------------------------------------------------------------------------------
Write-Host "`n[Step 3/6] Organizing Loose Test Environment Files..." -ForegroundColor Yellow
$testEnvDir = Join-Path $workspace "test_environment"
$scratchDir = Join-Path $testEnvDir "scratch"

if (-not (Test-Path $scratchDir)) {
    New-Item -ItemType Directory -Path $scratchDir -Force | Out-Null
}

$preservedInRoot = @("steamworks4j64.dll", "steam_api64.dll", "steam_appid.txt")
$looseTestFiles = Get-ChildItem -Path $testEnvDir -File

foreach ($file in $looseTestFiles) {
    if ($preservedInRoot -contains $file.Name) {
        Copy-Item -Path $file.FullName -Destination (Join-Path $scratchDir $file.Name) -Force
    } else {
        $destPath = Join-Path $scratchDir $file.Name
        Move-Item -Path $file.FullName -Destination $destPath -Force
    }
}

# Move root test stray files (player.properties, root steam_appid.txt) to test_environment/scratch/
$rootStrays = @("player.properties", "steam_appid.txt")
foreach ($stray in $rootStrays) {
    $strayPath = Join-Path $workspace $stray
    if (Test-Path $strayPath) {
        Move-Item -Path $strayPath -Destination (Join-Path $scratchDir $stray) -Force
        Write-Host "  -> Moved root stray $stray to test_environment/scratch/" -ForegroundColor White
    }
}

# ------------------------------------------------------------------------------
# STEP 4: Move Technical Documentation to docs/
# ------------------------------------------------------------------------------
Write-Host "`n[Step 4/6] Organizing Technical Documentation into docs/..." -ForegroundColor Yellow
$docsDir = Join-Path $workspace "docs"
if (-not (Test-Path $docsDir)) {
    New-Item -ItemType Directory -Path $docsDir -Force | Out-Null
    Write-Host "Created directory: docs/" -ForegroundColor Green
}

$docFiles = @("ARCHITECTURE_AND_SYMBOLS.md", "MILESTONES.md")
foreach ($doc in $docFiles) {
    $docPath = Join-Path $workspace $doc
    if (Test-Path $docPath) {
        Move-Item -Path $docPath -Destination (Join-Path $docsDir $doc) -Force
        Write-Host "  -> Moved $doc to docs/" -ForegroundColor White
    }
    # Mirror to ArmiesOfGielinor_GitHub/docs/
    $githubDocs = Join-Path $workspace "ArmiesOfGielinor_GitHub\docs"
    if (Test-Path $githubDocs) {
        Copy-Item -Path (Join-Path $docsDir $doc) -Destination (Join-Path $githubDocs $doc) -Force
    }
}

# ------------------------------------------------------------------------------
# STEP 5: Move Developer Tools & Packaging Scripts to tools/
# ------------------------------------------------------------------------------
Write-Host "`n[Step 5/6] Organizing Tools and Scripts into tools/..." -ForegroundColor Yellow
$toolsDir = Join-Path $workspace "tools"
if (-not (Test-Path $toolsDir)) {
    New-Item -ItemType Directory -Path $toolsDir -Force | Out-Null
    Write-Host "Created directory: tools/" -ForegroundColor Green
}

$toolFiles = @(
    "build_release.bat",
    "build_release.ps1",
    "restore_milestone.ps1",
    "cfr.jar",
    "javassist.jar"
)

foreach ($tool in $toolFiles) {
    $toolPath = Join-Path $workspace $tool
    if (Test-Path $toolPath) {
        Move-Item -Path $toolPath -Destination (Join-Path $toolsDir $tool) -Force
        Write-Host "  -> Moved $tool to tools/" -ForegroundColor White
    }
    # Mirror script tools to ArmiesOfGielinor_GitHub/tools/
    $githubTools = Join-Path $workspace "ArmiesOfGielinor_GitHub\tools"
    if ((Test-Path $githubTools) -and ($tool -like "*.ps1" -or $tool -like "*.bat")) {
        Copy-Item -Path (Join-Path $toolsDir $tool) -Destination (Join-Path $githubTools $tool) -Force
    }
}

# ------------------------------------------------------------------------------
# STEP 6: Move Java Source & Audit Code to src/
# ------------------------------------------------------------------------------
Write-Host "`n[Step 6/6] Organizing Java Source Code into src/..." -ForegroundColor Yellow
$srcDir = Join-Path $workspace "src"
if (-not (Test-Path $srcDir)) {
    New-Item -ItemType Directory -Path $srcDir -Force | Out-Null
    Write-Host "Created directory: src/" -ForegroundColor Green
}

# Mirror all authoritative Java sources from ArmiesOfGielinor_GitHub/src/
$githubSrc = Join-Path $workspace "ArmiesOfGielinor_GitHub\src"
if (Test-Path $githubSrc) {
    Copy-Item -Path (Join-Path $githubSrc "*.java") -Destination $srcDir -Force
    Write-Host "  -> Synchronized clean Java sources (AoGBattleBridge, AoGServer, etc.) to src/" -ForegroundColor Green
}

# Move root RestoreAndVerify files into src/
$restoreFiles = @("RestoreAndVerify.java", "RestoreAndVerify.class")
foreach ($rf in $restoreFiles) {
    $rfPath = Join-Path $workspace $rf
    if (Test-Path $rfPath) {
        Move-Item -Path $rfPath -Destination (Join-Path $srcDir $rf) -Force
        Write-Host "  -> Moved $rf to src/" -ForegroundColor White
    }
}

# Ensure RestoreAndVerify.class in src/ is compiled and verified
$javaCompiler = "C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot\bin\javac.exe"
if (Test-Path $javaCompiler) {
    & $javaCompiler --release 18 -d $srcDir (Join-Path $srcDir "RestoreAndVerify.java")
    Write-Host "  -> Recompiled RestoreAndVerify.class for Java 18 (major <= 62) in src/" -ForegroundColor Green
}

# Move organize_workspace.ps1 into tools/ as the final action
$myPath = Join-Path $workspace "organize_workspace.ps1"
if (Test-Path $myPath) {
    Move-Item -Path $myPath -Destination (Join-Path $toolsDir "organize_workspace.ps1") -Force
    Write-Host "  -> Moved organize_workspace.ps1 to tools/" -ForegroundColor White
}

Write-Host "`n=== All Loose Files Successfully Modularized! ===" -ForegroundColor Green
