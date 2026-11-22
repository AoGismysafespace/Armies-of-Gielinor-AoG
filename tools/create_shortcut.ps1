# ==============================================================================
# create_shortcut.ps1: Creates a Windows Desktop & Tools Shortcut with Hotkey
#
# 1. PURPOSE:
#    Creates an "Export Bug Report" shortcut on the user's Windows Desktop
#    and in tools/ with the global hotkey Ctrl+Alt+B.
# ==============================================================================

$WshShell = New-Object -ComObject WScript.Shell
$desktopPath = [System.Environment]::GetFolderPath('Desktop')
$toolsDir = $PSScriptRoot
if ([string]::IsNullOrEmpty($toolsDir)) {
    $toolsDir = (Resolve-Path "tools").Path
}
$workspace = (Resolve-Path (Join-Path $toolsDir "..")).Path
$scriptPath = Join-Path $toolsDir "export_bug_report.ps1"
$batPath = Join-Path $toolsDir "export_bug_report.bat"

# Create Desktop Shortcut
$desktopShortcutPath = Join-Path $desktopPath "Export Bug Report.lnk"
$desktopShortcut = $WshShell.CreateShortcut($desktopShortcutPath)
$desktopShortcut.TargetPath = $batPath
$desktopShortcut.WorkingDirectory = $workspace
$desktopShortcut.Description = "Export Armies of Gielinor Diagnostic Bug Report (Ctrl+Alt+B)"
$desktopShortcut.Hotkey = "Ctrl+Alt+B"
$desktopShortcut.Save()
Write-Host "Created Desktop Shortcut: $desktopShortcutPath [Hotkey: Ctrl+Alt+B]" -ForegroundColor Green

# Create Tools Shortcut
$toolsShortcutPath = Join-Path $toolsDir "Export Bug Report.lnk"
$toolsShortcut = $WshShell.CreateShortcut($toolsShortcutPath)
$toolsShortcut.TargetPath = $batPath
$toolsShortcut.WorkingDirectory = $workspace
$toolsShortcut.Description = "Export Armies of Gielinor Diagnostic Bug Report (Ctrl+Alt+B)"
$toolsShortcut.Hotkey = "Ctrl+Alt+B"
$toolsShortcut.Save()
Write-Host "Created Tools Shortcut: $toolsShortcutPath" -ForegroundColor Green
