/**
 * ====================================================================================================
 * Armies of Gielinor — Authoritative Multiplayer Battle Engine Contract
 * Component: BattleEngine.java (package net.alterorb.server)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Defines the clean boundary interface between the network transport / lobby coordinator
 *    ('net.alterorb.server.AoGServer') and the root-package authoritative battle simulation bridge
 *    ('AoGBattleBridge'), which directly manipulates obfuscated 2009 Jagex/FunOrb classes ('ha', 'mn',
 *    'oj', 'ag', 'jd') located in the unnamed default package.
 *    Keeping 'AoGServer' decoupled through this interface allows Java's package visibility rules to
 *    remain intact without modifying any original Jagex '.class' files.
 *
 * 2. ROSETTA STONE / PROTOCOL PACKET & OPCODE MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Protocol Symbol      | Direction        | Architectural Meaning & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    Server Packet 64     | Server -> Client | Match header (turn timer index, flags, player count, slot)
 *    Server Packet 77     | Server -> Client | Full serialized initial board snapshot ('ha.a(vh, int, slot)')
 *    Server Packet 67     | Server -> Client | Turn state sync [activeSlot, turnRound, turnTicksHi, turnTicksLo]
 *    Server Packet 68     | Server -> Client | Variable-length authoritative battle event ('kr', 'ag', 'mb', etc.)
 *    Server Packet 73     | Server -> Client | Eliminated player bitmask notification ('ha.e')
 *    Server Packet 76     | Server -> Client | Spectator stone turn-banner unpause & 'ic.Vb = turnRound' sync
 *    Server Packet 69     | Server -> Client | Match conclusion / Victory & Defeat screen payload
 *    Client Opcode 65     | Client -> Server | Tactical player command (Move+Attack, Recruit, Ability, [64]=EndTurn)
 *    Client Opcode 74     | Client -> Server | Active client entered modal combat animation lock ('ic.d(id, 4)')
 *    Client Opcode 73     | Client -> Server | subOp==0: Turn banner finished ('ic.j(109)')
 *                         |                  | subOp>0 : Combat strike animation finished ('ic.e(battleId, -56)')
 *    --------------------------------------------------------------------------------------------------
 * ====================================================================================================
 */
package net.alterorb.server;

import java.util.ArrayList;
import java.util.List;

public interface BattleEngine {
    public int getRoomOptionsLength();

    public byte[] getDefaultRoomOptions();

    public byte[] buildUnlockedProfilePacket();

    public MatchLaunchData startMatch(String[] playerNames, byte[] roomOptions, int gameType, boolean isTeams);

    /**
     * Launches an authoritative multiplayer battle with per-player custom Coats of Arms ('ln' serialized
     * as 12-byte pairs [shape0, color0, ..., shape5, color5], with shape '-1' encoded as unsigned 255).
     *
     * Architectural Rationale:
     *   Passing each connected peer's 12-byte 'ClientHandler.coatOfArms' explicitly prevents
     *   'AoGBattleBridge' from overwriting remote players with host heraldry ('ah.a') or randomized
     *   AI bot crests. The 5-argument default delegates to the legacy 4-argument signature so mock/test
     *   implementations of 'BattleEngine' remain source- and binary-compatible.
     *
     * @param playerNames Ordered commander display names for slots 0..W-1.
     * @param roomOptions 6-byte LobbyRoom option vector ('au.pc[0..5]').
     * @param gameType    Legacy game type parameter preserved for signature compatibility.
     * @param isTeams     True if Team Game was selected in the lobby.
     * @param playerCoats Optional per-slot 12-byte encoded Coat of Arms payloads (entries may be null).
     * @return Initial Server Packets 64, 77 (per player), and 67 for match broadcast.
     */
    default public MatchLaunchData startMatch(String[] playerNames, byte[] roomOptions, int gameType, boolean isTeams, byte[][] playerCoats) {
        return this.startMatch(playerNames, roomOptions, gameType, isTeams);
    }

    public ActionResult handleAction(int playerIdx, byte[] payload);

    public ActionResult forceAdvanceTurn();

    public ActionResult handleSurrender(int playerIdx);

    public boolean isMatchActive();

    public int getCurrentTurn();

    public int getActivePlayer();

    /**
     * Notifies the authoritative battle engine that the active client has finished rendering a modal
     * combat strike animation ('ag', identified by non-zero 'battleId' 1..255) via Client Opcode 73
     * ('ic.e(battleId, -56)').
     *
     * Architectural Rationale:
     *   While a combat animation is still playing on the active client, all tactical UI buttons
     *   (including End Turn) are locked ('ic.Q == true'). Any End Turn packet ('Opcode 65 [64]')
     *   arriving BEFORE this completion signal from an alive player is therefore guaranteed to be a
     *   spurious automated flush (e.g., queued multi-turn waypoint race in 'ic.b') and must be ignored.
     *   Once this completion hook fires, manual End Turn clicks are immediately honored again.
     *
     * @param playerIdx Slot index of the client reporting combat animation completion.
     * @param battleId  Non-zero combat strike identifier (1..255) assigned in Packet 68 ('ag.D').
     */
    default public void notifyCombatCompleted(int playerIdx, int battleId) {
        // Default no-op preserves binary compatibility with lightweight mock/test implementations.
    }

    public static final class MatchLaunchData {
        public final byte[][] playerPacket64;
        public final byte[][] playerPacket77;
        public final byte[] initialPacket67;
        public final int turnTicks;

        public MatchLaunchData(byte[][] playerPacket64, byte[][] playerPacket77, byte[] initialPacket67, int turnTicks) {
            this.playerPacket64 = playerPacket64;
            this.playerPacket77 = playerPacket77;
            this.initialPacket67 = initialPacket67;
            this.turnTicks = turnTicks;
        }
    }

    public static final class ActionResult {
        public final List<byte[]> eventPackets68 = new ArrayList<byte[]>();
        public byte[] packet67;
        public final List<byte[]> postTurnEventPackets68 = new ArrayList<byte[]>();
        public final List<Integer> newlyEliminatedPlayers = new ArrayList<Integer>();
        public byte[] packet69MatchEnd;
    }
}
