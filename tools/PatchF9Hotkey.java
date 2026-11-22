import javassist.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/**
 * ==============================================================================
 * PatchF9Hotkey: Standalone Launcher Harness Instrumentation (F9 Hotkey & Anti-Idle)
 * ==============================================================================
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Instruments net.alterorb.launcher.StandaloneRunner to:
 *    a) Intercept the [F9] key (KeyEvent.VK_F9 / keyCode 120) inside the AWT event listener.
 *       When pressed, it invokes net.alterorb.launcher.BugReporter.exportAsync(),
 *       capturing game state and recent logs without interrupting gameplay.
 *    b) Start net.alterorb.launcher.AntiIdleWatchdog at application startup (in main).
 *       Prevents the legacy 2009 Jagex 20-minute inactivity timer from closing the socket
 *       and disconnecting players during offline AI or multiplayer sessions.
 *
 * 2. BYTECODE COMPATIBILITY:
 *    Maintains strict compliance with Java 18 (major version 62.0).
 * ==============================================================================
 */
public class PatchF9Hotkey {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Patching In-Game [F9] Bug Reporter Hotkey ===");

        Path workspace = args.length > 0 ? Paths.get(args[0]) : Paths.get(".").toAbsolutePath().normalize();
        Path sourceJar = workspace.resolve("extracted/app/ArmiesOfGielinor.jar");
        Path bugReporterClass = workspace.resolve("src/net/alterorb/launcher/BugReporter.class");
        Path antiIdleClass = workspace.resolve("src/net/alterorb/launcher/AntiIdleWatchdog.class");

        if (!Files.exists(sourceJar)) {
            throw new FileNotFoundException("Cannot find target JAR: " + sourceJar);
        }
        if (!Files.exists(bugReporterClass)) {
            throw new FileNotFoundException("Cannot find compiled BugReporter.class: " + bugReporterClass);
        }
        if (!Files.exists(antiIdleClass)) {
            throw new FileNotFoundException("Cannot find compiled AntiIdleWatchdog.class: " + antiIdleClass);
        }

        // 1. Create a safe temporary JAR for Javassist inspection
        File tempJar = File.createTempFile("aog_f9_patch_", ".jar");
        tempJar.deleteOnExit();
        Files.copy(sourceJar, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(tempJar.getAbsolutePath());
        pool.insertClassPath(workspace.resolve("src").toString());

        CtClass runner = pool.get("net.alterorb.launcher.StandaloneRunner");

        // 1. Instrument Anti-Idle Watchdog at startup in main method
        CtMethod mainMethod = runner.getDeclaredMethod("main");
        mainMethod.insertBefore("net.alterorb.launcher.AntiIdleWatchdog.start();\n");
        System.out.println("[Instrumenting] Injected AntiIdleWatchdog startup into main method.");

        // 2. Instrument F9 key handler at the very start of lambda$main$25
        CtMethod listenerMethod = runner.getDeclaredMethod("lambda$main$25");
        System.out.println("[Instrumenting] Found listener method: " + listenerMethod.getLongName());

        listenerMethod.insertBefore(
            "if ($2 instanceof java.awt.event.KeyEvent) {\n" +
            "    java.awt.event.KeyEvent ke = (java.awt.event.KeyEvent)$2;\n" +
            "    if (ke.getID() == 401 && ke.getKeyCode() == 120) {\n" +
            "        ke.consume();\n" +
            "        net.alterorb.launcher.BugReporter.exportAsync();\n" +
            "        return;\n" +
            "    }\n" +
            "}\n"
        );

        byte[] patchedBytes = runner.toBytecode();
        System.out.println("[Instrumented Successfully] StandaloneRunner bytecode length: " + patchedBytes.length);

        // Verify major version <= 62
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(patchedBytes));
        int magic = dis.readInt();
        int minor = dis.readUnsignedShort();
        int major = dis.readUnsignedShort();
        System.out.println("[Bytecode Check] StandaloneRunner major version: " + major);
        if (major > 62) {
            throw new IllegalStateException("Bytecode exceeds Java 18 major version: " + major);
        }

        // 2. Persist patched StandaloneRunner.class, BugReporter.class, and AntiIdleWatchdog.class to jar_extracted/
        Path jarExtractedDir = workspace.resolve("jar_extracted/net/alterorb/launcher");
        Files.createDirectories(jarExtractedDir);
        Files.write(jarExtractedDir.resolve("StandaloneRunner.class"), patchedBytes);
        Files.copy(bugReporterClass, jarExtractedDir.resolve("BugReporter.class"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(antiIdleClass, jarExtractedDir.resolve("AntiIdleWatchdog.class"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[Saved] Updated jar_extracted with patched StandaloneRunner, BugReporter, and AntiIdleWatchdog");

        // 3. Inject classes into target JARs
        List<Path> targetJars = Arrays.asList(
            workspace.resolve("extracted/app/ArmiesOfGielinor.jar"),
            workspace.resolve("ArmiesOfGielinor/app/ArmiesOfGielinor.jar"),
            workspace.resolve("Aktuell/ArmiesOfGielinor/app/ArmiesOfGielinor.jar"),
            Paths.get(System.getProperty("user.home"), "AppData/Local/ArmiesOfGielinor_App/app/ArmiesOfGielinor.jar")
        );

        byte[] reporterBytes = Files.readAllBytes(bugReporterClass);
        byte[] antiIdleBytes = Files.readAllBytes(antiIdleClass);

        for (Path jarPath : targetJars) {
            if (!Files.exists(jarPath)) continue;
            injectEntry(jarPath, "net/alterorb/launcher/StandaloneRunner.class", patchedBytes);
            injectEntry(jarPath, "net/alterorb/launcher/BugReporter.class", reporterBytes);
            injectEntry(jarPath, "net/alterorb/launcher/AntiIdleWatchdog.class", antiIdleBytes);
            System.out.println("[Injected Successfully] " + jarPath);
        }

        System.out.println("=== In-Game [F9] Diagnostic Hotkey Successfully Patched & Verified! ===");
    }

    private static void injectEntry(Path jarPath, String entryName, byte[] data) throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("create", "false");
        URI uri = URI.create("jar:" + jarPath.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            Path targetEntry = fs.getPath(entryName);
            if (targetEntry.getParent() != null && !Files.exists(targetEntry.getParent())) {
                Files.createDirectories(targetEntry.getParent());
            }
            Files.write(targetEntry, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
