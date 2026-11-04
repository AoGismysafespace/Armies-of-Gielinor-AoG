# ==============================================================================
# build_release.ps1: Automated Packager for Armies of Gielinor Release ZIP
# Milestone: v1.0.6 — Spectator Banner Auto-Dismiss & Real-Time Map Scrolling Sync
#
# 1. ARCHITECTURAL CONTEXT & RUNTIME SPECIFICATIONS:
#    The game distribution embeds an OpenJDK JRE in extracted/runtime/ (JAVA_VERSION="18.0.2.1").
#    All patched bytecode within ArmiesOfGielinor.jar must strictly conform to class file major
#    version <= 62.0 (Java 18). Any class compiled with host JDK 21 (major=65) without --release 18
#    will immediately cause a fatal UnsupportedClassVersionError at runtime.
#
# 2. KEY COMPONENTS & SYMBOL MAPPING:
#    - ArmiesOfGielinor.jar: Patched game JAR containing StandaloneRunner, AoGServer, and AoGBattleBridge.
#    - AoGServer: Multiplayer host server handling TCP lobbies, packets, and turn coordination.
#      Patched to handle Opcode 73 (Client Turn Ready) and broadcast Packet 76 (turn unpause & Vb sync)
#      with a 2.5s fallback so spectator banners dismiss smoothly and map scrolling unlocks.
#    - AoGBattleBridge: Battle engine coordination interface (implements net.alterorb.server.BattleEngine)
#      with dynamic FIFO event pump broadcasting Packet 68 combat strikes (opcode 0x60),
#      scoped suppression of redundant Packet 68 'hp' packets, and authoritative win/loss conditions.
#    - Manual.pdf: Illustrated 9-page retro-style manual containing lore, Steam P2P operations,
#      UI layout, pantheons, structure/economy tables, terrain rules, and the complete 88-unit stat database.
#    - RestoreAndVerify: Automated audit & restoration tool ensuring 0 bytecode version violations exist.
#
# 3. MILESTONE ARCHIVES PRESERVED:
#    - v1.0.2: Partially Functional Multiplayer (Lobby & In-Game Sync Milestone)
#    - v1.0.3: Working Multiplayer Combat (Dynamic Battle Event Pump Milestone)
#    - v1.0.4: Fully Functional Multiplayer Combat & Unit Slaying (Redundant Death Packet Suppression Milestone)
#    - v1.0.5: Full Win/Loss Conditions & Authoritative Match Ending (Conquest & VP Match Resolution Milestone)
#    - v1.0.6: Spectator Banner Auto-Dismiss & Real-Time Map Scrolling Sync (Packet 73/76 Handshake Milestone)
# ==============================================================================

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrEmpty($PSScriptRoot)) {
    $workspace = (Get-Item ".").FullName
} else {
    $workspace = $PSScriptRoot
}
$stagingRoot = Join-Path $workspace "release_staging"
$stagingDir = Join-Path $stagingRoot "ArmiesOfGielinor"
$extractedDir = Join-Path $workspace "extracted"

# Primary Versioned Release
$releaseZipV106 = Join-Path $workspace "ArmiesOfGielinor_v1.0.6.zip"
# Backward-compatible references & prior milestones
$releaseZipV105 = Join-Path $workspace "ArmiesOfGielinor_v1.0.5.zip"
$releaseZipV104 = Join-Path $workspace "ArmiesOfGielinor_v1.0.4.zip"
$releaseZipV103 = Join-Path $workspace "ArmiesOfGielinor_v1.0.3.zip"
$releaseZipV102 = Join-Path $workspace "ArmiesOfGielinor_v1.0.2.zip"
$releaseZipV101 = Join-Path $workspace "ArmiesOfGielinor_v1.0.1.zip"
$legacyZip = Join-Path $workspace "ArmiesOfGielinor_Patched.zip"

# Dedicated Milestone Fallback Archives
$milestoneZipV106 = Join-Path $workspace "ArmiesOfGielinor_v1.0.6_Milestone_SpectatorBannerDismissalSync.zip"
$milestoneZipV105 = Join-Path $workspace "ArmiesOfGielinor_v1.0.5_Milestone_FullWinConditionsFunctional.zip"
$milestoneZipV104 = Join-Path $workspace "ArmiesOfGielinor_v1.0.4_Milestone_FullCombatFunctional.zip"
$backupDir = Join-Path $workspace "backups"

Write-Host "=== Building Clean Armies of Gielinor Release Package (v1.0.6) ==="

# 0. Bytecode Compatibility Audit & Restoration
Write-Host "Verifying bytecode compatibility (target: Java 18, major <= 62)..."
$javaExe = "C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot\bin\java.exe"
if (Test-Path (Join-Path $workspace "RestoreAndVerify.class")) {
    & $javaExe -cp $workspace RestoreAndVerify $workspace
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
# Include the illustrated retro game manual in the release root alongside README.md
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

# 5. Create compressed ZIP package containing the ArmiesOfGielinor/ root folder
Write-Host "Compressing release package to $releaseZipV106..."
if (Test-Path $releaseZipV106) { Remove-Item $releaseZipV106 -Force }
Compress-Archive -Path $stagingDir -DestinationPath $releaseZipV106 -Force

# 6. Keep aliases, backups, and backward-compatible references in sync
Write-Host "Syncing milestone and fallback archives..."
Copy-Item $releaseZipV106 $milestoneZipV106 -Force
Copy-Item $releaseZipV106 $releaseZipV105 -Force
Copy-Item $releaseZipV106 $releaseZipV104 -Force
Copy-Item $releaseZipV106 $releaseZipV103 -Force
Copy-Item $releaseZipV106 $releaseZipV102 -Force
Copy-Item $releaseZipV106 $releaseZipV101 -Force
Copy-Item $releaseZipV106 $legacyZip -Force
Copy-Item $releaseZipV106 (Join-Path $backupDir "ArmiesOfGielinor_v1.0.6_Milestone_SpectatorBannerDismissalSync.zip") -Force
Copy-Item (Join-Path $extractedDir "app\ArmiesOfGielinor.jar") (Join-Path $backupDir "ArmiesOfGielinor_v1.0.6_milestone.jar") -Force

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
}

# 8. Clean up temporary staging directory
Remove-Item -Path $stagingRoot -Recurse -Force

$zipSize = (Get-Item $releaseZipV106).Length / 1MB
Write-Host "=== Milestone v1.0.6 Release Package Created Successfully ($([math]::Round($zipSize, 2)) MB) ==="
