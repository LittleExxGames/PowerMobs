package com.powermobs.UI.chat;

import com.powermobs.PowerMobsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles chat input for configuration values
 */
public class ChatInputHandler implements Listener {

    private final PowerMobsPlugin plugin;
    private final Map<UUID, ChatInputSession> activeSessions = new HashMap<>();

    public ChatInputHandler(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Starts a new chat input session for a player
     */
    public void startInputSession(Player player, ChatInputType inputType, ChatInputCallback callback) {
        UUID playerId = player.getUniqueId();

        // Cancel any existing session
        activeSessions.remove(playerId);

        ChatInputSession session = new ChatInputSession(inputType, callback);
        activeSessions.put(playerId, session);

        // Send instructions to player
        sendInstructions(player, inputType);
    }

    /**
     * Cancels an active input session
     */
    public void cancelSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    /**
     * Checks if a player has an active session
     */
    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!activeSessions.containsKey(playerId)) {
            return; // No active session
        }

        event.setCancelled(true); // Cancel the chat message

        ChatInputSession session = activeSessions.get(playerId);
        String input = event.getMessage().trim();

        // Check for cancel command
        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("exit")) {
            activeSessions.remove(playerId);
            player.sendMessage(ChatColor.YELLOW + "Input cancelled. Returning to configuration menu.");

            // Return to GUI on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getGuiManager().getPageManager().openCurrentPage(player);
            });
            return;
        }

        // Validate and process input
        ChatInputResult result = validateInput(session.inputType(), input);

        if (result.valid()) {
            activeSessions.remove(playerId);
            player.sendMessage(ChatColor.GREEN + "Value set successfully!");

            // Execute callback on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                session.callback().onInputReceived(result.value(), player);
            });
        } else {
            player.sendMessage(ChatColor.RED + "Invalid input: " + result.errorMessage());
            sendInstructions(player, session.inputType());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeSessions.remove(event.getPlayer().getUniqueId());
    }

    private void sendInstructions(Player player, ChatInputType inputType) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "=== " + inputType.getDisplayName() + " Configuration ===");
        player.sendMessage(ChatColor.GRAY + inputType.getInstructions());
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Type 'cancel' or 'exit' to return to the menu without changes.");
        player.sendMessage("");
    }

    private ChatInputResult validateInput(ChatInputType inputType, String input) {
        try {
            return switch (inputType) {
                case SET_ID -> validateID(input);
                case STRING_NAMES -> validateStringNames(input);
                case DROPS_CONFIG -> validateDropsConfig(input);
                case CHANCE -> validateChance(input);
                case DROP_AMOUNT -> validateIntRange(input, "Amount", 50000);
                case SPAWN_DELAY -> validateIntRange(input, "Delay", 500000);
                case GLOW_TIME -> validateIntRange(input, "Time", 500000);
                case DESPAWN_TIME -> validateIntRange(input, "Time", 500000);
                case HEALTH -> validateIntRange(input, "Health", 999999999);
                case MULTIPLIER -> validateDoubleRange(input, "Multiplier", 0.0001, 10.0);
                case WEIGHT -> validateWeight(input);
                case RANGE -> validateIntRange(input, "Range", 1000);
                case ENCHANTMENTS -> validateEnchantment(input);
                case DIMENSIONS -> validateBooleans(input, 3);
                case TIMES -> validateBooleans(input, 2);
                case COORDINATES -> validateDistance(input);
                case BIOME_CONFIG -> validateBiome(input);
                case ABILITY_STRING -> validateStringNames(input);
                case ABILITY_INT -> validateInt(input, "Config", 1, 1000);
                case ABILITY_DOUBLE -> validateDouble(input, "Value", 0.0, 1000.0);
                case ABILITY_BOOLEAN -> validateBooleans(input, 1);
                default -> new ChatInputResult(false, null, "Unknown input type");
            };
        } catch (Exception e) {
            return new ChatInputResult(false, null, "Invalid format: " + e.getMessage());
        }
    }

    private ChatInputResult validateID(String input) {
        String pattern = "^set\\s+(.+)$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            String text = matcher.group(1).trim(); // Trim any trailing whitespace
            if (text.isEmpty()) {
                return new ChatInputResult(false, null, "Text cannot be empty. Use 'set <text>'");
            }

            // Check for invalid special characters (allow letters, numbers, spaces, and dashes)
            if (!text.matches("^[a-zA-Z0-9\\s-]+$")) {
                return new ChatInputResult(false, null, "Text can only contain letters, numbers, spaces, and dashes. Special characters are not allowed.");
            }

            // Replace spaces with dashes and collapse multiple spaces/dashes
            String processedText = text.replaceAll("\\s+", "-");

            // Collapse multiple consecutive dashes into single dash
            processedText = processedText.replaceAll("-+", "-");

            // Remove any leading or trailing dashes that might result from trimming
            processedText = processedText.replaceAll("^-+|-+$", "");

            // Ensure we still have content after processing
            if (processedText.isEmpty()) {
                return new ChatInputResult(false, null, "Text cannot be empty after processing. Please provide valid content.");
            }

            return new ChatInputResult(true, processedText, null);
        } else {
            return new ChatInputResult(false, null, "Invalid format. Use 'set <text>'");
        }
    }

    private ChatInputResult validateStringNames(String input) {
        String pattern = "^set\\s+(.+)$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            String text = matcher.group(1).trim(); // Trim any trailing whitespace
            if (text.isEmpty()) {
                return new ChatInputResult(false, null, "Text cannot be empty. Use 'set <text>'");
            }
            return new ChatInputResult(true, text, null);
        } else {
            return new ChatInputResult(false, null, "Invalid format. Use 'set <text>'");
        }
    }

    private ChatInputResult validateDropsConfig(String input) {
        String pattern = "^set\\s+(\\d+(?:-\\d+)?)\\s*(\\d+)?$";

        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);

        if (matcher.matches()) {
            String countInput = matcher.group(1); // Extract level (single or range)
            String weightInput = matcher.group(2); // Extract optional weight

            ChatInputResult countResult = validateIntRange(countInput, "Count", 64);
            if (!countResult.valid()) {
                return countResult;
            }
            ChatInputResult weightResult = validateWeight(weightInput);
            if (!weightResult.valid()) {
                return weightResult;
            }
            return new ChatInputResult(true, countInput + ":" + weightResult.value(), null);

        } else {
            return new ChatInputResult(false, null, "Invalid format. Use 'set <drops count> [weight]'");
        }

    }

    private ChatInputResult validateChance(String input) {
        String pattern = "^set\\s+(0(?:\\.\\d+)?|1(?:\\.0+)?)$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            try {
                double chance = Double.parseDouble(matcher.group(1));
                if (chance < 0.0 || chance > 1.0) {
                    return new ChatInputResult(false, null, "Chance must be between 0.0 and 1.0");
                }
                return new ChatInputResult(true, chance, null);
            } catch (NumberFormatException e) {
                return new ChatInputResult(false, null, "Must be a decimal number (e.g., 0.55)");
            }
        } else {
            return new ChatInputResult(false, null, "Incorrect format: Please use 'set <decimal>'");
        }
    }

    private ChatInputResult validateDoubleRange(String input, String type, double minLimit, double maxLimit) {
        String pattern = "^set\\s+(\\d+(?:\\.\\d+)?)(?:-(\\d+(?:\\.\\d+)?))?$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            if (input.contains("-")) {
                double min = Double.parseDouble(matcher.group(1));
                double max = Double.parseDouble(matcher.group(2));

                if (min < minLimit || max < minLimit) {
                    return new ChatInputResult(false, null, type + " values must be at least " + minLimit);
                }
                if (min > max) {
                    return new ChatInputResult(false, null, "Minimum cannot be greater than maximum");
                }
                if (max > maxLimit) {
                    return new ChatInputResult(false, null, "Maximum cannot exceed " + maxLimit);
                }
                return new ChatInputResult(true, min + "-" + max, null);
            } else {
                // Single value
                double amount = Double.parseDouble(matcher.group(1));
                if (amount < minLimit) {
                    return new ChatInputResult(false, null, type + " must be at least " + minLimit);
                }
                if (amount > maxLimit) {
                    return new ChatInputResult(false, null, type + " cannot exceed " + maxLimit);
                }
                return new ChatInputResult(true, amount, null);
            }
        } else {
            return new ChatInputResult(false, null, "Incorrect format: Please use 'set <decimal>' or 'set <decimal>-<decimal>'");
        }
    }

    private ChatInputResult validateDouble(String input, String type, double minLimit, double maxLimit) {
        String pattern = "^set\\s+(-?\\d+(?:\\.\\d+)?)$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input.trim());

        if (!matcher.matches()) {
            return new ChatInputResult(false, null, "Incorrect format: Please use 'set <decimal>'");
        }

        double amount = Double.parseDouble(matcher.group(1));
        if (amount < minLimit) {
            return new ChatInputResult(false, null, type + " must be at least " + minLimit);
        }
        if (amount > maxLimit) {
            return new ChatInputResult(false, null, type + " cannot exceed " + maxLimit);
        }
        return new ChatInputResult(true, amount, null);
    }


    private ChatInputResult validateIntRange(String input, String type, int maxLimit) {
        String pattern = "^set\\s+(\\d+)(?:-(\\d+))?$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        // Check if it's a range (e.g., "1-3")
        if (matcher.matches()) {
            if (input.contains("-")) {
                int min = Integer.parseInt(matcher.group(1));
                int max = Integer.parseInt(matcher.group(2));

                if (min < 0 || max < 0) {
                    return new ChatInputResult(false, null, type + " values must be at least 0");
                }
                if (min > max) {
                    return new ChatInputResult(false, null, "Minimum cannot be greater than maximum");
                }
                if (max > maxLimit) {
                    return new ChatInputResult(false, null, "Maximum cannot exceed " + maxLimit);
                }

                return new ChatInputResult(true, min + "-" + max, null);
            } else {
                int amount = Integer.parseInt(matcher.group(1));
                if (amount < 0) {
                    return new ChatInputResult(false, null, type + " must be at least 0");
                }
                if (amount > maxLimit) {
                    return new ChatInputResult(false, null, type + " cannot exceed " + maxLimit);
                }
                return new ChatInputResult(true, amount, null);
            }
        } else {
            return new ChatInputResult(false, null, "Incorrect format: Please use 'set <integer>' or 'set <integer>-<integer>'");
        }
    }

    private ChatInputResult validateInt(String input, String type, int minLimit, int maxLimit) {
        // Accepts: "set 1" (whole numbers only)
        String pattern = "^set\\s+(-?\\d+)$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input.trim());

        if (!matcher.matches()) {
            return new ChatInputResult(false, null, "Incorrect format: Please use 'set <integer>'");
        }

        int val = Integer.parseInt(matcher.group(1));
        if (val < minLimit) {
            return new ChatInputResult(false, null, type + " value must be at least " + minLimit);
        }
        if (val > maxLimit) {
            return new ChatInputResult(false, null, type + " value cannot exceed " + maxLimit);
        }
        return new ChatInputResult(true, val, null);
    }

    private ChatInputResult validateWeight(String input) {
        String pattern = "^set\\s+(\\d+)$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            if (matcher.group(1) == null) {
                return new ChatInputResult(true, 100, null);
            }
            int weight = Integer.parseInt(matcher.group(1));
            if (weight < 1 || weight > 200) {
                return new ChatInputResult(false, null, " Weight must be an integer between 1 and 200");
            }
            return new ChatInputResult(true, weight, null);
        } else {
            return new ChatInputResult(false, null, "Incorrect format: Weight must be an integer between 1 and 200");
        }
    }

    private ChatInputResult validateEnchantmentType(String input) {
        if (Registry.ENCHANTMENT.get(NamespacedKey.minecraft(input.toLowerCase())) != null) {
            return new ChatInputResult(true, input.toUpperCase(), null);
        }
        return new ChatInputResult(false, null, "Invalid enchantment type.");
    }

    private ChatInputResult validateEnchantmentLevel(String input) {
        // Check if it's a range (e.g., "1-4")
        String pattern = "^(\\d+)(?:-(\\d+))?$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            if (input.contains("-")) {
                int min = Integer.parseInt(matcher.group(1));
                int max = Integer.parseInt(matcher.group(2));

                if (min < 1 || max < 1) {
                    return new ChatInputResult(false, null, "Enchantment levels must be at least 1");
                }
                if (min > max) {
                    return new ChatInputResult(false, null, "Minimum level cannot be greater than maximum");
                }
                if (max > 255) {
                    return new ChatInputResult(false, null, "Maximum enchantment level cannot exceed 255");
                }

                return new ChatInputResult(true, min + "-" + max, null);
            } else {
                int level = Integer.parseInt(matcher.group(1));
                if (level < 1) {
                    return new ChatInputResult(false, null, "Enchantment level must be at least 1");
                }
                if (level > 255) {
                    return new ChatInputResult(false, null, "Enchantment level cannot exceed 255");
                }
                return new ChatInputResult(true, level, null);
            }
        } else {
            return new ChatInputResult(false, null, "Incorrect format: Please use 'add <enchantment> <integer>' or 'add <enchantment> <integer>-<integer> [weight]'");
        }

    }

    public ChatInputResult validateEnchantment(String input) {
        String addPattern = "^add\\s+(\\w+)\\s+(\\d+(?:-\\d+)?)\\s*(\\d+)?$";

        String removePattern = "^remove\\s+(\\w+)$";

        Pattern addRegex = Pattern.compile(addPattern);
        Pattern removeRegex = Pattern.compile(removePattern);

        Matcher addMatcher = addRegex.matcher(input);
        Matcher removeMatcher = removeRegex.matcher(input);

        if (addMatcher.matches()) {
            String enchant = addMatcher.group(1).toUpperCase(); // Extract enchant name
            String levelInput = addMatcher.group(2); // Extract level (single or range)
            String weightInput = addMatcher.group(3); // Extract optional weight
            ChatInputResult enchantResult = validateEnchantmentType(enchant);
            if (!enchantResult.valid()) {
                return enchantResult;
            }
            ChatInputResult levelResult = validateEnchantmentLevel(levelInput);
            if (!levelResult.valid()) {
                return levelResult;
            }
            int weightValue = 100; // default
            if (weightInput != null && !weightInput.isBlank()) {
                ChatInputResult weightResult = validateWeight("set " + weightInput);
                if (!weightResult.valid()) {
                    return weightResult;
                }
                weightValue = (Integer) weightResult.value();
            }

            return new ChatInputResult(true, "add:" + enchant + ":" + levelResult.value() + ":" + weightValue, null);
        } else if (removeMatcher.matches()) {
            String enchant = removeMatcher.group(1).toUpperCase(); // Extract enchant name

            ChatInputResult enchantResult = validateEnchantmentType(enchant);
            if (!enchantResult.valid()) {
                return enchantResult;
            }
            return new ChatInputResult(true, "remove:" + enchant, null);
        } else {
            return new ChatInputResult(false, null, "Invalid format. Use 'add <enchant> <level> [weight]' or 'remove <enchant>'");
        }
    }

    private ChatInputResult validateDistance(String input) {
        String pattern = "^set\\s+(-?\\d+|infinity)\\s+(-?\\d+|infinity)$";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            if (matcher.group(1).equalsIgnoreCase("infinity") || matcher.group(2).equalsIgnoreCase("infinity")) {
                return new ChatInputResult(true, matcher.group(1) + ":" + matcher.group(2), null);
            } else if (Integer.parseInt(matcher.group(1)) > Integer.parseInt(matcher.group(2))) {
                return new ChatInputResult(false, null, "Min distance cannot be greater than max distance");
            }
            return new ChatInputResult(true, matcher.group(1) + ":" + matcher.group(2), null);
        } else {
            return new ChatInputResult(false, null, "Invalid format. Use 'set <min> <max>'");
        }
    }

    private ChatInputResult validateBooleans(String input, int count) {
        String pattern = "^set" + "\\s+(true|false|t|f)".repeat(Math.max(0, count)) + "$";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(input);
        StringBuilder command = new StringBuilder("set ");
        command.append("<T/F> ".repeat(Math.max(0, count)));

        plugin.debug("Sought booleans: " + count, "ui");
        plugin.debug("Found booleans: " + matcher.groupCount(), "ui");
        if ((matcher.groupCount()) != count) {
            return new ChatInputResult(false, null, "Invalid format. Use '" + command + "'");
        } else if (matcher.matches()) {
            StringBuilder set = new StringBuilder();
            for (int i = 1; i <= count; i++) {
                set.append(matcher.group(i));
                if (i < count) {
                    set.append(":");
                }
            }
            return new ChatInputResult(true, set.toString(), null);
        } else {
            return new ChatInputResult(false, null, "Invalid format. Use '" + command + "'");
        }

    }

    private ChatInputResult validateBiome(String input) {
        if (Registry.BIOME.get(NamespacedKey.minecraft(input.toLowerCase())) != null) {
            return new ChatInputResult(true, input, null);
        }
        return new ChatInputResult(false, null, "Invalid biome type.");
    }

    /**
     * Represents an active chat input session
     */
    private record ChatInputSession(ChatInputType inputType, ChatInputCallback callback) {
    }

    /**
     * Result of input validation
     */
    public record ChatInputResult(boolean valid, Object value, String errorMessage) {
    }
}
