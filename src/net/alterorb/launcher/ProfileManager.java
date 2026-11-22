package net.alterorb.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * ====================================================================================================
 * Armies of Gielinor - Safe Player Profile & Offline Identity Manager
 * Component: net.alterorb.launcher.ProfileManager
 * Target Runtime: OpenJDK 18.0.2.1 (Class major version <= 62.0)
 *
 * 1. PURPOSE & ARCHITECTURAL CONTEXT:
 *    Safely manages the local commander's profile name, ensuring a privacy-preserving generic default
 *    ("Commander") for distributed copies, preventing legacy test names ("jesii98") from ever surfacing,
 *    and maintaining strict conformance with FunOrb's 12-character Base37 username limitations.
 *
 *    - In Online / Steam Mode:
 *      The player's active Steam display name (PersonaName) is automatically retrieved and sanitized
 *      (e.g., "E-Soul" for the host, or a friend's Steam name for guests).
 *
 *    - In Offline / Standalone Mode (Steam Closed):
 *      The game falls back to the local preference stored in `player.properties`. If no custom name
 *      was specified, or if an obsolete legacy placeholder ("jesii98") is encountered, it seamlessly
 *      defaults to the neutral, privatized title "Commander".
 *
 * 2. ROSETTA STONE / SYMBOL MAPPINGS:
 *    --------------------------------------------------------------------------------------------------
 *    Symbol                      | Real Architectural Meaning       | Role & Side Effects
 *    --------------------------------------------------------------------------------------------------
 *    `DEFAULT_OFFLINE_NAME`      | Privatized Generic Title         | "Commander" — safe for public release.
 *    `OBSOLETE_LEGACY_NAME`      | Obsolete Test Identifier         | "jesii98" — permanently intercepted & purged.
 *    `sanitizeUsername`          | FunOrb Format Sanitizer          | Enforces 1-12 chars, [a-zA-Z0-9_\- ].
 *    `player.properties`         | User Preference Store            | Stores local volume, resolution, and username.
 *    `Base37 Encoding`           | 2009 Jagex Network Protocol      | Encodes up to 12 chars into a 64-bit long.
 *
 * 3. INLINE EXPLANATIONS:
 *    - Pure Java 18 implementation without third-party dependencies.
 *    - Completely immune to regex denial-of-service and special unicode/emoji injection.
 * ====================================================================================================
 */
public final class ProfileManager {

    /** Default privatized title for standalone/offline play and public distribution archives. */
    public static final String DEFAULT_OFFLINE_NAME = "Commander";

    /** Obsolete pre-release testing placeholder that must never be presented to the player. */
    public static final String OBSOLETE_LEGACY_NAME = "jesii98";

    /** Maximum character length allowed by Jagex's Base37 long encoding in the 2009 network protocol. */
    public static final int MAX_FUNORB_NAME_LENGTH = 12;

    private ProfileManager() {
        // Utility class: prevent direct instantiation
    }

    /**
     * Sanitizes a raw username string loaded from disk or user input.
     * Intercepts obsolete test placeholders, strips non-ASCII/special characters,
     * truncates to 12 characters, and defaults to "Commander" if blank.
     *
     * @param rawName Raw username string (may be null or blank)
     * @return Cleaned, FunOrb-compliant username (1-12 chars, alphanumeric, space, hyphen, underscore)
     */
    public static String sanitizeUsername(String rawName) {
        if (rawName == null) {
            return DEFAULT_OFFLINE_NAME;
        }

        String trimmed = rawName.trim();
        // Intercept obsolete pre-release test placeholder or empty strings
        if (trimmed.isEmpty() || OBSOLETE_LEGACY_NAME.equalsIgnoreCase(trimmed)) {
            return DEFAULT_OFFLINE_NAME;
        }

        // FunOrb protocol allows only alphanumeric characters, underscores, hyphens, and spaces
        String cleaned = trimmed.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim();

        // Jagex 64-bit Base37 representation cannot encode more than 12 characters
        if (cleaned.length() > MAX_FUNORB_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_FUNORB_NAME_LENGTH).trim();
        }

        if (cleaned.isEmpty() || OBSOLETE_LEGACY_NAME.equalsIgnoreCase(cleaned)) {
            return DEFAULT_OFFLINE_NAME;
        }

        return cleaned;
    }
}
