# Armies of Gielinor – Standalone Steam Edition v1.0.16

**Armies of Gielinor v1.0.16** introduces the **Enhanced Options** configuration system, providing a clean English text file (`enhanced_options.txt`) and an interactive menu utility (`Enhanced_Options.bat`) to independently customize modern Skirmish tactical enhancements, making both AI Pantheon diversity and AI Heraldry / Coat of Arms randomization fully toggle-able.

---

### What's New in v1.0.16

1. **Dedicated Enhanced Options Configuration (`enhanced_options.txt`)**
   - Added a clean, fully documented, English-only configuration file (`enhanced_options.txt`) located in the root game directory and user profile folder.
   - Provides clear text toggles for Skirmish AI enhancements:
     - `ai_gods_mode=random` (Each AI opponent rolls 3 distinct, diverse gods across all 6 pantheons)
     - `ai_coats_mode=random` (Each AI opponent generates a unique Coat of Arms for portals, flags, and turn banners)
   - Setting either option to `mirror` instantly restores the original 2009 Jagex behavior where computer opponents duplicate the human player's choices.

2. **Toggle-able AI Heraldry / Coat of Arms**
   - The Coat of Arms randomizer (introduced in v1.0.14) is now fully configurable alongside AI pantheons.
   - Players who prefer the nostalgic 2009 visual experience of computer opponents copying the human player's heraldic crest can switch `ai_coats_mode=mirror` at any time without losing random AI gods or performance optimizations.

3. **Interactive Multi-Option Selector (`Enhanced_Options.bat`)**
   - Added an English-only interactive batch utility in the root directory and game folder:
     - `[1] Toggle AI Gods Selection` (Random <--> Mirrored)
     - `[2] Toggle AI Heraldry / Crests` (Random <--> Mirrored)
     - `[3] Enable ALL Enhanced Features` (Recommended)
     - `[4] Restore Original 2009 Rules`
   - Automatically synchronizes settings across local files and user profile stores.

4. **Player & Story Campaign Preservation**
   - The human player's custom crest and selected gods (Slot 0) remain 100% preserved.
   - Singleplayer Story Campaign matches preserve their hand-crafted story scenario crests and pantheons.

5. **Full Domino & Regression Suite Verified**
   - Preserved all previous milestone protections: tactical AI pantheon diversity (v1.0.15), Match-2 display lifecycle fix (v1.0.13), Direct3D hardware acceleration and 512 MB – 1 GB fixed heap (v1.0.12), AI frame-slicing optimization (v1.0.11), 20-minute inactivity watchdog (`AntiIdleWatchdog`), post-combat turn retention, spectator banner synchronization, and the one-click diagnostic bug exporter (F9).

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.16`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.16.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
