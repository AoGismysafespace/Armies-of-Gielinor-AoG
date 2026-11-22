# Armies of Gielinor – Standalone Steam Edition v1.0.21

**Armies of Gielinor v1.0.21** resolves the turn-order slot inversion issue in Play against Computer (Skirmish) battles, guaranteeing that human players 100% reliably retain their player-created heraldic crest and chosen gods regardless of whether the Jagex matchmaking engine assigns them to act first (Slot 0) or second (Slot 1).

---

### What's New in v1.0.21

1. **Skirmish AI Heraldry & Pantheon Slot Inversion Fix**
   - In Jagex's 2009 skirmish matchmaking logic (`ic.java`), player turn order slots are randomly shuffled at match start. In a 2-player match against the computer, the AI bot controller is placed into Slot 0 approximately 50% of the time, assigning the human player to Slot 1 (and vice-versa).
   - Previously, coat-of-arms randomization (`CoatManager`) and god selection (`GodManager`) assumed the human player was permanently in Slot 0. When placed into Slot 1, the AI bot inherited the player's crest and gods, while the human player's crest and gods were overwritten with generated AI heraldry.
   - Both `CoatManager` and `GodManager` now dynamically inspect active game controllers (`ic.Eb`): human slots are strictly protected and retain the player's custom crest and chosen gods across 100% of matches, while only computer bots receive randomized heraldry and diverse pantheons.

2. **Multi-Layered Safety Guards**
   - **Multiplayer & Hotseat Protection**: Guards against uninitialized or all-null controller arrays, ensuring smooth fallback and complete human protection in human vs human lobbies.
   - **God Wheel Transition Guard**: Checks `ha.Cb == true` during active God Wheel gameplay to protect against unexpected pantheon re-rolls.
   - **Backward Compatibility**: Fully maintains legacy overload signatures for all internal callers.

3. **Full Domino & Regression Suite Verified**
   - Verified 100% pass across all regression checks: `SkirmishCoatReproductionTest`, `TurnOrderCrestFixTest`, `BotCoatRandomizerTest`, `BotGodRandomizerTest`, `EnhancedOptionsTest`, `SettingsStreamDesyncReproductionTest`, and `AntiIdleWatchdogLiveVerificationTest`.
   - All historical protections remain intact: unified options launcher (`Enhanced_Options.bat`), achievement UI cleanliness (single Menu button), privatized offline identity (`Commander`), Match-2 display lifecycle fix, Direct3D hardware acceleration, AI frame-slicing optimization, 20-minute inactivity watchdog, and authoritative battle engine.

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.21`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.21.zip`** below (or access directly in `Aktuell/`).
2. Extract the ZIP folder anywhere on your PC (or run directly from `Aktuell/ArmiesOfGielinor/`).
3. Keep **Steam** running in the background for multiplayer and Steam display names.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
