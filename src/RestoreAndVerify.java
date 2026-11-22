/**
 * ====================================================================================================
 * Armies of Gielinor — Bytecode Compatibility Restoration & Integrity Verifier
 * Component: RestoreAndVerify.java
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Armies of Gielinor runs on an embedded OpenJDK runtime (extracted/runtime/release:
 *    JAVA_VERSION="18.0.2.1"). Java 18 strictly recognizes class file versions up to major version 62.0.
 *
 * 2. RATIONALE FOR BYTECODE VERIFICATION:
 *    Compiling on modern developer workstations with JDK 21+ without the '--release 18' flag emits
 *    bytecode with major version 65.0. At runtime, the bundled Java 18 JVM throws:
 *      java.lang.UnsupportedClassVersionError: AoGBattleBridge has been compiled by a more recent version...
 *    This component serves as an automated gatekeeper and self-healing tool:
 *      - It restores the pristine, verified Java 18 AoGBattleBridge bytecode.
 *      - It scans every .class entry in all target application JARs to verify 100% Java 18 compliance.
 *
 * 3. ROSETTA STONE / SYMBOL MAPPINGS:
 *    - AoGBattleBridge: Implementation of net.alterorb.server.BattleEngine coordinating battle turns.
 *    - StandaloneRunner: Launcher harness managing Steam P2P, screen transitions, and modal dismissal.
 *    - Major Version 62: Java 18 (bundled JRE target).
 *    - Major Version 65: Java 21 (host compiler default - incompatible with bundled JRE).
 *
 * 4. INLINE EXPLANATIONS:
 *    Resolves paths dynamically relative to the executing workspace root
 *    so builds remain fully portable, self-contained, and immune to cross-folder desynchronization.
 * ====================================================================================================
 */

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class RestoreAndVerify {

    // Bundled OpenJDK version is 18.0.2.1 -> Maximum recognized class file version is 62.0
    private static final int TARGET_MAX_MAJOR_VERSION = 62;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting Restoration & Bytecode Audit for Armies of Gielinor ===");

        // Determine workspace directory (current directory or passed argument)
        Path workspace = args.length > 0 ? Paths.get(args[0]) : Paths.get(".").toAbsolutePath().normalize();
        System.out.println("[Workspace] Target root: " + workspace);

        // Path to the pristine, Java 18 AoGBattleBridge class file
        Path sourceClassPath = workspace.resolve("jar_extracted/AoGBattleBridge.class");
        if (!Files.exists(sourceClassPath)) {
            sourceClassPath = workspace.resolve("test_environment/v101/ArmiesOfGielinor.jar");
        }
        
        byte[] pristineBytes;
        if (Files.exists(sourceClassPath) && sourceClassPath.toString().endsWith(".jar")) {
            try (JarFile jf = new JarFile(sourceClassPath.toFile())) {
                JarEntry je = jf.getJarEntry("AoGBattleBridge.class");
                pristineBytes = jf.getInputStream(je).readAllBytes();
            }
        } else if (Files.exists(sourceClassPath)) {
            pristineBytes = Files.readAllBytes(sourceClassPath);
        } else {
            throw new FileNotFoundException("Cannot locate pristine AoGBattleBridge source in: " + workspace);
        }

        int srcMajor = getMajorVersion(pristineBytes);
        System.out.println("[Source Verification] Pristine AoGBattleBridge size: " + pristineBytes.length 
                           + " bytes, major version: " + srcMajor + " (Java " + (srcMajor - 44) + ")");
        if (srcMajor > TARGET_MAX_MAJOR_VERSION) {
            throw new IllegalStateException("Pristine class has incompatible major version: " + srcMajor);
        }

        // Map of all verified pristine classes to synchronize into target JARs
        Map<String, byte[]> classesToSync = new LinkedHashMap<>();
        classesToSync.put("AoGBattleBridge.class", pristineBytes);

        // Also discover and sync verified AoGServer classes from jar_extracted/net/alterorb/server/
        Path serverClassDir = workspace.resolve("jar_extracted/net/alterorb/server");
        if (Files.exists(serverClassDir)) {
            try (var stream = Files.list(serverClassDir)) {
                for (Path p : stream.filter(f -> f.toString().endsWith(".class")).toList()) {
                    String relName = "net/alterorb/server/" + p.getFileName().toString();
                    byte[] classBytes = Files.readAllBytes(p);
                    int maj = getMajorVersion(classBytes);
                    if (maj > TARGET_MAX_MAJOR_VERSION) {
                        throw new IllegalStateException("Server class " + relName + " has incompatible major version: " + maj);
                    }
                    classesToSync.put(relName, classBytes);
                }
            }
        }

        // Also discover and sync verified launcher classes from jar_extracted/net/alterorb/launcher/
        Path launcherClassDir = workspace.resolve("jar_extracted/net/alterorb/launcher");
        if (Files.exists(launcherClassDir)) {
            try (var stream = Files.list(launcherClassDir)) {
                for (Path p : stream.filter(f -> f.toString().endsWith(".class")).toList()) {
                    String relName = "net/alterorb/launcher/" + p.getFileName().toString();
                    byte[] classBytes = Files.readAllBytes(p);
                    int maj = getMajorVersion(classBytes);
                    if (maj > TARGET_MAX_MAJOR_VERSION) {
                        throw new IllegalStateException("Launcher class " + relName + " has incompatible major version: " + maj);
                    }
                    classesToSync.put(relName, classBytes);
                }
            }
        }

        // Also discover and sync verified Steam tunnel/bridge classes (including inner classes like
        // SteamP2PTunnel$GuestProxy.class and SteamP2PTunnel$HostBridge.class) from jar_extracted/net/alterorb/steam/
        Path steamClassDir = workspace.resolve("jar_extracted/net/alterorb/steam");
        if (Files.exists(steamClassDir)) {
            try (var stream = Files.list(steamClassDir)) {
                for (Path p : stream.filter(f -> f.toString().endsWith(".class")).toList()) {
                    String relName = "net/alterorb/steam/" + p.getFileName().toString();
                    byte[] classBytes = Files.readAllBytes(p);
                    int maj = getMajorVersion(classBytes);
                    if (maj > TARGET_MAX_MAJOR_VERSION) {
                        throw new IllegalStateException("Steam class " + relName + " has incompatible major version: " + maj);
                    }
                    classesToSync.put(relName, classBytes);
                }
            }
        }

        // Also discover and sync verified patched ej.class (AI turn frame slicer) from jar_extracted/ej.class
        Path ejClassPath = workspace.resolve("jar_extracted/ej.class");
        if (Files.exists(ejClassPath)) {
            byte[] ejBytes = Files.readAllBytes(ejClassPath);
            int ejMaj = getMajorVersion(ejBytes);
            if (ejMaj > TARGET_MAX_MAJOR_VERSION) {
                throw new IllegalStateException("Class ej.class has incompatible major version: " + ejMaj);
            }
            classesToSync.put("ej.class", ejBytes);
        }

        // Also discover and sync verified patched kr.class (Achievement UI single Menu button) from jar_extracted/kr.class
        Path krClassPath = workspace.resolve("jar_extracted/kr.class");
        if (Files.exists(krClassPath)) {
            byte[] krBytes = Files.readAllBytes(krClassPath);
            int krMaj = getMajorVersion(krBytes);
            if (krMaj > TARGET_MAX_MAJOR_VERSION) {
                throw new IllegalStateException("Class kr.class has incompatible major version: " + krMaj);
            }
            classesToSync.put("kr.class", krBytes);
        }

        // Target JAR locations to update and audit
        List<Path> targetJars = Arrays.asList(
            workspace.resolve("extracted/app/ArmiesOfGielinor.jar"),
            workspace.resolve("ArmiesOfGielinor/app/ArmiesOfGielinor.jar"),
            Paths.get(System.getProperty("user.home"), "AppData/Local/ArmiesOfGielinor_App/app/ArmiesOfGielinor.jar")
        );

        for (Path jarPath : targetJars) {
            if (!Files.exists(jarPath)) {
                System.out.println("[Skip] Target JAR not found: " + jarPath);
                continue;
            }

            System.out.println("\n[Injecting] Updating " + jarPath.getFileName() + " with " + classesToSync.size() + " verified classes in a single atomic batch...");
            injectEntries(jarPath, classesToSync);
            System.out.println("[Injected Successfully] " + jarPath);

            // Audit the JAR to verify 100% compatibility across all classes
            int violations = auditJar(jarPath);
            if (violations > 0) {
                System.err.println("FAILED: Found " + violations + " incompatible classes in " + jarPath);
                System.exit(1);
            } else {
                System.out.println("[Verified OK] 0 incompatible classes in " + jarPath.getFileName());
            }
        }

        System.out.println("\n=== ALL JARS VERIFIED: 100% COMPATIBLE WITH BUNDLED JAVA 18 RUNTIME ===");
    }

    /**
     * Injects or replaces multiple bytecode class entries within a target ZIP/JAR file in a single atomic operation.
     * 
     * WHY BATCH INJECTION IS MANDATORY:
     * On Windows, opening and closing the ZipFileSystem provider repeatedly (e.g. 23 consecutive times)
     * triggers rapid temp-file creation (zipfstmp...tmp) and rename operations. If antivirus software,
     * indexing, or background file monitors inspect the temporary file during close, Windows throws a fatal
     * FileSystemException ("Der Prozess kann nicht auf die Datei zugreifen"). Opening the archive ONCE,
     * writing all classes, and closing it cleanly prevents file lock contention entirely.
     */
    private static void injectEntries(Path jarPath, Map<String, byte[]> entries) throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("create", "false");
        URI uri = URI.create("jar:" + jarPath.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                Path targetEntry = fs.getPath(entry.getKey());
                if (targetEntry.getParent() != null) {
                    Files.createDirectories(targetEntry.getParent());
                }
                Files.write(targetEntry, entry.getValue(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
    }

    /**
     * Scans every .class file within the JAR and verifies its major version against TARGET_MAX_MAJOR_VERSION.
     */
    private static int auditJar(Path jarPath) throws Exception {
        int violations = 0;
        int totalClasses = 0;
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    totalClasses++;
                    try (InputStream is = jar.getInputStream(entry)) {
                        byte[] header = new byte[8];
                        int read = is.read(header);
                        if (read == 8 && header[0] == (byte)0xCA && header[1] == (byte)0xFE 
                                      && header[2] == (byte)0xBA && header[3] == (byte)0xBE) {
                            int major = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                            if (major > TARGET_MAX_MAJOR_VERSION) {
                                System.err.println("  INCOMPATIBLE ENTRY: " + entry.getName() + " -> major=" + major);
                                violations++;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("  Scanned " + totalClasses + " classes in " + jarPath.getFileName() + " -> " + violations + " violations.");
        return violations;
    }

    /**
     * Reads the class file header magic and major version.
     */
    private static int getMajorVersion(byte[] classBytes) {
        if (classBytes.length < 8) return -1;
        if (classBytes[0] != (byte)0xCA || classBytes[1] != (byte)0xFE ||
            classBytes[2] != (byte)0xBA || classBytes[3] != (byte)0xBE) {
            return -1;
        }
        return ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
    }
}
