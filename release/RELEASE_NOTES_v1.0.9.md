# Armies of Gielinor — Standalone Steam Edition v1.0.9

**Armies of Gielinor v1.0.9** fixes an issue where adjusting options in the Settings menu (such as changing game speed, sound, or display settings) could suddenly pull players out of the menu, open the online multiplayer lobby, and freeze on a loading screen.

---

### What's Fixed in v1.0.9

1. **Settings Adjustments No Longer Trigger Ghost Multiplayer Lobbies**
   - Fixed an issue where changing game speed, audio levels, or graphics settings in the Options menu sent housekeeping signals that confused the multiplayer host.
   - The host now recognizes and smoothly processes all settings and telemetry signals, keeping the connection perfectly synchronized and preventing the game from mistakenly launching you into an unclosable multiplayer room.
2. **Safe & Clean Host Architecture**
   - Implemented cleanly within our host server layer, preserving singleplayer campaign progress, save games, and all existing multiplayer combat and turn rules 100% intact.
3. **One-Click Diagnostic Bug Reporter Included**
   - Includes the built-in F9 hotkey diagnostic reporter and standalone desktop shortcut to instantly capture game state and technical logs if any unexpected behavior occurs.

> **Multiplayer Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group — especially the Host — downloads and runs `v1.0.9`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.9.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
