# Armies of Gielinor – Standalone Steam Edition v1.0.15

**Armies of Gielinor v1.0.15** introduces tactical AI pantheons and skirmish god selection diversity, giving every computer opponent an authentic, randomized roster of 3 distinct gods (Saradomin, Zamorak, Guthix, Seren, Bandos, Menaphite) with an interactive 1-click batch toggle to switch between Random and Mirrored (2009) modes.

---

### What's New in v1.0.15

1. **Tactical AI Pantheon & Skirmish God Selection Diversity (Option A - Default)**
   - Eliminated the legacy 2009 limitation where all computer opponents in Skirmish / Sandbox matches automatically inherited and mirrored the human player's 3 chosen gods (`rd.j.xb.g(...)`).
   - Integrated `net.alterorb.launcher.GodManager`, which dynamically rolls 3 distinct, randomized gods from all 6 available pantheons for every computer opponent in offline and multiplayer skirmishes.
   - Battles feature genuine tactical diversity: encounter Bandos ork-and-goblin warbands, Seren elven archers and spellcasters, Zamorakian demons and necromancers, or Menaphite desert horrors in different enemy armies.
   - **Full Mid-Match Progression Support:** When AI commanders recruit units at captured portals and temples, their god levels naturally increase and are fully preserved across all match turns.

2. **1-Click Mode Toggle (`KI-Götter_Umschalten.bat` / `Toggle_AI_Gods.bat`)**
   - Added dedicated, bilingual 1-click batch utilities in the root directory and game folder allowing instant switching per double-click:
     - `[1] Random Gods (Option A - Maximum Diversity)` [Default]
     - `[2] Mirrored Gods (Option B - Original 2009 Behavior)`
   - Automatically persists the choice across launches in `player.properties` (`ai_gods_mode=random` or `mirror`).

3. **Player & Story Campaign Preservation**
   - The human player's selected gods (Slot 0) remain 100% untouched.
   - Singleplayer Story Campaign scenarios remain untouched (story bosses retain their fixed scenario pantheons).

4. **Full Domino & Regression Suite Verified**
   - Preserved all previous milestone protections: the tactical AI heraldry & crest randomizer (v1.0.14), Match-2 display lifecycle fix (v1.0.13), Direct3D hardware acceleration and 512 MB – 1 GB fixed heap (v1.0.12), AI frame-slicing optimization (v1.0.11), 20-minute inactivity watchdog (`AntiIdleWatchdog`), post-combat turn retention, spectator banner synchronization, and the one-click diagnostic bug exporter (F9).

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.15`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.15.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
