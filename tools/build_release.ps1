# ==============================================================================
# build_release.ps1: Automated Packager for Armies of Gielinor Release ZIP
# Milestone: v1.0 — Standalone Golden Master Release
#
# 1. ARCHITECTURAL CONTEXT & RUNTIME SPECIFICATIONS:
#    The game distribution embeds an OpenJDK JRE in extracted/runtime/ (JAVA_VERSION="18.0.2.1").
#    All patched bytecode within ArmiesOfGielinor.jar must strictly conform to class file major
#    version <= 62.0 (Java 18). Any class compiled with host JDK 21 (major=65) without --release 18
#    will immediately cause a fatal UnsupportedClassVersionError at runtime.
#
# 2. KEY COMPONENTS & SYMBOL MAPPING:
#    - ArmiesOfGielinor.jar: Patched game JAR containing StandaloneRunner, AoGServer, BattleEngine,
#      AoGBattleBridge, BugReporter, and AntiIdleWatchdog (zero original 2009 Jagex '.class' files modified).
#    - AntiIdleWatchdog: Background daemon thread resetting engine idle counters (av.a, fc.b)
#      before the 20-minute Jagex inactivity disconnect threshold is reached.
#    - AoGServer: Multiplayer host server handling TCP lobbies, packets, and turn coordination.
#    - AoGBattleBridge: Authoritative battle simulation bridge (implements BattleEngine).
#    - Enhanced_Options.bat: Unified English interactive launcher script for Skirmish AI pantheons and coats.
#    - Manual.pdf: Illustrated 31-page retro-style manual with the complete 75-unit Steckbrief compendium.
#    - RestoreAndVerify: Automated audit & restoration tool ensuring 0 bytecode version violations exist.
#
# 3. HISTORICAL ARCHIVE PRESERVATION POLICY:
#    Prior release ZIPs (v1.0.1 .. v1.0.24) are intentionally preserved untouched as version history.
#    This script builds v1.0.25 (plus its dedicated milestone backup) and updates Aktuell/
#    without overwriting historical versioned ZIPs.
# ==============================================================================

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrEmpty($PSScriptRoot)) {
    $workspace = (Get-Item ".").FullName
} else {
    $workspace = $PSScriptRoot
}
# If invoked from tools/ or ArmiesOfGielinor_GitHub/tools/, resolve parent workspace root
if (-not (Test-Path (Join-Path $workspace "extracted"))) {
    if (Test-Path (Join-Path $workspace "..\extracted")) {
        $workspace = (Resolve-Path (Join-Path $workspace "..")).Path
    } elseif (Test-Path (Join-Path $workspace "..\..\extracted")) {
        $workspace = (Resolve-Path (Join-Path $workspace "..\..")).Path
    }
}

$stagingRoot = Join-Path $workspace "release_staging"
$stagingDir = Join-Path $stagingRoot "ArmiesOfGielinor"
$extractedDir = Join-Path $workspace "extracted"

# Primary Versioned Release (v1.0) — historical v1.0.1..v1.0.28 ZIPs are preserved in releases/archive/
$releaseVersion = "v1.0"
$releasesDir = Join-Path $workspace "releases"
$archiveDir = Join-Path $releasesDir "archive"
$releaseZip = Join-Path $releasesDir "ArmiesOfGielinor_${releaseVersion}.zip"
$githubReleaseDir = Join-Path $workspace "ArmiesOfGielinor_GitHub\release"
$backupDir = Join-Path $workspace "backups"

Write-Host "=== Building Clean Armies of Gielinor Release Package ($releaseVersion) ==="

# 0. Bytecode Compatibility Audit & Restoration
Write-Host "Verifying bytecode compatibility (target: Java 18, major <= 62)..."
$javaExe = $null
$candidateJavas = @(
    "C:\Program Files\Java\jdk-26.0.2\bin\java.exe",
    "C:\Program Files\Java\jdk-19\bin\java.exe",
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot\bin\java.exe"
)
foreach ($c in $candidateJavas) {
    if (Test-Path $c) { $javaExe = $c; break }
}
if (-not $javaExe) {
    $javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
}

# Locate RestoreAndVerify class across modular source/tools paths
$restoreClassDir = $null
if (Test-Path (Join-Path $workspace "src\RestoreAndVerify.class")) {
    $restoreClassDir = Join-Path $workspace "src"
} elseif (Test-Path (Join-Path $workspace "RestoreAndVerify.class")) {
    $restoreClassDir = $workspace
} elseif (Test-Path (Join-Path $workspace "tools\RestoreAndVerify.class")) {
    $restoreClassDir = Join-Path $workspace "tools"
}

if ($restoreClassDir) {
    & $javaExe -cp $restoreClassDir RestoreAndVerify $workspace
    if ($LASTEXITCODE -ne 0) {
        throw "Bytecode compatibility verification failed! Aborting package build."
    }
}

# 1. Clean previous staging
if (Test-Path $stagingRoot) {
    Remove-Item -Path $stagingRoot -Recurse -Force
}
New-Item -ItemType Directory -Path (Join-Path $stagingDir "app") -Force | Out-Null
if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
}

# 2. Copy root launcher binaries, Steamworks DLLs, documentation, and retro game manual
Write-Host "Copying native binaries, Steamworks DLLs, documentation, and retro manual..."
Copy-Item (Join-Path $extractedDir "ArmiesOfGielinor.exe") $stagingDir
Copy-Item (Join-Path $extractedDir "steam_api64.dll") $stagingDir
Copy-Item (Join-Path $extractedDir "steamworks4j64.dll") $stagingDir
Copy-Item (Join-Path $extractedDir "steam_appid.txt") $stagingDir
Copy-Item (Join-Path $extractedDir "README.md") $stagingDir
if (Test-Path (Join-Path $extractedDir "enhanced_options.txt")) {
    Copy-Item (Join-Path $extractedDir "enhanced_options.txt") $stagingDir
}
if (Test-Path (Join-Path $extractedDir "Enhanced_Options.bat")) {
    Copy-Item (Join-Path $extractedDir "Enhanced_Options.bat") $stagingDir
}
if (Test-Path (Join-Path $extractedDir "Manual.pdf")) {
    Copy-Item (Join-Path $extractedDir "Manual.pdf") $stagingDir
}

# 3. Copy application JAR and CFG (excluding any .bak or temp files)
Write-Host "Copying patched application JAR and CFG..."
Copy-Item (Join-Path $extractedDir "app\ArmiesOfGielinor.jar") (Join-Path $stagingDir "app")
Copy-Item (Join-Path $extractedDir "app\ArmiesOfGielinor.cfg") (Join-Path $stagingDir "app")

# 4. Copy bundled Java 18 runtime
Write-Host "Copying bundled Java 18 runtime (18.0.2.1)..."
Copy-Item (Join-Path $extractedDir "runtime") $stagingDir -Recurse

# 4b. Copy portable caches directory
if (Test-Path (Join-Path $extractedDir "caches")) {
    Write-Host "Copying portable caches directory (armiesofgielinor)..."
    Copy-Item (Join-Path $extractedDir "caches") $stagingDir -Recurse -Force
}

# 5. Create compressed v1.0.9 ZIP package containing the ArmiesOfGielinor/ root folder
Write-Host "Compressing release package to $releaseZip..."
if (Test-Path $releaseZip) { Remove-Item $releaseZip -Force }
Compress-Archive -Path $stagingDir -DestinationPath $releaseZip -Force

# 6. Save v1.0.21 milestone backup & add alongside untouched historical ZIPs in releases/ and ArmiesOfGielinor_GitHub/release/
Write-Host "Saving $releaseVersion milestone backup and archiving prior versions alongside preserved historical releases..."
if (Test-Path $archiveDir) {
    # Automatically archive all prior version ZIPs (v0.X / v1.0.X) into releases/archive/
    Get-ChildItem -Path $releasesDir -File -Filter "ArmiesOfGielinor_*.zip" |
        Where-Object { $_.Name -ne (Split-Path $releaseZip -Leaf) } |
        ForEach-Object {
            Copy-Item $_.FullName (Join-Path $archiveDir $_.Name) -Force
        }
    Copy-Item $releaseZip (Join-Path $archiveDir "ArmiesOfGielinor_${releaseVersion}_GoldenMaster.zip") -Force
}
Copy-Item $releaseZip (Join-Path $backupDir "ArmiesOfGielinor_${releaseVersion}_GoldenMaster.zip") -Force
Copy-Item (Join-Path $extractedDir "app\ArmiesOfGielinor.jar") (Join-Path $backupDir "ArmiesOfGielinor_${releaseVersion}_milestone.jar") -Force
if (Test-Path $githubReleaseDir) {
    Copy-Item $releaseZip (Join-Path $githubReleaseDir "ArmiesOfGielinor_${releaseVersion}.zip") -Force
    $releaseNotes = Join-Path $releasesDir "RELEASE_NOTES_${releaseVersion}.md"
    if (Test-Path $releaseNotes) {
        Copy-Item $releaseNotes (Join-Path $githubReleaseDir "RELEASE_NOTES_${releaseVersion}.md") -Force
    }
}

# 7. Synchronize the local playable ArmiesOfGielinor/ folder
Write-Host "Syncing live playable ArmiesOfGielinor folder..."
$liveAppDir = Join-Path $workspace "ArmiesOfGielinor"
if (Test-Path $liveAppDir) {
    Copy-Item (Join-Path $extractedDir "app\ArmiesOfGielinor.jar") (Join-Path $liveAppDir "app\ArmiesOfGielinor.jar") -Force
    if (Test-Path (Join-Path $extractedDir "Manual.pdf")) {
        Copy-Item (Join-Path $extractedDir "Manual.pdf") (Join-Path $liveAppDir "Manual.pdf") -Force
    }
    if (Test-Path (Join-Path $extractedDir "README.md")) {
        Copy-Item (Join-Path $extractedDir "README.md") (Join-Path $liveAppDir "README.md") -Force
    }
    if (Test-Path (Join-Path $liveAppDir "KI-Götter_Umschalten.bat")) {
        Remove-Item (Join-Path $liveAppDir "KI-Götter_Umschalten.bat") -Force
    }
    if (Test-Path (Join-Path $liveAppDir "Toggle_AI_Gods.bat")) {
        Remove-Item (Join-Path $liveAppDir "Toggle_AI_Gods.bat") -Force
    }
    if (Test-Path (Join-Path $extractedDir "enhanced_options.txt")) {
        Copy-Item (Join-Path $extractedDir "enhanced_options.txt") (Join-Path $liveAppDir "enhanced_options.txt") -Force
    }
    if (Test-Path (Join-Path $extractedDir "Enhanced_Options.bat")) {
        Copy-Item (Join-Path $extractedDir "Enhanced_Options.bat") (Join-Path $liveAppDir "Enhanced_Options.bat") -Force
    }
    if (Test-Path (Join-Path $extractedDir "caches")) {
        Copy-Item (Join-Path $extractedDir "caches") $liveAppDir -Recurse -Force
    }
}

# 8. Synchronize the dedicated 'Aktuell/' folder per project rule
Write-Host "Syncing dedicated 'Aktuell/' folder with current release..."
$aktuellDir = Join-Path $workspace "Aktuell"
if (-not (Test-Path $aktuellDir)) {
    New-Item -ItemType Directory -Path $aktuellDir -Force | Out-Null
}
# Keep only the current release ZIP in Aktuell/
Get-ChildItem -Path $aktuellDir -File -Filter "ArmiesOfGielinor*.zip" | 
    Where-Object { $_.Name -ne (Split-Path $releaseZip -Leaf) } | 
    Remove-Item -Force
Copy-Item $releaseZip (Join-Path $aktuellDir (Split-Path $releaseZip -Leaf)) -Force

# Mirror playable game folder into Aktuell/ArmiesOfGielinor/
$aktuellAppDir = Join-Path $aktuellDir "ArmiesOfGielinor"
if (-not (Test-Path $aktuellAppDir)) {
    New-Item -ItemType Directory -Path $aktuellAppDir -Force | Out-Null
}
Copy-Item (Join-Path $liveAppDir "*") $aktuellAppDir -Recurse -Force
if (Test-Path (Join-Path $aktuellAppDir "KI-Götter_Umschalten.bat")) {
    Remove-Item (Join-Path $aktuellAppDir "KI-Götter_Umschalten.bat") -Force
}
if (Test-Path (Join-Path $aktuellAppDir "Toggle_AI_Gods.bat")) {
    Remove-Item (Join-Path $aktuellAppDir "Toggle_AI_Gods.bat") -Force
}

# 9. Clean up temporary staging directory
if (Test-Path $stagingRoot) {
    Start-Sleep -Milliseconds 800
    try {
        Remove-Item -Path $stagingRoot -Recurse -Force -ErrorAction SilentlyContinue
    } catch {
        # Non-fatal if temporary staging cleanup is delayed by Windows file indexer
    }
}

$zipSize = (Get-Item $releaseZip).Length / 1MB
Write-Host "=== Milestone $releaseVersion Release Package Created Successfully ($([math]::Round($zipSize, 2)) MB) ==="
