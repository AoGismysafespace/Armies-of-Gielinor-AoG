# Armies of Gielinor – Standalone Steam Edition v1.0.18

**Armies of Gielinor v1.0.18** introduces the **Safe Player Profile & Offline Identity Manager** (`ProfileManager`), establishing a clean, privacy-preserving generic default title (`"Commander"`) for offline play and shared distribution copies, while completely eliminating obsolete pre-release testing placeholders.

---

### What's New in v1.0.18

1. **Privatized Generic Offline Identity Fallback (`Commander`)**
   - In offline or standalone mode (when Steam is closed or not running), the game now cleanly and reliably defaults to the generic, neutral commander title `"Commander"`.
   - Distributing release packages to friends no longer carries any personal account identifiers or obsolete test names.

2. **Complete Purge of Obsolete Legacy Test Placeholders**
   - Automatically intercepts and eliminates any legacy pre-release testing names (`jesii98`) from older configuration files or profiles, ensuring that no stale test name can ever surface in the game or turn banners.

3. **Seamless Steam Persona Name Integration**
   - Whenever Steam is active in the background, the game automatically retrieves, sanitizes, and displays your active Steam Persona Name (e.g. `E-Soul` for the host, or your friends' Steam names in multiplayer lobbies).

4. **FunOrb Base37 Protocol Conformance**
   - Enforces strict 1–12 character limits and alphanumeric character set constraints (`[a-zA-Z0-9_\- ]`), preventing buffer framing errors and visual overflow across HUD turn banners and nameplates.

5. **Full Domino & Regression Suite Verified**
   - Preserved all previous milestone protections: top-right HUD heraldry synchronization (v1.0.17), enhanced options configuration (v1.0.16), tactical AI pantheon diversity (v1.0.15), Match-2 display lifecycle fix (v1.0.13), Direct3D hardware acceleration and 512 MB – 1 GB fixed heap (v1.0.12), AI frame-slicing optimization (v1.0.11), 20-minute inactivity watchdog (`AntiIdleWatchdog`), post-combat turn retention, spectator banner synchronization, and the one-click diagnostic bug exporter (F9).

> **Multiplayer & Skirmish Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.18`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.18.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background for multiplayer and Steam display names.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
