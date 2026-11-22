# Armies of Gielinor – Standalone Steam Edition v1.0.25

**Armies of Gielinor v1.0.25** resolves an online multiplayer issue where accepting a Steam game invitation from a friend did not automatically place the player into the host's lobby room. Joining Steam friends now enter the host's multiplayer room immediately upon accepting an invite, with zero manual menu navigation required.

---

### What's New in v1.0.25

1. **Automatic Steam Invite Lobby & Room Entry (`StandaloneRunner` & `AoGServer`)**
   - **Seamless Menu-to-Room Transition**: When accepting a Steam invitation while sitting on the Main Menu, the game now smoothly and automatically transitions the client straight into the multiplayer room instead of leaving the player waiting on the title screen.
   - **Immediate Host Welcome (`AoGServer.handleEnterLobby`)**: The host server automatically registers joining remote Steam friends into the multiplayer lobby and seats them directly into the host's room as soon as the connection is established.
   - **Flexible Connection Timing (`AoGServer.createRoom`)**: If a friend connects just before the host creates the room, the host server automatically gathers all waiting lobby guests and places them into the new room the moment it is opened.
   - **Redundant Click Protection**: Subsequent manual clicks or delayed lobby packets are safely ignored so players are never accidentally removed from their active room.

2. **Complete Multiplayer & Singleplayer Preservation**
   - Singleplayer campaigns, skirmishes, AI bot pantheons, God selection wheels, and custom coats of arms remain 100% untouched and fully functional.

> **Multiplayer Tip:** Make sure **everyone in your lobby — Host and joining Steam friends — downloads and runs `v1.0.25`** for the smoothest invitation experience.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.25.zip`** below (or access directly in `Aktuell/`).
2. Extract the ZIP folder anywhere on your PC (or run directly from `Aktuell/ArmiesOfGielinor/`).
3. Keep **Steam** running in the background for multiplayer and Steam display names.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
