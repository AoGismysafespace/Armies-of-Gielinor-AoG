import javassist.*;
import java.io.*;
import java.nio.file.*;

/**
 * ====================================================================================================
 * Armies of Gielinor - Tactical AI Heraldry & Coat Randomizer Patcher
 * Component: tools.PatchCoatRandomizer
 * Target Runtime: OpenJDK 18.0.2.1 (Major version <= 62.0)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Instruments `net.alterorb.launcher.StandaloneRunner` to link the AI Coat of Arms Randomizer
 *    into the launcher's background match monitor loop.
 *
 *    - Injects a call to `CoatManager.checkAndRandomizeActiveMatch()` into `flushCoatOfArms()`:
 *      During offline Skirmish/Sandbox matches, the Jagex engine defaults to assigning the human
 *      player's crest (`ah.a`) to all player slots (`rd.j.xb.T[i] = ah.a`).
 *      When `flushCoatOfArms` executes every 500ms, `checkAndRandomizeActiveMatch()` detects if
 *      bot slots (i > 0) still hold `ah.a` and immediately replaces each bot with a unique,
 *      randomized `ln` Coat of Arms.
 *    - Human player crest (slot 0) remains 100% untouched.
 *    - Campaign AI opponents (which have custom scenario crests) are never overridden.
 *    - Preserves all previously patched display and window lifecycle improvements (`DisplayManager`).
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Symbol                     | Real Architectural Meaning       | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    `flushCoatOfArms`          | Periodic Heraldry Persister      | Executed in main loop every 500ms.
 *    `CoatManager`              | AI Crest Generator & Dispatcher  | Generates valid, distinct bot crests.
 *    `checkAndRandomizeActive`  | Skirmish Match Crest Guard       | Checks active battlefield and swaps coats.
 *    `RestoreAndVerify`         | Bytecode Sync & Audit Engine     | Injects patched classes across all JARs.
 *    `Major Version 62`         | Java 18 Target Bytecode          | Required by bundled OpenJDK 18.0.2.1.
 * ====================================================================================================
 */
public class PatchCoatRandomizer {

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println("  PatchCoatRandomizer: Instrumenting Tactical AI Coat Randomization");
        System.out.println("==========================================================================");

        Path workspace = args.length > 0 ? Paths.get(args[0]) : Paths.get(".").toAbsolutePath().normalize();
        Path sourceJar = workspace.resolve("extracted/app/ArmiesOfGielinor.jar");
        Path coatManagerClass = workspace.resolve("src/net/alterorb/launcher/CoatManager.class");

        if (!Files.exists(sourceJar)) {
            throw new FileNotFoundException("Cannot find target application JAR: " + sourceJar);
        }
        if (!Files.exists(coatManagerClass)) {
            throw new FileNotFoundException("Cannot find compiled CoatManager.class: " + coatManagerClass);
        }

        File tempJar = File.createTempFile("aog_coat_patch_", ".jar");
        tempJar.deleteOnExit();
        Files.copy(sourceJar, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(tempJar.getAbsolutePath());
        pool.insertClassPath(workspace.resolve("src").toString());

        CtClass runner = pool.get("net.alterorb.launcher.StandaloneRunner");

        // Instrument flushCoatOfArms cleanly and idempotently via setBody() so:
        // 1. StandaloneRunner delegates loading, auto-saving, and non-guest AoGServer sync
        //    to CoatManager.syncAndFlushCoatOfArms($1) using GameConfig directly (avoiding the
        //    broken ah.a lookup in original StandaloneRunner bytecode and never polluting ah.a/vl.n).
        // 2. Active single-player/skirmish bot randomization still triggers afterward via
        //    CoatManager.checkAndRandomizeActiveMatch().
        // Note: $1 in Javassist refers to the GameConfig parameter passed to flushCoatOfArms.
        CtMethod flushCoatOfArms = runner.getDeclaredMethod("flushCoatOfArms");
        flushCoatOfArms.setBody(
            "{\n" +
            "    net.alterorb.launcher.CoatManager.syncAndFlushCoatOfArms($1);\n" +
            "    net.alterorb.launcher.CoatManager.checkAndRandomizeActiveMatch();\n" +
            "}"
        );
        System.out.println("[Instrumenting] Replaced flushCoatOfArms() body with syncAndFlushCoatOfArms($1) + checkAndRandomizeActiveMatch().");

        byte[] patchedBytes = runner.toBytecode();
        System.out.println("[Instrumented Successfully] StandaloneRunner bytecode length: " + patchedBytes.length);

        // Verify Java 18 bytecode compliance (major version <= 62)
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(patchedBytes));
        int magic = dis.readInt();
        int minor = dis.readUnsignedShort();
        int major = dis.readUnsignedShort();
        System.out.println("[Bytecode Check] StandaloneRunner major version: " + major);
        if (major > 62) {
            throw new IllegalStateException("Bytecode exceeds Java 18 major version: " + major);
        }

        // Update jar_extracted/net/alterorb/launcher/ (Rule 5.2 - Defend against auto-reversion trap)
        Path jarExtractedDir = workspace.resolve("jar_extracted/net/alterorb/launcher");
        Files.createDirectories(jarExtractedDir);
        Files.write(jarExtractedDir.resolve("StandaloneRunner.class"), patchedBytes);
        Files.copy(coatManagerClass, jarExtractedDir.resolve("CoatManager.class"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[Saved] Updated jar_extracted with patched StandaloneRunner.class & CoatManager.class");

        // Invoke RestoreAndVerify to sync into all target JARs in lockstep
        System.out.println("[Syncing] Running RestoreAndVerify to propagate patched classes...");
        RestoreAndVerify.main(new String[]{workspace.toString()});
        System.out.println("==========================================================================");
        System.out.println("  PatchCoatRandomizer COMPLETED SUCCESSFULLY 100%!");
        System.out.println("==========================================================================");
    }
}
