import javassist.*;
import javassist.expr.*;
import java.io.*;
import java.nio.file.*;

/**
 * ====================================================================================================
 * Armies of Gielinor - Safe Display & Window Lifecycle Patcher
 * Component: tools.PatchDisplayFreezeFix
 * Target Runtime: OpenJDK 18.0.2.1 (Major version <= 62.0)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Instruments net.alterorb.launcher.StandaloneRunner to eliminate the fatal "75% Extra Data" display
 *    freeze and resolution reversion trap:
 *
 *    a) Replaces `applyWindowMode` with a delegation call to `DisplayManager.applyWindowMode()`:
 *       Eliminates `jFrame.dispose()` and the destructive `tq.a = true` trigger. Fullscreen and windowed
 *       switches now occur seamlessly via standard Swing frame state and bounds transitions. The AWT Canvas
 *       (`si.b`) and Graphics2D drawing context remain permanently valid, preventing `pt.a` from throwing
 *       and silently freezing on the 75% loading frame.
 *
 *    b) Neutralizes the 50ms Boot Monitor Loop in `StandaloneRunner.main`:
 *       Uses Javassist ExprEditor to neutralize the 7 legacy polling calls to `applyWindowMode` and
 *       `updateAndSaveResolution` inside `main`. These legacy polling calls misread FunOrb's initial
 *       HUD mode (`ra.e.h == false`) during Archive 10 MIDI music unpacking as a user request to shrink
 *       to 640x480, forcibly downgrading the window and corrupting the drawing canvas.
 *       All legitimate user-triggered calls (F11 / Alt+Enter in `lambda$main$25`, in-game options menu
 *       in `applyInGameScreenMode`, manual window dragging in `ComponentAdapter`) remain 100% untouched.
 *
 *    c) Replaces `saveCurrentScreenSize` with `DisplayManager.saveCurrentScreenSize()`:
 *       Calculates resolution from actual screen / component dimensions rather than FunOrb's internal
 *       fixed HUD scale flag (`ra.e.h`), preventing accidental reversion to "Windowed - Fixed (640 x 480)".
 *
 *    d) Instruments `loadPlayerProperties` with resolution self-healing:
 *       Automatically intercepts legacy broken 640x480 resolution strings stored in player.properties
 *       and resets them safely to "Fullscreen (Native)", ensuring players with old configurations
 *       do not experience freezes.
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Symbol                      | Real Architectural Meaning       | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    `applyWindowMode`           | Window Mode Dispatcher           | Switches between fullscreen/windowed.
 *    `saveCurrentScreenSize`     | Resolution Persistence Hook      | Saves window size to player.properties.
 *    `loadPlayerProperties`      | Configuration Loader             | Reads user preferences on boot.
 *    `DisplayManager`            | Safe Display Wrapper             | Wrapper providing zero-dispose transitions.
 *    `ExprEditor`                | Javassist Bytecode Rewriter      | Surgically replaces method call opcodes.
 *    `Major Version 62`          | Java 18 Target                   | Required by bundled OpenJDK 18.0.2.1.
 *
 * 3. INLINE EXPLANATIONS:
 *    - Updates `jar_extracted/net/alterorb/launcher/` first to defend against the auto-reversion trap.
 *    - Invokes `RestoreAndVerify` to inject verified classes into all target JARs and perform audit.
 * ====================================================================================================
 */
public class PatchDisplayFreezeFix {

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println("  PatchDisplayFreezeFix: Instrumenting Safe Window Mode & Self-Healing");
        System.out.println("==========================================================================");

        Path workspace = args.length > 0 ? Paths.get(args[0]) : Paths.get(".").toAbsolutePath().normalize();
        Path sourceJar = workspace.resolve("extracted/app/ArmiesOfGielinor.jar");
        Path displayManagerClass = workspace.resolve("src/net/alterorb/launcher/DisplayManager.class");

        if (!Files.exists(sourceJar)) {
            throw new FileNotFoundException("Cannot find target application JAR: " + sourceJar);
        }
        if (!Files.exists(displayManagerClass)) {
            throw new FileNotFoundException("Cannot find compiled DisplayManager.class: " + displayManagerClass);
        }

        // 1. Create a safe temporary copy of the JAR for Javassist inspection
        File tempJar = File.createTempFile("aog_display_patch_", ".jar");
        tempJar.deleteOnExit();
        Files.copy(sourceJar, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(tempJar.getAbsolutePath());
        pool.insertClassPath(workspace.resolve("src").toString());

        CtClass runner = pool.get("net.alterorb.launcher.StandaloneRunner");

        // 2. Instrument applyWindowMode: Replace with DisplayManager call (Zero jFrame.dispose() & zero tq.a)
        CtMethod applyWindowMode = runner.getDeclaredMethod("applyWindowMode");
        applyWindowMode.setBody(
            "{\n" +
            "    net.alterorb.launcher.DisplayManager.applyWindowMode($1, $2, $3, $4, $5);\n" +
            "}"
        );
        System.out.println("[Instrumenting] Replaced applyWindowMode() with safe DisplayManager implementation.");

        // 3. Neutralize the 50ms Boot Monitor Loop inside StandaloneRunner.main
        // Replaces the 7 legacy polling calls inside main by method name (immune to line-number shifts)
        CtMethod mainMethod = runner.getDeclaredMethod("main");
        final int[] neutralizedCount = new int[1];

        mainMethod.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("applyWindowMode")) {
                    m.replace("{ /* neutralized legacy 50ms boot polling window switch */ }");
                    neutralizedCount[0]++;
                } else if (m.getMethodName().equals("updateAndSaveResolution")) {
                    m.replace("{ /* neutralized legacy 50ms boot polling resolution overwrite */ }");
                    neutralizedCount[0]++;
                }
            }
        });
        System.out.println("[Instrumenting] Neutralized " + neutralizedCount[0] + " legacy polling calls in main().");
        if (neutralizedCount[0] != 7 && neutralizedCount[0] != 0) {
            throw new IllegalStateException("Expected 7 calls (or 0 if already patched), found: " + neutralizedCount[0]);
        }
        if (neutralizedCount[0] == 0) {
            System.out.println("[Note] 0 calls found - main() was already cleanly neutralized in this JAR.");
        }

        // 4. Instrument saveCurrentScreenSize: Replace with DisplayManager call (No 640x480 default trap)
        CtMethod saveCurrentScreenSize = runner.getDeclaredMethod("saveCurrentScreenSize");
        saveCurrentScreenSize.setBody(
            "{\n" +
            "    net.alterorb.launcher.DisplayManager.saveCurrentScreenSize();\n" +
            "}"
        );
        System.out.println("[Instrumenting] Replaced saveCurrentScreenSize() with safe DisplayManager implementation.");

        // 5. Instrument loadPlayerProperties: Self-heal legacy broken 640x480 configurations
        CtMethod loadPlayerProperties = runner.getDeclaredMethod("loadPlayerProperties");
        loadPlayerProperties.insertAfter(
            "{\n" +
            "    if ($_ != null && $_.resolution != null) {\n" +
            "        $_.resolution = net.alterorb.launcher.DisplayManager.sanitizeResolution($_.resolution);\n" +
            "    }\n" +
            "}"
        );
        System.out.println("[Instrumenting] Injected resolution self-healing into loadPlayerProperties().");

        byte[] patchedBytes = runner.toBytecode();
        System.out.println("[Instrumented Successfully] StandaloneRunner bytecode length: " + patchedBytes.length);

        // 6. Verify Java 18 bytecode compliance (major version <= 62)
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(patchedBytes));
        int magic = dis.readInt();
        int minor = dis.readUnsignedShort();
        int major = dis.readUnsignedShort();
        System.out.println("[Bytecode Check] StandaloneRunner major version: " + major);
        if (major > 62) {
            throw new IllegalStateException("Bytecode exceeds Java 18 major version: " + major);
        }

        // 7. Update jar_extracted/net/alterorb/launcher/ (Rule 5.2 - Defend against auto-reversion trap)
        Path jarExtractedDir = workspace.resolve("jar_extracted/net/alterorb/launcher");
        Files.createDirectories(jarExtractedDir);
        Files.write(jarExtractedDir.resolve("StandaloneRunner.class"), patchedBytes);
        Files.copy(displayManagerClass, jarExtractedDir.resolve("DisplayManager.class"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[Saved] Updated jar_extracted with patched StandaloneRunner.class & DisplayManager.class");

        // 8. Invoke RestoreAndVerify to sync into all target JARs in lockstep
        System.out.println("[Syncing] Running RestoreAndVerify to propagate patched classes...");
        RestoreAndVerify.main(new String[]{workspace.toString()});

        System.out.println("==========================================================================");
        System.out.println("  PatchDisplayFreezeFix: Instrumentation & Verification Complete!");
        System.out.println("==========================================================================");
    }
}
