# Armies of Gielinor – Standalone Steam Edition v1.0.24

**Armies of Gielinor v1.0.24** fixes two critical online Steam multiplayer bugs: joining Steam friends can now select their own Gods at the start of every match, and every player's custom Coat of Arms displays accurately across lobby slots, portals, fortress flags, and turn banners.

---

### What's New in v1.0.24

1. **Steam Friends Can Choose Their Own Gods (`AoGBattleBridge.startMatch`)**
   - Removed unintended Skirmish/AI randomization calls (`GodManager.randomizeBotGods` and `CoatManager.randomizeBotCoats`) from the online multiplayer match builder.
   - All human players — both the Host and every connected Steam friend across 2-player and 4-player rooms — now open the God Selection Wheel and choose their own 3-God pantheon before battle begins.

2. **Steam Friends' Custom Coats of Arms Saved, Sent & Displayed (`CoatManager`, `SteamP2PTunnel`, `AoGServer`)**
   - **Startup Load & Auto-Save Parity (`CoatManager` / `StandaloneRunner.flushCoatOfArms`)**: Replaced broken cross-package reflection (missing `setAccessible(true)` on Coat Editor fields) so every player's custom Coat of Arms loads cleanly into the game at startup and auto-saves edits to `player.properties` without ever touching server state when joining a friend's room.
   - **Automatic Steam P2P Heraldry Transfer (`SteamP2PTunnel` `OP_OPEN`)**: Joining Steam friends now automatically transmit their saved 12-byte Coat of Arms inside the Steam P2P tunnel handshake (`OP_OPEN`) before lobby login — even if they never opened the in-game Coat Editor during the current session.
   - **Strict Host-vs-Guest Isolation (`AoGServer`)**: Replaced permissive login heuristics with strict `isHostClient` validation (loopback socket, non-tunnel origin, first host claim, and matching host name) so Steam friends can never overwrite or inherit the Host's Coat of Arms (even when connecting first or sharing a display name), and coatless clients cleanly fall back to neutral default heraldry.

> **Multiplayer Tip:** Since matches are coordinated by whoever creates the room (*Host & Play*) and joining friends send their heraldry automatically on connect, make sure **everyone in your lobby — Host and joining Steam friends — downloads and runs `v1.0.24`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.24.zip`** below (or access directly in `Aktuell/`).
2. Extract the ZIP folder anywhere on your PC (or run directly from `Aktuell/ArmiesOfGielinor/`).
3. Keep **Steam** running in the background for multiplayer and Steam display names.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
