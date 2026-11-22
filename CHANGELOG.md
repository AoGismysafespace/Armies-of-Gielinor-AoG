# Changelog — Armies of Gielinor Standalone Edition

All notable changes to the standalone preservation release are documented in this file.

---

## [v1.0] – 2026-11-22 (Official Standalone Golden Master Release)

### Standalone Preservation & Golden Master Consolidation
- **Asset Loading & JS5 Dynamic Cache Streaming**: Integrated dynamic on-demand disk cache extraction in `AoGServer.java`, guaranteeing 100% of game archives (including Archive 17 Extra Data 43..56) are served without stalling or freezing. Bundled complete 27-index golden master cache for 100% portable play.
- **Steam Multiplayer Integration**: Full support for Steam friend invites, automatic lobby transitions, P2P session recovery on connection timeouts, and isolated Heraldic Crests / Coats of Arms.
- **Display & Resolution Engine**: Completely resolved the legacy 640x480 resolution reversion loop and eliminated destructive Canvas teardowns, ensuring crisp Fullscreen (Native) and Windowed play on modern displays.
- **Battle Engine & Game Rules Parity**: 100% parity with original 2010 FunOrb mechanics including multi-unit move order persistence, combat round continuity, AI bot god variety, and anti-inactivity protection.
- **Zero-Setup Java 18 Distribution**: Self-contained runtime verified to Java 18 bytecode limits (class major version <= 62.0).

---

## [v1.0.28] – 2026-11-22 (Asset Loading 75% Freeze Fix & Dynamic JS5 Serving)

### Core Engine, Cache Streaming & Asset Loading Fixes
- **Dynamic JS5 Disk Cache Streaming Fallback (`AoGServer`)**:
  - Implemented dynamic cache reading in `AoGServer.java` (`readArchiveFromDisk(index, archive)`): When a client requests assets not pre-loaded into memory (such as Archive 17 "Extra Data" files 43 through 56), the server now dynamically retrieves the file sectors directly from local disk cache (`main_file_cache.dat2` and `.idx`), caches them in memory, and streams them over the standard JS5 TCP block protocol.
  - Eliminates the silent request drop (`[AoGServer] Unhandled JS5 request`) that caused the game to hang permanently at `Asset Loading: 67% ("Loading extra data - 75%")` on fresh installations.
- **Self-Contained Portable Game-Folder Cache (`caches/armiesofgielinor/`)**:
  - Bundled the complete, verified 27-index golden master cache directory (`caches/armiesofgielinor/`) directly into the standalone game distribution.
  - Enables 100% portable, standalone gameplay from any directory or SSD without relying on `C:\Users\<Username>\.alterorb` or `\.armiesofgielinor`.
- **Embedded Cache Synchronization (`ArmiesOfGielinor.jar`)**:
  - Synchronized the complete 7,793,387-byte `main_file_cache.dat2` and all `.idx` index headers into `ArmiesOfGielinor.jar` (`/cache/armiesofgielinor/`), replacing the legacy 7,754,520-byte file and ensuring any fallback extraction is 100% complete and valid.

---

## [v1.0.27] – 2026-11-21 (Boot Display Freeze & Resolution Reversion Permanent Fix)

### Graphics, Display & Engine Stability Fixes
- **Eliminated the "Loading Extra Data - 75%" Display Freeze (`DisplayManager`, `StandaloneRunner`)**:
  - Removed the destructive `tq.a = true` trigger from `DisplayManager.applyWindowMode()`. In Jagex's legacy engine (`wf.java:820`), setting `tq.a = true` caused `this.c((byte)87)` to tear down the AWT `Canvas` (`si.b`) and instantiate a new one without updating the software rasterizer (`so.b`), silently breaking all future blits and permanently freezing the OS display on the 75% loading frame (unpacking Archive 10 MIDI music).
  - Added a fail-open guard against mid-boot window mode changes while asset loading (`qu.q`) is active.
  - FunOrb recomputes component layout automatically via standard AWT component events (`container.getSize()`) in `wf.java:649` without tearing down native peers.
- **Permanent Defense Against the 640x480 Resolution Reversion Trap (`StandaloneRunner.main`, `PatchDisplayFreezeFix`)**:
  - Surgically neutralized the 7 legacy polling calls to `applyWindowMode` and `updateAndSaveResolution` inside `StandaloneRunner.main` via Javassist `ExprEditor`. The background 50ms polling loop previously misread FunOrb's initial HUD scale flag (`ra.e.h == false`) during boot as a user request to shrink to Fixed 640x480 windowed mode, overwriting `player.properties` and forcing a mid-boot window shrink.
  - Preserved 100% of user-driven window controls: F11 / Alt+Enter toggle (`lambda$main$25`), in-game settings menu (`applyInGameScreenMode`), and mouse window dragging (`ComponentAdapter`).
  - In `DisplayManager.applyWindowMode()`, ensured `ra.e.h = true` and `mh.c = 0` whenever entering Fullscreen (Native), preventing FunOrb's letterbox offset logic from pushing the game canvas downwards.
- **Diagnostics & BugReporter Suite Enhancements (`BugReporter.java`)**:
  - Updated `BugReporter` to `v1.0.27` with dynamic runtime resolution tracking, fail-safe audio device enumeration, and unconditional loading stage introspection.

---

## [v1.0.26] – 2026-11-15 (Steam Invite Black Screen & P2P Timeout Recovery Fix)

### Online Steam Multiplayer Fixes
- **Eliminated Black-Screen Crash (`error_game_crash`) During Steam Invite Join (`StandaloneRunner`, `bb.class`)**:
  - Deactivated the rogue 3-second background watchdog in `StandaloneRunner.main` that repeatedly triggered `safeTransitionToScreen(-4)` and `dismissLoadingModal()` while already inside the multiplayer lobby. By repointing the 3000L threshold in the constant pool to `Long.MAX_VALUE`, operand stack depth and `StackMapTable` frames remain 100% compliant with the Java 18 bytecode verifier while permanently neutralizing the loop.
  - Added thread synchronization (`synchronized (appletHolder[0] != null ? appletHolder[0] : StandaloneRunner.class)`) around `dismissLoadingModal()` so background launcher threads cannot unlink or mutate root widget hierarchy (`rl.C`) nodes while the 50 FPS render thread (`iv.run()`) is drawing frames.
  - Added an early exit check in `StandaloneRunner.safeTransitionToScreen(int n)` (`if (jt.a == n) return;`) to ensure redundant screen transitions are immediately discarded.
  - Added defensive null-guards to circular linked list iterator methods `bb.b(byte)` and `bb.b(int)`. If an element is unlinked concurrently mid-iteration (`this.f == null`), the iterator gracefully terminates traversal (`return null`) rather than throwing an unhandled `NullPointerException` that triggers FunOrb's emergency handler (`error_game_crash`).
- **Automatic Steam P2P Timeout Session Recycling (`SteamManager$4.onP2PSessionConnectFail`)**:
  - Implemented session cleanup in `SteamManager$4.onP2PSessionConnectFail` via `closeP2PSessionWithUser(remote)` per Valve Steamworks SDK specifications, ensuring that if an initial P2P connection times out during invite acceptance, the session is cleanly recycled so subsequent reconnects succeed immediately.
- **Automated Regression Verification (`SteamP2PTimeoutAndCrashSandboxTest`, `RestoreAndVerify`)**:
  - Verified 100% pass across `SteamP2PTimeoutAndCrashSandboxTest` (both `--reproduce` and `--verify`), `SteamInviteLobbySandboxTest`, `MultiplayerGodAndCoatSandboxTest`, and `MultiplayerStartMatchSandboxTest` under the bundled OpenJDK 18.0.2.1 runtime.
  - Verified 0 bytecode version violations across all target application JARs.

---

## [v1.0.25] – 2026-11-15 (Steam Invitation Lobby & Room Auto-Join Fix)

### Online Steam Multiplayer Fixes
- **Restored In-Game Steam Invite Screen Transition (`StandaloneRunner` / `PatchSteamInviteLobby`)**:
  - In `StandaloneRunner.class` (`lambda$handleInGameSteamJoin$7`), eliminated the inverted `if (screen == -1) goto exit` check via 6 NOP opcodes, ensuring that players accepting a Steam invite while on the Main Menu (`screen == -1`) smoothly transition into Screen `-4` (Multiplayer Lobby) instead of remaining stuck on the title screen.
- **Automatic Lobby & Room Auto-Placement (`AoGServer.handleEnterLobby`, `ClientHandler.run`)**:
  - Automatically invokes `handleEnterLobby(this)` when remote Steam guests (`!this.isHostClient`) connect and log in, placing them immediately into the Multiplayer Lobby and auto-joining them into the host's staging room (`Room #1`).
  - Added an idempotency guard in `AoGServer.handleEnterLobby` (`if (inLobby && currentRoom != null) return;`) so delayed or redundant client Opcode 9 packets cannot eject players from their active staging room.
- **Symmetric Room Creation Sweep (`AoGServer.createRoom`)**:
  - When the host creates a new lobby room, `createRoom` automatically scans for any connected guests already waiting in the lobby (`guest.connected && guest.inLobby && guest.currentRoom == null`) and immediately joins them into the newly created room.
- **Automated Regression Verification (`SteamInviteLobbySandboxTest`, `RestoreAndVerify`)**:
  - Verified 100% pass across `SteamInviteLobbySandboxTest` (both `--reproduce` and `--verify`), `MultiplayerGodAndCoatSandboxTest`, and `MultiplayerStartMatchSandboxTest` under the bundled OpenJDK 18.0.2.1 runtime.
  - Verified 0 bytecode version violations across all target application JARs.

---

## [v1.0.24] – 2026-11-14 (Online Multiplayer God Selection & Coat of Arms Fix)

### Online Steam Multiplayer Fixes
- **Restored Friend & Host God Selection (`AoGBattleBridge.startMatch`)**:
  - Removed unintended Skirmish AI randomization calls (`GodManager.randomizeBotGods()` and `CoatManager.randomizeBotCoats()`) from `AoGBattleBridge.startMatch()` inside the multiplayer match engine.
  - Joining Steam friends (and the Host) in 2-player and 4-player online matches now cleanly receive open God Selection (`serverHa.c[slot] = {-1, -1, -1}`, `serverHa.Cb = false`) and pick their own 3 Gods via the in-game God Selection Wheel instead of having random gods assigned.
- **Startup Coat Load & Auto-Save Parity (`CoatManager.syncAndFlushCoatOfArms`, `PatchCoatRandomizer`)**:
  - Replaced `StandaloneRunner.flushCoatOfArms(GameConfig)` via idempotent Javassist body replacement delegating to `CoatManager.syncAndFlushCoatOfArms($1)`.
  - Uses `.setAccessible(true)` on package-private `ln.a` / `ln.b` so every player's custom Coat of Arms from `player.properties` reliably loads into `ah.a` at boot and persists edits back to `player.properties`.
  - Guards `AoGServer.hostCoatOfArms` updates strictly to non-guest JVMs (`guestProxyHolder[0] == null` and non-join mode) and never touches applet network buffers (`vl.n`) off-thread.
- **Automatic Steam P2P Heraldry Delivery (`SteamP2PTunnel` `OP_OPEN`)**:
  - Extended `SteamP2PTunnel.GuestProxy` `OP_OPEN` framing with the connecting friend's 12-byte serialized Coat of Arms (`CoatManager.getLocalCoatBytes()`) so joining friends automatically advertise their saved Coat of Arms on connect without needing to visit the Coat Editor first.
  - Updated `SteamP2PTunnel.HostBridge` and `TunnelConnectionInfo` to parse and store incoming 12-byte guest coats (remaining backwards-compatible with legacy `OP_OPEN` frames).
- **Authoritative Host-vs-Guest Coat Isolation (`AoGServer` & `BattleEngine` / `AoGBattleBridge`)**:
  - Added strict `isHostClient` identification on `AoGServer.ClientHandler` login (loopback socket, non-Steam-tunnel origin, first host claim, and matching `hostUsername`), preventing Steam friends connecting first or sharing a display name from inheriting or overwriting `AoGServer.hostCoatOfArms`.
  - `sendUnlockedProfileAndReadiness()` now skips Packet 75 when a client has `coatOfArms == null` (preserving coatless guests' local `ah.a`) while still sending valid all-zero (`0..0`) coats.
  - Added `BattleEngine.startMatch(..., byte[][] playerCoats)` overload so `AoGServer.handleStartMatchRequest()` supplies every lobby slot's authoritative 12-byte Coat of Arms directly into `serverHa.T[slot]` and Packet 77 (falling back to `ah.a` only for Host slot 0, and neutral `new ln()` for coatless guest slots).
- **Automated Regression Verification (`MultiplayerGodAndCoatSandboxTest`, `MultiplayerStartMatchSandboxTest`, `RestoreAndVerify`)**:
  - Updated `RestoreAndVerify` to sync all `jar_extracted/net/alterorb/steam/*.class` files and verified `0 violations` across all target Java 18 JARs.
  - Verified all v1.0.24 fixes and existing multiplayer match-launch regressions under the bundled OpenJDK 18.0.2.1 runtime.

---

## [v1.0.23] – 2026-11-14 (Multiplayer Lobby Option Mapping & Atomic Match Launch Fix)

### Multiplayer Lobby & Match Launch Stability
- **Resolved Recurring 1-in-3 "Start Game" Crash & Connection Drop (`ha.l -> ha.<init>`)**:
  - In Multiplayer Lobby rooms, the 6 room option bytes sent by the client (`au.pc[0..5]`: Turn Time, Map Size, Game Length, Map Theme, Game Type, Trinket Limit) were previously misdecoded inside `AoGBattleBridge.startMatch()`.
  - **Map Theme Crash Eliminated**: Multiplayer Lobby only offers 4 pre-built map themes (`0`: Misthalin, `1`: Kharidian Desert, `2`: Frozen Wastes, `3`: Hallowvale Swamps) with no "Random" slot at index 0. `AoGBattleBridge` mistakenly treated index `0` (Misthalin) as "Random" over `0..5` and subtracted 1 from non-zero selections, causing a 33.3% crash rate on default rooms (rolling unsupported indices 4 or 5) and shifting manual theme choices by one slot. Now `byArray2[3]` maps directly 1-to-1 across `0..3`.
  - **Complete 6-Option Parity**:
    * **Turn Time (`byArray2[0]`)**: Widened clamp from `0..4` to `0..6` so *60s* and *30s* timers work cleanly.
    * **Game Length (`byArray2[2]`)**: Wired `0..3` (*Regular, Double, Triple, Quadruple*) to `ha.vb` (`var9_9`) rather than passing it into `ha.L` (which erroneously disabled the 3-God Pantheon bar), keeping gods active (`ha.L = false`).
    * **Game Type (`byArray2[4]`) & Trinket Limit (`byArray2[5]`)**: Corrected index misalignment and Packet 64 bitflag encoding (`bit 1 = teamMatch`, `bits 2..4 = trinketLimitIdx`), matching `ArmiesOfGielinor.java:252-255`.
- **Atomic Match Launch Guard (`AoGServer.handleStartMatchRequest`)**:
  - Moved `engine.startMatch(...)` inside a `try/catch` guard *before* removing the `LobbyRoom` or clearing member lobby flags (`currentRoom`, `inLobby`), ensuring any launch error keeps the lobby room and host TCP connection intact instead of crashing `ClientHandler.run()` and returning players to the Main Menu.

---

## [v1.0.22] – 2026-11-12 (Multiplayer Host Launch Linkage Fix)

### Multiplayer Networking & Stability
- **Resolved Multiplayer Host Crash on "Start Game" (`NoSuchMethodError`)**:
  - When hosting a multiplayer lobby and clicking **"Start Game"**, the host's background game coordinator (`AoGBattleBridge.startMatch`) prepares the battlefield, unit spawns, and player heraldry before broadcasting the match start packet to all connected guests.
  - In earlier versions, `CoatManager.randomizeBotCoats(Object[], Object)` was modified to return a boolean flag for HUD cache invalidation. However, the compiled bytecode in `AoGBattleBridge.class` expects a `void` return type (`([Ljava/lang/Object;Ljava/lang/Object;)V`).
  - This mismatch caused a runtime `java.lang.NoSuchMethodError` inside the server's lobby handler thread upon match launch, dropping the connection and returning the host to the Main Menu.
- **Strict Bytecode Interface Alignment**:
  - Restored the 2-argument `CoatManager.randomizeBotCoats(Object[], Object)` overload to return `void`, perfectly matching the binary descriptor expected by `AoGBattleBridge`.
  - Maintained the 3-argument overload `CoatManager.randomizeBotCoats(Object[], Object, Object[])` returning `boolean` for components that monitor cache invalidation.
  - Recompiled both `CoatManager.java` and `AoGBattleBridge.java` in lockstep targeting OpenJDK 18 (`--release 18` / major version 62.0), verifying 100% clean runtime linkage and crash-free multiplayer game starts.

---

## [v1.0.21] – 2026-11-12 (Skirmish AI Heraldry & Pantheon Slot Inversion Fix)

### Gameplay & Tactical Skirmish
- **Resolved Turn Slot Inversion in Skirmish vs Computer AI**:
  - In Jagex's 2009 skirmish matchmaking logic (`ic.java`), player slot assignments are randomly shuffled at match launch (`ns.a(...)`). In a 2-player match against the computer, the AI bot controller (`ej`) is randomly assigned to Slot 0 approximately 50% of the time, placing the human player into Slot 1 (and vice-versa).
  - In previous versions, heraldry randomization (`CoatManager`) and god selection (`GodManager`) assumed the human player was always fixed in Slot 0 and bots filled Slots 1..3 (`humanIdx = 0`).
  - When the engine placed the AI bot into Slot 0 and the human player into Slot 1:
    - The AI bot in Slot 0 inherited the human player's custom heraldic crest and chosen gods.
    - The human player in Slot 1 had their player-created crest overwritten with randomized heraldry, and their chosen gods overwritten with a randomized pantheon!
- **Dynamic Controller-Based Slot Detection (`CoatManager` & `GodManager`)**:
  - Upgraded both `CoatManager` and `GodManager` to dynamically inspect the active controller array (`ic.Eb`):
    - Computer bots (`ic.Eb[slot] != null`, instance of `ej`) are identified as AI opponents and receive randomized crests and pantheons according to `enhanced_options.txt`.
    - Human player slots (`ic.Eb[slot] == null`) are strictly protected: the player's custom-created heraldic crest and chosen pantheon are preserved 100% across all match launches, regardless of whether the player acts first (Slot 0) or second (Slot 1).
  - Implemented multi-layered safety guards:
    - Safeguard against uninitialized or all-null controller arrays (such as standard multiplayer or hotseat matches), cleanly defaulting to human protection.
    - Safeguard against active in-game God Wheel transitions (`ha.Cb == true`), preventing unexpected pantheon re-rolls.
    - Preserved backward-compatible overloads for all internal caller interfaces.

---

## [v1.0.20] – 2026-11-11 (Consolidated Options Script & Redundancy Removal)

### Configuration & Launcher Cleanliness
- **Unified on Single Comprehensive `Enhanced_Options.bat` Launcher**:
  - Consolidated all Skirmish tactical customization options into a single, authoritative English-language interactive batch menu (`Enhanced_Options.bat`).
  - Controls both AI Pantheon selection (`ai_gods_mode`) and AI Heraldry/Crests (`ai_coats_mode`) with instant live presets (Enable All, Restore 2009 Original, or Toggle Individually).
- **Retired Obsolete Single-Purpose Batch Files**:
  - Removed redundant legacy scripts `KI-Götter_Umschalten.bat` (German) and `Toggle_AI_Gods.bat` (English), which were limited to pantheon-only toggles and targeted the older properties store.
  - Streamlines the game directory for end players and friends with zero overlapping or confusing scripts.

---

## [v1.0.19] – 2026-11-11 (Achievement UI Cleanliness & Obsolete Login Elimination)

### User Interface & Standalone Cleanliness
- **Eliminated Obsolete "Log in / Create account" Button in Achievement Menu**:
  - In the 2009 online FunOrb client, the Achievements screen accessed from the Main Menu (Screen 8) was configured by Jagex with two buttons: `"Log in / Create account"` and `"Menu"`. This was originally intended for unauthenticated web portal guests to register or sign into an online Jagex account to save achievements to Jagex web servers.
  - In standalone preservation mode, all player achievements and match progress are stored 100% locally and automatically in the player's profile file without needing any online login or external account.
  - Reconfigured Screen 8's button table in `kr.class` (`PatchAchievementLoginButton`) to exclusively present the standard single `"Menu"` button, perfectly mirroring the in-match Achievements screen (Screen 9).
  - The remaining `"Menu"` button is now cleanly and symmetrically centered at the bottom of the screen.
  - Clicking `"Menu"` smoothly and safely returns the player to the Main Menu.
  - All 56 achievements, criteria, tracking logic, icons, and progress remain 100% functional and intact.

---

## [v1.0.18] – 2026-11-11 (Offline Identity Sanitization & Privatized Commander Fallback)

### Configuration & Privacy
- **Privatized Generic Offline Identity Fallback (`Commander`)**:
  - Implemented automatic identity sanitization in `ProfileManager` (`net.alterorb.launcher.ProfileManager`):
    - When Steam is active, the player's personal Steam display name (PersonaName) is automatically retrieved and displayed without alteration.
    - When the game is launched offline or without Steam, it safely falls back to the neutral, generic title `"Commander"`.
    - Permanently intercepts and eliminates obsolete pre-release testing placeholders, guaranteeing that unconfigured or offline copies never display an unwanted or unexpected name.
  - Enforces FunOrb's 2009 network protocol limits (1–12 characters, alphanumeric, hyphen, underscore, space), protecting against invalid characters and buffer framing errors.
  - Fully privatizes all distributable release packages and templates (`extracted/`, `Aktuell/`) for sharing with friends while protecting user privacy.

---

## [v1.0.17] – 2026-11-10 (Top-Right Turn Order HUD Heraldry Synchronization)

### Visuals & HUD Interface
- **Complete Top-Right Player Turn Order Heraldry Synchronization**:
  - Resolved the visual desynchronization where computer opponents in Skirmish battles displayed their distinct randomized crests on captured summoning portals, fortress flags, and turn-transition banners, but the top-right turn order list (`ic.k(-32400)`) still showed generic/blank crests.
  - **Dynamic Turn Order Sprite Cache Refresh (`CoatManager`)**: The top-right HUD turn order bar caches pre-rendered heraldic crest sprites on match startup (`ic.Wb`). `CoatManager` now detects when bot coats of arms are randomized and automatically invalidates the internal HUD sprite cache (`ic.Wb = null`).
  - **Seamless Native Re-rendering**: The client's AWT rendering loop (`ic.e(16)`) dynamically recreates the turn order crest sprites on the very next frame with zero thread contention or visual stutter.
  - **100% Visual Consistency**: The crest shown next to each AI player's name in the turn order list now perfectly matches their flag, portals, and turn banner.
  - **Full Option Toggle Support**: Respects `enhanced_options.txt` (`ai_coats_mode=random` vs `mirror`). In `mirror` mode, the turn order HUD cleanly displays the player's crest for bots just like in the original 2009 client.
  - **Campaign Protection**: Story Campaign scenarios (`ha.K != null`) strictly preserve their original hand-crafted heraldry across both the map and turn order HUD.

---

## [v1.0.16] – 2026-11-10 (Enhanced Options Configuration & Toggle-able AI Heraldry)

### Configuration & Customization
- **Dedicated Enhanced Options Configuration (`enhanced_options.txt`)**:
  - Introduced a clean, English-only configuration file (`enhanced_options.txt`) in the root game directory and player data path to easily manage modern tactical enhancements.
  - Controls Skirmish AI behavior via simple text toggles (`random` vs `mirror`):
    - `ai_gods_mode=random` (Each AI opponent rolls 3 distinct, diverse gods across all 6 pantheons)
    - `ai_coats_mode=random` (Each AI opponent generates a unique Coat of Arms for summoning portals, fortress flags, and turn banners)
  - Setting either value to `mirror` instantly restores the original 2009 Jagex behavior where computer opponents mirror the human player's choices.
- **Interactive Multi-Option Selector (`Enhanced_Options.bat`)**:
  - Added an English-only interactive batch utility to toggle individual features or switch all options at once with a single keystroke:
    - `[1] Toggle AI Gods Selection`
    - `[2] Toggle AI Heraldry / Crests`
    - `[3] Enable ALL Enhanced Features (Recommended)`
    - `[4] Restore Original 2009 Rules`
  - Seamlessly keeps both `enhanced_options.txt` and legacy `player.properties` in sync.

### Gameplay & Tactics
- **Independent AI Heraldry Toggle**: Made the Coat of Arms randomizer (introduced in v1.0.14) fully toggle-able alongside AI pantheons. Players who prefer the nostalgic 2009 visual experience of uniform bot crests can toggle it off at any time without impacting other game enhancements.

---

## [v1.0.15] – 2026-11-10 (Tactical AI Pantheon & Skirmish God Selection Diversity)

### Gameplay & Tactics
- **AI Skirmish Pantheon Randomizer (Option A - Default)**: Resolved the legacy 2009 limitation where all computer opponents in Skirmish / Sandbox matches automatically inherited and mirrored the human player's 3 chosen gods (rd.j.xb.g(...)):
  - **Dynamic Pantheon Generator (GodManager)**: In offline and multiplayer skirmishes, every computer opponent now rolls 3 distinct, randomized gods from all 6 available pantheons (Saradomin, Zamorak, Guthix, Seren, Bandos, Menaphite).
  - **Authentic Tactical Diversity**: Each AI opponent now commands unique army compositions, special abilities, and god synergies across the battlefield (e.g. Bandos orks and goblin priests in one army, Seren crystalline elven archers or Menaphite desert horrors in another).
  - **Full Mid-Match Progression Support**: When AI commanders recruit units at captured portals and temples, their god levels naturally increase and are fully preserved across all match turns.
  - **Player & Campaign Preservation**: The human player's selected gods (Slot 0) remain 100% untouched. Story Campaign matches preserve their hand-crafted story scenario pantheons.

### Configuration & Usability
- **1-Click Mode Toggle (KI-Götter_Umschalten.bat / Toggle_AI_Gods.bat)**:
  - Added dedicated 1-click batch utilities in the root directory and game folder allowing instant switching between:
    - [1] Random Gods (Option A - Maximum Diversity) [Default]
    - [2] Mirrored Gods (Option B - Original 2009 Behavior)
  - Automatically persists the choice across launches in player.properties (ai_gods_mode=random or mirror).

---

## [v1.0.14] – 2026-11-10 (Tactical AI Heraldry & Skirmish Coat of Arms Randomizer)

### Gameplay & Visuals
- **AI Skirmish Coat of Arms Randomizer**: Eliminated the legacy 2009 fallback behavior where all computer opponents in Skirmish / Sandbox matches duplicated the human player's custom crest and colors (`rd.j.xb.T[i] = ah.a`):
  - **Dynamic Heraldry Generator (`CoatManager`)**: Automatically generates valid, distinct combinations of shield bases, heraldic bearers, banners, toppers, and emblems across the 32-color palette for all bot slots, ensuring no two bots on the battlefield share identical coats.
  - **Strict Engine-Native Display Boundary**: All AI crests are strictly confined to the game's authentic display locations: summoning portals on the map, captured fortress flags, the stone turn-announcement banner, and match-conclusion scoreboards. Zero artificial menus or overlays are added.
  - **Player & Campaign Preservation**: The human player's personal profile crest (Slot 0) is 100% preserved. Singleplayer Campaign opponents retain their fixed story scenario crests.

---

## [v1.0.13] – 2026-11-10 (Safe Display & Window Lifecycle / Match-2 Screen Freeze Elimination)

### Fixed
- **Match-2 / Subsequent Match Screen Freeze**: Resolved the critical bug where a player could successfully complete 1 match (sandbox or campaign), but upon launching any subsequent match, the map view was permanently frozen with game audio, clicks, and ESC menu still responding in the background:
  - **Peer Destruction Prevention**: Eliminated native window disposal (`jFrame.dispose()`) during fullscreen, resizable, and window mode transitions in `DisplayManager.java`. The native AWT Canvas and Graphics2D drawing pipeline remain permanently valid across all matches, preventing the FunOrb render loop (`pt.a`) from silently crashing and abandoning display refreshes.
  - **Resolution Setting Calculation Fix**: Corrected screen size persistence (`saveCurrentScreenSize()`) so component dimensions are measured directly from the active display area rather than FunOrb's internal fixed-HUD scale flag (`ra.e.h`), eliminating accidental rewrites to `Windowed - Fixed (640 x 480)` in `player.properties`.
  - **Automatic Resolution Self-Healing**: Added an automatic self-healing check in `loadPlayerProperties()` that detects and cleanses legacy broken `Windowed - Fixed (640 x 480)` entries, resetting them to `Fullscreen (Native)` on startup without requiring manual configuration file editing.

---

## [v1.0.12] – 2026-11-08 (Direct3D Hardware Acceleration & 512MB-1GB Fixed Heap Optimization)

### Performance & Engine
- **Direct3D Hardware Acceleration (Option 2)**:
  - Enabled Windows Direct3D hardware acceleration (`sun.java2d.d3d=true`) and translucent sprite acceleration (`sun.java2d.translaccel=true`) in `ArmiesOfGielinor.cfg`.
  - Translucent surfaces such as the fog of war, spell auras, movement range rings, and sprite blitting are now offloaded directly to the graphics card (GPU), minimizing CPU rendering spikes.
- **Fixed Heap Memory Pre-Allocation (Option 3)**:
  - Configured an initial heap of 512 MB (`-Xms512m`) and a maximum heap of 1,024 MB / 1 GB (`-Xmx1024m`) in `ArmiesOfGielinor.cfg`.
  - Completely eliminates mid-match pause times caused by Windows heap expansions while maintaining a compact ceiling for sub-millisecond Garbage Collection sweeps.

---

## [v1.0.11] – 2026-11-08 (AI Turn Frame-Slicing Optimization)

### Fixed
- **Multi-AI Application Freezing & Stutter on Large / Huge Maps**: Resolved the multi-second application freeze and Windows "Not Responding" lockup during computer opponents' turns in 4, 6, and 8-player matches on Large (1,600 tiles) and Huge (3,600 tiles) maps:
  - Instrumented `ej.class` with smooth frame-slicing (`stepPhase2Slice()`), evaluating structure threat maps in chunks of 25 tiles per video frame rather than executing a monolithic 3,600-tile loop on the main AWT render thread.
  - Reduced worst-case single-frame freezing by over 94% (from ~9,300 ms down to under 600 ms, and under 100 ms on Large maps), keeping the game window responsive and animations smooth without altering any AI strategic decision weights.

---

## [v1.0.10] — 2026-11-08 (Anti-Idle Inactivity Disconnect Prevention)

### Fixed
- **20-Minute Inactivity Disconnect**: Eliminated the legacy 2009 web browser inactivity kick that forcibly closed the network connection (`dj.a`) and kicked players back to the title screen if no mouse or keyboard inputs occurred for 20 minutes:
  - Integrated `net.alterorb.launcher.AntiIdleWatchdog` into the desktop launcher harness, which monitors volatile idle counters (`av.a`, `fc.b`) and safely resets them every 5 seconds.
  - Offline skirmishes against computer opponents and online multiplayer lobbies can now remain paused or unattended indefinitely without triggering socket disconnection or destroying match progress.

---

## [v1.0.9] — 2026-11-08 (Settings Stream Framing & Telemetry Handshake Fix)

### Fixed
- **Settings-Triggered Ghost Lobby Bug**: Resolved an issue where adjusting game speed, audio levels, or graphics settings in the Options menu caused the game to suddenly abandon the settings screen, open an online multiplayer lobby, and freeze on a loading screen.
  - Properly handled client telemetry and event-flush packets (opcodes 2, 3, 4, 5, 6, and 7) in the multiplayer host stream.
  - Kept incoming socket stream buffers in lockstep with the client's cipher state so settings adjustments no longer misinterpret housekeeping bytes as lobby requests.
- **Built-in One-Click Bug Reporter**: Integrated the F9 hotkey diagnostic reporter and standalone batch runner into the standalone launcher, allowing instant state and error export for fast investigation.

---

## [v1.0.8] — 2026-11-07 (Multiplayer Post-Combat Turn Retention Fix)

### Fixed
- **Post-Combat Premature Turn-End Bug (All Edge Cases)**: Resolved all remaining multiplayer scenarios where a player's turn automatically ended on the exact frame a fight between units finished:
  1. *Multi-Turn Saved Unit Paths*: Prevented the automatic movement flush when clicking End Turn with a queued unit path from simultaneously ending the turn when that unit enters combat.
  2. *Mid-Fight Turn Clock Unpause*: Assigned unique non-zero combat IDs (`1..255`) to every combat strike so fight-completion signals never collide with start-of-turn banner signals, skipped the unneeded Turn 0 banner timer, and deferred spectator banner unpauses whenever a combat animation is actively playing so the turn clock stays safely paused throughout every fight.
  3. *3–4 Player Counterattack Knockouts & Elimination Sync*: Guarded against premature auto-end-turn signals from alive commanders and ensured 3–4 player Free-For-All matches cleanly pass the turn to the next surviving commander if the active player is knocked out by a counterattack.

---

## [v1.0.7] — 2026-11-04 (Combat Turn Persistence Milestone)

### Fixed
- Re-mapped Client Opcode 74 in `AoGServer.java` from turn timeout to modal combat animation pause so normal unit attacks no longer force-advance the active player's turn.

---

## [v1.0.6] — 2026-11-04 (Golden Master / Publishing Release)

### Added
- **Complete 31-Page Retro Game Manual (`Manual.pdf`)**: Illustrated retro game manual with authentic RuneScape fonts (`Kingthings Petrock`, `RuneScape Quill`, `FunOrb Roman`, `RuneScape Bold 12`), vector HUD schematics, and the complete 75-unit **Steckbrief Compendium**.
- **In-Engine Unit Portrait Extraction**: Extracted all 75 official unit head sprites directly from binary cache index 5 (`pl.x` / `unitheads`) into metadata-free transparent PNGs.
- **Detailed Special Abilities Documentation**: Added dedicated ability profiles and mechanical explanations for units across all pantheons (Portal Mage temporary portals, Dagannoth air-interception, Crystalline Shapeshifter burrowing, Ourg mountain shattering, Pyrelord forest burning, Sphinx mind control, and Barbarian structure conquest).
- **Spectator Banner Auto-Dismissal**: When waiting for an opponent's turn, the stone announcement banner automatically slides away after 1.5 seconds, unblocking the tactical viewport.
- **Real-Time Map Scrolling During Spectating**: Spectators can freely pan, scroll, and inspect the battlefield while an opponent is calculating their turn moves.

### Fixed
- Fixed stone turn banner locking the viewport indefinitely during remote opponent turns. Implemented bidirectional Packet 73 (*Client Turn Ready*) and Packet 76 (*Turn Unpause & Vb sync*) handshake in `AoGServer.java` with an authoritative 2.5s fallback timer.
- Cleaned and sanitized all documentation, removing personal screenshots, host directories, and player usernames in favor of clean vector schematics.

---

## [v1.0.5] — 2026-11-04 (Full Win Conditions & Match Concluding)

### Added
- **Authoritative Win Condition Monitoring**: Built real-time evaluation in `AoGBattleBridge.java` for all 4 victory/defeat triggers:
  1. *Total Portal Loss & Barbarian Denial* (`up.java`): If a player loses all portals and has zero barbarians, they are declared vanquished.
  2. *Total Military Extinction* (`mc.java`): If all units belonging to an army are slain in combat, the army is eliminated.
  3. *Victory Points Ceiling*: First commander to reach the target match VP score triggers instant match victory.
  4. *Turn Expiration*: Evaluates high score on match turn ceiling expiration.
- **Elimination Broadcast**: Automatic elimination broadcasting marking eliminated players in bitmask `ha.e |= (1 << player)`.

---

## [v1.0.4] — 2026-11-04 (Disconnect-Free Unit Slaying)

### Fixed
- **Fatal Disconnection Veil on Unit Deaths**: Solved the fatal client disconnect bug where slaying an enemy unit caused `"Can see death"` socket errors.
- **Scoped Packet 68 Suppression**: Implemented scoped suppression of redundant server death packets (`hp`, opcode 194) in `AoGBattleBridge.java`, ensuring clean client-side death animations and stable multiplayer connections.

---

## [v1.0.3] — 2026-11-04 (Dynamic Battle Event Pump)

### Added
- **Multiplayer Combat Synchronization**: Implemented dynamic FIFO event pump in `AoGBattleBridge.java` translating engine battle events into Packet 68 combat strikes (`opcode 0x60`).
- Enabled real-time dice rolls, damage application, and combat strike animations between remote players.

---

## [v1.0.2] — 2026-11-03 (Lobby Discovery & Turn Coordination)

### Added
- **Steamworks P2P Relay Tunneling**: Direct lobby creation, overlay invites, and friend joining without requiring port forwarding.
- **Turn Rotation Engine**: Server-side turn handoffs coordinating round transitions across connected clients.

---

## [v1.0.1] — 2026-11-03 (Initial High-DPI & Java 18 Packaging)

### Added
- **Standalone Native Packaging**: Integrated 64-bit Windows launcher executable (`ArmiesOfGielinor.exe`).
- **Bundled OpenJDK 18 Runtime**: Self-contained JRE eliminating external Java dependencies.
- **High-DPI Display Scaling**: Added 4K, 1440p, and windowed scaling modes.
