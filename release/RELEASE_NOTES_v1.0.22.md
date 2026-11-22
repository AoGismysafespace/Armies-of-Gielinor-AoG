# Armies of Gielinor – Standalone Steam Edition v1.0.22

**Armies of Gielinor v1.0.22** hotfixes the Multiplayer Host game start issue, ensuring that when the room host clicks **"Start Game"** in a multiplayer lobby, the authoritative match coordinator (`AoGBattleBridge`) links seamlessly without throwing a `NoSuchMethodError` or booting players back to the Main Menu.

---

### What's New in v1.0.22

1. **Multiplayer Host Launch Linkage Fix**
   - In previous builds, clicking "Start Game" as the host of a multiplayer room caused the background server coordinator to crash with `java.lang.NoSuchMethodError: void CoatManager.randomizeBotCoats(Object[], Object)`, abruptly returning the host to the Main Menu.
   - Restored the binary bytecode method descriptor to `void`, matching the exact interface expected by `AoGBattleBridge.startMatch()`.
   - Recompiled both `CoatManager.java` and `AoGBattleBridge.java` in lockstep targeting OpenJDK 18 (`--release 18` / major version 62.0).

2. **Full Domino & Regression Suite Verified**
   - Verified 100% pass across all regression checks:
     * `MultiplayerStartMatchReproductionTest` (Verifies both `void` and `boolean` method handle linkage)
     * `SkirmishCoatReproductionTest` (Verifies human slot crest retention in both Slot 0 and Slot 1)
     * `TurnOrderCrestFixTest` (Verifies HUD turn order sprite cache invalidation)
     * `BotCoatRandomizerTest` & `BotGodRandomizerTest` (Verifies AI heraldry and diverse pantheons)
     * `EnhancedOptionsTest` (Verifies batch script toggles)
     * `SettingsStreamDesyncReproductionTest` (Verifies packet framing)
     * `AntiIdleWatchdogLiveVerificationTest` (Verifies 20-minute inactivity watchdog)

> **Multiplayer Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.22`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.22.zip`** below (or access directly in `Aktuell/`).
2. Extract the ZIP folder anywhere on your PC (or run directly from `Aktuell/ArmiesOfGielinor/`).
3. Keep **Steam** running in the background for multiplayer and Steam display names.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
