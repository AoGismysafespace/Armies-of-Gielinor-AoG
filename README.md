# Armies of Gielinor — Standalone Steam Edition

[![Release](https://img.shields.io/badge/Release-v1.0.27-gold.svg)](https://github.com/alterorb/armies-of-gielinor/releases)
[![Platform](https://img.shields.io/badge/Platform-Windows%2064--bit-blue.svg)](#requirements)
[![Multiplayer](https://img.shields.io/badge/Multiplayer-Steam%20P2P%20Relay-brightgreen.svg)](#multiplayer-guide)
[![Java](https://img.shields.io/badge/Java%20Runtime-OpenJDK%2018%20(Bundled)-orange.svg)](#architecture)
[![Manual](https://img.shields.io/badge/Manual-31--Page%20Illustrated%20PDF-purple.svg)](Manual.pdf)

An enhanced standalone preservation and revival of Jagex's classic tactical turn-based strategy game **Armies of Gielinor** (originally released in 2010 on the FunOrb platform).

This Standalone Steam Edition integrates native 64-bit Windows packaging, Steamworks P2P multiplayer tunneling, modern high-DPI scaling, an authoritative multiplayer battle simulation engine, and a complete 31-page illustrated tactical manual with all 75 unit profile dossiers (*Steckbriefe*).

---

## Current Status: Version 1.0.27 (Boot Display Freeze & Resolution Reversion Permanent Fix)

> [!NOTE]
> **Production Ready**: All core singleplayer campaigns and multiplayer battle loops operate with 100% parity to the original FunOrb design: lobby discovery, unit recruitment, unit movement, tactical combat duels, multi-unit action persistence, unit slaying, objective/VP win-condition triggers, and spectator map synchronization. Make sure everyone in your lobby (both the **Host** and joining **Steam friends**) is running **v1.0.27**.

### Feature Highlights
- **Boot Display Freeze & Resolution Reversion Permanent Fix (v1.0.27)**: Permanently resolved the issue where the display visually froze at "Loading extra data - 75%" on certain hardware or audio configurations while the game continued running underneath. Removed the destructive Canvas teardown signal (`tq.a`) from `DisplayManager` and neutralized the legacy 50ms polling loop in `StandaloneRunner.main` that misread FunOrb's initial 480p HUD state as a user request to downscale to 640x480, completely eliminating the resolution overwrite trap in `player.properties`.
- **Steam Invite Stability & Black-Screen Crash Elimination (v1.0.26)**: Eliminated the fatal black-screen crash (`error_game_crash`) during Steam friend joins by neutralizing a rogue 3-second background watchdog in `StandaloneRunner.main`, synchronizing loading modal dismissals with the applet thread, and adding defensive null guards to the widget traversal iterator (`bb.class`). Also added automatic Steam P2P session recycling on handshake timeout (`SteamManager$4`), ensuring rock-solid lobby joins without process hangs.
- **Steam Invitation Lobby & Room Auto-Join (v1.0.25)**: Resolved an issue where accepting a Steam invitation from a friend while sitting on the Main Menu did not place the player into the host's lobby room. The launcher now smoothly executes the transition from the title screen into the multiplayer lobby, and `AoGServer` automatically seats joining remote friends straight into the host's room with zero manual clicks required.
- **Online Multiplayer God Selection & Coat of Arms Fix (v1.0.24)**: Removed unintended AI bot randomization from `AoGBattleBridge.startMatch()` so Steam friends choose their own 3 Gods freely at match start instead of getting random gods assigned. Added `CoatManager` reflection parity for loading and auto-saving Coats of Arms in `player.properties`, automatic 12-byte heraldry delivery in Steam P2P `OP_OPEN` handshakes, and strict `AoGServer` host-vs-guest Coat of Arms isolation so every player displays their own authentic heraldry across portals, fortress flags, and turn banners.
- **Multiplayer Lobby Option Mapping & Atomic Launch Fix**: Eliminated the recurring 1-in-3 "Start Game" crash and disconnect caused by misreading Multiplayer Lobby room options (especially treating default *Misthalin* maps as a random roll over non-existent themes). All 6 room settings (Turn Time `30s..240s`, Map Size `Small..Epic`, Game Length `Regular..Quadruple`, Map Theme `Misthalin/Desert/Snow/Swamps`, Game Type `FFA/Team`, and Trinket Limit) now map 1-to-1, and match generation completes atomically before tearing down the lobby room.
- **Multiplayer Host Launch Linkage Fix**: Resolved the runtime `NoSuchMethodError` crash occurring when the lobby host clicks "Start Game" in a multiplayer room. Restored strict binary bytecode descriptor parity (`void`) for the authoritative battle engine coordinator (`AoGBattleBridge`), guaranteeing smooth, instant transitions into battle for all players.
- **Skirmish AI Heraldry & Pantheon Slot Inversion Fix**: Completely resolved the turn-order heraldry and god assignment inversion occurring in Play against Computer (Skirmish) battles. By dynamically inspecting active game controllers (`ic.Eb`), the game now reliably distinguishes human players from computer bots regardless of whether the Jagex matchmaking engine places the player first (Slot 0) or second (Slot 1). Human players are guaranteed 100% preservation of their custom-created heraldic crest and chosen gods in every match, while only computer bots receive randomized crests and diverse pantheons.
- **Unified Options Launcher (`Enhanced_Options.bat`)**: Consolidated all tactical Skirmish enhancements into a single, clean English interactive launcher menu (`Enhanced_Options.bat`), removing redundant legacy scripts.
- **Achievement UI Cleanliness & Obsolete Login Elimination**: Removed the obsolete 2009 web portal `"Log in / Create account"` button from the Main Menu Achievements screen. Standalone players save all 56 achievements and match progress locally and automatically without requiring any online Jagex account, perfectly mirroring the in-match Achievements screen with a single, centered `"Menu"` button.
- **Privatized Offline Identity & Commander Fallback**: Automatic identity sanitization via `ProfileManager` ensures that standalone/offline sessions cleanly default to the neutral title `"Commander"`, completely purging obsolete test placeholders while preserving personal Steam display names when Steam is running.
- **Complete Top-Right Turn Order Heraldry Synchronization**: AI opponents in Skirmish battles now cleanly display their distinct randomized crests next to their names in the top-right turn order bar, dynamically refreshing the HUD sprite cache on match start to achieve 100% visual consistency with their fortress flags, portals, and turn banners.
- **Enhanced Options Configuration & Multi-Option Selector (`enhanced_options.txt` / `Enhanced_Options.bat`)**: Introduced an English-only configuration file and 1-click interactive batch menu enabling independent customization of modern Skirmish tactical enhancements:
  - `ai_gods_mode` (Random diverse pantheons vs Mirrored 2009 player-matching gods).
  - `ai_coats_mode` (Random unique bot crests/flags vs Mirrored 2009 uniform crests).
  - Quick presets: Toggle individual options, enable all enhancements, or restore 100% original 2009 rules in one keystroke.
- **Tactical AI Pantheon & Skirmish God Selection Diversity (Option A - Default)**: In Skirmish battles, computer opponents now roll 3 distinct, randomized gods from all 6 available pantheons (Saradomin, Zamorak, Guthix, Seren, Bandos, Menaphite), fielding diverse army factions while preserving human player choices, mid-match god leveling, and story campaigns. Configurable via `Enhanced_Options.bat` to seamlessly switch between Random and Mirrored (2009) modes.
- **Tactical AI Heraldry & Skirmish Coat Randomizer**: Computer opponents in Skirmish matches now receive unique, randomized heraldic crests across shield shapes, banners, toppers, and emblems, appearing authentically on their map portals, fortress flags, and turn banners while preserving human player crests and story campaigns.
- **Safe Display & Window Lifecycle (Match-2 Freeze Elimination)**: Eliminated native window disposal during fullscreen and windowed transitions, keeping the graphics pipeline permanently valid across consecutive matches and self-healing legacy broken 640x480 resolution settings.
- **Direct3D Hardware Acceleration & Optimized Memory**: Enabled native Windows Direct3D GPU acceleration for sprites, translucent fog of war, and animations, coupled with a pre-allocated 512 MB – 1 GB fixed heap in `ArmiesOfGielinor.cfg` that eliminates in-game Windows memory expansion pauses.
- **AI Turn Frame-Slicing Optimization**: Evaluates threat flood-fills across map tiles in smooth 25-tile slices per video frame, reducing peak frame hitches by over 94% and eliminating Windows "Not Responding" application freezes during multi-AI matches on Large and Huge maps.
- **Anti-Idle Inactivity Protection**: The launcher harness automatically prevents the legacy 20-minute inactivity timer from closing the socket, allowing players to pause, step away, or leave offline skirmishes and multiplayer sessions running indefinitely without unexpected disconnects.
- **Settings Framing & Telemetry Handshake**: Adjusting game speed, audio levels, or graphics settings in the Options menu cleanly passes telemetry packets through the server without triggering stream desynchronization or launching unclosable multiplayer lobbies.
- **Rock-Solid Post-Combat Turn Retention**: Fixed all remaining multiplayer situations where a player's turn could automatically end right after units finished fighting (queued multi-turn paths, mid-fight turn timer unpauses, and 3–4 player counterattack knockouts).
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
│   ├── ArmiesOfGielinor_v1.0.11.zip # Latest complete standalone release archive (recommended)
│   ├── ArmiesOfGielinor_v1.0.9.zip  # Archived v1.0.9 release
│   ├── ArmiesOfGielinor_v1.0.8.zip  # Archived v1.0.8 release
│   ├── ArmiesOfGielinor_v1.0.7.zip  # Archived v1.0.7 release
│   ├── ArmiesOfGielinor_v1.0.6.zip  # Archived v1.0.6 release
│   ├── RELEASE_NOTES_v1.0.11.md     # GitHub Releases markdown notes for v1.0.11
│   ├── RELEASE_NOTES_v1.0.9.md      # Archived v1.0.9 release notes
│   ├── RELEASE_NOTES_v1.0.8.md      # Archived v1.0.8 release notes
│   └── RELEASE_NOTES_v1.0.6.md      # Archived v1.0.6 release notes
├── manual/
│   ├── manual.html           # Raw HTML manual source with embedded vector schematics
│   ├── build_manual.ps1      # Edge headless PDF compiler script
│   ├── assets/unit_icons/    # 75 transparent in-game unit portrait sprites
│   └── fonts/                # Authentic RuneScape and FunOrb TTF fonts
├── src/
│   ├── AoGBattleBridge.java  # Authoritative battle engine pump, combat ID tags & win monitor
│   ├── AoGServer.java        # TCP multiplayer server, combat pause deferral & spectator sync
│   ├── BattleEngine.java     # Clean interface boundary between AoGServer and AoGBattleBridge
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
