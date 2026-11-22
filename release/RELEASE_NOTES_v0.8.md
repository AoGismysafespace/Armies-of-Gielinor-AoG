# Armies of Gielinor — Standalone Steam Edition v1.0.8

**Armies of Gielinor v1.0.8** fixes a subtle online multiplayer bug where, under specific in-game conditions, a player's turn would automatically end the exact moment two units finished fighting.

---

### What's Fixed in v1.0.8

1. **Saved Multi-Turn Unit Paths No Longer Cut Your Turn Short After Combat**
   - Fixed a race condition when a unit had a multi-turn movement order stored from an earlier turn and entered combat, which previously slipped an early "End Turn" signal behind the fight animation and skipped your remaining unmoved units.
2. **Turn Clock Stays Safely Paused Throughout Every Fight**
   - Every combat animation now carries its own unique ticket ID so fight-completion signals never get mixed up with start-of-turn banner signals.
   - If a spectator turn-banner clear timer comes due while two units are fighting, the host now waits until the fight animation finishes before sending it — keeping your turn clock paused during combat while still smoothly clearing the banner for watching players.
3. **Clean Turn Handoffs in 3–4 Player Matches**
   - Blocked stray early End-Turn signals while a player is still alive on the host's board, and ensured 3- and 4-player Free-For-All matches cleanly pass the turn to the next surviving player if the active commander's final Barbarian falls to a counterattack.
4. **Zero Original Game Files Modified**
   - All fixes live strictly inside the custom multiplayer host bridge, keeping singleplayer, unit death animations, victory screens, and spectator map scrolling 100% intact.

> **Multiplayer Tip:** Since matches run on whoever hosts the room (*Host & Play*), make sure **everyone in your group — especially the Host — downloads and runs `v1.0.8`**.

---

### Download & How to Play

1. Download **`ArmiesOfGielinor_v1.0.8.zip`** below.
2. Extract the ZIP folder anywhere on your PC.
3. Keep **Steam** running in the background.
4. Launch **`ArmiesOfGielinor.exe`** and enjoy!
