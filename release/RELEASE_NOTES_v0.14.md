# Armies of Gielinor – Standalone Steam Edition v1.0.14

**Armies of Gielinor v1.0.14** introduces tactical AI heraldry and skirmish Coat of Arms randomization, giving every computer opponent an authentic, unique crest on summoning portals, fortress banners, and turn announcements while preserving human player crests and story campaign scenarios.

---

### What's New in v1.0.14

1. **Tactical AI Heraldry & Skirmish Coat Randomizer**
   - Eliminated the legacy 2009 fallback behavior where all computer opponents in Skirmish / Sandbox matches duplicated the human player's custom crest and colors (`rd.j.xb.T[i] = ah.a`).
   - Integrated `net.alterorb.launcher.CoatManager`, which dynamically generates valid, distinct combinations of shield bases, heraldic bearers, banners, toppers, and emblems across the 32-color palette for all bot slots.
   - Every computer opponent in a 2-, 4-, 6-, or 8-player battle now displays their own distinctive flag and crest.

2. **Strict Engine-Native Display Boundary**
   - All AI crests are strictly confined to the game's authentic display locations:
     - **Summoning Portals:** Enemy summoning portals on the map display the bot's unique crest.
     - **Fortress Flags:** Captured structures and fortresses fly the bot's individual banner.
     - **Stone Turn Banner:** The announcement banner at the start of a bot's turn displays its crest.
     - **Victory & Scoreboard Screen:** End-of-game scoreboards reflect each combatant's heraldry.
   - Zero artificial menus, HUD widgets, or custom overlays were added.

3. **Player & Story Campaign Preservation**
   - The human player's personal profile crest (Slot 0) is 100% preserved.
   - Singleplayer Story Campaign scenarios remain untouched (story bosses retain their fixed scenario crests).

4. **Full Domino & Regression Suite Verified**
   - Preserved all previous milestone protections: the Match-2 display lifecycle fix (v1.0.13), Direct3D hardware acceleration and 512 MB – 1 GB fixed heap (v1.0.12), AI frame-slicing optimization (v1.0.11), 20-minute inactivity watchdog (`AntiIdleWatchdog`), post-combat turn retention, spectator banner synchronization, and the one-click diagnostic bug exporter (F9).

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.14`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.14.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
