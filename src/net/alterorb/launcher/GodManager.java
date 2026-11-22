package net.alterorb.launcher;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/**
 * ====================================================================================================
 * Armies of Gielinor - Tactical AI God Selection Randomizer
 * Component: net.alterorb.launcher.GodManager
 * Target Runtime: OpenJDK 18.0.2.1 (Major version <= 62.0)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    In the original 2009 FunOrb client, computer opponents in Skirmish (free battle / sandbox) mode
 *    did not choose their own gods or pantheons; the game engine defaulted to copying the human player's
 *    3 chosen gods into all AI player slots via `ha.g()` (`rd.j.xb.g(...)`). As a result, all bots
 *    fielded the exact same god units and abilities as the human player in every skirmish match.
 *
 *    `GodManager` introduces a toggle-able, authentic, and diverse pantheon selection for AI opponents:
 *    a) Option A (Default: `ai_gods_mode=random`):
 *       Each computer opponent receives 3 distinct, freshly randomized gods from all 6 available
 *       pantheons (Saradomin, Zamorak, Guthix, Seren, Bandos, Menaphite).
 *    b) Option B (`ai_gods_mode=mirror`):
 *       Preserves the legacy 2009 behavior where all bots inherit the human player's gods.
 *    c) Human player protection:
 *       Slot 0 (`rd.j.q` / local commander) and remote human players in multiplayer are 100% preserved.
 *    d) Story Campaign protection:
 *       Story Campaign matches (`ha.K != null`) have hand-crafted scenario pantheons and are NEVER altered.
 *    e) Mid-match progression protection:
 *       God level progression accumulated during combat (e.g. leveling up gods at summoning portals)
 *       is strictly preserved and never reset mid-battle.
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Symbol                     | Real Architectural Meaning       | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    `wf.q` (String[7])         | God Display Names Array          | 0=Neutral, 1=Saradomin, 2=Zamorak,
 *                               |                                  | 3=Guthix, 4=Seren, 5=Bandos, 6=Menaphite.
 *    `ha.c` (`int[W][3]`)       | Active Player Gods Array         | 3 selected god IDs per player (0..5:
 *                               |                                  | 0=Sara, 1=Zam, 2=Guth, 3=Ser, 4=Ban, 5=Men).
 *    `ha.E` (`int[W][7]`)       | Player God Levels Matrix         | Indices 0..5: levels per god (1..5).
 *                               |                                  | Index 6: maximum god level of player.
 *    `ha.W` (`int`)             | Total Player Count in Match      | Player count (2 to 8).
 *    `ha.K` (`om`)              | Campaign Scenario Definition     | Non-null in Campaign; null in Skirmish.
 *    `ha.Cb` (`boolean`)        | God Selection Wheel Finished     | False while wheel spins; true once finished.
 *    `rd.j` (`ic`)              | Client Match Coordinator         | Active client battle controller.
 *    `rd.j.xb` (`ha`)           | Master Game Simulation Engine    | Battlefield state, board tiles, players.
 *    `ic.Eb` (`ej[]`)           | AI Bot Controller Array          | Non-null for bot slots; null for human slots.
 *    `ic.lb` (`boolean`)        | Network Multiplayer Mode Flag    | True in multiplayer; false in local skirmish.
 *    `ic.q` (`int`)             | Local Human Player Slot Index    | Slot 0 in local skirmish matches.
 *    `player.properties`        | User Configuration Store         | Persists `ai_gods_mode=random|mirror`.
 *
 * 3. INLINE EXPLANATIONS:
 *    - All engine field interactions use reflection or safe object casting to prevent compile-time
 *      tight coupling and ensure complete safety if classes are loaded under different ClassLoaders.
 *    - Random selection uses Fisher-Yates shuffle over `{0, 1, 2, 3, 4, 5}` to mathematically guarantee
 *      zero duplicate gods within any bot's 3 chosen pantheons.
 *    - `lastRandomizedMatch` identity tracking ensures `randomizeBotGods` fires exactly once per match
 *      lifecycle, safeguarding in-game level-ups from being overwritten.
 * ====================================================================================================
 */
public final class GodManager {

    private static final Random RNG = new Random();

    /** Display names corresponding to god IDs 0..5 in `ha.c`. */
    public static final String[] GOD_NAMES = {
        "Saradomin", "Zamorak", "Guthix", "Seren", "Bandos", "Menaphite"
    };

    /** Tracks the last randomized match instance to prevent duplicate randomization and level resets. */
    private static Object lastRandomizedMatch = null;

    private GodManager() {}

    /**
     * Inspects configuration sources to determine whether AI opponents should receive randomized gods.
     * 
     * @return true if random AI gods are enabled (default: Option A); false if mirrored (Option B).
     */
    public static boolean isRandomAiGodsEnabled() {
        try {
            Properties props = new Properties();
            Path[] candidatePaths = new Path[]{
                Paths.get("enhanced_options.txt"),
                Paths.get("enhanced_options.properties"),
                Paths.get("..", "enhanced_options.txt"),
                Paths.get(System.getProperty("user.home"), ".armiesofgielinor", "enhanced_options.txt"),
                Paths.get("player.properties"),
                Paths.get("..", "player.properties"),
                Paths.get(System.getProperty("user.home"), ".armiesofgielinor", "player.properties"),
                Paths.get(System.getProperty("user.home"), ".alterorb", "player.properties")
            };

            for (Path p : candidatePaths) {
                if (Files.exists(p)) {
                    try (InputStream in = Files.newInputStream(p)) {
                        props.load(in);
                        String mode = props.getProperty("ai_gods_mode");
                        if (mode != null && !mode.isBlank()) {
                            return !"mirror".equalsIgnoreCase(mode.trim());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fall back safely to default enabled
        }
        return true;
    }

    /**
     * Updates the AI gods selection mode across local and user-profile configuration files.
     * 
     * @param mode "random" (Option A) or "mirror" (Option B).
     */
    public static void setAiGodsMode(String mode) {
        String cleanMode = "mirror".equalsIgnoreCase(mode) ? "mirror" : "random";
        Path[] targetPaths = new Path[]{
            Paths.get("enhanced_options.txt"),
            Paths.get(System.getProperty("user.home"), ".armiesofgielinor", "enhanced_options.txt")
        };

        for (Path p : targetPaths) {
            try {
                Properties props = new Properties();
                if (Files.exists(p)) {
                    try (InputStream in = Files.newInputStream(p)) {
                        props.load(in);
                    }
                } else if (p.getParent() != null) {
                    Files.createDirectories(p.getParent());
                }
                props.setProperty("ai_gods_mode", cleanMode);
                try (OutputStream out = Files.newOutputStream(p)) {
                    props.store(out, "Armies of Gielinor Enhanced Options");
                }
            } catch (Throwable ignored) {
                // Silently continue if path inaccessible
            }
        }
    }

    /**
     * Randomizes the 3 chosen gods and initializes starting god levels for all computer opponents.
     * Preserved for backward-compatibility with standalone server wrappers (e.g. AoGBattleBridge).
     * 
     * @param haInstance The active simulation engine (`ha`) instance.
     * @param humanPlayerIdx The player slot index of the human player (strictly preserved).
     */
    public static void randomizeBotGods(Object haInstance, int humanPlayerIdx) {
        if (haInstance == null) return;
        try {
            Field wField = haInstance.getClass().getDeclaredField("W");
            wField.setAccessible(true);
            int totalPlayers = wField.getInt(haInstance);
            Object[] mockEb = new Object[totalPlayers];
            for (int s = 0; s < totalPlayers; s++) {
                mockEb[s] = (s == humanPlayerIdx) ? null : new Object();
            }
            randomizeBotGods(haInstance, mockEb);
        } catch (Throwable t) {
            System.err.println("[GodManager] Note: Legacy randomizeBotGods fallback skipped: " + t.getMessage());
        }
    }

    /**
     * Randomizes the 3 chosen gods and initializes starting god levels for computer opponents (`eb[slot] != null`)
     * while strictly protecting human players (`eb[slot] == null`).
     *
     * In Skirmish mode, the 2009 Jagex engine randomly assigns computer opponents across player slots via
     * `var19_25 = ns.a(false, 1 + -mg.g + this.xb.W, li.n)` (`ic.java:10654`).
     * By dynamically inspecting the active AI controller array (`ic.Eb`), this method guarantees that
     * the human player's chosen gods are always preserved regardless of whether the Jagex engine placed
     * them into slot 0 or slot 1.
     *
     * @param haInstance The active simulation engine (`ha`) instance.
     * @param eb The active bot controllers array (`ic.Eb`).
     */
    public static void randomizeBotGods(Object haInstance, Object[] eb) {
        if (haInstance == null || !isRandomAiGodsEnabled()) {
            return;
        }

        try {
            Class<?> haClass = haInstance.getClass();

            // Story Campaign match guard: NEVER modify campaign matches
            Field kField = haClass.getDeclaredField("K");
            kField.setAccessible(true);
            if (kField.get(haInstance) != null) {
                return;
            }

            Field wField = haClass.getDeclaredField("W");
            wField.setAccessible(true);
            int totalPlayers = wField.getInt(haInstance);
            if (totalPlayers <= 1) {
                return;
            }

            Field cField = haClass.getDeclaredField("c");
            cField.setAccessible(true);
            int[][] c = (int[][]) cField.get(haInstance);
            if (c == null) {
                return;
            }

            Field eField = haClass.getDeclaredField("E");
            eField.setAccessible(true);
            int[][] E = (int[][]) eField.get(haInstance);
            if (E == null) {
                return;
            }

            for (int slot = 0; slot < totalPlayers; slot++) {
                boolean isBot = (eb != null && slot < eb.length) ? (eb[slot] != null) : (slot > 0);

                // Strict safety check: human player slots are 100% preserved
                if (!isBot) {
                    continue;
                }

                // Roll 3 distinct gods from 0..5 (Saradomin, Zamorak, Guthix, Seren, Bandos, Menaphite)
                List<Integer> pool = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
                Collections.shuffle(pool, RNG);

                int g0 = pool.get(0);
                int g1 = pool.get(1);
                int g2 = pool.get(2);

                c[slot][0] = g0;
                c[slot][1] = g1;
                c[slot][2] = g2;

                // Initialize starting god levels: level 1 for the 3 active gods, level 0 for inactive
                Arrays.fill(E[slot], 0);
                E[slot][g0] = 1;
                E[slot][g1] = 1;
                E[slot][g2] = 1;
                E[slot][6] = 1; // Master maximum god level for this player
            }
        } catch (Throwable t) {
            System.err.println("[GodManager] Note: God randomization skipped: " + t.getMessage());
        }
    }

    /**
     * Periodically invoked from StandaloneRunner's background monitor loop.
     * Detects when a singleplayer Skirmish match finishes the god selection wheel,
     * immediately replacing the mirrored bot gods with 3 distinct randomized pantheons
     * before the first turn begins.
     */
    public static void checkAndRandomizeActiveMatch() {
        try {
            Class<?> rdClass = Class.forName("rd");
            Field jField = rdClass.getDeclaredField("j");
            jField.setAccessible(true);
            Object ic = jField.get(null);
            if (ic == null) return;

            Field xbField = ic.getClass().getDeclaredField("xb");
            xbField.setAccessible(true);
            Object ha = xbField.get(ic);
            if (ha == null) return;

            // 1. Campaign guard: do not touch Story Campaign matches
            Field kField = ha.getClass().getDeclaredField("K");
            kField.setAccessible(true);
            if (kField.get(ha) != null) return;

            // 2. Multiplayer guard: do not touch Multiplayer matches (ic.lb == true)
            Field lbField = ic.getClass().getDeclaredField("lb");
            lbField.setAccessible(true);
            if (lbField.getBoolean(ic)) return;

            // 3. Check if match is already randomized once (preserve in-game progression)
            if (lastRandomizedMatch == ha) return;

            // 4. God selection wheel synchronization guard:
            // ha.Cb is false while the wheel is active; ha.g sets it to true once gods are finalized.
            Field cbField = ha.getClass().getDeclaredField("Cb");
            cbField.setAccessible(true);
            if (!cbField.getBoolean(ha)) {
                return; // God selection wheel is still active on screen
            }

            // 5. Inspect ic.Eb to identify human and bot slots
            Field ebField = ic.getClass().getDeclaredField("Eb");
            ebField.setAccessible(true);
            Object[] eb = (Object[]) ebField.get(ic);
            if (eb == null || eb.length <= 1) return;

            int botCount = 0;
            for (Object bot : eb) {
                if (bot != null) botCount++;
            }
            if (botCount == 0) return; // No bots in match (e.g. local hotseat)

            // Randomize bot slots while strictly preserving the human player's chosen gods
            randomizeBotGods(ha, eb);
            lastRandomizedMatch = ha;
        } catch (Throwable ignored) {
            // Silently ignore if engine classes not active
        }
    }

    /**
     * Resets the match tracking state (useful for sandbox tests).
     */
    public static void resetMatchTracker() {
        lastRandomizedMatch = null;
    }
}
