# Armies of Gielinor – Standalone Steam Edition v1.0.17

**Armies of Gielinor v1.0.17** brings complete visual heraldry synchronization to the top-right player turn order HUD (`ic.k(-32400)`), ensuring that each computer opponent's custom randomized Coat of Arms appears accurately across all in-game displays: captured summoning portals, fortress flags, turn-transition banners, and the turn order HUD list.

---

### What's New in v1.0.17

1. **Top-Right Turn Order HUD Heraldry Synchronization**
   - In previous versions, computer opponents in Skirmish battles displayed their distinct randomized crests on captured summoning portals, fortress flags, and turn-transition banners, but the top-right turn order list still showed generic or default crests due to internal sprite caching on match start (`ic.Wb`).
   - `CoatManager` now actively invalidates the HUD turn order sprite cache (`ic.Wb = null`) upon detecting AI coat randomization or a match transition.
   - The game client's rendering loop (`ic.e(16)`) dynamically regenerates the turn order crest sprites on the very next frame with zero thread contention or visual stutter.
   - The crest displayed next to each AI player's name in the turn order list now 100% matches their banner, flag, and portal heraldry.

2. **Full Option & Toggle Support**
   - Respects all settings in `enhanced_options.txt`:
     - When `ai_coats_mode=random`, distinct unique crests are generated and displayed on both the map and the top-right turn order list.
     - When `ai_coats_mode=mirror`, the top-right turn order list cleanly reflects the original 2009 mirrored crest behavior.

3. **Player & Story Campaign Preservation**
   - The human player's custom crest and selected gods (Slot 0) remain 100% preserved.
   - Singleplayer Story Campaign scenarios (`ha.K != null`) strictly preserve their authentic hand-crafted crests across both the battlefield and turn order HUD.

4. **Full Domino & Regression Suite Verified**
   - Preserved all previous milestone protections: enhanced options configuration (v1.0.16), tactical AI pantheon diversity (v1.0.15), Match-2 display lifecycle fix (v1.0.13), Direct3D hardware acceleration and 512 MB – 1 GB fixed heap (v1.0.12), AI frame-slicing optimization (v1.0.11), 20-minute inactivity watchdog (`AntiIdleWatchdog`), post-combat turn retention, spectator banner synchronization, and the one-click diagnostic bug exporter (F9).

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.17`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.17.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
