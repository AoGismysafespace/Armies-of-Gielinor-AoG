# ==============================================================================
# build_manual.ps1: Retro-Style Game Manual Generator & Edge PDF Compiler
# Milestone: v1.0 — Standalone Golden Master Release
#
# 1. ARCHITECTURAL CONTEXT & PRIVACY SPECIFICATIONS:
#    Compiles an authentic, fully self-contained HTML manual into a print-ready PDF booklet
#    using Microsoft Edge in headless mode. All user-identifying traces, steam accounts,
#    and personal screenshots are strictly excluded in favor of clean, vector HUD schematics
#    using generic commander roles (Commander 1, Commander 2).
#
# 2. KEY TYPOGRAPHY & ASSET MAPPINGS (Rosetta Stone):
#    - KingthingsPetrock (TTF): Iconic RuneScape branding and title font used for headers,
#      cover titles, and unit names. Rendered in bright RuneScape gold (#ffde00) with 3D drop-shadows.
#    - FunOrbRoman20 & FunOrbRoman12 (TTF): Exact serif interface typography extracted from
#      FunOrb game client caches. Used for section subheadings, unit tables, and body reading.
#    - RuneScapeBold12 & RuneScapePlain12 (TTF): The authentic Jagex in-game chat, button, and
#      HUD font. Used for interface controls, button pills, and stat labels (SPD, STR, HP, RNG).
#    - RuneScapeQuill (TTF): Authentic hand-drawn serif used for in-game books, scrolls, and lore.
#    - Schematics: Custom vector interface diagrams for Screen -4 (Lobby) and Screen 0 (Battlefield).
#
# 3. UNIT STAT EXTRACTION:
#    Dynamically runs DumpUnitStats against ArmiesOfGielinor.jar to extract all 83 units across
#    Neutral, Saradomin, Zamorak, Guthix, Bandos, Seren, and Menaphos factions.
# ==============================================================================

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrEmpty($PSScriptRoot)) {
    $workspace = (Get-Item ".").FullName
} else {
    $workspace = (Get-Item (Join-Path $PSScriptRoot "..")).FullName
}
$manualDir = Join-Path $workspace "manual"
$fontsDir  = Join-Path $manualDir "fonts"
$htmlPath  = Join-Path $manualDir "manual.html"
$pdfPath   = Join-Path $manualDir "Armies_of_Gielinor_Manual.pdf"

Write-Host "=== Building Authentic In-Game Styled Manual (Privacy-Safe & Sanitized) ==="

# 1. Load and base64-encode authentic Jagex fonts
Write-Host "Encoding authentic fonts to base64 data URIs..."
$petrockPath = Join-Path $fontsDir "Kingthings Petrock.ttf"
$quillPath   = Join-Path $fontsDir "runescape\ttf\RuneScape-Quill.ttf"
$bold12Path  = Join-Path $fontsDir "runescape\ttf\RuneScape-Bold-12.ttf"
$plain12Path = Join-Path $fontsDir "runescape\ttf\RuneScape-Plain-12.ttf"
$roman20Path = Join-Path $fontsDir "funorb\ttf\FunOrb-Roman20.ttf"
$roman12Path = Join-Path $fontsDir "funorb\ttf\FunOrb-Roman12.ttf"

$b64Petrock = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($petrockPath))
$b64Quill   = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($quillPath))
$b64Bold12  = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($bold12Path))
$b64Plain12 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($plain12Path))
$b64Roman20 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($roman20Path))
$b64Roman12 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($roman12Path))

# 2. Extract Complete Unit Database directly from bytecode & generate Steckbrief compendium
Write-Host "Compiling and running Unit Compendium & Steckbrief generator..."
$java = $null
$javac = $null
$candidateJavas = @(
    "C:\Program Files\Java\jdk-26.0.2\bin\java.exe",
    "C:\Program Files\Java\jdk-19\bin\java.exe",
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot\bin\java.exe"
)
foreach ($c in $candidateJavas) {
    if (Test-Path $c) {
        $java = $c
        $javac = $c.Replace("java.exe", "javac.exe")
        break
    }
}
if (-not $java) {
    $java = (Get-Command java -ErrorAction SilentlyContinue).Source
    $javac = (Get-Command javac -ErrorAction SilentlyContinue).Source
}
$jarPath = Join-Path $workspace "extracted\app\ArmiesOfGielinor.jar"
$testEnvDir = Join-Path $workspace "test_environment"

# Compile GenerateUnitCompendium targeting Java 18 release
& $javac --release 18 -cp $jarPath -d $testEnvDir (Join-Path $testEnvDir "GenerateUnitCompendium.java")

# Execute GenerateUnitCompendium to produce manual/generated_compendium.html
& $java -cp "$jarPath;$testEnvDir" GenerateUnitCompendium

$compendiumHtmlPath = Join-Path $manualDir "generated_compendium.html"
$compendiumHtml = [System.IO.File]::ReadAllText($compendiumHtmlPath, [System.Text.Encoding]::UTF8)

# Also generate the quick-reference master stat table
$rawUnitLines = & $java -cp "$jarPath;$testEnvDir" DumpUnitStats
$unitTableRows = ""

foreach ($line in $rawUnitLines) {
    if ($line -match "^\s*(\d+)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*)$") {
        $uName = $matches[2]
        $uGod  = $matches[3]
        $uSpd  = $matches[4]
        $uHp   = $matches[5]
        $uStr  = $matches[6]
        $uRng  = $matches[7]
        $uCost = $matches[8]
        $uMove = $matches[9]

        $godBadgeClass = "god-tag-" + $uGod.ToLower()
        $unitTableRows += @"
      <tr>
        <td class="unit-name">$uName</td>
        <td><span class="god-tag $godBadgeClass">$uGod</span></td>
        <td style="color:#c5bdb0;">$uMove</td>
        <td class="stat-spd">$uSpd</td>
        <td class="stat-str">$uStr</td>
        <td class="stat-hp">$uHp</td>
        <td class="stat-rng">$uRng</td>
        <td class="stat-mana">$uCost</td>
      </tr>
"@
    }
}

# 3. Build the Complete HTML Document
Write-Host "Constructing HTML source with authentic in-game theme..."
$htmlContent = @"
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Armies of Gielinor — Official Tactical Game Manual</title>
<style>
  /* ==================== AUTHENTIC IN-GAME TYPOGRAPHY ==================== */
  @font-face {
    font-family: 'KingthingsPetrock';
    src: url('data:font/truetype;charset=utf-8;base64,$b64Petrock') format('truetype');
    font-weight: normal;
    font-style: normal;
  }
  @font-face {
    font-family: 'RuneScapeQuill';
    src: url('data:font/truetype;charset=utf-8;base64,$b64Quill') format('truetype');
    font-weight: normal;
    font-style: normal;
  }
  @font-face {
    font-family: 'RuneScapeBold12';
    src: url('data:font/truetype;charset=utf-8;base64,$b64Bold12') format('truetype');
    font-weight: normal;
    font-style: normal;
  }
  @font-face {
    font-family: 'RuneScapePlain12';
    src: url('data:font/truetype;charset=utf-8;base64,$b64Plain12') format('truetype');
    font-weight: normal;
    font-style: normal;
  }
  @font-face {
    font-family: 'FunOrbRoman20';
    src: url('data:font/truetype;charset=utf-8;base64,$b64Roman20') format('truetype');
    font-weight: normal;
    font-style: normal;
  }
  @font-face {
    font-family: 'FunOrbRoman12';
    src: url('data:font/truetype;charset=utf-8;base64,$b64Roman12') format('truetype');
    font-weight: normal;
    font-style: normal;
  }

  /* ==================== GLOBAL IN-GAME STYLING ==================== */
  @page {
    size: A4;
    margin: 10mm 10mm 10mm 10mm;
    background-color: #121417;
  }

  * {
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
    box-sizing: border-box;
  }

  html, body {
    margin: 0;
    padding: 0;
    background-color: #121417;
    color: #e4ded4;
    font-family: 'RuneScapeQuill', Georgia, 'Times New Roman', serif;
    font-size: 11pt;
    line-height: 1.45;
  }

  .manual-wrapper {
    background: #171b1e radial-gradient(circle at 50% 15%, #252b31 0%, #15181b 90%);
    border: 3px solid #c59e35;
    outline: 1px solid #382c0e;
    box-shadow: inset 0 0 25px rgba(0,0,0,0.85), 0 0 20px rgba(0,0,0,0.9);
    padding: 22px 26px;
    margin: 0 auto;
    border-radius: 4px;
    max-width: 1000px;
  }

  /* ==================== COVER / HEADER ==================== */
  .cover-banner {
    text-align: center;
    border-bottom: 2px solid #c59e35;
    padding-bottom: 12px;
    margin-bottom: 18px;
    background: #1e2227 linear-gradient(180deg, #262c33 0%, #181b1e 100%);
    border-radius: 4px;
    padding-top: 16px;
    box-shadow: inset 0 0 12px rgba(0,0,0,0.6);
  }

  .cover-banner .subtitle {
    font-family: 'RuneScapeBold12', monospace;
    font-size: 11pt;
    color: #48d1cc;
    letter-spacing: 2px;
    text-transform: uppercase;
    margin-bottom: 4px;
  }

  .cover-banner h1 {
    font-family: 'KingthingsPetrock', serif;
    font-size: 34pt;
    color: #ffde00;
    margin: 4px 0 6px 0;
    text-shadow: 2px 2px 0px #000, 3px 3px 0px #3a2200;
    letter-spacing: 2px;
  }

  .cover-banner .tagline {
    font-family: 'KingthingsPetrock', serif;
    font-size: 13pt;
    color: #ffd700;
    margin-top: 4px;
  }

  .ornament {
    color: #c59e35;
    font-size: 13pt;
    margin-top: 6px;
    text-shadow: 1px 1px 2px #000;
  }

  /* ==================== CHAPTER STONE SLAB BANNERS ==================== */
  .stone-header {
    background: #2b221a linear-gradient(180deg, #382c22 0%, #1e1711 100%);
    border: 2px solid #6b5038;
    border-top: 2px solid #c59e35;
    border-bottom: 2px solid #0f0b07;
    border-radius: 5px;
    padding: 8px 16px;
    margin: 22px 0 14px 0;
    box-shadow: inset 0 0 10px rgba(0,0,0,0.8), 0 3px 8px rgba(0,0,0,0.6);
  }

  .stone-header h2 {
    font-family: 'KingthingsPetrock', serif;
    font-size: 19pt;
    color: #ffde00;
    margin: 0;
    text-shadow: 2px 2px 0 #000;
    letter-spacing: 1px;
  }

  .page-break {
    page-break-before: always;
    break-before: page;
  }

  /* ==================== TYPOGRAPHY & SECTIONS ==================== */
  h3.sub-section {
    font-family: 'KingthingsPetrock', serif;
    font-size: 14pt;
    color: #ffd700;
    border-bottom: 1px solid #5a4b2c;
    padding-bottom: 4px;
    margin: 16px 0 8px 0;
    text-shadow: 1px 1px 0 #000;
    letter-spacing: 0.5px;
  }

  p {
    margin: 0 0 10px 0;
    text-align: justify;
  }

  .lead-in {
    font-family: 'KingthingsPetrock', serif;
    font-size: 13pt;
    color: #ffde00;
    letter-spacing: 0.5px;
  }

  ul {
    margin: 6px 0 12px 18px;
    padding: 0;
  }

  li {
    margin-bottom: 5px;
  }

  /* ==================== IN-GAME UI BUTTONS & BADGES ==================== */
  .ui-btn {
    display: inline-block;
    background: #234857 linear-gradient(180deg, #2d5a6d 0%, #1a3844 100%);
    border: 1px solid #48d1cc;
    border-top: 1px solid #7beef0;
    border-bottom: 2px solid #0f242d;
    border-radius: 3px;
    padding: 2px 8px;
    font-family: 'RuneScapeBold12', monospace;
    font-size: 9.5pt;
    color: #ffffff;
    text-shadow: 1px 1px 0 #000;
    vertical-align: middle;
  }

  .ui-badge {
    display: inline-block;
    background: #1b2631;
    border: 1px solid #48d1cc;
    color: #48d1cc;
    font-family: 'RuneScapeBold12', monospace;
    font-size: 8.5pt;
    padding: 1px 5px;
    border-radius: 2px;
  }

  .key-badge {
    display: inline-block;
    background: #2c3238;
    border: 1px solid #5a636c;
    border-bottom: 2px solid #14171a;
    color: #ffde00;
    font-family: 'RuneScapeBold12', monospace;
    font-size: 9pt;
    padding: 1px 6px;
    border-radius: 3px;
    text-shadow: 1px 1px 0 #000;
  }

  /* ==================== IN-GAME CALLOUT BOXES ==================== */
  .stone-callout {
    background: #2b221a linear-gradient(180deg, #362a20 0%, #1e1610 100%);
    border: 2px solid #c59e35;
    border-radius: 5px;
    padding: 12px 18px;
    margin: 16px 0;
    box-shadow: inset 0 0 12px rgba(0,0,0,0.7), 0 3px 8px rgba(0,0,0,0.5);
  }

  .stone-callout .banner-title {
    font-family: 'KingthingsPetrock', serif;
    font-size: 13pt;
    color: #ffde00;
    margin-bottom: 6px;
    text-shadow: 1px 1px 0 #000;
  }

  .highlight-steam {
    border: 2px solid #48d1cc;
    background: #17242c linear-gradient(180deg, #1e303b 0%, #111a20 100%);
  }

  /* ==================== IN-GAME VECTOR HUD SCHEMATICS ==================== */
  .hud-schematic {
    background: #191d21 linear-gradient(180deg, #22272d 0%, #15181b 100%);
    border: 2px solid #c59e35;
    border-radius: 5px;
    padding: 12px;
    margin: 14px 0;
    box-shadow: inset 0 0 15px rgba(0,0,0,0.8), 0 4px 10px rgba(0,0,0,0.6);
  }

  .schematic-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 2px solid #5a4b2c;
    padding-bottom: 6px;
    margin-bottom: 10px;
  }

  .schematic-title {
    font-family: 'KingthingsPetrock', serif;
    font-size: 13.5pt;
    color: #ffde00;
    text-shadow: 1px 1px 0 #000;
  }

  .schematic-screen {
    font-family: 'RuneScapeBold12', monospace;
    font-size: 8.5pt;
    color: #48d1cc;
  }

  .schematic-body {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .schematic-panel {
    background: #20252b;
    border: 1px solid #3d444c;
    border-radius: 3px;
    padding: 10px;
  }

  .panel-title {
    font-family: 'RuneScapeBold12', monospace;
    font-size: 9pt;
    color: #ffde00;
    border-bottom: 1px solid #363c44;
    padding-bottom: 4px;
    margin-bottom: 8px;
  }

  .slot-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 4px 8px;
    margin-bottom: 4px;
    background: #171b1f;
    border-radius: 2px;
    font-size: 9.5pt;
  }

  .slot-role { color: #888; font-family: 'RuneScapeBold12', monospace; font-size: 8pt; }
  .slot-name { color: #fff; font-family: 'KingthingsPetrock', serif; font-size: 11pt; }
  .slot-status { font-family: 'RuneScapeBold12', monospace; font-size: 8.5pt; color: #5cf25c; }

  .param-row {
    display: flex;
    justify-content: space-between;
    padding: 3px 0;
    border-bottom: 1px dotted #2d333b;
    font-size: 9pt;
  }

  .param-row strong { color: #ffea88; font-family: 'KingthingsPetrock', serif; font-size: 10.5pt; }

  .viewport-grid-canvas {
    background: #14171a radial-gradient(circle at 50% 50%, #1e242a 0%, #0e1012 100%);
    border: 1px solid #3d444c;
    height: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border-radius: 4px;
    margin: 8px 0;
    text-align: center;
  }

  .grid-label {
    font-family: 'KingthingsPetrock', serif;
    font-size: 12.5pt;
    color: #ffde00;
    text-shadow: 1px 1px 0 #000;
  }

  .grid-sub {
    font-family: 'RuneScapeBold12', monospace;
    font-size: 8.5pt;
    color: #88929b;
    margin-top: 3px;
  }

  .map-icons {
    margin-top: 8px;
    display: flex;
    gap: 12px;
  }

  .icon-node {
    background: #1a2026;
    border: 1px solid #4a545e;
    padding: 2px 8px;
    border-radius: 3px;
    font-family: 'RuneScapeBold12', monospace;
    font-size: 8pt;
  }

  .viewport-hud-top, .viewport-hud-bottom {
    display: flex;
    justify-content: space-between;
    gap: 6px;
  }

  .hud-elem {
    background: #20252b;
    border: 1px solid #444b54;
    border-radius: 3px;
    padding: 4px 6px;
    font-family: 'RuneScapeBold12', monospace;
    font-size: 8pt;
    color: #ffde00;
    flex: 1;
    text-align: center;
  }

  /* ==================== PANTHEON HERALDRY CARDS ==================== */
  .pantheon-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin: 14px 0;
  }

  .god-card {
    border-radius: 4px;
    padding: 10px 14px;
    border: 1px solid #4a5159;
    box-shadow: inset 0 0 10px rgba(0,0,0,0.6), 0 2px 5px rgba(0,0,0,0.5);
  }

  .god-card h4 {
    font-family: 'KingthingsPetrock', serif;
    font-size: 14pt;
    margin: 0 0 4px 0;
    text-shadow: 1px 1px 0 #000;
  }

  .god-card p {
    font-size: 9.5pt;
    margin: 0;
    line-height: 1.4;
  }

  .god-saradomin { background: #162a4a; border-color: #2b5592; border-left: 4px solid #4a88e8; }
  .god-saradomin h4 { color: #8ec0ff; }
  .god-zamorak   { background: #421212; border-color: #7d2424; border-left: 4px solid #ff4444; }
  .god-zamorak h4   { color: #ff8888; }
  .god-guthix    { background: #14381b; border-color: #266b34; border-left: 4px solid #44d960; }
  .god-guthix h4    { color: #86f09b; }
  .god-seren     { background: #0e3c45; border-color: #1f6f7d; border-left: 4px solid #3fe0f0; }
  .god-seren h4     { color: #7cf4ff; }
  .god-bandos    { background: #3d2b17; border-color: #6e4e2a; border-left: 4px solid #d4a040; }
  .god-bandos h4    { color: #ffd685; }
  .god-menaphos  { background: #47300c; border-color: #825816; border-left: 4px solid #f5b025; }
  .god-menaphos h4  { color: #ffd275; }

  /* ==================== CHAPTER VII: UNIT STECKBRIEF (DOSSIER) ==================== */
  .pantheon-section {
    margin-top: 18px;
    margin-bottom: 24px;
  }
  .pantheon-banner {
    background: #1e1710 linear-gradient(90deg, rgba(35,26,16,0.95) 0%, rgba(20,15,10,0.85) 100%);
    border-left: 5px solid #d4af37;
    border-top: 1px solid rgba(212,175,55,0.4);
    border-bottom: 1px solid rgba(212,175,55,0.4);
    border-right: 1px solid rgba(212,175,55,0.2);
    border-radius: 0 4px 4px 0;
    padding: 10px 14px;
    margin-bottom: 14px;
    box-shadow: inset 0 0 12px rgba(0,0,0,0.7), 0 2px 6px rgba(0,0,0,0.5);
  }
  .pantheon-heading {
    font-family: 'KingthingsPetrock', serif;
    font-size: 15pt;
    margin: 0 0 3px 0;
    letter-spacing: 0.5px;
    text-shadow: 1px 1px 0 #000;
  }
  .pantheon-desc {
    font-family: 'RuneScapeQuill', serif;
    font-size: 10.5pt;
    color: #c9b496;
    margin: 0;
    line-height: 1.4;
  }
  .dossier-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-bottom: 20px;
  }
  .steckbrief {
    background: #17120c linear-gradient(180deg, #1f1811 0%, #120e09 100%);
    border: 1.5px solid #5a4525;
    border-top: 2.5px solid #d4af37;
    border-radius: 4px;
    padding: 10px 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    page-break-inside: avoid;
    break-inside: avoid;
    box-shadow: 0 3px 8px rgba(0,0,0,0.6), inset 0 0 12px rgba(0,0,0,0.5);
  }
  .sb-header {
    display: flex;
    align-items: center;
    gap: 10px;
    border-bottom: 1px solid rgba(212,175,55,0.25);
    padding-bottom: 6px;
  }
  .sb-portrait-frame {
    width: 44px;
    height: 44px;
    background: radial-gradient(circle, #2d2215 0%, #0d0a07 100%);
    border: 2px solid #d4af37;
    border-radius: 3px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: inset 0 0 5px rgba(0,0,0,0.9), 0 2px 4px rgba(0,0,0,0.6);
    flex-shrink: 0;
  }
  .sb-portrait {
    width: 38px;
    height: 38px;
    image-rendering: pixelated;
    display: block;
  }
  .sb-title-box {
    flex-grow: 1;
  }
  .sb-name {
    font-family: 'KingthingsPetrock', serif;
    font-size: 13.5pt;
    color: #ffdf78;
    font-weight: bold;
    letter-spacing: 0.5px;
    line-height: 1.1;
    margin: 0;
    text-shadow: 1px 1px 0 #000;
  }
  .sb-meta {
    font-family: 'RuneScapeBold12', sans-serif;
    font-size: 8.5pt;
    color: #9c8b74;
    margin-top: 2px;
  }
  .sb-cost-pill {
    background: #382813;
    border: 1px solid #d4af37;
    color: #ffe677;
    font-family: 'FunOrbRoman12', sans-serif;
    font-size: 9.5pt;
    font-weight: bold;
    padding: 2px 7px;
    border-radius: 10px;
    box-shadow: 0 0 5px rgba(212,175,55,0.3);
    white-space: nowrap;
  }
  .sb-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 4px;
    background: rgba(0,0,0,0.45);
    padding: 4px;
    border-radius: 3px;
    border: 1px solid rgba(90,69,37,0.6);
    text-align: center;
  }
  .sb-stat-item {
    display: flex;
    flex-direction: column;
  }
  .sb-stat-label {
    font-family: 'FunOrbRoman12', sans-serif;
    font-size: 7.5pt;
    text-transform: uppercase;
    color: #83725b;
    letter-spacing: 0.5px;
  }
  .sb-stat-val {
    font-family: 'FunOrbRoman20', sans-serif;
    font-size: 11pt;
    font-weight: bold;
    color: #ffffff;
  }
  .sb-traits {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
  .trait-pill {
    font-family: 'RuneScapeBold12', sans-serif;
    font-size: 7.5pt;
    padding: 2px 5px;
    border-radius: 3px;
    background: #202418;
    border: 1px solid #435c24;
    color: #9bd665;
    font-weight: 600;
  }
  .trait-pill.flight  { background: #14232d; border-color: #245075; color: #6bb9f0; }
  .trait-pill.decay   { background: #2d1414; border-color: #752424; color: #f06b6b; }
  .trait-pill.capture { background: #2d2414; border-color: #755f24; color: #f0c96b; }
  .trait-pill.amphib  { background: #142d2a; border-color: #24756b; color: #6bf0db; }
  .trait-pill.heal    { background: #28142d; border-color: #6a2475; color: #d66bf0; }
  .trait-pill.siege   { background: #2d1c14; border-color: #753f24; color: #f0896b; }
  .trait-pill.titan   { background: #30260e; border-color: #9e7d18; color: #ffd700; }
  .trait-pill.ranged  { background: #19271a; border-color: #2e6132; color: #7ad480; }
  .sb-ability {
    background: #1b1610 linear-gradient(180deg, #241c12 0%, #15100a 100%);
    border: 1px solid #755f24;
    border-left: 3.5px solid #ffde00;
    border-radius: 3px;
    padding: 6px 8px;
    margin: 2px 0;
    box-shadow: inset 0 0 6px rgba(0,0,0,0.6);
  }
  .sb-ability-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 2px;
  }
  .sb-ability-badge {
    font-family: 'RuneScapeBold12', sans-serif;
    font-size: 7.5pt;
    color: #ffde00;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .sb-ability-name {
    font-family: 'KingthingsPetrock', serif;
    font-size: 11pt;
    color: #ffffff;
    font-weight: bold;
    letter-spacing: 0.5px;
    text-shadow: 1px 1px 0 #000;
  }
  .sb-ability-desc {
    font-family: 'FunOrbRoman12', serif;
    font-size: 8.5pt;
    color: #dfcfb2;
    line-height: 1.35;
  }
  .sb-lore {
    font-family: 'RuneScapeQuill', serif;
    font-size: 9.5pt;
    line-height: 1.35;
    color: #bda88a;
    font-style: italic;
    background: rgba(0,0,0,0.25);
    border-left: 2px solid #8c6d32;
    padding: 5px 7px;
    border-radius: 0 3px 3px 0;
  }
  .sb-role {
    font-family: 'FunOrbRoman12', sans-serif;
    font-size: 8pt;
    color: #8c7b64;
  }
  .sb-role strong {
    color: #d4af37;
  }

  /* ==================== STAT TABLES ==================== */
  .stat-table {
    width: 100%;
    border-collapse: collapse;
    margin: 14px 0;
    font-size: 9pt;
    border: 1px solid #4a5159;
  }

  .stat-table th {
    background: #2b3036 linear-gradient(180deg, #363c44 0%, #22262a 100%);
    font-family: 'KingthingsPetrock', serif;
    font-size: 11pt;
    color: #ffde00;
    text-shadow: 1px 1px 0 #000;
    padding: 6px 8px;
    border: 1px solid #4a5159;
    border-bottom: 2px solid #c59e35;
    text-align: center;
  }

  .stat-table th.col-name {
    text-align: left;
  }

  .stat-table td {
    padding: 4px 8px;
    border: 1px solid #2d3339;
  }

  .stat-table td.unit-name {
    font-family: 'KingthingsPetrock', serif;
    font-size: 10.5pt;
    color: #ffea88;
    text-shadow: 1px 1px 0 #000;
    letter-spacing: 0.5px;
  }

  .stat-table tr:nth-child(even) { background: #1e2226; }
  .stat-table tr:nth-child(odd)  { background: #171a1d; }

  .stat-spd  { color: #5cf25c; font-family: 'RuneScapeBold12', monospace; font-size: 9.5pt; font-weight: bold; text-align: center; }
  .stat-str  { color: #ffcc33; font-family: 'RuneScapeBold12', monospace; font-size: 9.5pt; font-weight: bold; text-align: center; }
  .stat-hp   { color: #ff5555; font-family: 'RuneScapeBold12', monospace; font-size: 9.5pt; font-weight: bold; text-align: center; }
  .stat-rng  { color: #55bbee; font-family: 'RuneScapeBold12', monospace; font-size: 9.5pt; font-weight: bold; text-align: center; }
  .stat-mana { color: #bb77ff; font-family: 'RuneScapeBold12', monospace; font-size: 9.5pt; font-weight: bold; text-align: center; }

  .god-tag {
    display: inline-block;
    padding: 1px 5px;
    border-radius: 3px;
    font-family: 'RuneScapeBold12', monospace;
    font-size: 8pt;
    text-shadow: 1px 1px 0 #000;
  }
  .god-tag-neutral   { background: #33393e; color: #cfd6dc; border: 1px solid #5a626a; }
  .god-tag-saradomin { background: #162a4a; color: #9bc5ff; border: 1px solid #3362a3; }
  .god-tag-zamorak   { background: #421212; color: #ffa3a3; border: 1px solid #852a2a; }
  .god-tag-guthix    { background: #14381b; color: #96f2a7; border: 1px solid #2a7538; }
  .god-tag-bandos    { background: #3d2b17; color: #ffd685; border: 1px solid #7a562d; }
  .god-tag-seren     { background: #0e3c45; color: #7cf4ff; border: 1px solid #1f7785; }
  .god-tag-menaphos  { background: #47300c; color: #ffd275; border: 1px solid #8c5e17; }

  .footer-vintage {
    text-align: center;
    font-family: 'RuneScapeQuill', serif;
    font-size: 9.5pt;
    color: #8c744e;
    border-top: 1px solid #5a4b2c;
    padding-top: 12px;
    margin-top: 30px;
  }
</style>
</head>
<body>

<div class="manual-wrapper">

  <!-- ==================== COVER HEADER ==================== -->
  <div class="cover-banner">
    <div class="subtitle">STANDALONE STEAM EDITION &bull; AUTHORITATIVE BATTLE ENGINE</div>
    <h1>Armies of Gielinor</h1>
    <div class="tagline">Official Tactical Reference Manual & Battlefield Compendium</div>
    <div class="ornament">&#9670; &bull; &#10070; &bull; &#9670;</div>
  </div>

  <!-- ==================== CHAPTER 1: LORE ==================== -->
  <div class="stone-header">
    <h2>I. The God Wars of Gielinor</h2>
  </div>

  <p>
    <span class="lead-in">IN THE THIRD AGE OF GIELINOR</span>, the cataclysmic conflict known as the God Wars engulfed every continent, forest, and desert in relentless divine warfare. The fragile balance preserved by Guthix was shattered as rival gods led legion after legion through mystical stone portals to conquer territory, capture vital settlements, and harvest the ancient Mana embedded within the soil.
  </p>
  <p>
    As a supreme Commander chosen by fate, you are entrusted with the banners of war. By rallying local barbarian tribes, aligning with divine pantheons, and capturing strategic structures, you must direct mortal and immortal soldiers alike across rugged battlegrounds to secure total dominion.
  </p>
  <p>
    Victory demands not only sharp steel and devastating sorcery, but cold, deliberate tactical calculation. Each turn represents a vital stroke of military maneuvering where positioning, terrain, and attrition dictate who rises as conqueror and who is banished to the abyss.
  </p>

  <!-- ==================== CHAPTER 2: SETUP & MULTIPLAYER ==================== -->
  <div class="stone-header">
    <h2>II. System Operations & Steam Multiplayer</h2>
  </div>

  <h3 class="sub-section">Requirements & Native Runtime</h3>
  <p>
    The Standalone Edition includes an embedded OpenJDK 18 runtime environment (<code style="color:#48d1cc;">18.0.2.1</code>). No separate Java installation or system PATH configuration is required. Simply launch <code style="color:#ffde00;">ArmiesOfGielinor.exe</code> on 64-bit Windows 10 or Windows 11.
  </p>

  <h3 class="sub-section">Display Scaling & Graphics</h3>
  <p>
    The client rasterizer renders high-resolution 1:1 pixel assets with automatic high-DPI awareness, preventing blurry scaling on 1440p and 4K monitors. You may freely toggle between Borderless Fullscreen, Resizable Windowed, and Classic Fixed (640&times;480) viewports via the in-game options menu.
  </p>

  <h3 class="sub-section">Hosting & Joining Steam P2P Matches</h3>
  <p>
    Multiplayer networking is powered by Steamworks P2P datagram relays (AppID 480). Matches are private, secure, and require no router port forwarding:
  </p>
  <ul>
    <li><strong>Hosting a Match:</strong> Launch the game, select <em>Host</em>, and navigate from the Main Menu to <em>Custom Game &rarr; Create Room</em>. Press <span class="key-badge">F2</span> or open the Steam Overlay (<span class="key-badge">Shift + Tab</span>) to invite friends.</li>
    <li><strong>Friend Invitations:</strong> Friends receive standard Steam chat notifications with a direct <em>Join Game</em> prompt.</li>
  </ul>

  <!-- IMPORTANT USER NOTE -->
  <div class="stone-callout highlight-steam">
    <div class="ui-badge">CRITICAL STEAM INVITATION PROCEDURE</div>
    <div class="banner-title" style="margin-top:4px;">When accepting an invitation via Steam into a multiplayer match:</div>
    <p style="margin: 0;">
      The game client launches directly into the <strong>Main Menu</strong>.<br>
      From the Main Menu, simply click on <span class="ui-btn">Multiplayer</span> (or <em>Custom Game</em>) to re-enter the lobby. The Steam P2P relay will automatically route you straight into the host's room (<code style="color:#48d1cc;">Screen -4</code>), and you are immediately good to go!
    </p>
  </div>

  <!-- IN-GAME VECTOR SCHEMATIC: MULTIPLAYER LOBBY -->
  <div class="hud-schematic">
    <div class="schematic-header">
      <span class="schematic-title">CUSTOM GAME LOBBY</span>
      <span class="schematic-screen">SCREEN -4 &bull; STEAM P2P RELAY ROOM</span>
    </div>
    <div class="schematic-body">
      <div class="schematic-panel">
        <div class="panel-title">COMMANDER ROSTER (2/4)</div>
        <div class="slot-row">
          <span class="slot-role">[HOST]</span>
          <span class="slot-name">Commander 1</span>
          <span class="slot-status">LEADER</span>
        </div>
        <div class="slot-row">
          <span class="slot-role">[GUEST]</span>
          <span class="slot-name">Commander 2</span>
          <span class="slot-status">READY</span>
        </div>
        <div class="slot-row">
          <span class="slot-role">[OPEN]</span>
          <span class="slot-name">Open Slot</span>
          <span class="slot-status" style="color:#888;">WAITING</span>
        </div>
      </div>
      <div class="schematic-panel">
        <div class="panel-title">MATCH PARAMETERS</div>
        <div class="param-row"><span>Game Mode:</span> <strong>Free For All</strong></div>
        <div class="param-row"><span>Turn Clock:</span> <strong>60 Seconds</strong></div>
        <div class="param-row"><span>Map Dimension:</span> <strong>Medium (Grid)</strong></div>
        <div class="param-row"><span>Victory Ceiling:</span> <strong>1,000 VP</strong></div>
      </div>
    </div>
  </div>

  <!-- ==================== CHAPTER 3: INTERFACE ==================== -->
  <div class="stone-header page-break">
    <h2>III. Battlefield Command Interface</h2>
  </div>

  <!-- IN-GAME VECTOR SCHEMATIC: BATTLE VIEWPORT -->
  <div class="hud-schematic">
    <div class="schematic-header">
      <span class="schematic-title">TACTICAL BATTLE VIEWPORT HUD</span>
      <span class="schematic-screen">SCREEN 0 &bull; AUTHORITATIVE SIMULATION</span>
    </div>
    <div class="viewport-hud-top">
      <div class="hud-elem">[1] SUN CLOCK (60s)</div>
      <div class="hud-elem" style="color:#bb77ff;">[2] MANA: 300 &#9733;</div>
      <div class="hud-elem" style="color:#5cf25c;">[3] ACTIVE UNIT: BARBARIAN (HP: 10/10)</div>
    </div>
    <div class="viewport-grid-canvas">
      <div class="grid-label">&bull; TACTICAL BATTLEFIELD MAP (SCREEN 0) &bull;</div>
      <div class="grid-sub">Hexagonal Movement Terrain &bull; Line-of-Sight Fog &bull; Strategic Objectives</div>
      <div class="map-icons">
        <span class="icon-node" style="color:#bb77ff; border-color:#bb77ff;">&#9737; Stone Portal</span>
        <span class="icon-node" style="color:#5cf25c; border-color:#5cf25c;">&#9872; Settlement</span>
        <span class="icon-node" style="color:#ffd700; border-color:#ffd700;">&#9670; Mana Spring</span>
      </div>
    </div>
    <div class="viewport-hud-bottom">
      <div class="hud-elem" style="color:#48d1cc;">[4] STRUCTURES: 1 Portal &bull; 2 Villages &bull; 1 Tower</div>
      <div class="hud-elem" style="color:#e4ded4;">[5] CHAT: [Lobby: On] [Game: On] [Private: On] - Press TAB to Chat</div>
    </div>
  </div>

  <h3 class="sub-section">HUD Elements & Control Matrix</h3>
  <ul>
    <li><strong>[1] Sun / Moon Turn Clock:</strong> Upper-left corner. Indicates whose turn is active and ticks down the remaining turn time (default: 60 seconds).</li>
    <li><strong>[2] Mana Reserves (Stars):</strong> Tracks accumulated arcane energy required to teleport new units from portals onto the battlefield.</li>
    <li><strong>[3] Unit Inspector:</strong> Displays active unit level, hitpoints, combat strength, and movement capability.</li>
    <li><strong>[4] Structure Status Box:</strong> Displays the total number of settlements, portals, and towers held by your army versus opponent forces.</li>
    <li><strong>[5] Chat Console:</strong> Press <span class="key-badge">TAB</span> to chat; press <span class="key-badge">F10</span> to open Quick Chat presets.</li>
  </ul>

  <h3 class="sub-section">Camera & Navigation Controls</h3>
  <ul>
    <li><strong>Pan Map:</strong> Hold down the <span class="key-badge">Right Mouse Button</span> and drag, or use the <span class="key-badge">Arrow Keys</span>.</li>
    <li><strong>Select Unit:</strong> Left-click directly upon the unit's circular hexagonal base tile.</li>
    <li><strong>Movement & Attack Preview:</strong> Hover over any tile to preview the fastest path. Green highlighted tiles depict reachable movement squares; red tiles and battle swords indicate enemies within attack range.</li>
    <li><strong>Cycle Units:</strong> Left-click the upper globe button to cycle focus between unmoved units and active portals.</li>
    <li><strong>End Turn:</strong> Click the flashing <span class="ui-btn">END TURN</span> button once your maneuvers are completed.</li>
  </ul>

  <!-- ==================== CHAPTER 4: DIVINE PANTHEONS ==================== -->
  <div class="stone-header page-break">
    <h2>IV. The Divine Pantheons</h2>
  </div>
  <p>
    In every battle, commanders select three patron deities to support their campaign. As you recruit units aligned with a particular god, your divine favour with that deity rises, unlocking progressively more powerful holy champions and high-tier neutral mercenaries:
  </p>

  <div class="pantheon-grid">
    <div class="god-card god-saradomin">
      <h4>Saradomin (Good & Order)</h4>
      <p>Righteous god of light and wisdom. Specializes in divine armor, healing miracles, and resilient defense. Signature units: Paladins, White Knights, Monks, Centaurs, and the holy Icyene.</p>
    </div>
    <div class="god-card god-zamorak">
      <h4>Zamorak (Chaos & Wrath)</h4>
      <p>Mahjarrat god of discord. Prizes offensive ferocity, dark necromancy, poison, and demonic power. Signature units: Werewolves, Skeletons, Necromancers, Black Knights, and Greater Demons.</p>
    </div>
    <div class="god-card god-guthix">
      <h4>Guthix (Balance & Nature)</h4>
      <p>Primordial balancer of Gielinor. Manipulates natural mana, commands wild beasts, and gains amplified defense in forests. Signature units: Druids, Moss Giants, Terrorbirds, and ancient Ents.</p>
    </div>
    <div class="god-card god-seren">
      <h4>Seren (Crystal & Purity)</h4>
      <p>Crystalline patron goddess of elfkind. Leverages precision archery, nimble skirmish movement, and prism shapeshifting. Signature units: Elven Archers, Shieldmaidens, Elf Lords, and Shapeshifters.</p>
    </div>
    <div class="god-card god-bandos">
      <h4>Bandos (War & Devastation)</h4>
      <p>Big High War God of goblins, orks, and ogres. Overwhelms rivals through sheer physical strength and war beasts. Signature units: Goblins, Ogres, Orks, and colossal Ourg chieftains.</p>
    </div>
    <div class="god-card god-menaphos">
      <h4>Menaphite Pantheon (Desert)</h4>
      <p>Ancient gods Tumeken, Elidinis, and Icthlarin. Masters of desert attrition, mummy resurrection, and insect swarms. Signature units: Mummies, Jackals, Nomads, and the Kalphite Queen.</p>
    </div>
  </div>

  <!-- ==================== CHAPTER 5: STRUCTURES & TERRITORY ==================== -->
  <div class="stone-header">
    <h2>V. Structures & Territorial Control</h2>
  </div>

  <div class="stone-callout">
    <div class="ui-badge" style="background:#5a2010; border-color:#e05030; color:#ff8060;">THE BARBARIAN DOCTRINE</div>
    <p style="margin-top:6px;">
      Barbarians and Barbarian Chieftains are the <strong>only units in the game capable of capturing structures</strong>. If you lose all your barbarian bloodlines, you permanently lose the ability to capture new settlements or reclaim lost portals!
    </p>
  </div>

  <table class="stat-table">
    <thead>
      <tr>
        <th class="col-name" style="width: 22%;">Structure</th>
        <th style="width: 23%;">Capture Time</th>
        <th style="width: 32%;">Turn Income & Benefits</th>
        <th style="width: 23%;">Tactical Property</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td class="unit-name">Stone Portal</td>
        <td style="text-align:center;">1 Turn (Barbarian)</td>
        <td>Enables unit teleportation & reinforcements.</td>
        <td style="color:#ff6b6b;">Burns enemies for 3 DMG/turn.</td>
      </tr>
      <tr>
        <td class="unit-name">Village / Settlement</td>
        <td style="text-align:center;">2 Turns (Barbarian)<br>1 Turn (Chieftain)</td>
        <td>Generates Mana each round.<br>Heals resting units by 1 HP/turn.</td>
        <td style="color:#5cf25c;">Cures poison at turn start.</td>
      </tr>
      <tr>
        <td class="unit-name">Tower / Temple</td>
        <td style="text-align:center;">2 Turns (Barbarian)</td>
        <td>Grants extended line-of-sight vision and accelerates Victory Points.</td>
        <td style="color:#ffd700;">Heavy defensive cover bonus.</td>
      </tr>
    </tbody>
  </table>

  <!-- ==================== CHAPTER 6: COMBAT RULES ==================== -->
  <div class="stone-header page-break">
    <h2>VI. Tactical Combat & Terrain Rules</h2>
  </div>

  <p>
    Combat is engaged when an attacking unit moves adjacent to an enemy (or within ranged strike distance) and issues an attack command. Battle resolution calculates offensive strength against defensive resilience using weighted dice rolls:
  </p>
  <ul>
    <li><strong>Melee Engagements:</strong> The attacker rolls offensive dice based on their Strength rating. If the defender survives, they immediately execute a <em>Counter-Strike</em> using their defensive capability.</li>
    <li><strong>Ranged & Siege Volleys:</strong> Archers, spearwomen, and catapults attack from multiple tiles away (e.g. range 2–3 or 3–5). Defenders cannot counter-attack against ranged strikes unless they also possess adequate counter-range.</li>
    <li><strong>Healing & Field Medicine:</strong> Units that neither move nor attack during a round regain 1 HP at the start of the next turn (Resting). Priests, Monks, and Druids can heal adjacent allies by up to 3 HP per turn and neutralize toxic poisons.</li>
    <li><strong>Undead Decay:</strong> Skeletons, Mummies, and actively poisoned units lose 1 HP at the beginning of each turn. Skeletons and mummies can only stop their decay by occupying friendly portals.</li>
  </ul>

  <h3 class="sub-section">Terrain Modifiers Matrix</h3>
  <table class="stat-table">
    <thead>
      <tr>
        <th class="col-name" style="width: 25%;">Terrain Type</th>
        <th style="width: 35%;">Movement Effects</th>
        <th style="width: 40%;">Combat Modifiers & Restrictions</th>
      </tr>
    </thead>
    <tbody>
      <tr><td class="unit-name">Open Ground</td><td>Standard movement cost for all ground units.</td><td>Neutral combat terrain; no defensive bonuses.</td></tr>
      <tr><td class="unit-name">Cobblestone Roads</td><td style="color:#5cf25c;">Ground units move faster along established roadways.</td><td>No defensive cover; exposed to ranged ambushes.</td></tr>
      <tr><td class="unit-name">Dense Forests</td><td style="color:#ffd700;">Increased movement cost for non-Guthixian ground units.</td><td style="color:#5cf25c;">Defensive cover bonus. Guthix units receive double defense.</td></tr>
      <tr><td class="unit-name">Rivers & Seas</td><td style="color:#ff6b6b;">Impassable to standard foot troops.</td><td>Only Flying and Amphibious units may cross water.</td></tr>
      <tr><td class="unit-name">Rugged Mountains</td><td style="color:#ff6b6b;">Impassable to all terrestrial soldiers.</td><td>Only Flying units (Dragons, Aviansies, Owls) can traverse.</td></tr>
      <tr><td class="unit-name">Swamp Marshes</td><td style="color:#ff6b6b;">Severely impedes ground movement speed.</td><td>No cover; units take heavy penalties when ambushed.</td></tr>
      <tr><td class="unit-name">Fortified Walls</td><td>Blocks ground pathing.</td><td>Ranged arrows and magic volleys can shoot over walls.</td></tr>
    </tbody>
  </table>

  <!-- ==================== CHAPTER 7: UNIT COMPENDIUM & FIELD DOSSIERS ==================== -->
  <div class="stone-header page-break">
    <h2>VII. The Unit Compendium &amp; Tactical Dossiers</h2>
  </div>
  <p>
    The following intelligence compendium documents all 75 field-deployable combat units across the 7 Divine Pantheons, extracted directly from the authoritative battle simulation engine (<code>dn.Pb</code>, <code>bw.m</code>, <code>ut.cb</code>, and <code>pl.x</code>). Every unit features an individual <strong>Steckbrief</strong> (Tactical Dossier Card) containing authentic in-game portrait artwork, combat telemetry, special battlefield traits, and official lore briefings:
  </p>

$compendiumHtml

  <div class="stone-header page-break">
    <h2>Master Unit Reference Matrix</h2>
  </div>
  <p>
    The quick-reference index below catalogs all units and campaign commanders for tactical cross-referencing:
  </p>

  <table class="stat-table">
    <thead>
      <tr>
        <th class="col-name">Unit Name</th>
        <th>Faction</th>
        <th>Class / Movement</th>
        <th>SPD</th>
        <th>STR</th>
        <th>HP</th>
        <th>RNG</th>
        <th>Cost</th>
      </tr>
    </thead>
    <tbody>
$unitTableRows
    </tbody>
  </table>

  <!-- ==================== CHAPTER 8: WIN CONDITIONS ==================== -->
  <div class="stone-header page-break">
    <h2>VIII. Match Victory & Defeat Rules</h2>
  </div>

  <p>
    Multiplayer and skirmish battles in Armies of Gielinor are evaluated continuously by the authoritative battle engine. A match terminates and triggers the official Victory or Defeat declaration under the following four conditions:
  </p>
  <ul>
    <li>
      <strong style="color:#ffd700; font-family:'KingthingsPetrock', serif; font-size:12pt;">1. Total Portal Loss (Conquest & Barbarian Denial):</strong><br>
      Defined in <code>up.java</code>. If an opposing player loses control of all their portals, and simultaneously has zero surviving barbarians or barbarian chieftains on the battlefield, they are unable to ever capture or spawn units again. The server authoritatively declares them eliminated.
    </li>
    <li>
      <strong style="color:#ffd700; font-family:'KingthingsPetrock', serif; font-size:12pt;">2. Total Military Extinction:</strong><br>
      Defined in <code>mc.java</code>. If every single unit belonging to an army is slain in combat, that faction suffers total unit loss and is immediately vanquished from the war.
    </li>
    <li>
      <strong style="color:#ffd700; font-family:'KingthingsPetrock', serif; font-size:12pt;">3. Victory Point Domination:</strong><br>
      Controlling captured settlements, towers, and temples generates Victory Points at the end of every round. The first commander whose victory point gauge reaches the target match ceiling (e.g. 500, 1000, or 2000 VP) immediately secures decisive match victory.
    </li>
    <li>
      <strong style="color:#ffd700; font-family:'KingthingsPetrock', serif; font-size:12pt;">4. Turn Limit Expiration:</strong><br>
      When a battle reaches its preset maximum turn duration (e.g. 50 turns), sudden death evaluation occurs. The commander possessing the greatest cumulative score and structure tally is crowned the victor.
    </li>
  </ul>

  <!-- ==================== CHAPTER 9: CREDITS ==================== -->
  <div class="stone-header">
    <h2>IX. Historical Preservation & Credits</h2>
  </div>
  <p>
    <strong>Armies of Gielinor</strong> was originally conceived, designed, and published by <strong>Jagex Games Studio</strong> in 2010 on the FunOrb platform.
  </p>
  <p>
    This Standalone Steam Edition represents an enduring effort to preserve classic browser and applet strategy heritage for modern PC hardware, featuring native 64-bit packaging, Steam P2P relay tunneling, crisp high-DPI rasterization, and an authoritative multiplayer battle simulation engine.
  </p>
  <p style="color:#ffd700; text-align:center; font-family:'KingthingsPetrock', serif; font-size:14pt; margin-top:16px;">
    May your blades strike true and your portals burn bright across the sacred lands of Gielinor.
  </p>

  <div class="footer-vintage">
    Armies of Gielinor &bull; Standalone Edition v1.0 &bull; Published for Modern Windows & Steam P2P &bull; 2026
  </div>

</div>

</body>
</html>
"@

# 4. Write HTML file in pristine UTF-8
[System.IO.File]::WriteAllText($htmlPath, $htmlContent, [System.Text.Encoding]::UTF8)
Write-Host "Wrote updated HTML manual: $htmlPath"

# 5. Compile to PDF with Edge Headless
Write-Host "Compiling to PDF with Edge Headless..."
$candidateEdges = @(
    "C:\Program Files (x86)\Microsoft\EdgeCore\157.0.4341.60\msedge.exe",
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
)
$edgePath = $null
foreach ($e in $candidateEdges) {
    if (Test-Path $e) { $edgePath = $e; break }
}
if (-not $edgePath) {
    $edgePath = (Get-ChildItem -Path "C:\Program Files (x86)\Microsoft\Edge*" -Filter "msedge.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
}

Start-Process -FilePath $edgePath -ArgumentList @(
    "--headless",
    "--disable-gpu",
    "--no-pdf-header-footer",
    "--print-to-pdf=$pdfPath",
    $htmlPath
) -Wait

$pdfItem = Get-Item $pdfPath
Write-Host "Compiled PDF successfully! Size: $([math]::Round($pdfItem.Length / 1MB, 2)) MB"

# 6. Distribute PDF across workspace targets
Write-Host "Distributing Manual.pdf..."
Copy-Item $pdfPath (Join-Path $workspace "Manual.pdf") -Force
Copy-Item $pdfPath (Join-Path $workspace "extracted\Manual.pdf") -Force
Copy-Item $pdfPath (Join-Path $workspace "ArmiesOfGielinor\Manual.pdf") -Force

Write-Host "=== Manual Build and Distribution Complete! ==="
