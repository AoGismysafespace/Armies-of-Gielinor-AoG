package net.alterorb.launcher;

import java.lang.reflect.Field;

/**
 * ==============================================================================
 * AntiIdleWatchdog: Background Inactivity Disconnect Prevention Daemon
 * ==============================================================================
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    The original 2009 FunOrb client (wf.java:667) enforces a hardcoded 20-minute
 *    (60,000 tick) inactivity timeout designed for web browsers, which forcibly
 *    closes the network socket (via dj.a) and kicks the player to the main menu
 *    with a disconnection error:
 *    "We closed the connection because the game was left unattended for 20 minutes."
 *
 *    In this Standalone Steam Edition, games are played locally (skirmishes against AI)
 *    or via Steamworks P2P with friends. Disconnecting a player who steps away from
 *    their game (e.g. for a phone call, break, or overnight pause) abruptly destroys
 *    their match progress.
 *
 *    AntiIdleWatchdog runs as a silent, ultra-lightweight daemon thread that checks
 *    the engine's volatile idle counters every 5 seconds. If inactivity exceeds
 *    1,000 ticks (~20 seconds), it safely resets them back to 0 so the legacy
 *    60,000-tick disconnect threshold is never reached.
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    - `av.a` : volatile static int tracking mouse inactivity in engine ticks (50 ticks/sec).
 *    - `fc.b` : volatile static int tracking keyboard inactivity in engine ticks (50 ticks/sec).
 *    - `tp.b(-113)` : Method returning engine tick rate (50 ticks/sec).
 *    - 60,000 ticks: 20-minute hardcoded Jagex disconnect threshold (50 * 1200).
 *    - `of.bc`: Hardcoded Jagex notice string ("We closed the connection because...").
 *
 * 3. THREAD SAFETY & NON-INVASIVENESS:
 *    Both `av.a` and `fc.b` are declared `volatile` in the original engine bytecode,
 *    guaranteeing atomic, lock-free reads and writes across threads without contention.
 *    No original 2009 Jagex classes are modified.
 * ==============================================================================
 */
public class AntiIdleWatchdog {

    private static volatile boolean running = false;
    private static volatile boolean enabled = true;

    /**
     * Initializes and starts the background anti-idle watchdog daemon thread.
     * Safe to invoke multiple times; subsequent calls are no-ops.
     */
    public static synchronized void start() {
        if (running) {
            return;
        }
        running = true;

        Thread watchdog = new Thread(() -> {
            Field mouseIdleField = null;
            Field keyboardIdleField = null;

            while (running) {
                try {
                    // Sleep for 5 seconds between checks (0.0% CPU overhead)
                    Thread.sleep(5000L);

                    if (!enabled) {
                        continue;
                    }

                    // Dynamically resolve engine mouse idle counter (av.a)
                    if (mouseIdleField == null) {
                        try {
                            mouseIdleField = Class.forName("av").getDeclaredField("a");
                            mouseIdleField.setAccessible(true);
                        } catch (Throwable ignored) {}
                    }

                    // Dynamically resolve engine keyboard idle counter (fc.b)
                    if (keyboardIdleField == null) {
                        try {
                            keyboardIdleField = Class.forName("fc").getDeclaredField("b");
                            keyboardIdleField.setAccessible(true);
                        } catch (Throwable ignored) {}
                    }

                    // If mouse has been idle for more than 1,000 ticks (~20 seconds), reset counter
                    if (mouseIdleField != null) {
                        int mIdle = mouseIdleField.getInt(null);
                        if (mIdle > 1000) {
                            mouseIdleField.setInt(null, 0);
                        }
                    }

                    // If keyboard has been idle for more than 1,000 ticks (~20 seconds), reset counter
                    if (keyboardIdleField != null) {
                        int kIdle = keyboardIdleField.getInt(null);
                        if (kIdle > 1000) {
                            keyboardIdleField.setInt(null, 0);
                        }
                    }

                } catch (InterruptedException ie) {
                    break;
                } catch (Throwable ignored) {
                    // Defensive guard: never crash the watchdog thread on transient reflection errors
                }
            }
        }, "AntiIdleWatchdogThread");

        watchdog.setDaemon(true);
        watchdog.start();
        System.out.println("[AntiIdleWatchdog] Background inactivity guard started successfully.");
    }

    /**
     * Allows dynamically enabling or disabling watchdog resets (e.g. for testing).
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Stops the watchdog thread cleanly.
     */
    public static void stop() {
        running = false;
    }
}
