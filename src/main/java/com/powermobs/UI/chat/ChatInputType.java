package com.powermobs.UI.chat;

import lombok.Getter;
import org.bukkit.ChatColor;

/**
 * Types of chat input for configuration values
 */
@Getter
public enum ChatInputType {
    SET_ID(
            "Set ID",
            ChatColor.GRAY + "Enter the ID for the new mob." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <value>'" +
                    "\n" + ChatColor.YELLOW + "Duplicate IDs will not be accepted." +
                    "\n" + ChatColor.YELLOW + "Special characters besides '-' will not be accepted." +
                    "\n" + ChatColor.YELLOW + "Spaces will be replaced with '-'."
    ),
    STRING_NAMES(
            "Set Text",
            ChatColor.GRAY + "Enter the custom flavor text." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <text>'" + ChatColor.YELLOW + " to change the text" +
                    "\n" + ChatColor.YELLOW + "• Supports formatting codes for item names, lore, and mob names:" +
                    "\n" + ChatColor.YELLOW + "- Color Codes:" +
                    "\n" + ChatColor.GRAY + "&0 " + ChatColor.BLACK + "Black" +
                    ChatColor.GRAY + "  &1 " + ChatColor.DARK_BLUE + "Dark Blue" +
                    ChatColor.GRAY + "  &2 " + ChatColor.DARK_GREEN + "Dark Green" +
                    ChatColor.GRAY + "  &3 " + ChatColor.DARK_AQUA + "Dark Aqua" +
                    ChatColor.GRAY + "  &4 " + ChatColor.DARK_RED + "Dark Red" +
                    ChatColor.GRAY + "  &5 " + ChatColor.DARK_PURPLE + "Dark Purple" +
                    ChatColor.GRAY + "  &6 " + ChatColor.GOLD + "Gold" +
                    ChatColor.GRAY + "  &7 " + ChatColor.GRAY + "Gray" +
                    ChatColor.GRAY + "  &8 " + ChatColor.DARK_GRAY + "Dark Gray" +
                    ChatColor.GRAY + "  &9 " + ChatColor.BLUE + "Blue" +
                    ChatColor.GRAY + "  &a " + ChatColor.GREEN + "Green" +
                    ChatColor.GRAY + "  &b " + ChatColor.AQUA + "Aqua" +
                    ChatColor.GRAY + "  &c " + ChatColor.RED + "Red" +
                    ChatColor.GRAY + "  &d " + ChatColor.LIGHT_PURPLE + "Light Purple" +
                    ChatColor.GRAY + "  &e " + ChatColor.YELLOW + "Yellow" +
                    ChatColor.GRAY + "  &f " + ChatColor.WHITE + "White" +
                    "\n" + ChatColor.YELLOW + "  - Style Codes:" +
                    "\n" + ChatColor.GRAY + "  &l " + ChatColor.BOLD + "Bold" +
                    ChatColor.GRAY + "  &o " + ChatColor.ITALIC + "Italic" +
                    ChatColor.GRAY + "  &n " + ChatColor.UNDERLINE + "Underline" +
                    ChatColor.GRAY + "  &m " + ChatColor.STRIKETHROUGH + "Strikethrough" +
                    ChatColor.GRAY + "  &k " + ChatColor.MAGIC + "Obfuscated" +
                    ChatColor.GRAY + "  &r " + ChatColor.RESET + "Reset formatting" +
                    "\n" + ChatColor.YELLOW + "Text Limits:" +
                    "\n" + ChatColor.GRAY + "  • Mob names and item display names: " + ChatColor.WHITE + "Max 64 characters (including color codes and will be truncated if longer)" +
                    "\n" + ChatColor.GRAY + "  • Lore lines: " + ChatColor.WHITE + "No strict limit, but long lines may wrap or get cut off" +
                    "\n" + ChatColor.YELLOW + "Tip: Use '&r' to reset styles mid-text if needed."
    ),
    MULTIPLIER(
            "Multiplier Config",
            ChatColor.YELLOW + "Enter the multiplier setting." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <decimal>' or 'set <decimal>-<decimal>" + ChatColor.YELLOW + " to change the settings" +
                    "\n" + ChatColor.YELLOW + "• Single value example: " + ChatColor.WHITE + "0.2" +
                    "\n" + ChatColor.YELLOW + "• Range example: " + ChatColor.WHITE + "0.02-0.35 " + ChatColor.GRAY + "(random between 0.02 and 0.35)"
    ),

    DROPS_CONFIG(
            "Drops Config",
            ChatColor.GRAY + "Enter the drop count settings." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <count range> [weight]'" + ChatColor.YELLOW + " to change the settings" +
                    "\n" + ChatColor.YELLOW + "• Single value example: " + ChatColor.WHITE + "3" +
                    "\n" + ChatColor.YELLOW + "• Range example: " + ChatColor.WHITE + "1-4 " + ChatColor.GRAY + "(random between 1 and 4)" +
                    "\n" + ChatColor.YELLOW + "For weight distribution:" +
                    "\n" + ChatColor.YELLOW + "• Range: " + ChatColor.WHITE + "1-200" +
                    "\n" + ChatColor.YELLOW + "• 1-99: " + ChatColor.WHITE + "Favors lower counts" +
                    "\n" + ChatColor.YELLOW + "• 100: " + ChatColor.WHITE + "Equal chance (default)" +
                    "\n" + ChatColor.YELLOW + "• 101-200: " + ChatColor.WHITE + "Favors higher counts" +
                    "\n" + ChatColor.YELLOW + "Weight only applies when using count ranges."
    ),

    CHANCE(
            "Chance",
            ChatColor.GRAY + "Enter the chance." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <decimal>'" + ChatColor.YELLOW + " to change the settings" +
                    "\n" + ChatColor.YELLOW + "• Range: " + ChatColor.WHITE + "0.0 to 1.0" +
                    "\n" + ChatColor.YELLOW + "• Examples: " + ChatColor.WHITE + "0.5 (50%), 0.1 (10%), 1.0 (100%)"
    ),

    DROP_AMOUNT(
            "Drop Amount",
            ChatColor.GRAY + "Enter the amount of items to drop." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <integer>' or 'set <integer>-<integer>" + ChatColor.YELLOW + " to change the settings" +
                    "\n" + ChatColor.YELLOW + "• Single value example: " + ChatColor.WHITE + "3" +
                    "\n" + ChatColor.YELLOW + "• Range example: " + ChatColor.WHITE + "1-5 " + ChatColor.YELLOW + "(random between 1 and 5)" +
                    "\n" + ChatColor.GRAY + "Use ranges for variety in drops."
    ),

    RANGE(
            "Range",
            ChatColor.GRAY + "Enter the desired range." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <integer>' or 'set <integer>-<integer>" + ChatColor.YELLOW + " to change the settings" +
                    "\n" + ChatColor.YELLOW + "• Single value example: " + ChatColor.WHITE + "3" +
                    "\n" + ChatColor.YELLOW + "• Range example: " + ChatColor.WHITE + "1-5 " + ChatColor.YELLOW + "(random between 1 and 5)" +
                    "\n" + ChatColor.GRAY + "Use ranges for variety."
    ),

    SPAWN_DELAY(
            "Spawn Delay",
            ChatColor.GRAY + "Enter the delay to prevent repetitive spawns." +
                    "\n" + ChatColor.YELLOW + "Values represent seconds until a power mob can have a chance to spawn" +
                    "\n" + ChatColor.YELLOW + "i.e. '500' = 8 min and 20 seconds" +
                    "\n" + ChatColor.YELLOW + "Range example: " + ChatColor.WHITE + "480-1200 " + ChatColor.YELLOW + "(random between 8 and 20 min)"
    ),

    GLOW_TIME(
            "Glow Time",
            ChatColor.GRAY + "Enter the time(in seconds) for the mob glow duration after spawning." +
                    "\n" + ChatColor.YELLOW + "i.e. '500' = 8 min and 20 seconds" +
                    "\n" + ChatColor.YELLOW + "Range example: " + ChatColor.WHITE + "480-1200 " + ChatColor.YELLOW + "(random between 8 and 20 min)"
    ),

    DESPAWN_TIME(
            "Despawn Time",
            ChatColor.GRAY + "Enter the time(in seconds) desired until this power mob will despawn." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <integer>' or 'set <integer>-<integer>" + ChatColor.YELLOW + " to change the settings" +
                    "\n" + ChatColor.YELLOW + "i.e. '500' = 8 min and 20 seconds" +
                    "\n" + ChatColor.YELLOW + "Range example: " + ChatColor.WHITE + "480-1200 " + ChatColor.YELLOW + "(random between 8 and 20 min)"
    ),

    WEIGHT(
            "Weight",
            ChatColor.GRAY + "Enter the weight for weighted distribution." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <integer>'" + ChatColor.YELLOW + " to change the settings" +
                    "\n" + ChatColor.YELLOW + "• Range: " + ChatColor.WHITE + "1-200" +
                    "\n" + ChatColor.YELLOW + "• 1-99: " + ChatColor.WHITE + "Favors lower amounts" +
                    "\n" + ChatColor.YELLOW + "• 100: " + ChatColor.WHITE + "Equal chance (default)" +
                    "\n" + ChatColor.YELLOW + "• 101-200: " + ChatColor.WHITE + "Favors higher amounts" +
                    "\n" + ChatColor.GRAY + "Only applies when using ranges."
    ),
    HEALTH(
            "Health Settings",
            ChatColor.GRAY + "Enter the health range for your mob." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <integer>' or 'set <integer>-<integer>" + ChatColor.YELLOW + " to change the settings" +
                    "\n" + ChatColor.YELLOW + "• Single value example: " + ChatColor.WHITE + "75" +
                    "\n" + ChatColor.YELLOW + "• Range example: " + ChatColor.WHITE + "50-200 " + ChatColor.GRAY + "(random between 50 and 200)" +
                    "\n" + ChatColor.GRAY + "Use ranges for variety in health."
    ),

    ENCHANTMENTS(
            "Enchantments",
            ChatColor.GRAY + "Choose enchantments to apply to this item." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'add <enchantment> <level>'" + ChatColor.YELLOW + " to add an enchantment or replace an existing one." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'remove <enchantment>'" + ChatColor.YELLOW + " to remove an enchantment " +
                    "\n" + ChatColor.YELLOW + "• Single value example: " + ChatColor.WHITE + "3" +
                    "\n" + ChatColor.YELLOW + "• Range example: " + ChatColor.WHITE + "1-4 " + ChatColor.GRAY + "(random between 1 and 4)" +
                    "\n" + ChatColor.YELLOW + "For weight distribution, use " + ChatColor.GREEN + "'add <enchantment> <level> <enchant weight>'." +
                    "\n" + ChatColor.YELLOW + "• Range: " + ChatColor.WHITE + "1-200" +
                    "\n" + ChatColor.YELLOW + "• 1-99: " + ChatColor.WHITE + "Favors lower levels" +
                    "\n" + ChatColor.YELLOW + "• 100: " + ChatColor.WHITE + "Equal chance (default)" +
                    "\n" + ChatColor.YELLOW + "• 101-200: " + ChatColor.WHITE + "Favors higher levels" +
                    "\n" + ChatColor.YELLOW + "Weight only applies when using level ranges."
    ),

    DIMENSIONS(
            "Dimensions",
            ChatColor.GRAY + "Set what dimensions to include or exclude the mob from spawning in." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <T/F> <T/F> <T/F>'" +
                    "\n" + ChatColor.YELLOW + "1 - OVERWORLD " +
                    "\n" + ChatColor.YELLOW + "2 - NETHER " +
                    "\n" + ChatColor.YELLOW + "3 - THE_END " +
                    "\n" + ChatColor.YELLOW + "• Example: 'set true false true"
    ),

    TIMES(
            "Times",
            ChatColor.GRAY + "Set what times to include or exclude the mob from spawning at." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <T/F> <T/F>'" +
                    "\n" + ChatColor.YELLOW + "1 - DAY " +
                    "\n" + ChatColor.YELLOW + "2 - NIGHT " +
                    "\n" + ChatColor.YELLOW + "• Example: 'set true false'"
    ),
    COORDINATES(
            "Coordinates",
            ChatColor.GRAY + "Set the range for the mob to spawn in." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <min> <max>'" + ChatColor.YELLOW + " set the coordinate range" +
                    "\n" + ChatColor.YELLOW + "• Use 'infinity' to allow anything past the other value"
    ),

    BIOME_CONFIG(
            "Biomes",
            ChatColor.GRAY + "Choose what biomes from the group to add or remove." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'add <biome>'" + ChatColor.YELLOW + " to enable a biome" +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'remove <biome>'" + ChatColor.YELLOW + " to disable a biome"
    ),
    ABILITY_STRING(
            "Strings",
            ChatColor.GRAY + "Enter the string value." +
            "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <string>'"),
    ABILITY_INT(
            "Integers",
            ChatColor.GRAY + "Enter an integer value." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <integer>'" +
                    "\n" + ChatColor.YELLOW + "• Example: 'set 2'"),
    ABILITY_DOUBLE(
            "Doubles",
            ChatColor.GRAY + "Enter a double value." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <double>'" +
                    "\n" + ChatColor.YELLOW + "• Example: 'set 2.5'"),
    ABILITY_BOOLEAN("Booleans",
            ChatColor.GRAY + "Enter as true or false." +
                    "\n" + ChatColor.YELLOW + "• Use " + ChatColor.GREEN + "'set <boolean>'" +
                    "\n" + ChatColor.YELLOW + "• Example: 'set t' or 'set false'");

    private final String displayName;
    private final String instructions;

    ChatInputType(String displayName, String instructions) {
        this.displayName = displayName;
        this.instructions = instructions;
    }

}