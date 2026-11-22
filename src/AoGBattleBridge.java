/**
 * ====================================================================================================
 * Armies of Gielinor — Authoritative Server Battle Engine Bridge
 * Component: AoGBattleBridge.java (implements net.alterorb.server.BattleEngine)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    AoGBattleBridge acts as the server-side authoritative simulation bridge for multiplayer matches.
 *    In AlterOrb's headless server architecture, incoming player actions (movement, combat, summoning,
 *    special abilities, and turn transitions) are validated against a headless instance of the FunOrb
 *    game state ('ha'). Any resulting board modifications are serialized into raw FunOrb network packets
 *    (predominantly Packet 68 variable-length battle events, Packet 67 turn signals, and Packet 69
 *    victory/match end broadcasts) and sent to all connected peers (Host and Guests) in lockstep.
 *
 * 2. ROSETTA STONE / FUNORB OBFUSCATED SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Obfuscated Symbol | Real / Architectural Meaning      | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    ha                | GameState / BoardState           | Authoritative board matrix, unit table, event queue ('l')
 *    mn                | AbstractCommand                  | Base class for incoming player actions
 *    am                | CommandMoveAndAttack             | Player issue: move unit and engage in combat
 *    hw                | CommandRecruitUnit               | Player issue: recruit a unit onto a portal tile
 *    jc                | CommandUnitAbility / Stance      | Player issue: toggle stance or trigger unit ability
 *    ee                | CommandTargetedAbility           | Player issue: ability targeted at specific tile/unit
 *    to                | CommandCastSpell                 | Player issue: cast god/faction spell
 *    bd                | ValidatedActionHandler           | Returned by mn.a(118, ha); handles action validation
 *    ld                | ActionMoveAndAttackHandler       | Validated handler for am; queues 'kr' and 'ns' events
 *    oj                | BaseBattleEvent                  | Base class for all events in game state event queue ('l')
 *    kr                | EventUnitMove                    | Movement path animation event; serialized to Packet 68 (opcode 0x00)
 *    ns                | EventAttack                      | Intermediate attack resolution & damage calculation
 *    ag                | EventCombatStrike                | Combat strike & counter-strike animation + damage application;
 *                      |                                  | Serialized to Packet 68 (opcode 0x60, 10 bytes)
 *    mb                | EventRecruit                     | Unit spawned on board; serialized to Packet 68 (opcode 0x20)
 *    md                | EventStructureCapture            | Structure captured / converted; Packet 68 (opcode 0x64)
 *    qq                | EventTurnIncome / ManaUpdate     | Mana & victory point generation; Packet 68 (opcode 0xA0)
 *    hp                | EventUnitDeath / Removal         | Unit killed or purged; Packet 68 (opcode 0xC2 / 194, 3 bytes [194, X, Y])
 *    rl / vp / pj / q  | Secondary Tactical Events        | Morale, spells, buffs, visual fx events
 *    vh                | PacketBuffer                     | FunOrb raw byte serialization buffer
 *    pd                | PackedCoordinateMath             | Helper for packing relative delta offsets (dX, dY)
 *    ha.Gb             | GameTurnRound                    | Match round counter; synchronized to clients via Packet 76
 *    ha.gb             | ActivePlayerSlot                 | Current active turn player slot (0..W-1)
 *    ha.e              | EliminatedPlayersBitmask         | Bitmask ((1 << slot)) of authoritatively eliminated commanders
 *    ag.D              | CombatStrikeBattleId             | Non-zero battle ticket (1..255) paired across ic.d / ic.e via ic.db
 *    ic.db             | ActiveClientModalBattleId        | Asserts 0 on ic.d(id,4) entry and matches id on ic.e(id,-56) exit
 *    ic.Q              | ModalCombatEventQueueLock        | Holds queued kc (Packet 67) in ha.l until ag finishes animating
 *    jd.db / jd.k      | QueuedMultiTurnWaypoint          | Stored unit path; in ic.b(4,123), dead 'if (unit.s)' check flushes
 *                      |                                  | [Move+Attack] + [0x40 EndTurn] in a single network frame
 *    Packet 73         | ClientTurnReady / CombatDone     | subOp==0: Turn banner finished; subOp>0: Combat battleId finished
 *    Packet 76         | ServerTurnUnpauseSync            | Server -> Client: Broadcasts acknowledged turn (Vb = Gb) & unpauses spectator banner
 *    au.pc[0..5]       | MultiplayerLobbyOptions          | 0:TurnTime(0..6), 1:MapSize(0..3), 2:GameLength(0..3),
 *                      | (pf.a, bv.A={7,4,4,4,2,5},       | 3:MapTheme(0..3 -> ns.A[0..3]), 4:GameType(0=FFA,1=Team),
 *                      |  am.B={2,0,0,0,0,0})             | 5:TrinketLimit(0..4 -> lf.i={0,25,75,150,0})
 *    Packet 64 byte[1] | ClientLaunchBitflags (n12)       | ArmiesOfGielinor.java:252-255: bit0=isRated, bit1=isTeamMatch,
 *                      |                                  | bits2..4=trinketLimitIdx (0..4), bit6=ha.L (gods disabled)
 *    --------------------------------------------------------------------------------------------------
 *
 * 3. EVENT LIFECYCLE, COMBAT CASCADE & DISCONNECT MITIGATION:
 *    When a combat command ('am') is executed, 'ld.a()' puts an initial movement event ('kr') and an
 *    attack event ('ns') into the queue. Executing 'ns.a()' performs dice rolls and pushes the visual
 *    strike event ('ag') into the queue.
 *    The event loop below uses a live FIFO queue pump to guarantee that cascade events ('ag') are never
 *    silently swallowed, ensuring both host and client receive the 10-byte combat packet in lockstep.
 *
 *    CRITICAL DISCONNECT PREVENTION (CAN SEE DEATH DESYNC):
 *    When a combat strike ('ag') slays an attacker or defender ('ag.N=true' or 'ag.M=true'), the client-side
 *    game loop automatically generates an internal 'hp' (EventUnitDeath) when processing 'ag'.
 *    If the server ALSO broadcasts a redundant Packet 68 'hp' (opcode 194) for that same unit tile,
 *    the client encounters an already-cleared tile ('c == null') when processing the second death event.
 *    In 'tb.a', the FunOrb client checks 'if (null == unit) { ha.a(0, "Can see death", hp); }' which
 *    closes the TCP socket and triggers the fatal modal ("Sorry, you were removed from the game you were in...").
 *    To prevent this, 'combatSlainTiles' tracks tiles of units whose deaths are already signaled by 'ag',
 *    suppressing the redundant network packet while still executing 'hp.a(serverHa)' to authoritatively
 *    update the server board state.
 *
 * 4. POST-COMBAT TURN RETENTION & PROTOCOL COLLISION PROTECTION (v1.0.8):
 *    - Non-Zero Combat Ticket ('nextBattleId' 1..255): Offline 'ns.java:186' constructs 'ag' with 'D = 0'.
 *      Assigning a monotonically cycling 1..255 'battleId' in 'serializeEvent' ensures 'ic.e(battleId, -56)'
 *      emits Client Opcode 73 with 'subOp == battleId > 0', never colliding with Turn Banner Ready ('subOp == 0')
 *      while strictly honoring the 'ic.db' pairing assertion in 'ic.d' / 'ic.e'.
 *    - Pending-Combat Premature End-Turn Suppression ('combatResolutionPending'): While 'ag' is animating on
 *      the active client, all UI buttons including End Turn are locked ('ic.Q == true', 'ic.a(true) == false').
 *      Any 'Opcode 65 [64]' arriving from an authoritatively alive player BEFORE 'Opcode 73 [battleId > 0]'
 *      (within 'COMBAT_PENDING_GUARD_TIMEOUT_MS') is guaranteed to be an automated premature packet (such as
 *      the 'ic.b' queued multi-turn waypoint race or a client-side 'ha.e' mismatch) and is safely dropped.
 *    - 3P/4P Self-Elimination Turn Progression: If an active commander in a 3P/4P match loses their final
 *      portal-capable unit in counterattack ('ag.N == true') while another duel continues, the server cleanly
 *      advances 'ha.gb' to the next surviving commander via 'advanceTurnInternal'.
 *
 * 5. MULTIPLAYER HUMAN GOD SELECTION & HERALDRY PRESERVATION (v1.0.24):
 *    - Online multiplayer rooms only contain connected human peers (no AI bot slots in 'AoGBattleBridge').
 *    - Previously calling 'GodManager.randomizeBotGods' and 'CoatManager.randomizeBotCoats' inside
 *      'startMatch' pre-assigned 3 random gods and random bot coats to slots >= 1, causing Server Packet 77
 *      ('ha.a' -> 'ha.b') to set 'Cb = true, D = 3' (skipping the God Selection Wheel) on Steam Friends.
 *    - Now all human slots retain 'c[slot] = {-1, -1, -1}' ('Cb = false, D = 0') so every peer chooses
 *      their own pantheon, and each slot's 12-byte wire Coat of Arms is decoded directly into a fresh 'ln'
 *      instance via 'decodePlayerCoat' (never aliasing static 'ah.a' or leaking host heraldry to guests).
 * ====================================================================================================
 */
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.alterorb.server.AoGServer;
import net.alterorb.server.BattleEngine;

public final class AoGBattleBridge
implements BattleEngine {
    private static final Field MB_UNIT_TYPE = AoGBattleBridge.getField(mb.class, "A");
    private static final Field MB_COST_DELTA = AoGBattleBridge.getField(mb.class, "I");
    private static final Field MD_OWNER = AoGBattleBridge.getField(md.class, "C");
    private static final Field AG_UNSEEN = AoGBattleBridge.getField(ag.class, "z");
    private static final Field AG_ATK_COUNT = AoGBattleBridge.getField(ag.class, "H");
    private static final Field AG_DEF_COUNT = AoGBattleBridge.getField(ag.class, "y");
    private static final Field AG_DEF_DEPLOYED = AoGBattleBridge.getField(ag.class, "E");
    private static final Field QQ_SUBTYPE = AoGBattleBridge.getField(qq.class, "F");
    private static final Field RL_VAL = AoGBattleBridge.getField(rl.class, "B");
    private static final Field PJ_PLAYER = AoGBattleBridge.getField(pj.class, "z");
    private static final Field TR_MODS = AoGBattleBridge.getField(tr.class, "B");
    private static final Field EC_IS_JUMP = AoGBattleBridge.getField(ec.class, "D");
    private static final Field EC_DX = AoGBattleBridge.getField(ec.class, "z");
    private static final Field EC_DY = AoGBattleBridge.getField(ec.class, "y");
    private static final Field KF_ORIG_X = AoGBattleBridge.getField(kf.class, "J");
    private static final Field KF_ORIG_Y = AoGBattleBridge.getField(kf.class, "y");
    private static final Field KF_STRENGTH = AoGBattleBridge.getField(kf.class, "z");
    private static final Field KF_OWNER = AoGBattleBridge.getField(kf.class, "C");
    /**
     * Safety ceiling (in milliseconds) for the pending-combat premature End-Turn guard.
     * Standard combat strike animations ('ag') run for ~1.2s–2.8s (60–140 ticks at 50Hz) and immediately
     * clear 'combatResolutionPending' via Client Opcode 73 ('subOp == battleId > 0'). Expiring the guard
     * automatically after 4.5s guarantees that even an anomalous dropped packet can never permanently
     * block a human commander from clicking End Turn.
     */
    private static final long COMBAT_PENDING_GUARD_TIMEOUT_MS = 4500L;

    private ha serverHa;
    private int turnTicks = 3000;
    private int lastEliminatedMask = 0;
    private boolean matchEnded = false;
    /**
     * Monotonically cycling non-zero combat strike identifier (1..255) written into byte 9 ('ag.D')
     * of serialized Packet 68 combat strike payloads so 'ic.e(battleId, -56)' never emits 'subOp == 0'.
     */
    private int nextBattleId = 1;
    /**
     * True while at least one serialized combat strike ('ag') has been dispatched during the current turn
     * and the active client has not yet reported animation completion via Opcode 73 ('notifyCombatCompleted').
     */
    private boolean combatResolutionPending = false;
    private int pendingBattleId = 0;
    private long combatDispatchedAtMs = 0L;
    private final Random rng = new Random();
    /**
     * Set of tile coordinates (packed as (x | (y << 16))) where a unit was slain as the direct result
     * of a combat strike ('ag'). The FunOrb client simulates 'ag' -> 'nw' -> 'hp' locally and animates/removes
     * the slain unit. Suppressing secondary network Packet 68 'hp' packets for these tiles avoids duplicate death
     * resolution in 'tb.a', which would otherwise trigger "Can see death" and disconnect the match.
     */
    private final Set<Integer> combatSlainTiles = new HashSet<Integer>();

    private static Field getField(Class<?> clazz, String string) {
        try {
            Field field = clazz.getDeclaredField(string);
            field.setAccessible(true);
            return field;
        }
        catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static int getInt(Field field, Object object) {
        try {
            return field.getInt(object);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static boolean getBool(Field field, Object object) {
        try {
            return field.getBoolean(object);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public int getRoomOptionsLength() {
        try {
            if (tq.b > 0) {
                return tq.b;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return 6;
    }

    @Override
    public byte[] getDefaultRoomOptions() {
        int n2 = this.getRoomOptionsLength();
        byte[] byArray = new byte[n2];
        try {
            if (am.B != null) {
                System.arraycopy(am.B, 0, byArray, 0, Math.min(n2, am.B.length));
                return byArray;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (n2 > 0) {
            byArray[0] = 2;
        }
        return byArray;
    }

    @Override
    public byte[] buildUnlockedProfilePacket() {
        try {
            Map<Integer, int[]> map;
            int n2;
            int n3;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(512);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(7);
            for (n3 = 0; n3 < 135; ++n3) {
                dataOutputStream.writeByte(250);
            }
            for (n3 = 0; n3 < 5; ++n3) {
                dataOutputStream.writeShort(65535);
            }
            dataOutputStream.writeInt(99999);
            dataOutputStream.writeShort(511);
            for (n3 = 1; n3 < 7; ++n3) {
                dataOutputStream.writeShort(2047);
            }
            dataOutputStream.writeInt(0);
            dataOutputStream.writeInt(99999);
            Map<Integer, Map<Integer, int[]>> map2 = AoGServer.getCampaignProgress();
            int n4 = 0;
            if (map2 != null) {
                for (n2 = 0; n2 < 3; ++n2) {
                    map = map2.get(n2);
                    if (map == null || map.isEmpty()) continue;
                    n4 |= 1 << n2;
                }
            }
            dataOutputStream.writeShort(n4);
            if (map2 != null && n4 != 0) {
                for (n2 = 0; n2 < 3; ++n2) {
                    if ((n4 & 1 << n2) == 0) continue;
                    map = map2.get(n2);
                    if (map != null) {
                        for (Map.Entry<Integer, int[]> entry : map.entrySet()) {
                            int n5 = entry.getKey();
                            int[] nArray = entry.getValue();
                            dataOutputStream.writeByte(n5);
                            dataOutputStream.writeShort(nArray[0]);
                            dataOutputStream.writeShort(nArray[1]);
                        }
                    }
                    dataOutputStream.writeByte(255);
                }
            }
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (Exception exception) {
            System.err.println("[AoGBattleBridge] Error building profile packet: " + exception.getMessage());
            return new byte[0];
        }
    }

    @Override
    public synchronized BattleEngine.MatchLaunchData startMatch(String[] stringArray, byte[] byArray, int n2, boolean bl2) {
        return this.startMatch(stringArray, byArray, n2, bl2, null);
    }

    /**
     * Launches an authoritative multiplayer battle with per-player custom Coats of Arms.
     * Preserves human God Selection Wheel for all slots ('c[slot] = {-1, -1, -1}') and decodes
     * each peer's 12-byte wire Coat of Arms payload into its own isolated 'ln' instance.
     */
    @Override
    public synchronized BattleEngine.MatchLaunchData startMatch(String[] stringArray, byte[] byArray, int n2, boolean bl2, byte[][] playerCoats) {
        int n3;
        int n4 = stringArray.length;
        byte[] byArray2 = byArray != null && byArray.length >= 6 ? byArray : this.getDefaultRoomOptions();

        /*
         * Decode Multiplayer Lobby Room Options (ArmiesOfGielinor.java:2290, pf.a, bv.A={7,4,4,4,2,5}, ku.L):
         *   - Note: Unlike Singleplayer Skirmish, Multiplayer Lobby does NOT place a "Random" entry at index 0
         *     of the map theme array; index 0 is Misthalin directly, and ew.e.i (ns.A) only contains 4 themes (0..3).
         *     Previously treating 0 as random over 0..5 crashed ~33.3% of default launches in ha.l(0,0) when 4 or 5 rolled,
         *     and shifted non-zero theme selections by -1.
         */
        // Option 0: Turn time (0..6 -> 240s, 180s, 150s, 120s, 90s, 60s, 30s in ce.O)
        int n5 = AoGBattleBridge.clamp(byArray2[0] & 0xFF, 0, 6);
        // Option 1: Map size (0..3 -> Small, Medium, Large, Epic in ha.cb; ha.<init> uses rn.a for Epic on 4+ players)
        int n6 = AoGBattleBridge.clamp(byArray2[1] & 0xFF, 0, 3);
        // Option 2: Game length (0..3 -> Regular, Double, Triple, Quadruple in ha.vb; score threshold multiplier = 1 + vb)
        int n8 = AoGBattleBridge.clamp(byArray2[2] & 0xFF, 0, 3);
        // Option 3: Map theme (0..3 -> Misthalin, Kharidian Desert, Frozen Wastes, Hallowvale Swamps in ha.hb / ns.A[0..3])
        int n9 = AoGBattleBridge.clamp(byArray2[3] & 0xFF, 0, 3);
        // Option 4: Game Type (0 = Free For All, 1 = Team Game in ha.wb; team allocation in ha.<init> requires even count > 2)
        boolean blTeamOption = bl2 || (byArray2.length > 4 && (byArray2[4] & 0xFF) == 1);
        boolean bl3 = blTeamOption && n4 > 2 && n4 % 2 == 0;
        // Option 5: Trinket Limit (0..4 -> Off, 25, 75, 150, No limit in ha.Q / lf.i)
        int n11 = byArray2.length > 5 ? AoGBattleBridge.clamp(byArray2[5] & 0xFF, 0, 4) : 0;
        // Keep 3-God Pantheon selection bar enabled (ha.L = false when var14_14 == 0; previously Game Length was passed here!)
        int n7 = 0;

        int[][] nArray = new int[n4][7];
        int[][] nArray2 = new int[n4][5];
        for (n3 = 0; n3 < n4; ++n3) {
            nArray[n3][0] = 511;
            Arrays.fill(nArray[n3], 1, 7, 2047);
            Arrays.fill(nArray2[n3], -1);
        }
        // Pass n8 (Game Length 0..3) to var9_9 (ha.vb) and n7=0 to var14_14 (ha.L)
        this.serverHa = new ha(n5, stringArray, -1, nArray, -1, true, false, n6, n8, n9, bl3, nArray2, n11, n7, 0, null);
        this.serverHa.sb = -1;
        // Assign each human peer's custom Coat of Arms into its own isolated 'ln' instance.
        // Leave serverHa.c[*][*] as {-1, -1, -1} (never calling AI randomizers) so Packet 77 unpacks
        // Cb == false and D == 0 for every human player, ensuring everyone sees the God Selection Wheel.
        this.serverHa.T = new ln[n4];
        for (n3 = 0; n3 < n4; ++n3) {
            if (playerCoats != null && n3 < playerCoats.length && playerCoats[n3] != null && playerCoats[n3].length >= 12) {
                this.serverHa.T[n3] = AoGBattleBridge.decodePlayerCoat(playerCoats[n3]);
            } else if (n3 == 0 && ah.a != null && ah.a.a != null && ah.a.b != null) {
                // Fallback for Host only (slot 0) when server coat wasn't explicitly supplied
                this.serverHa.T[n3] = new ln((int[])ah.a.a.clone(), (int[])ah.a.b.clone());
            } else {
                // Guests without a supplied coat default cleanly to a blank coat, never host's ah.a
                this.serverHa.T[n3] = new ln();
            }
        }
        this.turnTicks = 3000;
        try {
            if (ce.O != null && n5 >= 0 && n5 < ce.O.length && ce.O[n5] > 0) {
                this.turnTicks = ce.O[n5];
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.lastEliminatedMask = this.serverHa.e;
        this.matchEnded = false;
        this.nextBattleId = 1;
        this.clearCombatPendingState();
        this.serverHa.a(true, 0, 0);
        this.drainAndApplyEventsOnly();
        // Pack Packet 64 byte[1] to match client decoder (ArmiesOfGielinor.java:252-255):
        // bit 0 (1) = rated (0), bit 1 (2) = team match (bl3), bits 2..4 = trinketLimitIdx (n11), bit 6 (64) = ha.L (0)
        int n12 = (bl3 ? 2 : 0) | (n11 & 7) << 2 | (n7 & 1) << 6;
        byte[][] byArrayArray = new byte[n4][];
        byte[][] byArrayArray2 = new byte[n4][];
        for (int i2 = 0; i2 < n4; ++i2) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(128);
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                dataOutputStream.writeByte(n5);
                dataOutputStream.writeByte(n12);
                dataOutputStream.writeByte(n4);
                dataOutputStream.writeByte(i2);
                for (String string : stringArray) {
                    dataOutputStream.writeByte(0);
                    dataOutputStream.write(string.getBytes(StandardCharsets.ISO_8859_1));
                    dataOutputStream.writeByte(0);
                }
                dataOutputStream.flush();
                byArrayArray[i2] = byteArrayOutputStream.toByteArray();
                vh object = new vh(32768);
                this.serverHa.a(object, -25771, i2);
                int n13 = object.q;
                byte[] byArray3 = new byte[n13 + 2];
                byArray3[0] = (byte)(n13 >>> 8 & 0xFF);
                byArray3[1] = (byte)(n13 & 0xFF);
                System.arraycopy(object.o, 0, byArray3, 2, n13);
                byArrayArray2[i2] = byArray3;
                continue;
            }
            catch (Exception exception) {
                throw new RuntimeException("Failed building match launch packets for player " + i2, exception);
            }
        }
        byte[] byArray4 = AoGBattleBridge.buildPacket67(0, 0, this.turnTicks);
        return new BattleEngine.MatchLaunchData(byArrayArray, byArrayArray2, byArray4, this.turnTicks);
    }

    @Override
    public synchronized BattleEngine.ActionResult handleAction(int n2, byte[] byArray) {
        BattleEngine.ActionResult actionResult = new BattleEngine.ActionResult();
        if (this.serverHa == null || this.matchEnded || byArray == null || byArray.length == 0) {
            return actionResult;
        }
        int n3 = byArray[0] & 0xFF;
        if (byArray.length == 2 && (n3 & 0x80) != 0) {
            if (n2 >= 0 && n2 < this.serverHa.W) {
                int n4 = AoGBattleBridge.clamp(n3 & 0x7F, 0, 5);
                int n5 = AoGBattleBridge.clamp(byArray[1] >>> 4 & 0xF, 0, 5);
                int n6 = AoGBattleBridge.clamp(byArray[1] & 0xF, 0, 5);
                this.serverHa.c[n2][0] = n4;
                this.serverHa.c[n2][1] = n5;
                this.serverHa.c[n2][2] = n6;
                this.serverHa.E[n2][n4] = Math.max(1, this.serverHa.E[n2][n4]);
                this.serverHa.E[n2][n5] = Math.max(1, this.serverHa.E[n2][n5]);
                this.serverHa.E[n2][n6] = Math.max(1, this.serverHa.E[n2][n6]);
            }
            return actionResult;
        }
        if (n2 != this.serverHa.gb) {
            return actionResult;
        }
        try {
            if (byArray.length == 1 && n3 == 64) {
                // Authoritative Pending-Combat End-Turn Guard:
                // While a combat strike ('ag') is animating on the active client, all UI buttons including
                // End Turn are locked ('ic.Q == true', 'ic.a(true) == false') until 'ic.e(battleId, -56)'
                // transmits Opcode 73 (battleId > 0). Therefore, any 'Opcode 65 [64]' arriving from an
                // authoritatively alive player BEFORE combat completion is a premature automated flush
                // (e.g., queued multi-turn waypoint race in 'ic.b(4, 123)' or a client-side 'ha.e' mismatch)
                // and must be suppressed so the commander retains their turn and remaining unmoved units.
                if ((this.serverHa.e & (1 << n2)) == 0 && this.isCombatResolutionCurrentlyPending()) {
                    System.out.printf(
                            "[AoGBattleBridge] Suppressed premature End Turn [64] from alive player %d while combat resolution (battleId=%d) is still pending.%n",
                            n2, this.pendingBattleId);
                    return actionResult;
                }
                this.advanceTurnInternal(actionResult);
                return actionResult;
            }
            if (byArray.length == 4 && n3 == 32) {
                this.clearCombatPendingState();
                int n7 = byArray[1] & 0xFF;
                int n8 = byArray[2] & 0xFF;
                int n9 = byArray[3] & 0xFF;
                this.executeCommand(new hw(n2, n8, n9, n7), actionResult);
                this.checkEliminationsAndVictory(actionResult);
                this.advanceTurnIfActivePlayerSelfEliminated(actionResult);
                return actionResult;
            }
            if ((n3 & 0xE0) == 96) {
                this.clearCombatPendingState();
                if (byArray.length == 3 || byArray.length == 4) {
                    int n10 = byArray[1] & 0xFF;
                    int n11 = byArray[2] & 0xFF;
                    int n12 = byArray.length == 4 ? byArray[3] & 0xFF : -1;
                    this.executeCommand(new jc(n2, n10, n11, n12), actionResult);
                } else if (byArray.length >= 9) {
                    int n13 = byArray[1] & 0xFF;
                    int n14 = byArray[2] & 0xFF;
                    vh vh2 = new vh(byArray);
                    vh2.q = 3;
                    this.executeCommand(new ee(n2, n13, n14, new am(vh2, n2)), actionResult);
                }
                this.checkEliminationsAndVictory(actionResult);
                this.advanceTurnIfActivePlayerSelfEliminated(actionResult);
                return actionResult;
            }
            if (n3 == 160 && byArray.length >= 4) {
                this.clearCombatPendingState();
                byte by = byArray[1];
                vh vh3 = new vh(byArray);
                vh3.q = 2;
                ik ik2 = bo.a(n2, vh3, 4368051);
                if (ik2 != null) {
                    this.executeCommand(new to(ik2, by, n2), actionResult);
                    if (vh3.q < byArray.length) {
                        am am2 = new am(vh3, n2);
                        am2.o = ik2.c;
                        this.executeCommand(am2, actionResult);
                    }
                }
                this.checkEliminationsAndVictory(actionResult);
                this.advanceTurnIfActivePlayerSelfEliminated(actionResult);
                return actionResult;
            }
            if ((n3 & 0xE0) == 0 && byArray.length >= 6) {
                this.clearCombatPendingState();
                vh vh4 = new vh(byArray);
                vh4.q = 0;
                this.executeCommand(new am(vh4, n2), actionResult);
                this.checkEliminationsAndVictory(actionResult);
                this.advanceTurnIfActivePlayerSelfEliminated(actionResult);
                return actionResult;
            }
        }
        catch (Throwable throwable) {
            System.err.println("[AoGBattleBridge] Ignored invalid/out-of-sync action from player " + n2 + ": " + String.valueOf(throwable));
        }
        return actionResult;
    }

    @Override
    public synchronized BattleEngine.ActionResult forceAdvanceTurn() {
        BattleEngine.ActionResult actionResult = new BattleEngine.ActionResult();
        if (this.serverHa == null || this.matchEnded) {
            return actionResult;
        }
        this.advanceTurnInternal(actionResult);
        return actionResult;
    }

    @Override
    public synchronized BattleEngine.ActionResult handleSurrender(int n2) {
        BattleEngine.ActionResult actionResult = new BattleEngine.ActionResult();
        if (this.serverHa == null || this.matchEnded || n2 < 0 || n2 >= this.serverHa.W) {
            return actionResult;
        }
        try {
            this.serverHa.a(0, n2);
            this.drainAndApplyEventsOnly();
            this.checkEliminationsAndVictory(actionResult);
            if (!this.matchEnded && this.serverHa.gb == n2) {
                this.advanceTurnInternal(actionResult);
            }
        }
        catch (Throwable throwable) {
            System.err.println("[AoGBattleBridge] Error handling surrender for player " + n2 + ": " + String.valueOf(throwable));
        }
        return actionResult;
    }

    @Override
    public synchronized boolean isMatchActive() {
        return this.serverHa != null && !this.matchEnded;
    }

    /**
     * Retrieves the current match round/turn number (ha.Gb in the game engine).
     * Used by AoGServer to construct Packet 76 turn-acknowledgment broadcasts.
     *
     * @return Current 0-based turn/round index.
     */
    @Override
    public synchronized int getCurrentTurn() {
        return this.serverHa != null ? this.serverHa.Gb : 0;
    }

    /**
     * Retrieves the player slot index currently holding the active turn (ha.gb in the game engine).
     * Used by AoGServer to validate incoming turn actions and Packet 73 signals.
     *
     * @return 0-based active player slot index.
     */
    @Override
    public synchronized int getActivePlayer() {
        return this.serverHa != null ? this.serverHa.gb : 0;
    }

    /**
     * Records that the active client has completed rendering combat strike animation 'battleId'
     * via Client Opcode 73 ('ic.e(battleId, -56)'), unlocking subsequent manual End Turn requests.
     */
    @Override
    public synchronized void notifyCombatCompleted(int playerIdx, int battleId) {
        if (this.serverHa == null || this.matchEnded || battleId <= 0) {
            return;
        }
        if (playerIdx == this.serverHa.gb) {
            if (!this.combatResolutionPending || this.pendingBattleId == 0 || this.pendingBattleId == battleId) {
                this.clearCombatPendingState();
            }
        }
    }

    private void clearCombatPendingState() {
        this.combatResolutionPending = false;
        this.pendingBattleId = 0;
        this.combatDispatchedAtMs = 0L;
    }

    private boolean isCombatResolutionCurrentlyPending() {
        if (!this.combatResolutionPending) {
            return false;
        }
        long elapsedMs = System.currentTimeMillis() - this.combatDispatchedAtMs;
        if (elapsedMs < 0L || elapsedMs >= COMBAT_PENDING_GUARD_TIMEOUT_MS) {
            this.clearCombatPendingState();
            return false;
        }
        return true;
    }

    /**
     * In 3-player / 4-player matches where at least two opposing teams remain alive ('!matchEnded'),
     * if the active player ('serverHa.gb') was just authoritatively eliminated during their own turn
     * (e.g., losing their final portal-capable Barbarian/Portal Mage to counterattack 'ag.N == true'),
     * immediately emit 'Packet 67' via 'advanceTurnInternal' (matching 'handleSurrender') so turn order
     * progresses cleanly after the combat animation finishes rather than stalling on an eliminated slot.
     */
    private void advanceTurnIfActivePlayerSelfEliminated(BattleEngine.ActionResult actionResult) {
        if (this.serverHa != null
                && !this.matchEnded
                && actionResult.packet67 == null
                && (this.serverHa.e & (1 << this.serverHa.gb)) != 0) {
            this.advanceTurnInternal(actionResult);
        }
    }

    private oj popEvent() {
        if (this.serverHa == null || this.serverHa.l == null) {
            return null;
        }
        if (this.serverHa.l.b(false) <= 0) {
            return null;
        }
        return (oj)this.serverHa.l.a(0);
    }

    /**
     * Executes a validated gameplay action command and pumps all resulting battle events.
     *
     * Architectural Rationale:
     *   Previously, executeCommand collected events into a static arrayList and executed each
     *   event sequentially while invoking drainAndApplyEventsOnly() at each iteration. This caused
     *   secondary events (specifically 'ag' CombatStrike events spawned dynamically when 'ns' EventAttack
     *   executes) to be drained from serverHa.l and applied on the server without ever being serialized
     *   into actionResult.eventPackets68. Consequently, clients never received Packet 68 for combat
     *   strikes, causing unit battles (such as Chieftain vs Chieftain) to freeze without damage or animation.
     *
     * Fix & Dynamic FIFO Event Pump:
     *   We process serverHa.l as a live FIFO queue. When an event (such as EventAttack 'ns') triggers
     *   cascading events (such as CombatStrike 'ag'), those new events are appended to serverHa.l.
     *   The while-loop pops each event, serializes all network-relevant events into Packet 68,
     *   binds the target unit, and applies it to the authoritative server state.
     *
     * Symbol Mappings:
     *   - mn: Abstract game action / command (e.g., am = move/attack, hw = recruit, jc = stance).
     *   - bd: Validated action execution handler (e.g., ld = move/combat coordinator).
     *   - oj: Base event class queued in serverHa.l.
     *   - kr: Movement event (serialized into 4+ bytes Packet 68).
     *   - ns: Attack dice calculation event (internal server calculation, serializeEvent returns null).
     *   - ag: Combat strike event (serialized into 10 bytes Packet 68, opcode 0x60).
     *   - bindEventUnit: Locates and binds the unit reference (oj.w) before application.
     *   - oj.a(ha, -26661): Applies event effects (damage, position, state) to the authoritative game board.
     *
     * @param mn2 The incoming client command
     * @param actionResult The container collecting network packets to broadcast to all players
     */
    private void executeCommand(mn mn2, BattleEngine.ActionResult actionResult) {
        bd bd2 = mn2.a(118, this.serverHa);
        if (bd2 == null) {
            System.out.println("[AoGBattleBridge] Command rejected by ha validator: " + mn2.getClass().getSimpleName());
            return;
        }
        bd2.a(64, this.serverHa);
        // Clear combat-slain tracking for the current command scope
        this.combatSlainTiles.clear();
        oj oj2;
        int maxEvents = 512;
        while ((oj2 = this.popEvent()) != null && --maxEvents >= 0) {
            if (oj2 instanceof ag) {
                ag ag2 = (ag) oj2;
                // If defender dies in combat strike, track defender tile (attX + dX, attY + dY)
                if (ag2.M) {
                    int defX = ag2.Q + ag2.F;
                    int defY = ag2.G + ag2.B;
                    this.combatSlainTiles.add(defX | (defY << 16));
                }
                // If attacker dies by counter-attack, track attacker tile (attX, attY)
                if (ag2.N) {
                    int attX = ag2.Q;
                    int attY = ag2.G;
                    this.combatSlainTiles.add(attX | (attY << 16));
                }
            }
            byte[] byArray = this.serializeEvent(oj2);
            if (byArray != null) {
                actionResult.eventPackets68.add(byArray);
            }
            this.bindEventUnit(oj2);
            try {
                oj2.a(this.serverHa, -26661);
            } catch (Throwable t) {
                // Non-fatal event execution warning (e.g., secondary visual effects)
                System.err.println("[AoGBattleBridge] Warning: non-fatal exception applying event " + oj2.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
    }

    private void advanceTurnInternal(BattleEngine.ActionResult actionResult) {
        oj oj2;
        int n2;
        this.clearCombatPendingState();
        int n3 = this.serverHa.W;
        int n4 = this.serverHa.gb;
        for (n2 = 0; n2 < n3 && (this.serverHa.e & 1 << (n4 = (n4 + 1) % n3)) != 0; ++n2) {
        }
        n2 = this.serverHa.Gb + 1 & 0xFF;
        this.serverHa.a(true, n4, n2);
        ArrayList<oj> arrayList = new ArrayList<oj>();
        while ((oj2 = this.popEvent()) != null) {
            arrayList.add(oj2);
        }
        for (oj oj3 : arrayList) {
            byte[] byArray;
            if (oj3 instanceof qq && AoGBattleBridge.getInt(QQ_SUBTYPE, (qq)oj3) == 0 && (byArray = this.serializeEvent(oj3)) != null) {
                actionResult.postTurnEventPackets68.add(byArray);
            }
            this.bindEventUnit(oj3);
            oj3.a(this.serverHa, -26661);
            this.drainAndApplyEventsOnly();
        }
        this.serverHa.a(1);
        actionResult.packet67 = AoGBattleBridge.buildPacket67(n4, n2, this.turnTicks);
        this.checkEliminationsAndVictory(actionResult);
    }

    private void drainAndApplyEventsOnly() {
        oj oj2;
        int n2 = 0;
        while ((oj2 = this.popEvent()) != null && n2++ < 512) {
            this.bindEventUnit(oj2);
            oj2.a(this.serverHa, -26661);
        }
    }

    private void bindEventUnit(oj oj2) {
        if (oj2 == null || this.serverHa == null) {
            return;
        }
        if (oj2.w != null) {
            return;
        }
        try {
            if (oj2 instanceof kr) {
                kr kr2 = (kr)oj2;
                oj2.w = this.serverHa.c(kr2.I, true, kr2.H);
                if (oj2.w != null && oj2.w.bb) {
                    oj2.w = oj2.w.b(false);
                }
            } else if (oj2 instanceof ec) {
                ec ec2 = (ec)oj2;
                oj2.w = this.serverHa.c(ec2.B, true, ec2.A);
                if (oj2.w != null && oj2.w.bb) {
                    oj2.w = oj2.w.b(false);
                }
            } else if (oj2 instanceof q) {
                q q2 = (q)oj2;
                oj2.w = this.serverHa.c(q2.E, true, q2.z);
                if (oj2.w != null && oj2.w.bb) {
                    oj2.w = oj2.w.b(false);
                }
            } else if (oj2 instanceof tr) {
                tr tr2 = (tr)oj2;
                oj2.w = this.serverHa.c(tr2.D, true, tr2.E);
            } else if (oj2 instanceof qq) {
                qq qq2 = (qq)oj2;
                oj2.w = this.serverHa.c(qq2.J, true, qq2.I);
            } else if (oj2 instanceof hp) {
                hp hp2 = (hp)oj2;
                oj2.w = this.serverHa.c(hp2.D, true, hp2.y);
            }
        }
        catch (Throwable throwable) {
            System.err.println("[AoGBattleBridge] Error binding unit to " + oj2.getClass().getSimpleName() + ": " + String.valueOf(throwable));
        }
    }

    private void checkEliminationsAndVictory(BattleEngine.ActionResult actionResult) {
        int n2;
        if (this.serverHa == null || this.matchEnded) {
            return;
        }

        // Authoritative Server Defeat & Elimination Evaluation:
        // In multiplayer, the engine relies on the authoritative server to evaluate active players'
        // defeat conditions (up.java: Total Portal Loss, and mc.java: Total Unit Loss) via ha.b(p, true).
        // If an active player meets their defeat condition, ha.b() marks them eliminated in ha.e
        // (ha.e |= (1 << p)) and slays remaining units.
        try {
            boolean newlyEliminated = false;
            for (int p = 0; p < this.serverHa.W; ++p) {
                if ((this.serverHa.e & (1 << p)) == 0) {
                    if (this.serverHa.b(p, true)) {
                        newlyEliminated = true;
                    }
                }
            }
            if (newlyEliminated) {
                // Drain and apply server-side cleanup events (unit removal from ha.b) so serverHa remains
                // in lockstep without broadcasting redundant Packet 68 'hp' packets (clients locally slay
                // units upon receiving Packet 73 via ha.g).
                this.drainAndApplyEventsOnly();
            }
        } catch (Throwable t) {
            System.err.println("[AoGBattleBridge] Warning: exception during elimination evaluation: " + t.getMessage());
        }

        int n3 = this.serverHa.e;
        int n4 = n3 & ~this.lastEliminatedMask;
        if (n4 != 0) {
            actionResult.newlyEliminatedPlayers.add(n3);
            this.lastEliminatedMask = n3;
        }
        int n5 = 0;
        int n6 = 0;
        boolean[] blArray = new boolean[this.serverHa.tb];
        for (n2 = 0; n2 < this.serverHa.W; ++n2) {
            int n7;
            if ((this.serverHa.e & 1 << n2) != 0 || (n7 = this.serverHa.o(n2, -1)) < 0 || n7 >= blArray.length || blArray[n7]) continue;
            blArray[n7] = true;
            ++n5;
            n6 = n7;
        }
        if (n5 <= 1 || this.serverHa.X || this.serverHa.R != 0) {
            this.matchEnded = true;
            n2 = n5 <= 1 ? n6 : (this.serverHa.p != null && this.serverHa.p.length > 0 ? this.serverHa.p[0] : n6);
            actionResult.packet69MatchEnd = this.buildPacket69(n2, blArray);
        }
    }

    private byte[] buildPacket69(int n2, boolean[] blArray) {
        try {
            int n3;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(64);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(1);
            dataOutputStream.writeByte(n2);
            for (n3 = 0; n3 < this.serverHa.tb; ++n3) {
                if (n3 == n2) continue;
                dataOutputStream.writeByte(n3);
            }
            for (n3 = 0; n3 < this.serverHa.W; ++n3) {
                boolean bl2 = this.serverHa.o(n3, -1) == n2 && (this.serverHa.e & 1 << n3) == 0;
                dataOutputStream.writeByte(bl2 ? 50 : 15);
                dataOutputStream.writeShort(bl2 ? 1525 : 1485);
            }
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (Exception exception) {
            return null;
        }
    }

    private static byte[] buildPacket67(int n2, int n3, int n4) {
        return new byte[]{(byte)(n2 & 0xFF), (byte)(n3 & 0xFF), (byte)(n4 >>> 8 & 0xFF), (byte)(n4 & 0xFF)};
    }

    private byte[] serializeEvent(oj oj2) {
        try {
            vh vh2 = new vh(256);
            if (oj2 instanceof kr) {
                kr kr2 = (kr)oj2;
                int[] nArray = kr2.B != null ? kr2.B : new int[]{};
                int n2 = nArray.length & 0x1FFF;
                vh2.b(true, n2);
                vh2.b(1, kr2.H);
                vh2.b(1, kr2.I);
                int n3 = 0;
                while (n3 < n2) {
                    int n4 = 0;
                    for (int i2 = 0; i2 < 4 && n3 < n2; ++i2, ++n3) {
                        n4 |= (nArray[n3] & 3) << i2 * 2;
                    }
                    vh2.b(1, n4);
                }
            } else if (oj2 instanceof mb) {
                mb mb2 = (mb)oj2;
                int n5 = AoGBattleBridge.getInt(MB_UNIT_TYPE, mb2);
                int n6 = AoGBattleBridge.getInt(MB_COST_DELTA, mb2);
                vh2.b(1, 32);
                vh2.b(1, n5);
                vh2.b(1, (mb2.D & 7) << 5 | n6 & 0x1F);
                vh2.b(1, mb2.z);
                vh2.b(1, mb2.H);
            } else if (oj2 instanceof md) {
                md md2 = (md)oj2;
                int n7 = AoGBattleBridge.getInt(MD_OWNER, md2);
                vh2.b(1, 100);
                vh2.b(1, 0);
                vh2.b(1, md2.D);
                vh2.b(1, md2.B);
                pd.a(md2.E, md2.z, vh2, -78);
                vh2.b(1, n7);
                vh2.b(1, 0);
                vh2.b(1, 0);
                vh2.b(1, 0);
            } else if (oj2 instanceof ag) {
                ag ag2 = (ag)oj2;
                int n8 = AoGBattleBridge.getInt(AG_UNSEEN, ag2);
                int n9 = AoGBattleBridge.getInt(AG_ATK_COUNT, ag2);
                int n10 = AoGBattleBridge.getInt(AG_DEF_COUNT, ag2);
                boolean bl2 = AoGBattleBridge.getBool(AG_DEF_DEPLOYED, ag2);
                int n11 = 0x60 | (ag2.N ? 1 : 0) | (ag2.M ? 2 : 0);
                int n12 = n9 & 7 | (n10 & 7) << 4 | (ag2.J ? 8 : 0) | (bl2 ? 128 : 0);
                // Assign a non-zero cycling battleId (1..255) instead of offline 'ag2.D' (0).
                // On clients, Packet 68 populates 'ag.D = battleId', which pairs cleanly across
                // 'ic.d(ag.D, 4)' (sets 'ic.db = battleId') and 'ic.e(ag.D, -56)' (verifies 'ic.db == battleId',
                // resets 'ic.db = 0', and sends Client Opcode 73 with 'subOp == battleId > 0' so combat
                // completion never collides with Turn Banner Ready 'subOp == 0').
                int battleId = this.nextBattleId;
                this.nextBattleId = (this.nextBattleId % 255) + 1;
                vh2.b(1, n11);
                vh2.b(1, n8);
                vh2.b(1, ag2.Q);
                vh2.b(1, ag2.G);
                pd.a(ag2.F, ag2.B, vh2, -78);
                vh2.b(1, ag2.K);
                vh2.b(1, ag2.R);
                vh2.b(1, n12);
                vh2.b(1, battleId);
                this.combatResolutionPending = true;
                this.pendingBattleId = battleId;
                this.combatDispatchedAtMs = System.currentTimeMillis();
            } else if (oj2 instanceof qq) {
                qq qq2 = (qq)oj2;
                int n13 = AoGBattleBridge.getInt(QQ_SUBTYPE, qq2);
                vh2.b(1, 0xA0 | (n13 & 3) << 3 | qq2.G & 7);
                vh2.b(1, qq2.I);
                vh2.b(1, qq2.J);
            } else if (oj2 instanceof rl) {
                vh2.b(1, 205);
                vh2.b(1, AoGBattleBridge.getInt(RL_VAL, oj2));
            } else if (oj2 instanceof vp) {
                vp vp2 = (vp)oj2;
                vh2.b(1, 206);
                vh2.b(1, vp2.E);
            } else if (oj2 instanceof pj) {
                pj pj2 = (pj)oj2;
                int n14 = AoGBattleBridge.getInt(PJ_PLAYER, pj2);
                int n15 = pj2.y < 0 ? 15 : pj2.y & 0xF;
                vh2.b(1, 197);
                vh2.b(1, (n14 & 0xF) << 4 | n15);
                pj2.B.a(-11637, vh2);
                vh2.b(1, 0);
                vh2.b(1, 0);
            } else if (oj2 instanceof q) {
                q q2 = (q)oj2;
                vh2.b(1, 193);
                vh2.b(1, q2.z);
                vh2.b(1, q2.E);
            } else if (oj2 instanceof hp) {
                hp hp2 = (hp)oj2;
                int tileKey = hp2.y | (hp2.D << 16);
                if (this.combatSlainTiles.contains(tileKey)) {
                    // Suppress redundant Packet 68 'hp' (opcode 194) broadcast:
                    // This unit's death was already authoritatively broadcast via Packet 68 'ag' (Combat Strike)
                    // with defenderDies/attackerDies flag set. The client's internal animation loop spawns its own
                    // 'hp' event to animate and remove the unit. Emitting a duplicate network Packet 68 'hp' causes
                    // the client to look up a unit on an already-cleared tile (c == null), triggering tb.a ("Can see death")
                    // which immediately disconnects the peer.
                    return null;
                }
                vh2.b(1, 194);
                vh2.b(1, hp2.y);
                vh2.b(1, hp2.D);
            } else if (oj2 instanceof tr) {
                tr tr2 = (tr)oj2;
                int n16 = AoGBattleBridge.getInt(TR_MODS, tr2);
                if (n16 == 0) {
                    vh2.b(1, 199);
                    vh2.b(1, tr2.E);
                    vh2.b(1, tr2.D);
                } else {
                    vh2.b(1, 200);
                    vh2.b(1, tr2.E);
                    vh2.b(1, tr2.D);
                    vh2.b(1, n16);
                }
            } else if (oj2 instanceof ec) {
                ec ec2 = (ec)oj2;
                boolean bl3 = AoGBattleBridge.getBool(EC_IS_JUMP, ec2);
                int n17 = AoGBattleBridge.getInt(EC_DX, ec2);
                int n18 = AoGBattleBridge.getInt(EC_DY, ec2);
                vh2.b(1, 0xC0 | (bl3 ? 11 : 17));
                vh2.b(1, ec2.A);
                vh2.b(1, ec2.B);
                pd.a(n17, n18, vh2, -78);
            } else if (oj2 instanceof kf) {
                kf kf2 = (kf)oj2;
                vh2.b(1, 207);
                vh2.b(1, kf2.H);
                vh2.b(1, kf2.F);
                vh2.b(1, AoGBattleBridge.getInt(KF_ORIG_X, kf2));
                vh2.b(1, AoGBattleBridge.getInt(KF_ORIG_Y, kf2));
                vh2.b(1, AoGBattleBridge.getInt(KF_STRENGTH, kf2));
                vh2.b(1, AoGBattleBridge.getInt(KF_OWNER, kf2));
            } else {
                return null;
            }
            return Arrays.copyOf(vh2.o, vh2.q);
        }
        catch (Throwable throwable) {
            System.err.println("[AoGBattleBridge] Failed serializing oj event " + oj2.getClass().getName() + ": " + String.valueOf(throwable));
            return null;
        }
    }

    /**
     * Decodes a 12-byte wire Coat of Arms payload ([shape0, color0, ..., shape5, color5]) into a fresh 'ln'
     * instance with bounds validation. Wire shape byte 255 represents optional '-1' ("None"), whereas all-zero
     * bytes (0,0,0,0,0,0:0,0,0,0,0,0) represent a valid all-index-0 Coat Editor choice and are preserved intact.
     */
    private static ln decodePlayerCoat(byte[] coatBytes) {
        int[] shapes = new int[6];
        int[] colors = new int[6];
        for (int i = 0; i < 6; ++i) {
            int rawShape = coatBytes[i * 2] & 0xFF;
            int shape = rawShape == 255 ? -1 : rawShape;
            // Layers 0 (shield base) and 3 (banner) are mandatory per od.c; clamp any invalid negative to 0
            if (i == 0 || i == 3) {
                if (shape < 0) {
                    shape = 0;
                }
            } else if (shape < -1) {
                shape = -1;
            }
            shapes[i] = shape;
            // Validate palette color index against the 32-entry palette bd.q to prevent AIOOBE during rendering
            colors[i] = AoGBattleBridge.clamp(coatBytes[i * 2 + 1] & 0xFF, 0, 31);
        }
        return new ln(shapes, colors);
    }

    private static int clamp(int n2, int n3, int n4) {
        return Math.max(n3, Math.min(n4, n2));
    }
}

