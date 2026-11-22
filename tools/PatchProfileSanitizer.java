import javassist.*;
import java.io.*;
import java.nio.file.*;

/**
 * ====================================================================================================
 * Armies of Gielinor - Safe Player Profile & Offline Identity Patcher
 * Component: tools.PatchProfileSanitizer
 * Target Runtime: OpenJDK 18.0.2.1 (Major version <= 62.0)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Instruments net.alterorb.launcher.StandaloneRunner to safeguard player identity and privacy:
 *
 *    a) Sanitizes loaded usernames via `ProfileManager.sanitizeUsername()`:
 *       Intercepts obsolete test placeholders ("jesii98"), blank usernames, and invalid characters,
 *       ensuring the game cleanly falls back to the generic privatized title "Commander" whenever
 *       Steam is not running or when the game is distributed to other players.
 *
 *    b) Preserves Steam Identity:
 *       When Steam is active, the player's personal Steam display name (PersonaName) continues to be
 *       automatically retrieved and displayed without alteration.
 *
 *    c) Enforces FunOrb Base37 Specifications:
 *       Guarantees all names conform to the 2009 Jagex network protocol (1-12 characters, alphanumeric,
 *       hyphen, underscore, space).
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Symbol                      | Real Architectural Meaning       | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    `loadPlayerProperties`      | Configuration Loader             | Boot hook reading user preferences.
 *    `ProfileManager`            | Safe Identity Wrapper            | Sanitizes and enforces generic fallback.
 *    `DEFAULT_OFFLINE_NAME`      | Privatized Generic Title         | "Commander" — safe for public distribution.
 *    `OBSOLETE_LEGACY_NAME`      | Obsolete Test Identifier         | "jesii98" — purged and intercepted.
 *    `Major Version 62`          | Java 18 Target                   | Required by bundled OpenJDK 18.0.2.1.
 *
 * 3. INLINE EXPLANATIONS:
 *    - Updates `jar_extracted/net/alterorb/launcher/` first to defend against the auto-reversion trap.
 *    - Invokes `RestoreAndVerify` to inject verified classes into all target JARs in lockstep.
 * ====================================================================================================
 */
public class PatchProfileSanitizer {

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println("  PatchProfileSanitizer: Instrumenting Safe Player Profile Management");
        System.out.println("==========================================================================");

        Path workspace = args.length > 0 ? Paths.get(args[0]) : Paths.get(".").toAbsolutePath().normalize();
        Path sourceJar = workspace.resolve("extracted/app/ArmiesOfGielinor.jar");
        Path profileManagerClass = workspace.resolve("src/net/alterorb/launcher/ProfileManager.class");

        if (!Files.exists(sourceJar)) {
            throw new FileNotFoundException("Cannot find target application JAR: " + sourceJar);
        }
        if (!Files.exists(profileManagerClass)) {
            throw new FileNotFoundException("Cannot find compiled ProfileManager.class: " + profileManagerClass);
        }

        // 1. Create a safe temporary copy of the JAR for Javassist inspection
        File tempJar = File.createTempFile("aog_profile_patch_", ".jar");
        tempJar.deleteOnExit();
        Files.copy(sourceJar, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(tempJar.getAbsolutePath());
        pool.insertClassPath(workspace.resolve("src").toString());

        CtClass runner = pool.get("net.alterorb.launcher.StandaloneRunner");

        // 2. Instrument loadPlayerProperties: Intercept obsolete names & enforce generic fallback
        CtMethod loadPlayerProperties = runner.getDeclaredMethod("loadPlayerProperties");
        loadPlayerProperties.insertAfter(
            "{\n" +
            "    if ($_ != null) {\n" +
            "        $_.username = net.alterorb.launcher.ProfileManager.sanitizeUsername($_.username);\n" +
            "    }\n" +
            "}"
        );
        System.out.println("[Instrumenting] Injected ProfileManager.sanitizeUsername into loadPlayerProperties().");

        byte[] patchedBytes = runner.toBytecode();
        System.out.println("[Instrumented Successfully] StandaloneRunner bytecode length: " + patchedBytes.length);

        // 3. Verify Java 18 bytecode compliance (major version <= 62)
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(patchedBytes));
        int magic = dis.readInt();
        int minor = dis.readUnsignedShort();
        int major = dis.readUnsignedShort();
        System.out.println("[Bytecode Check] StandaloneRunner major version: " + major);
        if (major > 62) {
            throw new IllegalStateException("Bytecode exceeds Java 18 major version: " + major);
        }

        // 4. Update jar_extracted/net/alterorb/launcher/ (Rule 5.2 - Defend against auto-reversion trap)
        Path jarExtractedDir = workspace.resolve("jar_extracted/net/alterorb/launcher");
        Files.createDirectories(jarExtractedDir);
        Files.write(jarExtractedDir.resolve("StandaloneRunner.class"), patchedBytes);
        Files.copy(profileManagerClass, jarExtractedDir.resolve("ProfileManager.class"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[Saved] Updated jar_extracted with patched StandaloneRunner.class & ProfileManager.class");

        // 5. Invoke RestoreAndVerify to sync into all target JARs in lockstep
        System.out.println("[Syncing] Running RestoreAndVerify to propagate patched classes...");
        RestoreAndVerify.main(new String[]{workspace.toString()});

        System.out.println("==========================================================================");
        System.out.println("  PatchProfileSanitizer: Instrumentation & Verification Complete!");
        System.out.println("==========================================================================");
    }
}
