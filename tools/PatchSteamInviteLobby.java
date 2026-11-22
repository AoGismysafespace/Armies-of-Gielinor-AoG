import javassist.*;
import javassist.bytecode.*;
import java.io.*;
import java.nio.file.*;

/**
 * ====================================================================================================
 * Armies of Gielinor — Steam Invite Lobby & Room Transition Patcher
 * Component: tools.PatchSteamInviteLobby
 * Target Runtime: OpenJDK 18.0.2.1 (Major version <= 62.0)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    When a player receives and accepts a Steam game invitation while sitting on the Main Menu,
 *    the launcher's in-game join worker thread ('lambda$handleInGameSteamJoin$7' inside StandaloneRunner)
 *    establishes the Steam P2P tunnel connection and attempts to transition the client screen to
 *    Screen -4 (Multiplayer Lobby).
 *
 *    In unpatched bytecode at offsets 1075-1080 of 'lambda$handleInGameSteamJoin$7':
 *      1068: iload 15
 *      1070: bipush -2
 *      1072: if_icmpeq target 1276
 *      1075: iload 15
 *      1077: iconst_m1 (-1)
 *      1078: if_icmpeq target 1276
 *    The check 'if (screen == -1) goto 1276' erroneously aborted the screen transition whenever the
 *    player was on the Main Menu (Screen -1) — the exact screen players are on when accepting invites!
 *    As a result, the client remained visually on the Main Menu, never transitioned to Screen -4,
 *    and never sent Opcode 9 until the player manually clicked "Join Lobby".
 *
 *    This tool safely replaces the 6-byte inverted check (1075-1080) with 6 NOP opcodes (0x00),
 *    allowing clients on the Main Menu to seamlessly proceed directly to 'safeTransitionToScreen(-4)'
 *    without modifying method length, exception tables, or stack map frames.
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Symbol                     | Real Architectural Meaning       | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    'jt.a'                     | ActiveScreenId                   | Screen ID (-1=MainMenu, -2=Room, -4=Lobby)
 *    'safeTransitionToScreen'   | Engine Screen Switch Method      | Transitions FunOrb client to target screen ID
 *    'lambda$handleInGameSteamJoin$7' | Steam In-Game Join Task    | Connects tunnel and transitions to lobby
 *    'RestoreAndVerify'         | Bytecode Sync & Verifier         | Synchronizes patched classes into all JARs
 *    'Major Version 62'         | Java 18 Target Bytecode          | Required by embedded OpenJDK 18.0.2.1
 *    --------------------------------------------------------------------------------------------------
 * ====================================================================================================
 */
public class PatchSteamInviteLobby {

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println("  PatchSteamInviteLobby: Instrumenting Steam In-Game Screen Transition");
        System.out.println("==========================================================================");

        Path workspace = args.length > 0 ? Paths.get(args[0]) : Paths.get(".").toAbsolutePath().normalize();
        Path sourceJar = workspace.resolve("extracted/app/ArmiesOfGielinor.jar");
        Path runnerClassPath = workspace.resolve("jar_extracted/net/alterorb/launcher/StandaloneRunner.class");

        if (!Files.exists(sourceJar)) {
            throw new FileNotFoundException("Cannot find target application JAR: " + sourceJar);
        }
        if (!Files.exists(runnerClassPath)) {
            throw new FileNotFoundException("Cannot find StandaloneRunner.class: " + runnerClassPath);
        }

        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(runnerClassPath.getParent().getParent().getParent().getParent().toString());
        pool.insertClassPath(sourceJar.toString());

        CtClass runner = pool.get("net.alterorb.launcher.StandaloneRunner");
        CtMethod method = runner.getDeclaredMethod("lambda$handleInGameSteamJoin$7");
        MethodInfo minfo = method.getMethodInfo();
        CodeAttribute ca = minfo.getCodeAttribute();
        byte[] code = ca.getCode();

        // Verify the 6-byte pattern at offset 1075-1080:
        // 1075: 0x15, 1076: 0x0F (iload 15)
        // 1077: 0x02             (iconst_m1)
        // 1078: 0x9F, 1079: 0x00, 1080: 0xC6 (if_icmpeq +198 -> target 1276)
        if ((code[1075] & 0xFF) == 0x15 && (code[1076] & 0xFF) == 0x0F && (code[1077] & 0xFF) == 0x02 &&
            (code[1078] & 0xFF) == 0x9F && (code[1079] & 0xFF) == 0x00 && (code[1080] & 0xFF) == 0xC6) {
            System.out.println("[Found] Verified unpatched Main Menu skip check at offsets 1075-1080.");
            for (int i = 1075; i <= 1080; i++) {
                code[i] = 0x00; // NOP
            }
            System.out.println("[Instrumented] Replaced offsets 1075-1080 with 6 NOPs.");
        } else if ((code[1075] & 0xFF) == 0x00 && (code[1076] & 0xFF) == 0x00 && (code[1077] & 0xFF) == 0x00 &&
                   (code[1078] & 0xFF) == 0x00 && (code[1079] & 0xFF) == 0x00 && (code[1080] & 0xFF) == 0x00) {
            System.out.println("[Idempotent] Offsets 1075-1080 are already NOPed. No changes needed.");
        } else {
            throw new IllegalStateException("Bytecode pattern mismatch at offsets 1075-1080 in lambda$handleInGameSteamJoin$7!");
        }

        byte[] patchedBytes = runner.toBytecode();
        System.out.println("[Instrumented Successfully] StandaloneRunner bytecode length: " + patchedBytes.length);

        // Verify Java 18 bytecode compliance (major version <= 62)
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(patchedBytes));
        int magic = dis.readInt();
        int minor = dis.readUnsignedShort();
        int major = dis.readUnsignedShort();
        System.out.println("[Bytecode Check] StandaloneRunner major version: " + major + " (Java 18 = 62)");
        if (major > 62) {
            throw new IllegalStateException("Bytecode exceeds Java 18 major version: " + major);
        }

        // Save to jar_extracted/net/alterorb/launcher/ (Rule 5.2 - Defend against auto-reversion trap)
        Files.write(runnerClassPath, patchedBytes);
        System.out.println("[Saved] Updated " + runnerClassPath + " successfully.");
    }
}
