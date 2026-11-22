# Armies of Gielinor – Standalone Steam Edition v1.0.23

**Armies of Gielinor v1.0.23** eliminates a recurring multiplayer launch crash where pressing **"Start Game"** in a hosted room failed roughly 1-in-3 times (freezing on the first click and returning the host to the Main Menu on the second click), and restores 1-to-1 accuracy for all six Multiplayer Lobby room settings.

---

### What's New in v1.0.23

1. **Fixed Recurring 1-in-3 Multiplayer "Start Game" Map Theme Crash (`ha.l -> ha.<init>`)**
   - In Multiplayer Lobby rooms, the 4 available map themes (*Misthalin*, *Kharidian Desert*, *Frozen Wastes*, *Hallowvale Swamps*) are numbered `0..3` with no "Random" slot.
   - `AoGBattleBridge.startMatch()` previously treated slot `0` (*Misthalin*, the default) as "Random" and rolled over `0..5`, crashing the server thread whenever 4 or 5 rolled (33.3% failure rate) and shifting explicit selections (*Desert*, *Frozen Wastes*, *Swamps*) by one slot.
   - Now map theme index `0..3` maps directly 1-to-1 to the chosen battlefield with 0 crashes across 100/100 sandbox launches.

2. **Full 6-Option Lobby Room Parity**
   - **Turn Timer (`30s..240s`)**: Widened clamp to allow all 7 timer options (unlocking `60s` and `30s`).
   - **Game Length (`Regular..Quadruple`)**: Correctly applied to `ha.vb` (victory score multiplier) without accidentally turning off the 3-God Pantheon bar (`ha.L`).
   - **Game Type (`Free For All / Team Game`) & Trinket Limit (`Off .. No limit`)**: Aligned indices and fixed Packet 64 client launch flags (`bit 1 = teamMatch`, `bits 2..4 = trinketLimit`).

3. **Atomic Lobby Launch Guard (`AoGServer.handleStartMatchRequest`)**
   - Match generation (`engine.startMatch(...)`) now executes inside a `try/catch` guard *before* tearing down the lobby room or clearing member states, preventing any launch error from dropping the host's TCP socket.

> **Multiplayer Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group – especially the Host – downloads and runs `v1.0.23`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.23.zip`** below (or access directly in `Aktuell/`).
2. Extract the ZIP folder anywhere on your PC (or run directly from `Aktuell/ArmiesOfGielinor/`).
3. Keep **Steam** running in the background for multiplayer and Steam display names.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
