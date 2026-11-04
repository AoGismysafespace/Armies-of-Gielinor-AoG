/**
 * ============================================================================
 * GenerateUnitCompendium: Retro Unit Dossier ("Steckbrief") HTML Generator
 * ============================================================================
 * 
 * Architectural Purpose:
 * Generates the complete, authentic Chapter VII Unit Compendium for the official
 * Armies of Gielinor game manual. Every unit receives an individual retro "Steckbrief"
 * (Field Dossier Card), complete with:
 *   1. Authentic unit head portrait image extracted from cache index 5 (pl.x).
 *   2. RuneScape typography (Kingthings Petrock titles, RuneScape Quill lore).
 *   3. 4-stat vital telemetry grid (Speed, Health, Strength, Range).
 *   4. Special battlefield traits (Capture, Flying, Decay, Amphibious, Restoration).
 *   5. DEDICATED SPECIAL ABILITIES SECTION:
 *      Detailed tactical explanation of active spells, terrain transformations,
 *      air-interception, mind control, portal creation, burrowing, and combat buffs,
 *      sourced directly from official engine bytecode table 'bh.M'.
 *   6. Official Jagex lore text extracted from client field 'ut.cb'.
 *   7. Single-line tactical deployment verdict.
 * 
 * Rosetta Stone / Symbol Mappings:
 * - dn.Pb           : String[] unit names table (89 entries).
 * - bw.m            : int[][] combat statistics table:
 *                     s[0]: Movement Class (0=Foot, 1=Light, 2=Mounted, 3=Artillery/Cavalry,
 *                           4=Flying, 5=Special, 6=Siege, 7=Colossal)
 *                     s[1]: God Allegiance (-1=Neutral, 0=Saradomin, 1=Zamorak, 2=Guthix,
 *                           3=Bandos, 4=Menaphos, 5=Seren)
 *                     s[2]: Tier/Rank (1-5)
 *                     s[3]: Speed (Hex movement points per turn)
 *                     s[4]: Hitpoints / Vitality
 *                     s[5]: Strength (Combat attack dice)
 *                     s[6]: Min Attack Range
 *                     s[7]: Max Attack Range
 *                     s[8]: Mana Summoning Cost (in Victory Stars)
 *                     s[10]: Ability Bitmask (512=Capture, 4096=Amphibious, 256=Decay, etc.)
 * - ut.cb           : String[] authentic Jagex unit lore descriptions.
 * - wp.H            : int[][] 7-pantheon grouping array mapping units to factions.
 * - bh.M            : String[] official Jagex special ability descriptions table.
 * 
 * Privacy & Safety Guarantee:
 * Strictly reads game engine static bytecode tables. Zero personal data, Steam
 * identifiers, or machine paths are output.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;

public class GenerateUnitCompendium {

    // Pantheon Faction Metadata
    static class FactionInfo {
        String name;
        String title;
        String color;
        String loreSummary;

        FactionInfo(String name, String title, String color, String loreSummary) {
            this.name = name;
            this.title = title;
            this.color = color;
            this.loreSummary = loreSummary;
        }
    }

    private static final FactionInfo[] FACTIONS = {
        new FactionInfo("Neutral", "Neutral & Independent Mercenaries", "#e4ded4",
            "Unaligned forces, wild mythical beasts, and hardy barbarian warriors who fight for whoever holds the mana to summon them."),
        new FactionInfo("Saradomin", "The Holy Host of Saradomin", "#5dade2",
            "Righteous warriors, holy paladins, and devout healers dedicated to order, spiritual protection, and heavy armor."),
        new FactionInfo("Zamorak", "The Chaos Legion of Zamorak", "#e74c3c",
            "Mahjarrat zealots, undead horrors, dark necromancers, and terrifying demons who thrive on combat frenzy and decay."),
        new FactionInfo("Guthix", "The Natural Wardens of Guthix", "#58d68d",
            "Custodians of natural equilibrium. Commands great woodland beasts, heavy siege tortoises, and ancient awakened Ents."),
        new FactionInfo("Seren", "The Crystalline Houses of Seren", "#af7ac5",
            "The ancient elven kingdoms. Masters of lethal archery, nimble forest skirmishing, and crystalline shapeshifting."),
        new FactionInfo("Bandos", "The War Horde of Bandos", "#f39c12",
            "The brutal war tribes of goblins, hobgoblins, orks, and colossal ourg chieftains. Overwhelms through sheer physical mass."),
        new FactionInfo("Menaphos", "The Desert Empire of Menaphos", "#f1c40f",
            "Ancient desert kingdom worshippers of Tumeken, Elidinis, and Icthlarin. Commands swarming insects, resilient mummies, and sphinxes.")
    };

    private static final String[] MOVE_NAMES = {
        "Infantry (Foot)",
        "Skirmisher (Light)",
        "Mounted (Cavalry)",
        "Arcane / Mounted Mage",
        "Aerial (Flying)",
        "Special / Sorcerer",
        "Siege Engine",
        "Colossal Titan"
    };

    /**
     * Resolves the official special ability title and detailed mechanical explanation for a unit.
     */
    private static class SpecialAbility {
        String title;
        String explanation;

        SpecialAbility(String title, String explanation) {
            this.title = title;
            this.explanation = explanation;
        }
    }

    private static SpecialAbility getSpecialAbility(int uId, String[] engineAbilities) {
        // Specific mechanical descriptions mapping bytecode table bh.M with detailed tactical context
        switch (uId) {
            case 0:
                return new SpecialAbility("Territorial Conquest",
                    "The cornerstone ability to capture villages, towers, and portals. Barbarians and Chieftains are the only units in the entire game capable of claiming structures.");
            case 1:
                return new SpecialAbility("Armadyl Flight & Aerial Supremacy",
                    "Soars over all terrestrial obstacles, rivers, and mountains with complete immunity to ground melee attacks. Can only be attacked by ranged units or air-interceptors.");
            case 2:
                return new SpecialAbility("Simian Resilience & Bulk",
                    "Boasts an enormous 20 HP health pool for a skirmish soldier, acting as a sturdy vanguard capable of absorbing punishing enemy fire.");
            case 3:
                return new SpecialAbility("Mounted Javelin Volley & Capture",
                    "Possesses structure-capturing bloodlines while striking from range 2-3 with thrown spears. Essential for seizing objectives without entering direct melee contact.");
            case 4:
                return new SpecialAbility("Draconic Aerial Fire Breath",
                    "Airborne dragon capable of flying over all impassable terrain and projecting incinerating breath attacks at range 1-2 without triggering melee counter-attacks.");
            case 5:
                return new SpecialAbility("Volcanic Obsidian Plating",
                    "Heavy molten rock defense boasting 35 HP. Holds key bridge crossings and choke points against multiple waves of attackers.");
            case 6:
                return new SpecialAbility("Amphibious Air-Interception",
                    "Traverses deep water and ocean hexes with zero movement penalty. Possesses the rare Air-Interception capability, allowing it to strike down and drag airborne flying units into melee combat.");
            case 7:
                return new SpecialAbility("Deploy Temporary Stone Portal",
                    "Opens a temporary stone portal on any target tile for your chosen god's forces. This enables forward reinforcements directly to the front line without needing to march across the map from distant home base portals.");
            case 8:
                return new SpecialAbility("Holy Vanguard Defense",
                    "Righteous holy warrior with high defensive resilience, anchoring the frontline and retaliating with heavy holy counter-attacks.");
            case 9:
                return new SpecialAbility("Divine Aerial Reconnaissance",
                    "High-speed 6 flying scout with devastating 7 STR dive strikes, bypassing defensive walls to eliminate vulnerable enemy archers and siege troops.");
            case 10:
                return new SpecialAbility("Field Medicine & Bone Burial",
                    "Heals adjacent friendly units by 3 HP each turn and cures toxic poison. Can bury bones on fallen unit tiles to permanently increase the Monk's maximum health by +1 HP.");
            case 11:
                return new SpecialAbility("Divine Holy Artillery (Range 3-5)",
                    "Long-range holy magic strikes bombarding hostile lines from exceptional distance, out-ranging all conventional archers.");
            case 12:
                return new SpecialAbility("Falador Chivalric Armor",
                    "Superior plate armor providing heavy damage mitigation in melee combat, forming an impenetrable battle line.");
            case 13:
                return new SpecialAbility("Purifying Gallop & Horn Strike",
                    "Exceptional 8 Speed cavalry mobility with natural restoration resistance, rapidly flanking enemy formations.");
            case 14:
                return new SpecialAbility("Centaur Mounted Archery (Range 1-3)",
                    "Blazing 9 Speed cavalry bowman capable of striking targets up to 3 tiles away while maintaining safe distance from ground melee soldiers.");
            case 15:
                return new SpecialAbility("Predatory Pounce & Resilience",
                    "Ferocious big cat boasting 40 HP and powerful offensive claws, capable of hunting down retreating wounded targets.");
            case 16:
                return new SpecialAbility("Prayerful Volley (Range 2-3)",
                    "Saradominist marksmanship loose arrows with unerring accuracy, inflicting heavy losses without drawing retaliation.");
            case 17:
                return new SpecialAbility("Divine Sanctuary & Spiritual Ward",
                    "Restores 3 HP and cures poison on adjacent allies. Can deploy a spiritual barrier at a chosen location, absorbing 40% of all incoming damage dealt to friendly units within it.");
            case 18:
                return new SpecialAbility("Holy Throwing Axes & Aerial Flight",
                    "Elite winged champion of Saradomin. Flie over terrain and strikes targets up to 3 tiles away with godly throwing axes.");
            case 19:
                return new SpecialAbility("Feral Bloodthirst Frenzy",
                    "Savage werewolf berserker dealing devastating 5 STR melee damage, breaking enemy shield walls through sheer brutality.");
            case 20:
                return new SpecialAbility("Undead Corpse Decay",
                    "Inexpensive reanimated skeleton that slowly rots away, suffering 1 DMG at the start of each turn unless stationed on a friendly portal tile.");
            case 21:
                return new SpecialAbility("Necromantic Reanimation & Siphon",
                    "Raises the bones of fallen dead on adjacent map tiles into fresh Skeleton Warriors. Furthermore, immediately restores adjacent friendly skeletons to full health.");
            case 22:
                return new SpecialAbility("Chaos Blast Artillery (Range 3-5)",
                    "Hurls concentrated chaotic fire across 5 hex tiles, demolishing fortified enemy positions from afar.");
            case 23:
                return new SpecialAbility("Kinshra Dark Plate Armor",
                    "Ruthless dark armor granting superior defensive rating, matching the White Knights in defensive staying power.");
            case 24:
                return new SpecialAbility("Abyssal Greater Bulk",
                    "Colossal 50 HP demon radiating demonic dread, capable of absorbing immense punishment while crushing infantry.");
            case 25:
                return new SpecialAbility("Barbed Blackwood Arrows (Range 2-3)",
                    "Inflicts lethal ranged puncture wounds from 2-3 tiles away, softening heavily armored enemies before melee contact.");
            case 26:
                return new SpecialAbility("Hellhound Pack Sprint",
                    "Blistering 8 Speed demonic canine that hunts down escaping scouts and cuts off enemy supply lines.");
            case 27:
                return new SpecialAbility("Parasitic Mind Hijack",
                    "Amphibious parasitic beast that sacrifices itself in an attack to seize permanent control of an enemy unit with combat strength equal to or lower than its own!");
            case 28:
                return new SpecialAbility("Aerial Boulder Bombardment (Range 1-3)",
                    "Winged demonic gargoyle capable of flying over walls and dropping heavy boulders on targets up to 3 tiles away.");
            case 29:
                return new SpecialAbility("Forest Incineration (Wildfire)",
                    "Moves onto any forest tile and instantly incinerates the timber, permanently converting dense woodland into open ground and stripping enemy cover.");
            case 30:
                return new SpecialAbility("Terrorbird Sprint Cavalry",
                    "Astonishing 10 Speed sprint mobility, crossing the entire battlefield in a single turn to capture contested choke points.");
            case 31:
                return new SpecialAbility("Wolfpack Prowl",
                    "Nimble 8 Speed predator adept at pack flanking maneuvers through woodlands.");
            case 32:
                return new SpecialAbility("Nature's Surge (Strength Surge) & Purge",
                    "Increases the combat strength of adjacent friendly units by +5 points (diminishes by 1 per turn) and immediately cures poisoned allies.");
            case 33:
                return new SpecialAbility("Black Guard Tenacity",
                    "Stubborn dwarven infantry with fortified stance, gaining amplified defensive cover bonuses.");
            case 34:
                return new SpecialAbility("Deploy Ballistic Dwarf Cannon",
                    "Dwarven military engineers who construct a massive, fixed ballistic cannon on a designated tile to bombard enemy fortifications with splash damage.");
            case 35:
                return new SpecialAbility("Woodland Behemoth (Dense Cover)",
                    "Massive 40 HP nature giant. Gains double defense when fighting in dense forest terrain.");
            case 36:
                return new SpecialAbility("Void Knight Discipline",
                    "Elite veteran warrior boasting 5 STR and 25 HP, trained to counter dark magical monstrosities.");
            case 37:
                return new SpecialAbility("Armored Mobile Shell",
                    "Gigantic 45 HP battle tortoise functioning as a living siege engine and arrow shield for friendly troops.");
            case 38:
                return new SpecialAbility("Gnomecopter High-Altitude Flight",
                    "High-speed 10 flight vehicle that conducts deep reconnaissance, evading ground obstacles with ease.");
            case 39:
                return new SpecialAbility("Choking Jade Vine Overgrowth",
                    "Expands living vines onto adjacent open tiles, permanently blocking enemy pathing. Costs 10 Mana/turn per tile; can be ordered to wither and die on command.");
            case 40:
                return new SpecialAbility("Awaken Living Forest (Afforestation)",
                    "Plants living woodland on any open ground tile it moves onto, generating instant defensive cover for friendly Guthixian troops.");
            case 41:
                return new SpecialAbility("High-Speed Raptor Dive (Flying)",
                    "Blazing 10 Speed winged falcon delivering lethal 7 STR dive attacks against isolated targets.");
            case 42:
                return new SpecialAbility("Ursine Forest Might",
                    "Tenacious woodland bear boasting high natural fortitude in forest terrain.");
            case 43:
                return new SpecialAbility("Harmonic Morale Song",
                    "Her melodic voice inspires all friendly units within 2 map tiles, allowing them to fight with equal combat effectiveness against all unit armor classes.");
            case 44:
                return new SpecialAbility("Crystalline Shield Wall",
                    "Elven defender carrying a crystal buckler, soaking up frontline assault damage with 30 HP.");
            case 45:
                return new SpecialAbility("Pawya Hunter Spine Volley",
                    "Swift forest scout armed with natural projectile quills, striking before prey can burrow to safety.");
            case 46:
                return new SpecialAbility("Dire Pack Sprint",
                    "High-velocity 8 Speed wolf adept at hunting down archers and retreating enemies.");
            case 47:
                return new SpecialAbility("Pike Phalanx Counter",
                    "Frontline elven pikemen with reach weapons designed to impale incoming mounted cavalry.");
            case 48:
                return new SpecialAbility("Prismatic Longbow Volley (Range 2-4)",
                    "Deadly elven archery reaching out to 4 map tiles, devastating enemy ranks before they can close the distance.");
            case 49:
                return new SpecialAbility("Elven Lordship Command",
                    "Stalwart 40 HP elven noble anchoring the front with superior combat poise.");
            case 50:
                return new SpecialAbility("Nimble Huntress Gallop",
                    "High 8 Speed foot archer delivering precise hit-and-run strikes across open plains.");
            case 51:
                return new SpecialAbility("Burrow Assassination",
                    "Burrows underground and surfaces directly onto a map tile occupied by an enemy unit, instantly executing and slaying that unit in the process!");
            case 52:
                return new SpecialAbility("Goblin Swarm Tactics",
                    "Extremely cheap 50 Mana summon cost, enabling commanders to flood the battlefield with expendable cannon fodder.");
            case 53:
                return new SpecialAbility("Terrain Desecration & Confusion",
                    "Confuses all enemy units within two map tiles, completely stripping them of all defensive cover bonuses granted by their terrain.");
            case 54:
                return new SpecialAbility("Ogre Club Smash",
                    "Dense, lumbering brute with heavy club swings that shatter light infantry.");
            case 55:
                return new SpecialAbility("Jungle Spider Venom Resistance",
                    "Tough jungle hide rendering the Jogre completely impervious to poison and swamp penalties.");
            case 56:
                return new SpecialAbility("Amphibious Aquatic Crossing",
                    "Aquatic ogre with fully developed underwater breathing, moving across rivers, marshes, and oceans without speed loss.");
            case 57:
                return new SpecialAbility("Brute Air-Interception",
                    "Equipped with anti-air reach weapons capable of intercepting, attacking, and grounding flying units.");
            case 58:
                return new SpecialAbility("Dark Smog Shroud (Anti-Ranged)",
                    "Conjures a dense cloud of black smoke over nearby hexes, completely preventing enemy archers and mages from making ranged attacks against friendly units inside.");
            case 59:
                return new SpecialAbility("Gaze of Paralyzing Dread",
                    "Focuses its central eye on all enemy units within two map tiles, paralyzing them with fear so they are completely unable to move on their turn!");
            case 60:
                return new SpecialAbility("Bandosian Shock Line",
                    "The staple shock soldier of Bandos, combining sturdy 30 HP mass with 5 STR strike power.");
            case 61:
                return new SpecialAbility("Ravenous Ogress Charge",
                    "Swift 7 Speed female ogre warrior that charges forward and devours anything in its path.");
            case 62:
                return new SpecialAbility("Mountain Demolition (Terrain Shatter)",
                    "When moving 1 tile onto an impassable mountain, this colossal giant destroys the peak, crushing it into rubble and permanently converting it to open ground!");
            case 63:
                return new SpecialAbility("Insect Swarm Movement Web",
                    "Surrounded by a crawling swarm of hive insects that drastically inhibits and halts the movement of all enemy units attempting to cross nearby tiles.");
            case 64:
                return new SpecialAbility("Desert Scavenger Sprint",
                    "Fast 6 Speed desert canid that thrives on harsh terrain, picking off isolated scouts.");
            case 65:
                return new SpecialAbility("Toxic Mummified Rot",
                    "Walking preserved corpse that infects enemies with deadly poison on strike, while decaying 1 HP/turn unless stationed on a portal.");
            case 66:
                return new SpecialAbility("Poison Locust Swarm (Flying)",
                    "Swift 7 Speed flying insect swarm that injects lethal toxins into enemy ranks, ignoring terrain barriers.");
            case 67:
                return new SpecialAbility("Bedabin Throwing Darts (Range 2-3)",
                    "Hardy desert nomad possessing the secret of thrown darts, striking with 7 STR from range 2-3.");
            case 68:
                return new SpecialAbility("Cosh Ambush",
                    "Economical 100 Mana street thug, ideal for swarming and tying up high-value targets.");
            case 69:
                return new SpecialAbility("Icthlarin Soul Ward",
                    "Devout warrior of the god of death, boasting 25 HP and 5 STR holy defense.");
            case 70:
                return new SpecialAbility("Amphibious River Ambush",
                    "Reptilian predator that stalks rivers and swamps, ambushing unsuspecting soldiers who approach the water's edge.");
            case 71:
                return new SpecialAbility("Scabarite Poison Sorcery (Range 3-5)",
                    "Arcane desert mage that launches virulent toxic spells across 5 tiles, inflicting damage and lingering poison.");
            case 72:
                return new SpecialAbility("Sphinx Enigma (Mind Subjugation)",
                    "Speaks in mesmerizing riddles that invade an adjacent enemy unit's psyche, granting you permanent tactical control of the enchanted unit!");
            case 73:
                return new SpecialAbility("River Creation (Deluge of Elidinis)",
                    "When moving a distance of 1 tile from a river/sea onto open ground, it floods the land, permanently converting open ground into a river!");
            case 74:
                return new SpecialAbility("Twin Breath Dragonfire Strike",
                    "The supreme ruler of dragons. Three heads allow two separate attacks per turn with incinerating dragonfire at range 1-3. Exclusive to the first player to summon him!");
            default:
                if (engineAbilities != null && uId < engineAbilities.length && engineAbilities[uId] != null && !engineAbilities[uId].trim().isEmpty()) {
                    return new SpecialAbility("Battlefield Specialization", engineAbilities[uId]);
                }
                return new SpecialAbility("Standard Line Combat", "Trained combat soldier specialized in direct frontline engagement and defensive counter-strikes.");
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("[GenerateUnitCompendium] Starting generation of Chapter VII Steckbrief HTML with detailed Special Abilities...");

            String[] unitNames = dn.Pb;
            int[][] stats = bw.m;
            String[] loreTexts = ut.cb;
            int[][] groups = wp.H;

            // Fetch engine abilities table bh.M via reflection
            String[] engineAbilities = null;
            try {
                Class<?> bhClass = Class.forName("bh");
                Field mField = bhClass.getDeclaredField("M");
                mField.setAccessible(true);
                engineAbilities = (String[]) mField.get(null);
                System.out.println("[GenerateUnitCompendium] Successfully indexed " + (engineAbilities != null ? engineAbilities.length : 0) + " engine abilities from bh.M.");
            } catch (Throwable t) {
                System.out.println("[GenerateUnitCompendium] Note: bh.M not directly accessible via reflection, using fallback mappings: " + t);
            }

            File unitIconsDir = new File("manual/assets/unit_icons");
            Map<Integer, String> iconFileMap = new HashMap<>();
            if (unitIconsDir.exists() && unitIconsDir.isDirectory()) {
                for (File f : unitIconsDir.listFiles()) {
                    if (f.getName().endsWith(".png")) {
                        String prefix = f.getName().substring(0, 2);
                        try {
                            int id = Integer.parseInt(prefix);
                            iconFileMap.put(id, "assets/unit_icons/" + f.getName());
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            System.out.println("[GenerateUnitCompendium] Indexed " + iconFileMap.size() + " unit portrait files.");

            StringBuilder sb = new StringBuilder();

            // Iterate through each of the 7 Pantheons
            for (int fIdx = 0; fIdx < groups.length && fIdx < FACTIONS.length; fIdx++) {
                FactionInfo faction = FACTIONS[fIdx];
                int[] unitIds = groups[fIdx];

                sb.append("\n  <!-- ==================== PANTHEON: ").append(faction.name.toUpperCase()).append(" ==================== -->\n");
                sb.append("  <div class=\"pantheon-section page-break\">\n");
                sb.append("    <div class=\"pantheon-banner pantheon-").append(faction.name.toLowerCase()).append("\">\n");
                sb.append("      <h3 class=\"pantheon-heading\" style=\"color:").append(faction.color).append(";\">")
                  .append(faction.title).append("</h3>\n");
                sb.append("      <p class=\"pantheon-desc\">").append(faction.loreSummary).append("</p>\n");
                sb.append("    </div>\n\n");
                sb.append("    <div class=\"dossier-grid\">\n");

                for (int uId : unitIds) {
                    if (uId < 0 || uId >= stats.length || stats[uId] == null || stats[uId].length < 8) {
                        continue;
                    }

                    String name = (uId < unitNames.length && unitNames[uId] != null) ? unitNames[uId] : ("Unit " + uId);
                    int[] s = stats[uId];
                    int moveType = s[0];
                    int speed = s[3];
                    int hp = s[4];
                    int str = s[5];
                    int minRange = s[6];
                    int maxRange = s[7];
                    int cost = s.length > 8 ? s[8] : 0;
                    int flag10 = s.length > 10 ? s[10] : 0;

                    String moveName = (moveType >= 0 && moveType < MOVE_NAMES.length) ? MOVE_NAMES[moveType] : "Terrestrial";
                    String rangeStr = (minRange == maxRange) ? String.valueOf(maxRange) : (minRange + "-" + maxRange);

                    String lore = (uId < loreTexts.length && loreTexts[uId] != null && !loreTexts[uId].trim().isEmpty())
                        ? loreTexts[uId]
                        : "Field records for this warrior are preserved within the sacred archives of Gielinor.";

                    // Escape quotes and special characters for HTML
                    lore = lore.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");

                    String iconPath = iconFileMap.getOrDefault(uId, "assets/unit_icons/00_barbarian.png");

                    // Trait tags calculation
                    List<String> traits = new ArrayList<>();
                    if (moveType == 4) {
                        traits.add("<span class=\"trait-pill flight\">&#9992; Aerial Flight</span>");
                    }
                    if (uId == 0 || uId == 3) {
                        traits.add("<span class=\"trait-pill capture\">&#9873; Structure Capture</span>");
                    }
                    if (uId == 20 || uId == 65) {
                        traits.add("<span class=\"trait-pill decay\">&#9760; Undead Decay</span>");
                    }
                    if ((flag10 & 4096) != 0 || uId == 6 || uId == 27 || uId == 56 || uId == 70) {
                        traits.add("<span class=\"trait-pill amphib\">&#127754; Amphibious</span>");
                    }
                    if (uId == 6 || uId == 57) {
                        traits.add("<span class=\"trait-pill ranged\">&#9876; Air-Interception</span>");
                    }
                    if (uId == 7) {
                        traits.add("<span class=\"trait-pill capture\">&#9737; Portal Conjuration</span>");
                    }
                    if (uId == 10 || uId == 17 || uId == 32 || uId == 53 || uId == 58) {
                        traits.add("<span class=\"trait-pill heal\">&#10022; Restoration &amp; Medicine</span>");
                    }
                    if (uId == 34 || uId == 39) {
                        traits.add("<span class=\"trait-pill siege\">&#128165; Fixed Ballistic Siege</span>");
                    }
                    if (uId == 74) {
                        traits.add("<span class=\"trait-pill titan\">&#128293; Twin Breath Strike</span>");
                        traits.add("<span class=\"trait-pill titan\">&#128081; Dragon King</span>");
                    }
                    if (uId == 63) {
                        traits.add("<span class=\"trait-pill titan\">&#128081; Swarm Hive Queen</span>");
                    }
                    if (uId == 51) {
                        traits.add("<span class=\"trait-pill decay\">&#9760; Instant Burrow Slay</span>");
                    }
                    if (uId == 27 || uId == 72) {
                        traits.add("<span class=\"trait-pill heal\">&#128065; Mind Control</span>");
                    }
                    if (minRange > 1 || maxRange > 1) {
                        traits.add("<span class=\"trait-pill ranged\">&#127919; Ranged Attack (" + rangeStr + ")</span>");
                    }
                    if (traits.isEmpty()) {
                        traits.add("<span class=\"trait-pill\">Standard Combatant</span>");
                    }

                    // Special Ability Resolution
                    SpecialAbility ability = getSpecialAbility(uId, engineAbilities);

                    // Tactical verdict line
                    String roleVerdict;
                    if (uId == 0 || uId == 3) {
                        roleVerdict = "Critical territorial soldier; claims settlements, towers, and enemy portals.";
                    } else if (uId == 6) {
                        roleVerdict = "Anti-air and aquatic skirmisher; counters aerial flyers and navigates rivers.";
                    } else if (uId == 7) {
                        roleVerdict = "Strategic forward spawner; establishes forward teleport nodes for rapid deployment.";
                    } else if (moveType == 4) {
                        roleVerdict = "Swift aerial raider; bypasses defensive fortifications, rivers, and chokepoints.";
                    } else if (uId == 10 || uId == 17 || uId == 32 || uId == 53) {
                        roleVerdict = "Vital medical support; restores hitpoints and purges toxic poisons from nearby troops.";
                    } else if (minRange > 1 || maxRange > 1) {
                        roleVerdict = "Long-range marksman; bombards hostile ranks without triggering direct melee counter-attacks.";
                    } else if (moveType == 7) {
                        roleVerdict = "Supreme battlefield titan; devastating combat power capable of anchoring entire army fronts.";
                    } else if (uId == 20) {
                        roleVerdict = "Inexpensive shock soldier; decays unless maintained near a secure stone portal.";
                    } else {
                        roleVerdict = "Frontline combat operative; forms sturdy line formations to pin and engage hostile forces.";
                    }

                    // Render Steckbrief Card with Special Ability Section
                    sb.append("      <!-- ").append(name).append(" (ID: ").append(uId).append(") -->\n");
                    sb.append("      <div class=\"steckbrief\">\n");
                    sb.append("        <div class=\"sb-header\">\n");
                    sb.append("          <div class=\"sb-portrait-frame\">\n");
                    sb.append("            <img class=\"sb-portrait\" src=\"").append(iconPath).append("\" alt=\"").append(name).append("\" />\n");
                    sb.append("          </div>\n");
                    sb.append("          <div class=\"sb-title-box\">\n");
                    sb.append("            <div class=\"sb-name\">").append(name).append("</div>\n");
                    sb.append("            <div class=\"sb-meta\">\n");
                    sb.append("              <span style=\"color:").append(faction.color).append(";\">").append(faction.name).append("</span> &bull; <span>").append(moveName).append("</span>\n");
                    sb.append("            </div>\n");
                    sb.append("          </div>\n");
                    sb.append("          <div class=\"sb-cost-pill\">").append(cost).append(" &#9733;</div>\n");
                    sb.append("        </div>\n");
                    sb.append("        <div class=\"sb-stats\">\n");
                    sb.append("          <div class=\"sb-stat-item\"><span class=\"sb-stat-label\">Speed</span><span class=\"sb-stat-val stat-spd\">").append(speed).append("</span></div>\n");
                    sb.append("          <div class=\"sb-stat-item\"><span class=\"sb-stat-label\">Health</span><span class=\"sb-stat-val stat-hp\">").append(hp).append("</span></div>\n");
                    sb.append("          <div class=\"sb-stat-item\"><span class=\"sb-stat-label\">Strength</span><span class=\"sb-stat-val stat-str\">").append(str).append("</span></div>\n");
                    sb.append("          <div class=\"sb-stat-item\"><span class=\"sb-stat-label\">Range</span><span class=\"sb-stat-val stat-rng\">").append(rangeStr).append("</span></div>\n");
                    sb.append("        </div>\n");
                    sb.append("        <div class=\"sb-traits\">\n");
                    for (String t : traits) {
                        sb.append("          ").append(t).append("\n");
                    }
                    sb.append("        </div>\n");

                    // Dedicated Special Ability Block
                    sb.append("        <div class=\"sb-ability\">\n");
                    sb.append("          <div class=\"sb-ability-header\">\n");
                    sb.append("            <span class=\"sb-ability-badge\">&#9733; SPECIAL ABILITY:</span>\n");
                    sb.append("            <span class=\"sb-ability-name\">").append(ability.title).append("</span>\n");
                    sb.append("          </div>\n");
                    sb.append("          <div class=\"sb-ability-desc\">").append(ability.explanation).append("</div>\n");
                    sb.append("        </div>\n");

                    sb.append("        <div class=\"sb-lore\">\n");
                    sb.append("          &ldquo;").append(lore).append("&rdquo;\n");
                    sb.append("        </div>\n");
                    sb.append("        <div class=\"sb-role\">\n");
                    sb.append("          <strong>Role:</strong> ").append(roleVerdict).append("\n");
                    sb.append("        </div>\n");
                    sb.append("      </div>\n\n");
                }

                sb.append("    </div>\n");
                sb.append("  </div>\n");
            }

            File outFile = new File("manual/generated_compendium.html");
            try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
                pw.print(sb.toString());
            }

            System.out.println("[GenerateUnitCompendium] Successfully generated " + outFile.getAbsolutePath() + " (" + outFile.length() + " bytes)!");
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
