# Armies of Gielinor – Standalone Steam Edition v1.0.27

**Armies of Gielinor v1.0.27** fixes a critical display freeze where the screen could visually hang on "Loading extra data - 75%" on certain PC hardware or audio setups, while the game was actually running normally underneath. It also permanently fixes the 640x480 resolution reversion trap, ensuring that Fullscreen and custom resolutions are strictly preserved across launches.

---

### What's New in v1.0.27

1. **Eliminated "Loading Extra Data - 75%" Display Freeze**
   * **Permanent Canvas Peer Protection (`DisplayManager`)**: Removed the obsolete browser applet signal (`tq.a`) that tore down and recreated the drawing canvas during window mode changes. The game's software rasterizer now retains an unbroken connection to the window surface, guaranteeing smooth 60 FPS drawing throughout the entire startup and match lifecycle.
   * **Active Asset Loading Guard**: Added a fail-open safeguard that suppresses any automatic mid-boot window mode changes while game archives and music files are unpacking.

2. **Permanent Resolution Reversion Fix (`player.properties`)**
   * **Deactivated Legacy 50ms Polling Loop (`StandaloneRunner`)**: Neutralized the outdated polling loop that previously misread the game's initial 480p HUD setup during boot as a user command to shrink to 640x480. Your chosen resolution (including Fullscreen Native) is now 100% stable and will never be overwritten.
   * **Canvas Alignment Lock**: Fixed vertical canvas letterbox calculations in Fullscreen mode, ensuring the game board is always perfectly positioned and never shifted down.
   * **Player Controls Intact**: All manual controls (F11 / Alt+Enter hotkey, in-game Options menu, and window border dragging) remain completely functional.

3. **Enhanced Diagnostics Suite (`BugReporter`)**
   * Updated the automated Bug Reporter to v1.0.27 with dynamic runtime resolution tracking, fail-safe audio device enumeration, and unconditional loading stage introspection.

> **Multiplayer Tip:** Make sure **everyone in your lobby — Host and joining Steam friends — downloads and runs `v1.0.27`** for seamless compatibility.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.27.zip`** below (or access directly in `Aktuell/`).
2. Extract the ZIP folder anywhere on your PC (or run directly from `Aktuell/ArmiesOfGielinor/`).
3. Keep **Steam** running in the background for multiplayer and Steam display names.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
