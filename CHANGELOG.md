# Changelog — Armies of Gielinor Standalone Edition

All notable changes to the standalone preservation release are documented in this file.

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
