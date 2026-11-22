# Armies of Gielinor — Project Milestones & Fallback Archive Register

This document tracks all recognized project milestones, their verification status, known capabilities, architectural solutions, and preserved backup archives.

---

## Milestone Register

| Version | Status | Milestone Name | Fallback Archive Path |
| :--- | :--- | :--- | :--- |
| **v1.0.12** | **Golden Master (Current / Recommended)** | **Direct3D Hardware Acceleration & 512MB-1GB Fixed Heap** | `releases/archive/ArmiesOfGielinor_v1.0.12_Milestone_Direct3DAndMemoryOptimization.zip` |
| **v1.0.11** | Fully Functional | AI Turn Frame-Slicing Optimization | `releases/archive/ArmiesOfGielinor_v1.0.11_Milestone_AiTurnFrameSlicingOptimization.zip` |
| **v1.0.10** | Fully Functional | Anti-Idle Inactivity Disconnect Prevention | `releases/archive/ArmiesOfGielinor_v1.0.10_Milestone_AntiIdleDisconnectFix.zip` |
| **v1.0.9** | Fully Functional | Settings Stream Framing & Telemetry Handshake Fix | `releases/archive/ArmiesOfGielinor_v1.0.9_Milestone_SettingsFramingFix.zip` |
| **v1.0.8** | Fully Functional | Post-Combat Turn Retention & Multi-Turn Move Desync Fix | `releases/archive/ArmiesOfGielinor_v1.0.8_Milestone_PostCombatTurnRetention.zip` |
| **v1.0.7** | Fully Functional | Combat Turn Persistence & Timer Pause | `releases/archive/ArmiesOfGielinor_v1.0.7_Milestone_CombatTurnPersistence.zip` |
| **v1.0.6** | Stable | Spectator Banner Auto-Dismiss & Real-Time Map Scrolling Sync | `releases/archive/ArmiesOfGielinor_v1.0.6_Milestone_SpectatorBannerDismissalSync.zip` |
| **v1.0.5** | Fully Functional | Full Win/Loss Conditions & Authoritative Match Ending | `releases/archive/ArmiesOfGielinor_v1.0.5_Milestone_FullWinConditionsFunctional.zip` |
| **v1.0.4** | Fully Functional | Disconnect-Free Combat & Unit Slaying (Death Packet Suppression) | `releases/archive/ArmiesOfGielinor_v1.0.4_Milestone_FullCombatFunctional.zip` |
| **v1.0.3** | Functional (Combat Engaged) | Dynamic Battle Event Streaming (Packet 68 FIFO Pump) | `backups/ArmiesOfGielinor_v1.0.3_milestone.jar` |
| **v1.0.2** | Partially Functional (Lobby/Match) | Multiplayer Lobby & In-Game Viewport Sync | `releases/archive/ArmiesOfGielinor_v1.0.2_Milestone_PartiallyFunctionalMultiplayer.zip` |
| **v1.0.1** | Functional (Singleplayer) | High-DPI Scaling & Offline Mode Fix | `releases/archive/ArmiesOfGielinor_v1.0.1.zip` |

---

## Detailed Milestone Specifications

### Milestone v1.0.12: Direct3D Hardware Acceleration & 512MB-1GB Fixed Heap (Current Golden Master)
* **Date Established**: November 8, 2026
* **Classification**: **Golden Master / Recommended Release Candidate**
* **Primary Objective**: Offload 2D sprite rendering and translucent effects to the GPU via Direct3D, and pre-allocate memory to eliminate mid-match Windows heap expansion pauses.
* **Architectural Fix**:
  - Configured `ArmiesOfGielinor.cfg` with native Direct3D hardware acceleration (`-Dsun.java2d.d3d=true`), translucent sprite acceleration (`-Dsun.java2d.translaccel=true`), initial heap size of 512 MB (`-Xms512m`), and max heap size of 1,024 MB (`-Xmx1024m`).
  - Translucent fog-of-war layers, spell auras, and movement rings are rendered with GPU pipeline acceleration.
  - Generous 512 MB startup buffer avoids in-match allocation stalls, while the 1 GB ceiling prevents garbage collection pauses.
* **Preserved Backup**: `releases/archive/ArmiesOfGielinor_v1.0.12_Milestone_Direct3DAndMemoryOptimization.zip`

### Milestone v1.0.10: Anti-Idle Inactivity Disconnect Prevention (Current Golden Master)
* **Date Established**: November 8, 2026
* **Classification**: **Golden Master / Recommended Release Candidate**
* **Primary Objective**: Prevent the legacy 20-minute inactivity timer from closing the network connection and kicking players to the title screen during unattended or paused matches.
* **Architectural Fix**:
  - In `net.alterorb.launcher.AntiIdleWatchdog`, implemented an ultra-lightweight daemon thread that checks volatile engine idle counters (`av.a` for mouse, `fc.b` for keyboard) every 5 seconds.
  - When inactivity exceeds 1,000 ticks (~20 seconds), resets both counters back to 0, permanently preventing them from reaching the hardcoded 60,000-tick (20-minute) disconnect threshold in `wf.java:667`.
  - Zero original 2009 Jagex classes modified; runs completely inside the custom desktop launcher harness.
* **Preserved Backup**: `releases/archive/ArmiesOfGielinor_v1.0.10_Milestone_AntiIdleDisconnectFix.zip` and `backups/ArmiesOfGielinor_v1.0.10_milestone.jar`

### Milestone v1.0.9: Settings Stream Framing & Telemetry Handshake Fix
* **Date Established**: November 8, 2026
* **Classification**: **Settings Handshake Milestone**
* **Primary Objective**: Prevent adjusting Settings options (game speed, volume, resolution) from triggering desynchronization and pulling players into a frozen multiplayer lobby.
* **Architectural Fix**:
  - In `AoGServer.java`, handled client telemetry and event-flush packets (opcodes 2, 3, 4, 5, 6, and 7) by reading their exact wire frames (fixed-length or variable-byte payloads).
  - Maintained lockstep cipher alignment on incoming TCP socket packets so flushed events during settings adjustments are cleanly consumed rather than misread as lobby join commands.
  - Built-in F9 hotkey one-click diagnostic bug reporter for instantaneous reproduction and state logging.
* **Preserved Backup**: `releases/archive/ArmiesOfGielinor_v1.0.9_Milestone_SettingsFramingFix.zip` and `backups/ArmiesOfGielinor_v1.0.9_milestone.jar`

### Milestone v1.0.8: Post-Combat Turn Retention & Multi-Turn Move Desync Fix
* **Date Established**: November 7, 2026
* **Classification**: **Turn Retention Milestone**
* **Primary Objective**: Eliminate unexpected automatic turn endings after combat strikes and multi-turn waypoint movement.
* **Architectural Fix**:
  - In `ic.java:1894`, when a unit attacks an enemy at the end of a multi-turn waypoint path, the client incorrectly generated an end-turn packet (`[64]`).
  - Added combat resolution tracking (`activeBattleId`) and suppressed premature end-turn packets while combat animations resolve.
  - Ensured the active commander retains full control after combat strikes until they explicitly click the "End Turn" button or the turn timer runs out.
* **Preserved Backup**: `releases/archive/ArmiesOfGielinor_v1.0.8_Milestone_PostCombatTurnRetention.zip` and `backups/ArmiesOfGielinor_v1.0.8_milestone.jar`

### Milestone v1.0.7: Combat Turn Persistence & Timer Pause
* **Date Established**: November 5, 2026
* **Classification**: **Turn Persistence Milestone**
* **Primary Objective**: Retain the active player's turn during combat encounters by pausing turn timers during combat animations.
* **Architectural Fix**:
  - Implemented Opcode 74 modal combat pause handling in `AoGServer.java`.
  - Paused server-side turn progression timers while client combat animations play.
* **Preserved Backup**: `releases/archive/ArmiesOfGielinor_v1.0.7_Milestone_CombatTurnPersistence.zip` and `backups/ArmiesOfGielinor_v1.0.7_milestone.jar`

### Milestone v1.0.6: Spectator Banner Auto-Dismiss & Real-Time Map Scrolling Sync (Current Golden Master)
* **Date Established**: November 4, 2026
* **Classification**: **Golden Master / Publishing Release Candidate**
* **Primary Objective**: Allow players who are waiting for an opponent's turn to freely view and scroll the battlefield without obstruction.
* **Architectural Fix**:
  - In `ic.java:2879`, spectator clients intentionally pause at tick 50 (`if (50 == this.j && this.Vb < this.xb.Gb && this.xb.gb != this.q) break;`), suppressing map scrolling (`if (0 < this.j) return -1;`) until Packet 76 arrives.
  - Implemented handling for Opcode 73 (`ClientTurnReady`, 3 bytes) in `AoGServer.java`.
  - When the active player finishes their transition countdown, the server broadcasts Packet 76 (`[0, (turn >> 8) & 0xFF, turn & 0xFF]`).
  - Added a 2.5-second fallback timer via `keepaliveExecutor` ensuring the banner dismisses even during network lag or disconnects.
  - Spectator acknowledged turn `Vb` updates to match `Gb`, resuming the animation countdown from 50 to 0, clearing battlefield dimming, and unlocking free map scrolling.
* **Preserved Backup**: `backups/ArmiesOfGielinor_v1.0.6_Milestone_SpectatorBannerDismissalSync.zip`

### Milestone v1.0.5: Full Win/Loss Conditions & Authoritative Match Ending
* **Date Established**: November 4, 2026
* **Classification**: **Full Victory & Defeat Rules Milestone**
* **Primary Objective**: Accurately evaluate match win/loss conditions and conclude multiplayer games with the official Victory / Defeat screen.
* **Architectural Fix**:
  - Implemented `checkEliminationsAndVictory` in `AoGBattleBridge.java` evaluating active players via `serverHa.b(p, true)`.
  - Accurately triggers on: Total Portal Loss / Barbarian Denial (`up.java`), Total Unit Loss (`mc.java`), Victory Points thresholds, and Turn limits.
  - Authoritatively broadcasts Packet 73 (elimination) and Packet 69 (match termination with winning slot).
* **Preserved Backup**: `backups/ArmiesOfGielinor_v1.0.5_Milestone_FullWinConditionsFunctional.zip`

### Milestone v1.0.4: Disconnect-Free Combat & Unit Slaying
* **Date Established**: November 4, 2026
* **Classification**: **Combat Slaying & Disconnect Prevention Milestone**
* **Primary Objective**: Prevent the fatal socket drop and disconnection veil when destroying an enemy unit or chieftain.
* **Architectural Fix**:
  - When a combat strike (`ag`) slays a unit (`ag.N=true` or `ag.M=true`), client-side `tb.a` automatically generates an internal unit death event.
  - If the server also broadcasted a redundant Packet 68 `hp` (opcode 194), the client encountered an empty tile and threw `ha.a(0, "Can see death", hp)`, closing the TCP connection.
  - `AoGBattleBridge` tracks `combatSlainTiles` and suppresses redundant Packet 68 `hp` broadcasts while keeping server state updated.
* **Preserved Backup**: `backups/ArmiesOfGielinor_v1.0.4_Milestone_FullCombatFunctional.zip`

### Milestone v1.0.3: Dynamic FIFO Battle Event Streaming
* **Date Established**: November 4, 2026
* **Classification**: **Multiplayer Combat Engaged Milestone**
* **Primary Objective**: Allow units to engage in melee and ranged attacks across network clients.
* **Architectural Fix**:
  - Replaced static event polling with a dynamic FIFO event pump in `AoGBattleBridge.java`.
  - Intermediate attack events (`ns`) cascading into visual combat strike events (`ag`, opcode `0x60`, 10 bytes) are captured and streamed to all clients in lockstep.
* **Preserved Backup**: `backups/ArmiesOfGielinor_v1.0.3_milestone.jar`

### Milestone v1.0.2: Multiplayer Lobby & In-Game Viewport Sync
* **Date Established**: November 4, 2026
* **Classification**: **Lobby Discovery & Match Transition Milestone**
* **Primary Objective**: Enable Steam P2P invites, lobby room joining, and match launch transition.
* **Architectural Fix**:
  - Integrated Steamworks P2P socket tunneling.
  - Resolved lobby dismissal veil (`bl.b = true`, `rd.j.V = false`).
* **Preserved Backup**: `backups/ArmiesOfGielinor_v1.0.2_Milestone_PartiallyFunctionalMultiplayer.zip`
