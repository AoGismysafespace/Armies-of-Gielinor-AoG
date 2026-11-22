# Armies of Gielinor – Standalone Steam Edition v1.0.26

**Armies of Gielinor v1.0.26** fixes a critical black-screen crash (`error_game_crash`) and connection hang that could occur when accepting a Steam game invitation. With thread-synchronized screen transitions, defensive widget protection, and automatic Steam P2P timeout recycling, joining friends now experience seamless and rock-solid entry into multiplayer lobbies.

---

### What's New in v1.0.26

1. **Eliminated Black-Screen Crash (`error_game_crash`) on Steam Invite Join**
   - **Rogue Lobby Watchdog Removal (`StandaloneRunner`)**: Deactivated the recurring 3-second background reload timer in the multiplayer lobby, completely eliminating unwanted background screen tear-downs while waiting in a lobby room.
   - **Thread-Safe Modal Dismissals**: Synchronized loading modal cleanups and screen switches with the main applet lifecycle, ensuring background threads never alter the active UI hierarchy while the graphics engine is actively rendering frames.
   - **Defensive Widget Traversal Shield (`bb.class`)**: Added protective null checks to the widget list iterator. If any interface element is unlinked during a transition, the game safely finishes the frame instead of throwing an unhandled error or turning the screen black.

2. **Automatic Steam P2P Timeout Recovery (`SteamManager`)**
   - **Instant Session Recycling**: If an initial Steam networking handshake times out or encounters a brief relay delay, the P2P session is immediately and cleanly reset so subsequent connection attempts succeed without getting stuck.

3. **Complete Multiplayer & Singleplayer Preservation**
   - Singleplayer campaigns, skirmishes, AI bot pantheons, God selection wheels, custom coats of arms, and all previous multiplayer features remain 100% functional and preserved.

> **Multiplayer Tip:** Make sure **everyone in your lobby — Host and joining Steam friends — downloads and runs `v1.0.26`** for the smoothest invitation experience.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.26.zip`** below (or access directly in `Aktuell/`).
2. Extract the ZIP folder anywhere on your PC (or run directly from `Aktuell/ArmiesOfGielinor/`).
3. Keep **Steam** running in the background for multiplayer and Steam display names.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
