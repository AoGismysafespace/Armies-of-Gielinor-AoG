# Armies of Gielinor — Standalone Steam Edition

[![Release](https://img.shields.io/badge/Release-v1.0.6-gold.svg)](https://github.com/alterorb/armies-of-gielinor/releases)
[![Platform](https://img.shields.io/badge/Platform-Windows%2064--bit-blue.svg)](#requirements)
[![Multiplayer](https://img.shields.io/badge/Multiplayer-Steam%20P2P%20Relay-brightgreen.svg)](#multiplayer-guide)
[![Java](https://img.shields.io/badge/Java%20Runtime-OpenJDK%2018%20(Bundled)-orange.svg)](#architecture)
[![Manual](https://img.shields.io/badge/Manual-31--Page%20Illustrated%20PDF-purple.svg)](Manual.pdf)

An enhanced standalone preservation and revival of Jagex's classic tactical turn-based strategy game **Armies of Gielinor** (originally released in 2010 on the FunOrb platform).

This Standalone Steam Edition integrates native 64-bit Windows packaging, Steamworks P2P multiplayer tunneling, modern high-DPI scaling, an authoritative multiplayer battle simulation engine, and a complete 31-page illustrated tactical manual with all 75 unit profile dossiers.

---

## Current Status: Version 1.0.6 (Golden Master / Publishing Release)

> [!NOTE]
> **Production Ready**: All core singleplayer campaigns and multiplayer battle loops operate with 100% parity to the original FunOrb design: lobby discovery, unit recruitment, unit movement, tactical combat duels, unit slaying, objective/VP win-condition triggers, and spectator map synchronization.

### Feature Highlights
- **Authoritative Multiplayer Battle Engine**: All 75 units (including Barbarian Chieftains, Aviansies, Black Knights, Dragons, and god champions) attack, counter-attack, roll combat dice, apply damage, and trigger combat strike animations in real time.
- **Disconnect-Free Unit Slaying**: Redundant server death packets (`hp`, opcode 194) are suppressed on lethal blows, eliminating the Jagex engine `"Can see death"` socket drop and disconnection veil.
- **Authoritative Match Concluding & Win Conditions**: Matches properly evaluate and trigger the authentic Victory/Defeat screen when:
  - An opponent loses all Portals and Barbarians (*Conquest & Barbarian Denial*).
  - An opponent suffers total army elimination (*Zero remaining units*).
  - A player achieves the target Victory Points (*VP*) threshold.
  - The maximum turn limit expires (*winner determined by highest score*).
- **Spectator Banner Auto-Dismissal & Map Scrolling**: During an opponent's turn, the stone announcement banner displays for ~1.5 seconds and automatically slides away (Packet 73 / 76 handshake with a 2.5s safety fallback). Waiting players enjoy unobstructed map visibility and free real-time map scrolling.
- **Steamworks P2P Lobby Discovery**: Automated lobby creation, Steam Friends list overlay integration, direct invite acceptance, and cold-launch join support via Steam URL (`+connect_lobby <lobbyID>`).
- **Complete Singleplayer Mode**: Full offline campaign, skirmish against AI, and army loadout customization without requiring an internet connection.
- **High-DPI Display & Screen Modes**: Crisp 1:1 pixel rendering on 4K, 1440p, and high-DPI scaling displays (Borderless Fullscreen, Resizable Windowed, and Classic Fixed 640x480).
- **Embedded Java 18 Runtime**: Self-contained OpenJDK 18.0.2.1 JRE included — zero system Java installation or PATH configuration needed.
- **31-Page Retro Game Manual (`Manual.pdf`)**: Complete illustrated tactical manual featuring RuneScape typography, vector interface schematics, master stat tables, and 75 unit profile dossiers (*Steckbriefe*) with authentic portrait artwork and special ability briefings.

---

## The Official Game Manual (`Manual.pdf`)

Included in the root of the game and this repository is [`Manual.pdf`](Manual.pdf) — a 31-page illustrated handbook designed in the style of classic strategy game manuals:

- **Chapter I: The God Wars & Historical Setting**: The Third Age background of Gielinor and the divine conflict between the gods.
- **Chapter II: Standalone Launch & Steam P2P Networking**: Step-by-step setup, multiplayer hosting, guest joining, and Steam invite routing with vector lobby schematics.
- **Chapter III: Battlefield Interface & Camera Navigation**: Comprehensive HUD breakdown (Sun Clock, Mana Reserves, Unit Inspector, Structure Tracker, Chat) and hotkey matrix.
- **Chapter IV: The Divine Pantheons**: Lore, combat doctrine, and signature champions for all 7 factions (*Neutral, Saradomin, Zamorak, Guthix, Bandos, Seren, Menaphos*).
- **Chapter V: Structures & Territorial Control**: Turn income, healing benefits, and the Barbarian capture doctrine.
- **Chapter VI: Tactical Combat & Terrain Rules**: Combat dice resolution, counter-strikes, archery ranges, field healing, undead decay, and the terrain modifier matrix.
- **Chapter VII: The Complete Unit Compendium & Tactical Dossiers**:
  - **75 Individual Steckbrief Dossier Cards**: Organized across all 7 Pantheons, each featuring authentic unit portrait artwork extracted directly from the game's binary cache (`pl.x` / `unitheads`).
  - **Special Abilities & Mechanics**: Detailed explanations for units with active spells and unique battlefield mechanics:
    - *Portal Mage*: `Deploy Temporary Stone Portal` (forward reinforcement node creation).
    - *Dagannoth*: `Amphibious Air-Interception` (ocean/river crossing and drag-down aerial combat).
    - *Crystalline Shape-shifter*: `Burrow Assassination` (underground execution).
    - *Ourg & Pyrelord*: `Mountain Demolition` & `Forest Incineration` (permanent terrain destruction).
    - *Sphinx & Sea Slug*: `Mind Subjugation` & `Parasitic Hijack` (permanent tactical enemy control).
    - *Cyclops & Ogre Shaman*: `Gaze of Dread` (movement paralysis) & `Smog Shroud` (ranged missile denial).
    - *Barbarians*: `Territorial Conquest` (the only units capable of capturing structures).
  - **Master Unit Reference Matrix**: Quick-reference table cataloging all 83 units and campaign boss commanders.
- **Chapter VIII: Match Victory & Defeat Rules**: Detailed analysis of the 4 authoritative win conditions.
- **Chapter IX: Historical Preservation & Credits**: Credits and archival documentation.

---

## Quick Start Guide

### 1. Requirements
- **Operating System**: Windows 10 or Windows 11 (64-bit).
- **Steam Client**: Steam must be running in the background for multiplayer matchmaking and friend invites (uses Steam AppID `480`).
- **No Extra Dependencies**: An embedded, isolated OpenJDK 18 runtime is bundled directly inside the game package.

### 2. How to Play Multiplayer
1. **Host**:
   - Launch `ArmiesOfGielinor.exe`.
   - On the launcher screen, select **Host** and click **Play**.
   - In the main menu, navigate to **Custom Game** -> **Create Room**.
   - Press **F2** or open the **Steam Overlay** (`Shift + Tab`) to invite friends from your Steam Friends list.
2. **Guest (Friend)**:
   - Accept the Steam game invitation from your chat window, or right-click your friend in Steam and click **Join Game**.
   - When joining via Steam invite, the game client launches into the **Main Menu**. Click on **Multiplayer** (or **Custom Game**) to enter the lobby — the Steam P2P relay will automatically route you straight into the host's room!
3. **Starting the Match**:
   - The host chooses battlefield options (turn time, map size, victory points, game mode).
   - Once players have selected their factions and marked ready, the host clicks **Start Game**.
   - All players seamlessly transition into the tactical battle map.

---

## Controls Reference

| Control | Action |
|:---|:---|
| <kbd>Right Mouse Button</kbd> + Drag | Pan tactical battlefield camera |
| <kbd>Arrow Keys</kbd> | Scroll camera across map |
| <kbd>Left Click</kbd> | Select unit base tile / Issue move or attack command |
| <kbd>Hover</kbd> | Preview movement route (green) and attack range (red) |
| <kbd>TAB</kbd> | Toggle multiplayer chat console |
| <kbd>F10</kbd> | Open Quick Chat preset menu |
| <kbd>F2</kbd> / <kbd>Shift + Tab</kbd> | Open Steam Overlay & Friend Invites |
| Globe Icon | Cycle focus between unmoved units and active portals |
| <kbd>END TURN</kbd> | Finish moves and pass initiative to the next commander |

---

## Repository Structure

```text
├── Manual.pdf                # 31-page illustrated tactical reference manual
├── README.md                 # Project overview and player instructions
├── LICENSE.md                # Licensing and preservation disclaimer
├── CHANGELOG.md              # Version milestones and technical changelog
├── CONTRIBUTING.md           # Developer guidelines & bytecode rules
├── .gitignore                # Git ignore configuration
├── release/
│   ├── ArmiesOfGielinor_v1.0.6.zip  # Pre-built complete standalone release archive (41 MB)
│   └── RELEASE_NOTES_v1.0.6.md      # GitHub Releases markdown template
├── manual/
│   ├── manual.html           # Raw HTML manual source with embedded vector schematics
│   ├── build_manual.ps1      # Edge headless PDF compiler script
│   ├── assets/unit_icons/    # 75 transparent in-game unit portrait sprites
│   └── fonts/                # Authentic RuneScape and FunOrb TTF fonts
├── src/
│   ├── AoGBattleBridge.java  # Authoritative battle engine pump & win condition monitor
│   ├── AoGServer.java        # TCP multiplayer server with spectator sync
│   ├── GenerateUnitCompendium.java # Unit Steckbrief HTML generator
│   └── RestoreAndVerify.java # Bytecode compatibility auditor & injector
├── tools/
│   ├── build_release.ps1     # Automated packaging script with bytecode verification
│   └── build_manual.ps1      # Manual compilation script
└── docs/
    └── ARCHITECTURE_AND_SYMBOLS.md # Rosetta Stone obfuscation mapping
```

---

## Technical Architecture & Patches

For developers and technical preservationists, key components include:

- **`AoGBattleBridge.java`**: Implements `net.alterorb.server.BattleEngine`. Features a dynamic FIFO battle event pump that broadcasts Packet 68 combat strikes (`opcode 0x60`), performs scoped suppression of redundant fatal-blow `hp` packets to avoid client socket drops, and monitors win conditions continuously.
- **`AoGServer.java`**: Multiplayer host server handling TCP lobbies and turn coordination. Patched to handle Opcode 73 (*Client Turn Ready*) and broadcast Packet 76 (*turn unpause & Vb sync*) with a 2.5s fallback so spectator banners dismiss smoothly and map scrolling unlocks.
- **`Hook.java`**: Intercepts Jagex client file system requests and redirects them to persistent local cache directories.
- **`RestoreAndVerify.java`**: Scans all 719 classes in `ArmiesOfGielinor.jar` to ensure 100% compliance with class major version $\le 62.0$ (Java 18).

---

## Legal & Historical Preservation Disclaimer

**Armies of Gielinor** is the intellectual property of **Jagex Games Studio** (originally published in 2010 on the FunOrb platform).

This project is a non-commercial, educational historical preservation effort designed to preserve classic web and applet gaming heritage for modern personal computers. All custom launcher wrappers, bridge interfaces, tools, and documentation are provided under the MIT License.
