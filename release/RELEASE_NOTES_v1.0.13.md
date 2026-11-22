# Armies of Gielinor – Standalone Steam Edition v1.0.13

**Armies of Gielinor v1.0.13** permanently eliminates the Match-2 display freeze bug, ensuring smooth, uninterrupted visual rendering across multiple consecutive matches and automatically self-healing legacy screen resolution configuration files.

---

### What's New in v1.0.13

1. **Elimination of the Match-2 Screen Freeze Bug**
   - Fixed the critical issue where completing one skirmish or campaign battle and opening a second match caused the visual map display to freeze completely while game audio, clicks, and menus continued in the background.
   - Refactored fullscreen and windowed mode transitions in `DisplayManager.java` to operate entirely without tearing down native window peers (`jFrame.dispose()`).
   - The game's native drawing surface (AWT Canvas) and graphics pipeline remain permanently valid and intact across consecutive matches, preventing the internal render loop from crashing silently.

2. **Accurate Screen Size & Resolution Calculation**
   - Corrected the screen resolution calculation logic so window dimensions are read directly from the active display area rather than FunOrb's internal fixed-HUD scaling flag (`ra.e.h`).
   - Prevents the launcher from inadvertently overwriting user preferences with `Windowed - Fixed (640 x 480)`.

3. **Automatic Resolution Configuration Self-Healing**
   - Added automatic detection and sanitization for legacy configuration files (`player.properties`).
   - Any broken `Windowed - Fixed (640 x 480)` setting is automatically sanitized and upgraded to `Fullscreen (Native)` on launch, ensuring returning players never encounter the frozen display state.

4. **Single-Pass Atomic Bytecode Injection**
   - Upgraded `RestoreAndVerify` to inject and audit all patched bytecode classes in a single atomic file archive operation, eliminating file lock contention on Windows systems.

5. **Full Domino & Regression Suite Verified**
   - Preserved all previous milestone protections: Direct3D hardware acceleration, 512 MB – 1 GB fixed heap (v1.0.12), AI frame-slicing optimization (v1.0.11), 20-minute inactivity watchdog (`AntiIdleWatchdog`), post-combat turn retention, spectator banner synchronization, and the one-click diagnostic bug exporter (F9).

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.13`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.13.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
