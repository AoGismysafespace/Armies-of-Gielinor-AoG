# Armies of Gielinor — Standalone Steam Edition v1.0.10

**Armies of Gielinor v1.0.10** removes the legacy 20-minute inactivity disconnect, ensuring offline skirmishes against computer opponents and online multiplayer matches can remain unattended or paused for as long as you want without kicking you back to the main menu.

---

### What's Fixed in v1.0.10

1. **No More Inactivity Disconnects in Offline & Online Matches**
   - In the original 2009 web browser game, an automatic 20-minute timer kicked players to the title screen if no mouse or keyboard actions were detected:
     > *"We closed the connection because the game was left unattended for 20 minutes. Please feel free to reconnect immediately if you are there."*
   - In modern standalone play, this interrupted players who stepped away from an offline skirmish against computer opponents or took a break during a match.
   - Added a silent background guard to the launcher wrapper that gently resets the engine's inactivity counter before the 20-minute mark is ever reached.
   - You can now safely pause, step away for a phone call, or leave a match running unattended for hours or overnight without fear of disconnection.
2. **Zero Original Game Bytecode Modified**
   - The anti-idle guard runs cleanly inside our custom desktop launcher wrapper (`net.alterorb.launcher.AntiIdleWatchdog`), leaving all 2009 original campaign rules, save games, and combat mechanics 100% untouched.
3. **One-Click Diagnostic Bug Reporter Updated**
   - Synchronized the built-in F9 hotkey and standalone diagnostic exporter to report Version 1.0.10 metadata.

> **Multiplayer Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group — especially the Host — downloads and runs `v1.0.10`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.10.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
