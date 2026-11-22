package net.alterorb.launcher;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * ==============================================================================
 * BugReporter: Automated Game Diagnostic & Privacy-Scrubbed Log Exporter
 * Milestone: v1.0 — Standalone Golden Master Release
 * ==============================================================================
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Armies of Gielinor players encountering unexpected battlefield behaviors,
 *    desyncs, graphical redraw pauses, or audio stalls previously had to manually
 *    locate hidden log files in the user home directory (~/.armiesofgielinor/standalone.log).
 *    BugReporter provides an instantaneous, zero-friction diagnostic capture:
 *      - Triggered in-game via the [F9] keyboard shortcut (asynchronous background worker).
 *      - Triggered outside the game via tools/export_bug_report.bat or desktop shortcuts.
 *
 * 2. PRIVACY & ANONYMIZATION (Rule 3 / Privacy Sweep):
 *    Adheres strictly to the mandatory privacy sweep rules. All personal computer paths
 *    (e.g., <DRIVE>\Users\<Username>\...) and personal account identifiers are scrubbed
 *    and replaced with generic tokens (<USER_HOME>, <PLAYER_USER>).
 *
 * 3. ROSETTA STONE / SYMBOL MAPPINGS:
 *    - jt.a  : Active Screen ID:
 *              * 16 : Title / Splash Screen ("Runewars Titlescreen") - awaits player click/Enter.
 *              * -2 : Asset Loading Screen - displays percentage progress bar.
 *              * -1 : Main Menu (Campaign / Skirmish / Multiplayer selection).
 *              * -4 : Multiplayer Lobby (Room creation, player list).
 *              * -5 : Host Game / Match Setup screen.
 *              * -6 : Server Browser / Game List screen.
 *              * -7 : Room / Game Details screen.
 *              * -8 : Connection Error / Disconnect notification screen.
 *              *  0 : Active Tactical Battle (Hexagonal turn-based combat).
 *              * 11 : Campaign World Map & Mission Selection screen.
 *              * 12 : Mission Briefing screen.
 *              * 13 : Mission Results / Debriefing screen.
 *              * 14 : Options & Audio/Video Settings screen.
 *              * 18 : Custom Army & Pantheon Selection screen.
 *              * 19 : Highscores & Player Statistics screen.
 *              * 20 : Credits & Game Lore screen.
 *              * 21 : How to Play / Rules Tutorial screen.
 *    - p.b   : Engine initialization handshake state (10=OK / Connected, 11=Reconnecting).
 *    - q.C   : Static float field representing current asset loading percentage (0.0f..100.0f).
 *    - wa.e  : Static String field containing current asset loading stage description.
 *    - ic.Vb : Spectator acknowledged turn count in multiplayer games.
 *    - ic.xb : Game state reference containing active player (gb) and turn (Gb).
 *    - reports/ : Dedicated repository folder where timestamped reports are stored.
 *
 * 4. INLINE EXPLANATIONS & FAILURE MODES:
 *    - Trap 1 (q.C Float vs. Int): q.C is a float, not an int. Probed via getFloat(null).
 *    - Trap 2 (Loading progress retention): Probed unconditionally so Screen 16 displays
 *      the completed 100%/75% loading stage even after switching away from Screen -2.
 *    - Trap 3 (Headless / External Exporter): Guarded against HeadlessException and separates
 *      in-game live focus analysis from external CLI exporter invocations.
 *    - Trap 4 (Audio Subsystem): Queries AudioSystem mixer count to alert on missing audio.
 * ==============================================================================
 */
public class BugReporter {

    public static final String GAME_VERSION = "v1.0";
    private static final int MAX_LOG_LINES = 150;

    /**
     * Main entry point for external invocation (e.g., from tools/export_bug_report.bat).
     */
    public static void main(String[] args) {
        System.out.println("=== Armies of Gielinor: Automated Diagnostic Exporter ===");
        File reportFile = export();
        if (reportFile != null) {
            System.out.println("[Success] Bug report generated: " + reportFile.getAbsolutePath());
        } else {
            System.err.println("[Failed] Could not generate bug report.");
            System.exit(1);
        }
    }

    /**
     * Asynchronously triggers the diagnostic export on a dedicated background thread.
     * Prevents any hitching, input lag, or frame drop on the AWT Event Dispatch Thread.
     */
    public static void exportAsync() {
        Thread thread = new Thread(() -> export(), "BugReporterThread");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Primary diagnostic capture method. Safe to invoke from any thread or process.
     *
     * @return The created report File, or null if generation failed.
     */
    public static synchronized File export() {
        try {
            // Audio/system feedback indicating that the shortcut was received
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Throwable ignored) {}

            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String displayTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Determine workspace and reports directory
            Path reportsDir = findReportsDirectory();
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }
            Path reportPath = reportsDir.resolve("BugReport_" + timestamp + ".txt");

            // Gather diagnostic metadata
            StringBuilder sb = new StringBuilder();
            sb.append("======================================================================\n");
            sb.append("              ARMIES OF GIELINOR - AUTOMATED BUG REPORT               \n");
            sb.append("======================================================================\n");
            sb.append("Generated On : ").append(displayTime).append("\n");
            sb.append("Game Version : ").append(detectVersionString()).append("\n");
            sb.append("Java Runtime : ").append(System.getProperty("java.version"))
              .append(" (").append(System.getProperty("java.vendor")).append(")\n");
            sb.append("OS Version   : ").append(System.getProperty("os.name"))
              .append(" ").append(System.getProperty("os.version"))
              .append(" (").append(System.getProperty("os.arch")).append(")\n");

            // Inspect screen settings and player configuration if present
            appendConfigurationInfo(sb);

            // Probe hardware / environmental state (Audio, Display, Focus)
            appendEnvironmentHealth(sb);

            // Probe in-game reflection state if running inside live client
            appendLiveGameState(sb);

            sb.append("----------------------------------------------------------------------\n");
            sb.append("PLAYER OBSERVATIONS (Fill in what you remember - all fields optional):\n");
            sb.append("----------------------------------------------------------------------\n");
            sb.append("1. Mode (Campaign Mission # or Multiplayer Host/Guest):\n");
            sb.append("   > \n\n");
            sb.append("2. Last Action (Which unit moved, attacked, cast spell, or clicked):\n");
            sb.append("   > \n\n");
            sb.append("3. What happened on screen (Freeze / dark veil / turn skip / error):\n");
            sb.append("   > \n\n");

            sb.append("----------------------------------------------------------------------\n");
            sb.append("RECENT GAME LOG (Last ").append(MAX_LOG_LINES).append(" events, privacy scrubbed):\n");
            sb.append("----------------------------------------------------------------------\n");

            // Append privacy-scrubbed recent log events
            appendRecentLog(sb);

            sb.append("======================================================================\n");
            sb.append("END OF BUG REPORT\n");
            sb.append("======================================================================\n");

            // Write report to disk
            String finalReport = sb.toString();
            Files.writeString(reportPath, finalReport, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Copy report content to system clipboard for instant pasting
            try {
                StringSelection selection = new StringSelection(finalReport);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                System.out.println("[BugReporter] Report copied to system clipboard.");
            } catch (Throwable t) {
                System.err.println("[BugReporter] Notice: Could not access clipboard: " + t.getMessage());
            }

            // Launch report in default text editor (Notepad) for immediate viewing
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(reportPath.toFile());
                } else {
                    new ProcessBuilder("notepad.exe", reportPath.toAbsolutePath().toString()).start();
                }
            } catch (Throwable t) {
                // Non-critical: file is already safely persisted on disk
            }

            System.out.println("[BugReporter] Successfully exported diagnostic report to: " + reportPath);
            return reportPath.toFile();

        } catch (Throwable t) {
            System.err.println("[BugReporter] Error generating diagnostic report: " + t);
            t.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Resolves the target reports directory, checking workspace and user home fallbacks.
     */
    private static Path findReportsDirectory() {
        // Check current working directory for workspace root
        Path cwd = Paths.get(".").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("releases")) || Files.exists(cwd.resolve("extracted"))) {
            return cwd.resolve("reports");
        }
        if (Files.exists(cwd.resolve("../releases")) || Files.exists(cwd.resolve("../extracted"))) {
            return cwd.resolve("../reports").normalize();
        }
        // Fallback to user home directory
        return Paths.get(System.getProperty("user.home"), ".armiesofgielinor", "reports");
    }

    /**
     * Detects version string dynamically from CHANGELOG / manifest if present,
     * cleanly falling back to the compile-time GAME_VERSION constant.
     */
    private static String detectVersionString() {
        try {
            Path changelog = Paths.get("CHANGELOG.md");
            if (Files.exists(changelog)) {
                List<String> lines = Files.readAllLines(changelog, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.startsWith("## [") && line.contains("]")) {
                        int start = line.indexOf('[') + 1;
                        int end = line.indexOf(']');
                        String v = line.substring(start, end).trim();
                        if (!v.isEmpty()) {
                            return v;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return GAME_VERSION;
    }

    /**
     * Appends player.properties configuration (resolution, sound volumes).
     */
    private static void appendConfigurationInfo(StringBuilder sb) {
        Path propsFile = Paths.get(System.getProperty("user.home"), ".armiesofgielinor", "player.properties");
        if (Files.exists(propsFile)) {
            try {
                Properties p = new Properties();
                try (InputStream is = Files.newInputStream(propsFile)) {
                    p.load(is);
                }
                sb.append("Resolution   : ").append(p.getProperty("resolution", "Default")).append("\n");
                sb.append("Music Volume : ").append(p.getProperty("music_volume", "Default")).append("\n");
                sb.append("Sound Volume : ").append(p.getProperty("sound_volume", "Default")).append("\n");
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Probes audio subsystem and window focus health to diagnose common hardware/OS issues.
     */
    private static void appendEnvironmentHealth(StringBuilder sb) {
        // 1. Audio Subsystem Health Check
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            int mixerCount = (mixers != null) ? mixers.length : 0;
            sb.append("Audio Output : ").append(mixerCount).append(" audio device(s) detected");
            if (mixerCount == 0) {
                sb.append(" (Warning: No audio output detected - sound initialization may stall)");
            }
            sb.append("\n");
        } catch (Throwable t) {
            sb.append("Audio Output : Probe error (").append(t.getMessage()).append(")\n");
        }

        // 2. Window Focus Health Check (Guarded against Headless environments)
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                boolean isLiveJvm = isGameRunningInCurrentJvm();
                if (isLiveJvm) {
                    KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                    java.awt.Window focusedWindow = kfm.getFocusedWindow();
                    java.awt.Component focusOwner = kfm.getFocusOwner();
                    boolean hasFocus = (focusedWindow != null);
                    sb.append("Window Focus : ").append(hasFocus ? "Focused / Active" : "Unfocused / In Background")
                      .append(focusOwner != null ? " (Focus owner: " + focusOwner.getClass().getSimpleName() + ")" : "")
                      .append("\n");
                } else {
                    sb.append("Window Focus : N/A (External Exporter)\n");
                }
            } catch (Throwable t) {
                sb.append("Window Focus : Probed (").append(t.getMessage()).append(")\n");
            }
        }
    }

    /**
     * Safely probes live client state via reflection if running within the game process.
     */
    private static void appendLiveGameState(StringBuilder sb) {
        try {
            Class<?> jtClass = Class.forName("jt");
            Field screenField = jtClass.getDeclaredField("a");
            screenField.setAccessible(true);
            int screenId = screenField.getInt(null);

            String screenName;
            switch (screenId) {
                case -2:
                    screenName = "Loading Screen (-2) - Assets loading in progress";
                    break;
                case 16:
                    screenName = "Title Screen (16) - Ready / Waiting for player click or Enter to proceed to Main Menu";
                    break;
                case -1:
                    screenName = "Main Menu (-1) - Mode selection";
                    break;
                case -4:
                    screenName = "Multiplayer Lobby (-4) - Game room";
                    break;
                case -5:
                    screenName = "Host Game / Match Setup (-5)";
                    break;
                case -6:
                    screenName = "Server Browser / Game List (-6)";
                    break;
                case -7:
                    screenName = "Room / Game Details (-7)";
                    break;
                case -8:
                    screenName = "Connection Error / Disconnect (-8)";
                    break;
                case 0:
                    screenName = "Active Tactical Battle (0)";
                    break;
                case 11:
                    screenName = "Campaign Selection (11) - Mission map";
                    break;
                case 12:
                    screenName = "Mission Briefing (12)";
                    break;
                case 13:
                    screenName = "Mission Debriefing / Victory (13)";
                    break;
                case 14:
                    screenName = "Options & Settings (14)";
                    break;
                case 18:
                    screenName = "Army Builder (18) - Pantheon & Army customization";
                    break;
                case 19:
                    screenName = "Highscores & Statistics (19)";
                    break;
                case 20:
                    screenName = "Credits & Lore (20)";
                    break;
                case 21:
                    screenName = "Rules & Tutorial (21)";
                    break;
                default:
                    screenName = "Screen ID " + screenId;
                    break;
            }
            sb.append("Active Screen: ").append(screenName).append("\n");

            // Probe connection / handshake state
            Class<?> pClass = Class.forName("p");
            Field pbField = pClass.getDeclaredField("b");
            pbField.setAccessible(true);
            int pb = pbField.getInt(null);
            sb.append("Engine State : p.b=").append(pb).append(pb == 10 ? " (OK / Connected)" : " (Handshake / Stalled)").append("\n");

            // Probe asset loading stage and progress percentage (Trap 1: float q.C)
            appendLoadingStage(sb);

        } catch (ClassNotFoundException e) {
            sb.append("Client State : External Exporter (Game not running in this JVM)\n");
        } catch (Throwable t) {
            sb.append("Client State : Probed (").append(t.getMessage()).append(")\n");
        }
    }

    /**
     * Reflectively reads loading bar progress (float q.C) and status description (wa.e).
     * Probed unconditionally whenever classes are present so reports prove asset loading
     * completed cleanly even after transitioning from Loading Screen (-2) to Title Screen (16).
     */
    private static void appendLoadingStage(StringBuilder sb) {
        try {
            Class<?> qClass = Class.forName("q");
            Field progressField = qClass.getDeclaredField("C");
            progressField.setAccessible(true);
            float progress = progressField.getFloat(null);

            String status = null;
            try {
                Class<?> waClass = Class.forName("wa");
                Field statusField = waClass.getDeclaredField("e");
                statusField.setAccessible(true);
                status = (String) statusField.get(null);
            } catch (Throwable ignored) {}

            sb.append("Asset Loading: ").append(Math.round(progress)).append("%");
            if (status != null && !status.trim().isEmpty()) {
                sb.append(" (\"").append(status.trim()).append("\")");
            }
            sb.append("\n");
        } catch (Throwable ignored) {
            // Gracefully omit if loading engine classes are absent
        }
    }

    /**
     * Helper to verify if the live game client classes are loaded in the current JVM.
     */
    private static boolean isGameRunningInCurrentJvm() {
        try {
            Class.forName("jt");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Reads recent log events and applies privacy scrubbing.
     */
    private static void appendRecentLog(StringBuilder sb) {
        Path logFile = Paths.get(System.getProperty("user.home"), ".armiesofgielinor", "standalone.log");
        if (!Files.exists(logFile)) {
            sb.append("[Notice] No game log found at: ").append(sanitizePrivacy(logFile.toString())).append("\n");
            return;
        }

        try {
            List<String> allLines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            int total = allLines.size();
            int start = Math.max(0, total - MAX_LOG_LINES);
            List<String> tail = allLines.subList(start, total);

            for (String line : tail) {
                sb.append(sanitizePrivacy(line)).append("\n");
            }
        } catch (Throwable t) {
            sb.append("[Error reading log file: ").append(t.getMessage()).append("]\n");
        }
    }

    /**
     * Strict privacy scrubbing: strips user home paths and usernames.
     */
    public static String sanitizePrivacy(String text) {
        if (text == null) return "";
        String userHome = System.getProperty("user.home");
        String userName = System.getProperty("user.name");

        String scrubbed = text;
        if (userHome != null && !userHome.isEmpty()) {
            scrubbed = scrubbed.replace(userHome, "<USER_HOME>");
            scrubbed = scrubbed.replace(userHome.replace('\\', '/'), "<USER_HOME>");
        }
        if (userName != null && !userName.isEmpty()) {
            scrubbed = scrubbed.replace(userName, "<PLAYER_USER>");
        }
        return scrubbed;
    }
}
