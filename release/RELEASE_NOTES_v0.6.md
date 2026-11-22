# Armies of Gielinor — Standalone Steam Edition v1.0.6 (Golden Master)

We are thrilled to publish **Armies of Gielinor v1.0.6 (Golden Master)**!

This release represents the complete standalone preservation of Jagex's classic 2010 tactical turn-based strategy game for modern 64-bit Windows systems. All singleplayer campaigns and multiplayer battle loops operate with 100% parity to the original FunOrb design, backed by Steamworks P2P relay networking and an embedded OpenJDK 18 runtime.

---

### What's New in v1.0.6

#### 1. Official 31-Page Retro Game Manual (`Manual.pdf`)
- A comprehensive, illustrated handbook styled with authentic RuneScape fonts (`Kingthings Petrock`, `RuneScape Quill`, `FunOrb Roman`, and `RuneScape Bold 12`).
- **Complete 75-Unit Steckbrief Compendium**: Every single unit across all 7 Divine Pantheons (*Neutral, Saradomin, Zamorak, Guthix, Bandos, Seren, Menaphos*) has an individual tactical dossier card with:
  - Official in-game unit portrait artwork extracted directly from the game's JS5 binary cache (`pl.x` / `unitheads`).
  - 4-stat vital combat telemetry (`SPD`, `HP`, `STR`, `RNG`).
  - Mechanics and ability badges (`[⚑ CAPTURE]`, `[✈ FLYING]`, `[☠ DECAY]`, `[🌊 AMPHIBIOUS]`, `[✦ RESTORATION]`, `[💥 SIEGE]`, `[⚔ AIR-INTERCEPTION]`).
  - Dedicated **Special Abilities Section** detailing tactical mechanics:
    - **Portal Mage**: `Deploy Temporary Stone Portal` (forward reinforcement node creation).
    - **Dagannoth**: `Amphibious Air-Interception` (ocean/river crossing and drag-down aerial combat).
    - **Crystalline Shape-shifter**: `Burrow Assassination` (underground execution).
    - **Ourg & Pyrelord**: `Mountain Demolition` & `Forest Incineration` (permanent terrain destruction).
    - **Sphinx & Sea Slug**: `Mind Subjugation` & `Parasitic Hijack` (permanent tactical enemy control).
    - **Cyclops & Ogre Shaman**: `Gaze of Dread` (movement paralysis) & `Smog Shroud` (ranged missile denial).
    - **Barbarians**: `Territorial Conquest` (the only units capable of capturing structures).
  - Authentic Jagex field lore narratives in `RuneScape Quill` italic typeface.
  - Master Unit Reference Matrix quick-lookup table.

#### 2. Spectator Banner Auto-Dismissal & Map Scrolling
- During an opponent's turn, the stone announcement banner now automatically slides away after ~1.5 seconds.
- Spectators can freely pan, scroll, and inspect the battlefield in real time while waiting for their turn.
- Implemented a robust bidirectional Packet 73 (*Client Turn Ready*) and Packet 76 (*Turn Unpause & Vb sync*) handshake with an authoritative 2.5s fallback timer.

#### 3. Complete Multiplayer Combat & Match Concluding
- **100% Slaying Stability**: Redundant server death packets are intelligently suppressed, eliminating the fatal Jagex socket drop (`"Can see death"`).
- **Authoritative Win Conditions**: Full match termination and Victory/Defeat evaluation across Conquest, Army Extinction, VP threshold, and Turn Limit expiration.

#### 4. Steam P2P Overlay Integration
- Automated lobby hosting and friend invitations via the Steam Overlay (`Shift + Tab` or `F2`).
- Seamless friend joining: when accepting an invite while outside the match, clicking **Multiplayer** in the Main Menu automatically routes directly into the host's room.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.6.zip`** attached below (41 MB).
2. Extract the ZIP archive to any folder on your PC.
3. Make sure the **Steam client** is running in the background.
4. Run **`ArmiesOfGielinor.exe`** and start playing!
   - Select **Singleplayer** to play the campaign or skirmish offline.
   - Select **Host** -> **Custom Game** -> **Create Room** and invite friends via Steam to play multiplayer.
   - Refer to **`Manual.pdf`** in the game folder for the complete game rules, unit stats, and special abilities!
