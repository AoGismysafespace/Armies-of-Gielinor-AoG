# ==============================================================================
# export_bug_report.ps1: 1-Click Game Diagnostic & Privacy-Scrubbed Log Exporter
#
# 1. PURPOSE & ARCHITECTURAL CONTEXT:
#    Enables instant, 1-click diagnostic generation when a player experiences
#    a game freeze, disconnection veil, or unexpected behavior.
#    Extracts recent events from ~/.armiesofgielinor/standalone.log, scrubs
#    personal computer paths for privacy (Rule 3), formats a structured report
#    with player observation prompts, copies the result to the Windows clipboard,
#    and opens the report file in Notepad.
#
# 2. RUNTIME RESOLUTION:
#    Uses the bundled OpenJDK 18 runtime (ArmiesOfGielinor/runtime/bin/java.exe)
#    or system Java, ensuring zero external dependencies.
#
# 3. ROSETTA STONE / SYMBOL MAPPINGS:
#    - reports/ : Dedicated folder where timestamped reports are stored.
#    - net.alterorb.launcher.BugReporter : The underlying diagnostic engine.
# ==============================================================================

$ErrorActionPreference = "Stop"

$scriptDir = $PSScriptRoot
if ([string]::IsNullOrEmpty($scriptDir)) {
    $scriptDir = (Get-Item ".").FullName
}

# Resolve workspace root (parent of tools/)
$workspace = (Resolve-Path (Join-Path $scriptDir "..")).Path
if (-not (Test-Path (Join-Path $workspace "extracted"))) {
    if (Test-Path (Join-Path $workspace "..\extracted")) {
        $workspace = (Resolve-Path (Join-Path $workspace "..")).Path
    }
}

# Locate Java executable (prefer bundled Java 18 runtime)
$javaExe = Join-Path $workspace "ArmiesOfGielinor\runtime\bin\java.exe"
if (-not (Test-Path $javaExe)) {
    $javaExe = Join-Path $workspace "extracted\runtime\bin\java.exe"
}
if (-not (Test-Path $javaExe)) {
    $javaExe = "C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot\bin\java.exe"
}
if (-not (Test-Path $javaExe)) {
    $javaExe = "java.exe"
}

# Locate BugReporter classpath (src/ or JAR)
$cp = Join-Path $workspace "src"
if (-not (Test-Path (Join-Path $cp "net\alterorb\launcher\BugReporter.class"))) {
    $cp = Join-Path $workspace "extracted\app\ArmiesOfGielinor.jar"
}

Write-Host "=== Generating Armies of Gielinor Diagnostic Bug Report ===" -ForegroundColor Cyan
& $javaExe -cp $cp net.alterorb.launcher.BugReporter

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[Done] Report is saved in the 'reports/' folder and copied to your clipboard!" -ForegroundColor Green
} else {
    Write-Error "Failed to generate bug report."
}
