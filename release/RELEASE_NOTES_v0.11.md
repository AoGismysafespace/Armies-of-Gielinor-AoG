# Armies of Gielinor – Standalone Steam Edition v1.0.11

**Armies of Gielinor v1.0.11** optimizes AI turn calculations during large-scale skirmishes (4, 6, and 8-player matches), completely eliminating the multi-second application freezing ("Not Responding") that previously occurred during computer opponents' turns on Large and Huge maps.

---

### What's Fixed in v1.0.11

1. **Eliminated Multi-AI Application Freezing & Stutter on Large / Huge Maps**
   - In 6-player and 8-player matches on Large (1,600 tiles) and Huge (3,600 tiles) maps, the original 2009 AI decision loop evaluated structure threats across all map tiles inside a single video frame on the main graphical display thread.
   - On Large maps, this caused a 1.3-second screen freeze per computer player; on Huge maps, it froze the game for up to 9.3 seconds per computer player (adding up to over a minute of total unresponsive window lockup per round where Windows reported "Not Responding").
   - Implemented smooth frame-slicing in the AI controller (`ej.stepPhase2Slice`): the map threat evaluation is now processed in small chunks of 25 tiles per video tick.
   - Peak frame time is reduced by over **94%** (from 9,300 ms down to under 600 ms in worst-case scenarios, and under 100 ms on Large maps).
   - The game window remains interactive and responsive, animations and turn banners glide smoothly, and Windows never flags the application as unresponsive.
2. **100% Identical AI Strategic Intelligence & Tactical Choices**
   - The slicing algorithm evaluates the exact same tile sets in the exact same order with the exact same scoring weights.
   - Computer players do not lose strategic awareness, do not overlook distant portals or structures, and make identical tactical moves.
3. **Full Domino & Regression Suite Verified**
   - Preserved all previous milestone protections: the 20-minute inactivity guard (`AntiIdleWatchdog`), post-combat manual End Turn retention, spectator banner synchronization, and the one-click diagnostic bug exporter (F9).

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.11`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.11.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
