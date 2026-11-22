package net.alterorb.launcher;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/**
 * ====================================================================================================
 * Armies of Gielinor - Tactical AI Coat of Arms Randomizer & HUD Heraldry Synchronizer
 * Component: net.alterorb.launcher.CoatManager
 * Target Runtime: OpenJDK 18.0.2.1 (Major version <= 62.0)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    In the original 2009 FunOrb client, computer opponents in Skirmish (free battle / sandbox) mode
 *    did not have unique heraldic crests; the game engine defaulted to copying the human player's
 *    custom Coat of Arms (`ah.a`) into every AI player slot (`rd.j.xb.T[i] = ah.a`), or assigning
 *    the blank/default generic coat (`new ln()`). As a result, AI portals, fortress flags, turn
 *    announcement banners, scoreboard entries, and the top-right turn order list duplicated the player's
 *    crest or showed generic crests.
 *
 *    Furthermore, the in-game HUD displays player turn order in the top-right corner (`ic.k(-32400)`)
 *    using a dedicated, cached sprite array: `ic.Wb` (type `wk[]`), which is generated once on frame 1
 *    via `ic.e(16)`. If `rd.j.xb.T` was randomized after frame 1 or initialized with generic crests,
 *    `ic.Wb` was never invalidated, leaving the top-right turn order list permanently displaying generic
 *    crests even while map portals, flags, and turn banners showed the new random coats.
 *
 *    `CoatManager` dynamically generates authentic, distinct, and aesthetically balanced heraldic
 *    coats for computer opponents with full toggle-able configuration via `enhanced_options.txt`:
 *    a) Option A (Default: `ai_coats_mode=random`):
 *       Generates randomized, valid combinations of shield bases, heraldic bearers, banners,
 *       toppers, and symbols across the 32-color palette for all bot slots.
 *    b) Option B (`ai_coats_mode=mirror`):
 *       Preserves the original 2009 FunOrb behavior where all bots inherit the player's crest.
 *    c) Top-Right Turn Order Synchronization (`ic.Wb`):
 *       When bot coats are randomized or a new skirmish match begins, safely invalidates the cached
 *       `ic.Wb` sprite array (`ic.Wb = null`), triggering the engine's render loop (`ic.TC`) to cleanly
 *       regenerate `ic.Wb` with the new randomized crests on the very next video frame with zero race conditions.
 *    d) Preserves human player crests (Slot 0 and any remote human player) with 100% integrity.
 *    e) Preserves Story Campaign matches intact (Campaign scenario bosses keep their fixed crests;
 *       guarded by `ha.K != null`).
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Symbol                     | Real Architectural Meaning       | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    `ln`                       | Coat of Arms Data Model          | Encapsulates shapes (a) and colors (b).
 *    `ln.a` (int[6])            | Layer Shape / Symbol Indices     | Indices 0..5 (Base, L-Bearer, R-Bearer,
 *                               |                                  | Banner, Topper, Main Emblem).
 *    `ln.b` (int[6])            | Layer Color Palette Indices      | Indices 0..5 (0..31 in palette bd.q).
 *    `ah.a` (`ln`)              | Static Human Player Crest        | Persisted profile crest loaded on boot.
 *    `ha.T` (`ln[]`)            | Battlefield Player Crest Array   | Array indexed by player slot (0..W-1).
 *    `ha.K` (`om`)              | Campaign Scenario Descriptor     | null in Skirmish; non-null in Campaign.
 *    `ic.Wb` (`wk[]`)           | Turn Order Crest Sprite Cache    | Cached player crest sprites drawn top-right.
 *    `ic.e(int)`                | Crest Sprite Generation Method   | Renders each `ha.T[n]` into `ic.Wb[n]`.
 *    `ic.k(-32400)` / `TC`      | Top-Right HUD Render Procedure   | Draws turn order list using `ic.Wb[n]`.
 *    `qp.a`                     | Crest & Frame Drawing Routine    | Draws `ic.Wb[n]` within player team frame.
 *    `rd.j.xb` (`ha`)           | Active Battlefield Simulation    | Holds game rules, units, and player data.
 *    `bc.j` (`ru[][]`)          | Cached Heraldry Sprite Matrix    | 6 sub-arrays of sprites loaded from JS5.
 *    `bd.q` (`int[32]`)         | Master Heraldic Color Palette    | 32 RGB hex color definitions.
 *    `od.c` (`boolean[6]`)      | Layer Optionality Flags          | Specifies which layers can be -1 (empty).
 *    `ic.Eb` (`ej[]`)           | AI Bot Controller Array          | Non-null for bot slots; null for human slots.
 *    `ic.lb` (`boolean`)        | Network Multiplayer Mode Flag    | True in multiplayer; false in local skirmish.
 *    `enhanced_options.txt`     | User Configuration Store         | Persists `ai_coats_mode=random|mirror`.
 *
 * 3. INLINE EXPLANATIONS:
 *    - All sprite array lengths are dynamically read from `bc.j` when available, with conservative
 *      fallbacks ensuring no index ever triggers an out-of-bounds error.
 *    - Layer optionality adheres to `od.c`: base (0) and banner (3) are always present;
 *      bearers (1, 2) and toppers (4) are optional (-1).
 *    - Bearers are generated symmetrically by default for classic medieval heraldic balance.
 *    - `isGenericCoat` identifies blank default `new ln()` instances (`[0, -1, -1, 0, -1, 0]`, all zero colors).
 *    - `isSameCoat` performs structural shape & color comparison across distinct object instances.
 *    - Invalidation of `ic.Wb` is performed by writing `null` to the field pointer, which is atomic
 *      in Java. The render thread itself then calls `this.e(16)` inside its active graphics context,
 *      eliminating any multi-threaded rendering race condition on `ku.J` or `qn`.
 * ====================================================================================================
 */
public final class CoatManager {

    private static final Random RNG = new Random();

    // Default conservative bounds matching verified FunOrb cache indices
    private static final int DEFAULT_MAX_BASES = 8;
    private static final int DEFAULT_MAX_BEARERS = 5;
    private static final int DEFAULT_MAX_BANNERS = 4;
    private static final int DEFAULT_MAX_TOPPERS = 3;
    private static final int DEFAULT_MAX_ICONS = 9;
    private static final int PALETTE_COLOR_COUNT = 32;

    // Tracks active match instance and HUD sprite cache invalidation state
    private static Object lastRandomizedMatch = null;
    private static boolean matchWbRefreshed = false;
    // Tracks whether initial player.properties coat has been deserialized into ah.a
    private static volatile boolean initialCoatLoaded = false;

    private CoatManager() {}

    /**
     * Inspects configuration sources to determine whether AI opponents should receive randomized crests.
     * 
     * @return true if random AI crests are enabled (default: Option A); false if mirrored (Option B).
     */
    public static boolean isRandomAiCoatsEnabled() {
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
                        String mode = props.getProperty("ai_coats_mode");
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
     * Updates the AI heraldry selection mode across local and user-profile configuration files.
     * 
     * @param mode "random" (Option A) or "mirror" (Option B).
     */
    public static void setAiCoatsMode(String mode) {
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
                props.setProperty("ai_coats_mode", cleanMode);
                try (OutputStream out = Files.newOutputStream(p)) {
                    props.store(out, "Armies of Gielinor Enhanced Options");
                }
            } catch (Throwable ignored) {
                // Silently continue if path inaccessible
            }
        }
    }

    /**
     * Checks whether a coat object represents the default blank generic coat (`new ln()`).
     * 
     * @param coat The coat object to inspect.
     * @return true if coat is null or has default generic shapes [0, -1, -1, 0, -1, 0] and all-zero colors.
     */
    public static boolean isGenericCoat(Object coat) {
        if (coat == null) return true;
        try {
            Field fA = coat.getClass().getDeclaredField("a");
            Field fB = coat.getClass().getDeclaredField("b");
            fA.setAccessible(true);
            fB.setAccessible(true);
            int[] s = (int[]) fA.get(coat);
            int[] c = (int[]) fB.get(coat);
            if (s == null || c == null || s.length < 6 || c.length < 6) return true;
            boolean defaultShapes = s[0] == 0 && s[1] == -1 && s[2] == -1 && s[3] == 0 && s[4] == -1 && s[5] == 0;
            boolean allZeroColors = c[0] == 0 && c[1] == 0 && c[2] == 0 && c[3] == 0 && c[4] == 0 && c[5] == 0;
            return defaultShapes && allZeroColors;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Checks structural equality between two coat objects by comparing their shape and color arrays.
     * 
     * @param coat1 First coat object.
     * @param coat2 Second coat object.
     * @return true if both coats have identical shape and color layers.
     */
    public static boolean isSameCoat(Object coat1, Object coat2) {
        if (coat1 == coat2) return true;
        if (coat1 == null || coat2 == null) return false;
        try {
            Field fA = coat1.getClass().getDeclaredField("a");
            Field fB = coat1.getClass().getDeclaredField("b");
            fA.setAccessible(true);
            fB.setAccessible(true);
            int[] s1 = (int[]) fA.get(coat1);
            int[] c1 = (int[]) fB.get(coat1);
            int[] s2 = (int[]) fA.get(coat2);
            int[] c2 = (int[]) fB.get(coat2);
            return Arrays.equals(s1, s2) && Arrays.equals(c1, c2);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Generates a unique, randomized `ln` Coat of Arms instance suitable for an AI opponent.
     * 
     * @return A newly instantiated `ln` object, or a default instance if reflection fails.
     */
    public static Object generateRandomCoat() {
        try {
            Class<?> lnClass = Class.forName("ln");
            Constructor<?> ctor = lnClass.getDeclaredConstructor(int[].class, int[].class);
            ctor.setAccessible(true);

            int[] shapes = new int[6];
            int[] colors = new int[6];

            int maxBases = DEFAULT_MAX_BASES;
            int maxBearers = DEFAULT_MAX_BEARERS;
            int maxBanners = DEFAULT_MAX_BANNERS;
            int maxToppers = DEFAULT_MAX_TOPPERS;
            int maxIcons = DEFAULT_MAX_ICONS;

            // Attempt dynamic discovery from loaded sprite cache (bc.j)
            try {
                Class<?> bcClass = Class.forName("bc");
                Field jField = bcClass.getDeclaredField("j");
                jField.setAccessible(true);
                Object[][] spriteMatrix = (Object[][]) jField.get(null);
                if (spriteMatrix != null && spriteMatrix.length >= 6) {
                    if (spriteMatrix[0] != null && spriteMatrix[0].length > 0) maxBases = spriteMatrix[0].length;
                    if (spriteMatrix[1] != null && spriteMatrix[1].length > 0) maxBearers = spriteMatrix[1].length;
                    if (spriteMatrix[3] != null && spriteMatrix[3].length > 0) maxBanners = spriteMatrix[3].length;
                    if (spriteMatrix[4] != null && spriteMatrix[4].length > 0) maxToppers = spriteMatrix[4].length;
                    if (spriteMatrix[5] != null && spriteMatrix[5].length > 0) maxIcons = spriteMatrix[5].length;
                }
            } catch (Throwable ignored) {
                // Fall back cleanly to verified FunOrb cache defaults
            }

            // Layer 0: Base shield shape (mandatory: index 0..maxBases-1)
            shapes[0] = RNG.nextInt(maxBases);
            colors[0] = RNG.nextInt(PALETTE_COLOR_COUNT);

            // Layer 1 & 2: Bearers (symmetrical heraldry: 50% chance of bearers)
            if (RNG.nextBoolean() && maxBearers > 0) {
                int bearerShape = RNG.nextInt(maxBearers);
                int bearerColor = RNG.nextInt(PALETTE_COLOR_COUNT);
                shapes[1] = bearerShape;
                colors[1] = bearerColor;
                shapes[2] = bearerShape;
                colors[2] = bearerColor;
            } else {
                shapes[1] = -1;
                colors[1] = 0;
                shapes[2] = -1;
                colors[2] = 0;
            }

            // Layer 3: Banner style (mandatory: index 0..maxBanners-1)
            shapes[3] = RNG.nextInt(maxBanners);
            colors[3] = RNG.nextInt(PALETTE_COLOR_COUNT);

            // Layer 4: Topper / Crest (optional: 60% chance of a topper)
            if (RNG.nextInt(10) < 6 && maxToppers > 0) {
                shapes[4] = RNG.nextInt(maxToppers);
                colors[4] = RNG.nextInt(PALETTE_COLOR_COUNT);
            } else {
                shapes[4] = -1;
                colors[4] = 0;
            }

            // Layer 5: Main icon / emblem (mandatory: index 0..maxIcons-1)
            shapes[5] = RNG.nextInt(maxIcons);
            colors[5] = RNG.nextInt(PALETTE_COLOR_COUNT);

            return ctor.newInstance(shapes, colors);
        } catch (Throwable t) {
            System.err.println("[CoatManager] Warning: Reflection coat generation fallback: " + t.getMessage());
            try {
                Class<?> lnClass = Class.forName("ln");
                Constructor<?> ctor = lnClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (Throwable fallbackEx) {
                return null;
            }
        }
    }

    /**
     * Inspects a match player crest array and populates computer opponent slots (indices >= 1)
     * with distinct, newly generated coats while strictly protecting the human player's crest in slot 0.
     * Preserved with void return type for strict binary backward-compatibility with standalone server wrappers (e.g. AoGBattleBridge).
     *
     * @param coats The battlefield heraldry array (`ha.T`).
     * @param humanCoat The reference to the human player's custom crest (`ah.a`).
     */
    public static void randomizeBotCoats(Object[] coats, Object humanCoat) {
        randomizeBotCoats(coats, humanCoat, null);
    }

    /**
     * Inspects a match player crest array and populates computer opponent slots (`eb[slot] != null`)
     * with distinct, newly generated coats while strictly protecting the human player's crest (`eb[slot] == null`).
     * 
     * In Skirmish mode, the 2009 Jagex engine randomly assigns computer opponents across player slots via
     * `var19_25 = ns.a(false, 1 + -mg.g + this.xb.W, li.n)` (`ic.java:10654`).
     * By dynamically inspecting the active AI controller array (`ic.Eb`), this method guarantees that
     * the human player always retains their custom-designed crest (`ah.a`) regardless of whether the Jagex engine
     * placed them into slot 0 or slot 1.
     *
     * @param coats The battlefield heraldry array (`ha.T`).
     * @param humanCoat The reference to the human player's custom crest (`ah.a`).
     * @param eb The active bot controllers array (`ic.Eb`). If null, falls back to slot 0 = human.
     * @return true if any bot slot was modified with a new randomized coat.
     */
    public static boolean randomizeBotCoats(Object[] coats, Object humanCoat, Object[] eb) {
        if (coats == null || coats.length <= 1) {
            return false;
        }

        boolean randomEnabled = isRandomAiCoatsEnabled();
        boolean anyModified = false;
        Set<String> usedSignatures = new HashSet<>();

        // Record human coat signature to prevent bots from duplicating it
        if (humanCoat != null) {
            try {
                Field fA = humanCoat.getClass().getDeclaredField("a");
                Field fB = humanCoat.getClass().getDeclaredField("b");
                fA.setAccessible(true);
                fB.setAccessible(true);
                int[] s = (int[]) fA.get(humanCoat);
                int[] c = (int[]) fB.get(humanCoat);
                String sig = StandaloneRunner.serializeCoat(s, c);
                if (sig != null) {
                    usedSignatures.add(sig);
                }
            } catch (Throwable ignored) {}
        }

        // Accurately determine human and bot slots:
        // Human slots (`eb[i] == null`) MUST have humanCoat!
        // Bot slots (`eb[i] != null`) receive generated random coats (or mirrored if Option B)
        for (int i = 0; i < coats.length; i++) {
            boolean isBot = (eb != null && i < eb.length) ? (eb[i] != null) : (i > 0);

            if (!isBot) {
                // Strictly protect and restore human player crest
                if (humanCoat != null && (coats[i] == null || isGenericCoat(coats[i]) || !isSameCoat(coats[i], humanCoat))) {
                    coats[i] = humanCoat;
                    anyModified = true;
                }
            } else {
                // Computer opponent slot
                if (randomEnabled) {
                    if (coats[i] == null || coats[i] == humanCoat || isSameCoat(coats[i], humanCoat) || isGenericCoat(coats[i])) {
                        Object randomCoat = null;
                        for (int attempt = 0; attempt < 10; attempt++) {
                            Object candidate = generateRandomCoat();
                            if (candidate == null) break;
                            try {
                                Field fA = candidate.getClass().getDeclaredField("a");
                                Field fB = candidate.getClass().getDeclaredField("b");
                                fA.setAccessible(true);
                                fB.setAccessible(true);
                                int[] s = (int[]) fA.get(candidate);
                                int[] c = (int[]) fB.get(candidate);
                                String sig = StandaloneRunner.serializeCoat(s, c);
                                if (sig != null && !usedSignatures.contains(sig)) {
                                    usedSignatures.add(sig);
                                    randomCoat = candidate;
                                    break;
                                }
                            } catch (Throwable ignored) {
                                randomCoat = candidate;
                                break;
                            }
                        }
                        if (randomCoat != null) {
                            coats[i] = randomCoat;
                            anyModified = true;
                        }
                    }
                } else {
                    // Option B: Mirror human coat
                    if (humanCoat != null && (coats[i] == null || isGenericCoat(coats[i]) || !isSameCoat(coats[i], humanCoat))) {
                        coats[i] = humanCoat;
                        anyModified = true;
                    }
                }
            }
        }
        return anyModified;
    }

    /**
     * Periodically called by StandaloneRunner's main loop (every 50ms).
     * If an active Skirmish match is running (`rd.j.xb.T` non-null):
     * 1. Detects computer opponents with generic or human-cloned crests and assigns unique coats.
     * 2. Synchronizes the top-right turn order HUD sprite cache (`ic.Wb`), invalidating stale
     *    sprites so `ic.k(-32400)` cleanly redraws the turn order list with the new AI heraldry.
     * 3. Story Campaign matches (`ha.K != null`) and human player crests are 100% preserved.
     */
    public static void checkAndRandomizeActiveMatch() {
        try {
            Class<?> rdClass = Class.forName("rd");
            Field jField = rdClass.getDeclaredField("j");
            jField.setAccessible(true);
            Object ic = jField.get(null);
            if (ic == null) {
                lastRandomizedMatch = null;
                matchWbRefreshed = false;
                return;
            }

            Field xbField = ic.getClass().getDeclaredField("xb");
            xbField.setAccessible(true);
            Object ha = xbField.get(ic);
            if (ha == null) {
                lastRandomizedMatch = null;
                matchWbRefreshed = false;
                return;
            }

            Class<?> haClass = ha.getClass();

            // 1. Story Campaign match guard: NEVER modify campaign matches (ha.K != null)
            Field kField = haClass.getDeclaredField("K");
            kField.setAccessible(true);
            if (kField.get(ha) != null) {
                return;
            }

            // 2. Multiplayer guard: NEVER modify multiplayer matches (ic.lb == true)
            Field lbField = ic.getClass().getDeclaredField("lb");
            lbField.setAccessible(true);
            if (lbField.getBoolean(ic)) {
                return;
            }

            // 3. Inspect ic.Eb (AI bot controller array)
            Field ebField = ic.getClass().getDeclaredField("Eb");
            ebField.setAccessible(true);
            Object[] eb = (Object[]) ebField.get(ic);
            if (eb == null || eb.length <= 1) {
                return;
            }

            // 4. Count bots: If botCount == 0 (e.g. local hotseat), do not modify anything!
            int botCount = 0;
            for (Object bot : eb) {
                if (bot != null) botCount++;
            }
            if (botCount == 0) {
                return;
            }

            // Track match transitions
            if (ha != lastRandomizedMatch) {
                lastRandomizedMatch = ha;
                matchWbRefreshed = false;
            }

            Field tField = haClass.getDeclaredField("T");
            tField.setAccessible(true);
            Object[] coats = (Object[]) tField.get(ha);
            if (coats == null || coats.length <= 1 || coats.length != eb.length) return;

            Class<?> ahClass = Class.forName("ah");
            Field aField = ahClass.getDeclaredField("a");
            aField.setAccessible(true);
            Object humanCoat = aField.get(null);

            boolean coatsModified = randomizeBotCoats(coats, humanCoat, eb);

            // Turn Order HUD Cache Synchronization (`ic.Wb`):
            // In the top-right turn order list, `ic.k(-32400)` draws player crests using the
            // cached sprite array `ic.Wb` (type `wk[]`), generated once on frame 1 by `ic.e(16)`.
            // If `rd.j.xb.T` was randomized after frame 1 or initialized with generic crests,
            // resetting `ic.Wb` to null triggers the engine's render loop (`ic.TC`) to cleanly
            // regenerate `ic.Wb` with the newly assigned bot crests on the very next video frame.
            if (coatsModified || !matchWbRefreshed) {
                try {
                    Field wbField = ic.getClass().getDeclaredField("Wb");
                    wbField.setAccessible(true);
                    Object wb = wbField.get(ic);
                    if (wb != null) {
                        wbField.set(ic, null);
                        matchWbRefreshed = true;
                    }
                } catch (Throwable ignored) {
                    // Silently continue if field is inaccessible
                }
            }
        } catch (Throwable ignored) {
            // Silently ignore if engine classes not yet active or during menu navigation
        }

        // Tactical AI Pantheon: Dispatch GodManager check in lockstep with coat randomization
        try {
            GodManager.checkAndRandomizeActiveMatch();
        } catch (Throwable ignored) {
            // Silently ignore if engine classes not yet active
        }
    }

    /**
     * Resolves static 'ah.a' ('ln') and creates a default instance if 'ah.a' is null.
     */
    private static Object getOrCreateLocalCoatObject() throws Exception {
        Class<?> ahClass = Class.forName("ah");
        Field aField = ahClass.getDeclaredField("a");
        aField.setAccessible(true);
        Object coatObj = aField.get(null);
        if (coatObj == null) {
            Class<?> lnClass = Class.forName("ln");
            Constructor<?> ctor = lnClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            coatObj = ctor.newInstance();
            aField.set(null, coatObj);
        }
        return coatObj;
    }

    /**
     * Extracts accessible shape and color arrays ('ln.a', 'ln.b') from an 'ln' coat object.
     * Note: 'final class ln' and fields 'a'/'b' are package-private in the default package,
     * so setAccessible(true) is mandatory when accessed from net.alterorb.launcher.
     */
    private static int[][] extractCoatArrays(Object coatObj) throws Exception {
        if (coatObj == null) return null;
        Field shapesField = coatObj.getClass().getDeclaredField("a");
        Field colorsField = coatObj.getClass().getDeclaredField("b");
        shapesField.setAccessible(true);
        colorsField.setAccessible(true);
        int[] shapes = (int[]) shapesField.get(coatObj);
        int[] colors = (int[]) colorsField.get(coatObj);
        if (shapes == null || colors == null || shapes.length < 6 || colors.length < 6) {
            return null;
        }
        return new int[][]{ shapes, colors };
    }

    /**
     * Encodes 6 shape indices (with -1 mapped to unsigned 255) and 6 palette color indices
     * into the 12-byte FunOrb wire format shared by Client Opcode 71, Server Packet 75, and Steam OP_OPEN.
     */
    private static byte[] encodeCoatToBytes(int[] shapes, int[] colors) {
        byte[] wire = new byte[12];
        for (int i = 0; i < 6; i++) {
            wire[i * 2] = (byte) (shapes[i] & 0xFF);
            wire[i * 2 + 1] = (byte) (colors[i] & 0xFF);
        }
        return wire;
    }

    /**
     * Replaces StandaloneRunner.flushCoatOfArms's broken cross-package reflection (missing setAccessible(true)
     * on package-private ln.a / ln.b) cleanly without injecting off-thread network packets into vl.n.
     *
     * 1. Initial call (!initialCoatLoaded): loads any saved GameConfig.coatOfArms into ah.a and returns
     *    before allowing any write back so startup never clobbers persisted coats.
     * 2. Subsequent calls: persists edited ah.a back into GameConfig + player.properties and updates
     *    local AoGServer.hostCoatOfArms in memory (ONLY when not acting as a joining client).
     */
    public static synchronized void syncAndFlushCoatOfArms(Object gameConfig) {
        if (gameConfig == null) {
            return;
        }
        try {
            Object coatObj = getOrCreateLocalCoatObject();
            int[][] arrays = extractCoatArrays(coatObj);
            if (arrays == null) {
                return;
            }
            int[] shapes = arrays[0];
            int[] colors = arrays[1];

            Field coatField = gameConfig.getClass().getDeclaredField("coatOfArms");
            coatField.setAccessible(true);
            String savedCoat = (String) coatField.get(gameConfig);

            if (!initialCoatLoaded) {
                initialCoatLoaded = true;
                if (savedCoat != null && !savedCoat.isBlank()) {
                    StandaloneRunner.deserializeCoat(savedCoat, shapes, colors);
                    System.out.println("[CoatOfArms] Loaded player coat of arms: " + savedCoat);
                }
                return;
            }

            String currentSig = StandaloneRunner.serializeCoat(shapes, colors);
            if (currentSig != null && !currentSig.equals(savedCoat)) {
                coatField.set(gameConfig, currentSig);
                if (gameConfig instanceof StandaloneRunner.GameConfig) {
                    StandaloneRunner.savePlayerProperties((StandaloneRunner.GameConfig) gameConfig);
                }
                System.out.println("[CoatOfArms] Auto-saved updated coat of arms: " + currentSig);

                // Keep local Host server's in-memory coat synchronized without touching disk or server state on guests
                boolean hasGuestProxy = StandaloneRunner.guestProxyHolder != null
                        && StandaloneRunner.guestProxyHolder.length > 0
                        && StandaloneRunner.guestProxyHolder[0] != null;
                String mode = null;
                try {
                    Field modeField = gameConfig.getClass().getDeclaredField("mode");
                    modeField.setAccessible(true);
                    mode = (String) modeField.get(gameConfig);
                } catch (Throwable ignored) {}
                boolean isJoiningMode = "steam_join".equalsIgnoreCase(mode) || "join".equalsIgnoreCase(mode);
                if (!hasGuestProxy && !isJoiningMode) {
                    net.alterorb.server.AoGServer.hostCoatOfArms = encodeCoatToBytes(shapes, colors);
                }
            }
        } catch (Throwable ignored) {
            // Silently ignore if engine classes not yet initialized
        }
    }

    /**
     * Returns the local player's current Coat of Arms in 12-byte wire format for Steam P2P OP_OPEN
     * handshake delivery so a joining peer never needs to open Screen 13 before launching a battle.
     *
     * @return 12-byte serialized coat array, or null if local coat cannot be read.
     */
    public static synchronized byte[] getLocalCoatBytes() {
        try {
            if (!initialCoatLoaded && StandaloneRunner.activeConfig != null) {
                syncAndFlushCoatOfArms(StandaloneRunner.activeConfig);
            }
            Object coatObj = getOrCreateLocalCoatObject();
            int[][] arrays = extractCoatArrays(coatObj);
            if (arrays == null) {
                return null;
            }
            return encodeCoatToBytes(arrays[0], arrays[1]);
        } catch (Throwable t) {
            return null;
        }
    }
}
