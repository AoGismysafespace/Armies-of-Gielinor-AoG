package net.alterorb.launcher;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * ====================================================================================================
 * Armies of Gielinor - Safe Display & Window Lifecycle Manager
 * Component: net.alterorb.launcher.DisplayManager
 * Target Runtime: OpenJDK 18.0.2.1 (Major version <= 62.0)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Manages game window modes (Fullscreen Native vs. Resizable Windowed) and screen resolution
 *    persistence without disrupting the AWT drawing surface, native display peers, or active asset loading.
 *
 *    THE ROOT CAUSE OF THE "75% EXTRA DATA" FREEZE & RESOLUTION REVERSION TRAP:
 *    1) The Reversion Trap:
 *       During startup (specifically at 75% loading while Archive 10 MIDI music uncompressed),
 *       Jagex's engine initializes FunOrb's HUD scale flag `ra.e.h` to false. A 50ms monitor loop
 *       in `StandaloneRunner.main` misread this as a user request for Fixed 640x480 windowed mode.
 *       It forcibly called `applyWindowMode(false, 640x480, true)` and wrote "Windowed - Fixed (640 x 480)"
 *       to `player.properties`, overriding user configs and shrinking the window mid-boot.
 *    2) The 75% Display Freeze (`tq.a` Canvas Destruction):
 *       `applyWindowMode` previously signaled `tq.a = true`. In Jagex's engine (`wf.java:820`),
 *       `tq.a` triggers `this.c((byte)87)`, which detaches and removes the AWT Canvas (`si.b`) and
 *       creates a new Canvas. However, the software rasterizer (`so.b`) was initialized once at startup
 *       (`iv.java:492`) with the original Canvas and is NEVER rebound. Consequently, subsequent blits
 *       in `pt.a` silently fail in an empty catch block, permanently freezing the OS window buffer
 *       on the 75% loading frame while the game continues running underneath (Screen 16 Title Screen).
 *
 *    ARCHITECTURAL FIX:
 *    - Suppresses mid-boot window mode changes while asset loading (`qu.q`) is active (fail-open).
 *    - Eliminates the destructive `tq.a = true` trigger entirely. FunOrb recalculates viewport
 *      layout automatically via standard AWT component events (`container.getSize()`) in `wf.java:649`.
 *    - Guarantees zero `jFrame.dispose()`, preserving the native Canvas peer indefinitely.
 *    - In Fullscreen, ensures `ra.e.h = true` and `mh.c = 0` so the game HUD is never offset or pushed down.
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Symbol / Concept            | Real Architectural Meaning       | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    `qu.q`                      | Asset Loading Complete Flag      | true = game reached title screen.
 *                                |                                  | false = actively loading archives.
 *    `ra.e.h`                    | FunOrb Fixed HUD Mode Flag       | false = 640x480 fixed HUD scale.
 *                                |                                  | true = dynamically resizable HUD.
 *    `mh.c`                      | FunOrb Vertical Canvas Offset    | In 640x480 mode, sets letterbox Y.
 *                                |                                  | Must be 0 in Fullscreen to prevent shift.
 *    `tq.a`                      | FunOrb Canvas Destruction Signal | In wf.java:820, calling this removes
 *                                |                                  | si.b and instantiates a new Canvas,
 *                                |                                  | breaking so.b rasterizer blits!
 *    `si.b`                      | Game AWT Canvas                  | Child component inside JFrame
 *                                |                                  | where game sprites are blitted.
 *    `so.b` / `jo` / `dh`        | Software Rasterizer Buffer       | Pixel array drawing into Canvas.
 *    `pt.a(int, int, Canvas, int)`| Video Blit Loop                 | Copies rasterizer pixels to Canvas.
 *    `player.properties`         | User Preference Store            | Persists username, volume, and
 *                                |                                  | window resolution across launches.
 *
 * 3. INLINE EXPLANATIONS:
 *    - `applyWindowMode()`: Dispatches on Swing EventQueue without calling `dispose()` or touching `tq.a`.
 *    - `sanitizeResolution()`: Detects legacy broken "Fixed 640x480" entries from older player configs
 *      and automatically self-heals them to "Fullscreen (Native)".
 *    - `saveCurrentScreenSize()`: Reads true component dimensions rather than FunOrb's internal HUD flag,
 *      preventing accidental reversion to 640x480 letterboxing.
 * ====================================================================================================
 */
public final class DisplayManager {

    private DisplayManager() {
        // Static utility class
    }

    /**
     * Safely applies Fullscreen (Native) or Resizable Windowed mode to the running game window.
     * Guaranteed NEVER to invoke jFrame.dispose() or trigger tq.a, preserving the native Canvas peer.
     *
     * @param jFrame     The main application JFrame.
     * @param appletObj  The game Applet component.
     * @param fullscreen True for borderless fullscreen, false for resizable windowed.
     * @param dimension  The target windowed dimension (ignored in fullscreen).
     * @param triggerTq  Legacy parameter, intentionally ignored to protect the Canvas peer.
     */
    public static void applyWindowMode(final JFrame jFrame, final Object appletObj, final boolean fullscreen, final Dimension dimension, final boolean triggerTq) {
        if (jFrame == null || appletObj == null) {
            return;
        }
        final Component applet = (Component) appletObj;

        // Fail-open guard: If initial asset loading is still active (!qu.q), suppress mid-boot window changes
        try {
            Field qField = Class.forName("qu").getDeclaredField("q");
            qField.setAccessible(true);
            if (!qField.getBoolean(null)) {
                System.out.println("[DisplayManager] Suppressed mid-boot window mode transition during active asset loading (!qu.q).");
                return;
            }
        } catch (Throwable ignored) {
            // Fail-open: If qu cannot be probed (e.g. in test harness or mock runner), proceed safely
        }

        SwingUtilities.invokeLater(() -> {
            try {
                if (fullscreen) {
                    // Fullscreen (Native): Expand bounds to cover primary monitor and maximize
                    Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
                    jFrame.setBounds(bounds);
                    jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    applet.setBounds(bounds);
                    StandaloneRunner.isBorderlessFullscreen = true;

                    // Defend against mh.c canvas displacement domino:
                    // Ensure ra.e.h is true (resizable HUD) and mh.c is 0 (no vertical letterbox shift)
                    try {
                        Field raField = Class.forName("ra").getDeclaredField("e");
                        raField.setAccessible(true);
                        Object raObj = raField.get(null);
                        if (raObj != null) {
                            Field hField = raObj.getClass().getDeclaredField("h");
                            hField.setAccessible(true);
                            hField.set(raObj, Boolean.TRUE);
                        }
                    } catch (Throwable ignored) {}

                    try {
                        Field mhField = Class.forName("mh").getDeclaredField("c");
                        mhField.setAccessible(true);
                        mhField.set(null, 0);
                    } catch (Throwable ignored) {}

                    jFrame.revalidate();
                    jFrame.repaint();
                    System.out.println("[DisplayManager] Applied Fullscreen (Native) cleanly without peer dispose. Bounds: " + bounds);
                } else {
                    // Windowed: Restore normal window state, apply target dimensions, pack and center
                    jFrame.setExtendedState(JFrame.NORMAL);
                    jFrame.setResizable(true);

                    Dimension targetDim = (dimension != null && dimension.width >= 640 && dimension.height >= 480)
                            ? dimension
                            : (StandaloneRunner.lastWindowedSize != null ? StandaloneRunner.lastWindowedSize : new Dimension(1280, 720));

                    applet.setPreferredSize(targetDim);
                    applet.setSize(targetDim);
                    jFrame.pack();
                    jFrame.setLocationRelativeTo(null);
                    StandaloneRunner.isBorderlessFullscreen = false;

                    jFrame.revalidate();
                    jFrame.repaint();
                    System.out.println("[DisplayManager] Applied Windowed mode cleanly without peer dispose. Size: " + targetDim);
                }

                if (!jFrame.isVisible()) {
                    jFrame.setVisible(true);
                }
                jFrame.toFront();

                // NOTE: triggerTq is intentionally NEVER set to true here!
                // Setting tq.a = true causes wf.java:820 to destroy si.b (Canvas), which orphans
                // so.b and freezes the display permanently on the last blitted frame.
            } catch (Throwable t) {
                System.err.println("[DisplayManager] Error applying window mode: " + t.getMessage());
                t.printStackTrace(System.err);
            }
        });
    }

    /**
     * Inspects the running window and determines the correct resolution string to persist.
     * Prevents FunOrb's internal fixed HUD scale flag (ra.e.h) from overwriting user resolutions.
     */
    public static void saveCurrentScreenSize() {
        try {
            JFrame jFrame = StandaloneRunner.frameHolder[0];
            Object appletObj = StandaloneRunner.appletHolder[0];
            if (jFrame == null || appletObj == null) {
                return;
            }
            Component applet = (Component) appletObj;

            String resolutionStr;
            if (StandaloneRunner.isBorderlessFullscreen) {
                resolutionStr = "Fullscreen (Native)";
            } else {
                int w = applet.getWidth();
                int h = applet.getHeight();
                if (w <= 0 || h <= 0) {
                    if (StandaloneRunner.lastWindowedSize != null && StandaloneRunner.lastWindowedSize.width >= 640 && StandaloneRunner.lastWindowedSize.height >= 480) {
                        w = StandaloneRunner.lastWindowedSize.width;
                        h = StandaloneRunner.lastWindowedSize.height;
                    } else {
                        w = 1280;
                        h = 720;
                    }
                }
                // Only save fixed 640x480 if the actual window is explicitly sized to 640x480
                if (w <= 640 && h <= 480) {
                    resolutionStr = "Windowed - Fixed (640 x 480)";
                } else {
                    w = Math.max(640, w);
                    h = Math.max(480, h);
                    resolutionStr = "Windowed - Resizable (" + w + " x " + h + ")";
                }
            }
            StandaloneRunner.updateAndSaveResolution(resolutionStr);
        } catch (Throwable t) {
            System.err.println("[DisplayManager] Error saving screen size: " + t.getMessage());
        }
    }

    /**
     * Sanitizes raw resolution strings loaded from player.properties.
     * Automatically self-heals legacy broken 640x480 configurations to Fullscreen (Native).
     *
     * @param rawResolution The raw resolution string from disk.
     * @return Clean, verified resolution preference.
     */
    public static String sanitizeResolution(String rawResolution) {
        if (rawResolution == null || rawResolution.isBlank()) {
            return "Fullscreen (Native)";
        }
        String trimmed = rawResolution.trim();
        // Self-heal: If an older build left 640x480 or fixed mode, default safely to Fullscreen (Native)
        if ("Resizable (Auto-Fit Window)".equalsIgnoreCase(trimmed)
                || trimmed.toLowerCase().contains("fixed")
                || trimmed.contains("640")) {
            System.out.println("[DisplayManager] Self-healed legacy broken resolution ('" + trimmed + "') -> defaulting to Fullscreen (Native).");
            return "Fullscreen (Native)";
        }
        return trimmed;
    }
}
