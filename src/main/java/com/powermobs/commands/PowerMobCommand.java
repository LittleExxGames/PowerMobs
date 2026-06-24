package com.powermobs.commands;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.config.PowerMobConfig;
import com.powermobs.config.SpawnBlockerManager;
import com.powermobs.mobs.PowerMob;
import com.powermobs.mobs.SpawnContext;
import com.powermobs.stats.CachedStats;
import com.powermobs.stats.StatsManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command handler for the powermob command
 */
@RequiredArgsConstructor
public class PowerMobCommand implements CommandExecutor, TabCompleter {

    private final PowerMobsPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn":
                return handleSpawn(sender, args);
            case "list":
                return handleList(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "config":
                return handleConfig(sender, args);
            case "timers":
                return handleTimers(sender, args);
            case "give":
                return handleGive(sender, args);
            case "spawnblocker":
                return handleSpawnBlockerCommand(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "help":
                sendHelp(sender);
                return true;
            case "update":
                //TODO: seperate updates for mobs, items, spawn keys, and spawn blockers
                return updateConfig(sender);
            case "populate":
                return handlePopulate(sender, args);
            case "stats":
                return handleStats(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                sendHelp(sender);
                return true;
        }
    }

    private boolean updateConfig(CommandSender sender) {
        if (!sender.hasPermission("powermobs.update")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Updating custom items config...");

        boolean saved = this.plugin.getEquipmentManager().saveEquipment();
        if (!saved) {
            sender.sendMessage(ChatColor.RED + "Failed to update custom items.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Custom items updated successfully.");
        sender.sendMessage(ChatColor.GRAY + "Weapons: " + this.plugin.getEquipmentManager().getWeapons().size()
                + ", Armor: " + this.plugin.getEquipmentManager().getArmor().size()
                + ", Uniques: " + this.plugin.getEquipmentManager().getUniques().size()
                + ", Spawn blockers: " + this.plugin.getEquipmentManager().getSpawnBlockerItems().size()
                + ", Spawn keys: " + this.plugin.getEquipmentManager().getSpawnKeyItems().size());

        return true;
    }

    private boolean handlePopulate(CommandSender sender, String[] args){
        if (!sender.hasPermission("powermobs.populate")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob populate <abilities|items|mobs|keys|blockers>");
            return true;
        }
        String arg = args[1].toLowerCase();
        switch (arg)
        {
            case "abilities":
                sender.sendMessage(ChatColor.YELLOW + "Adding missing ability settings and defaults to config...");
                this.plugin.getConfigManager().getAbilitiesConfigManager().appendMissingDefaults(2);
                sender.sendMessage(ChatColor.YELLOW + "Updated ability settings to include missing configs and defaults!");
                return true;
            case "items":
                sender.sendMessage(ChatColor.YELLOW + "Adding missing item settings and defaults to config...");
                this.plugin.getConfigManager().getItemsConfigManager().appendMissingDefaults(3);
                sender.sendMessage(ChatColor.YELLOW + "Updated item settings to include missing configs and defaults!");
                return true;
            case "mobs":
                sender.sendMessage(ChatColor.YELLOW + "Adding missing Power Mob settings and defaults to config...");
                this.plugin.getConfigManager().getMobsConfigManager().appendMissingDefaults(2);
                sender.sendMessage(ChatColor.YELLOW + "Updated Power Mob settings to include missing configs and defaults!");
                return true;
            case "keys":
                sender.sendMessage(ChatColor.YELLOW + "Adding missing spawn key settings and defaults to config...");
                this.plugin.getSpawnKeyManager().getSpawnKeyConfig().appendMissingDefaults(2);
                sender.sendMessage(ChatColor.YELLOW + "Updated spawn key settings to include missing configs and defaults!");
                return true;
            case "blockers":
                sender.sendMessage(ChatColor.YELLOW + "Adding missing spawn blocker settings and defaults to config...");
                this.plugin.getSpawnBlockerManager().getSpawnBlockersConfig().appendMissingDefaults(2);
                sender.sendMessage(ChatColor.YELLOW + "Updated spawn blocker settings to include missing configs and defaults!");
                return true;
            case "main":
                sender.sendMessage(ChatColor.YELLOW + "Adding missing main settings and defaults to config...");
                this.plugin.getConfigManager().appendMissingDefaults(2);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Handles the spawn subcommand
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return True if the command was handled
     */
    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.spawn")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob spawn <mob-id> [count]");
            return true;
        }

        String mobId = args[1];
        int count = 1;

        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
                if (count < 1 || count > 50) {
                    sender.sendMessage(ChatColor.RED + "Count must be between 1 and 50.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid count: " + args[2]);
                return true;
            }
        }

        // Check if the mob ID is valid
        PowerMobConfig config = this.plugin.getConfigManager().getPowerMob(mobId);
        if (config == null && !mobId.equals("random")) {
            sender.sendMessage(ChatColor.RED + "Invalid mob ID: " + mobId);
            return true;
        }

        // Get the entity type
        EntityType entityType = mobId.equals("random") ?
                EntityType.ZOMBIE : // Default for random mobs
                config.getEntityType();

        // Get the player
        Location location = player.getTargetBlock(null, 100).getLocation().add(0.5, 1, 0.5);

        // Spawn the mobs
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            try {
                // Spawn the entity
                LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);

                // Create the power mob
                PowerMob powerMob = this.plugin.getPowerMobManager().createAndRegisterPowerMob(entity, mobId);
                if (powerMob != null) {
                    spawned++;
                }
            } catch (Exception e) {
                this.plugin.getLogger().severe("Error spawning power mob: " + e.getMessage());
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Spawned " + spawned + " power mobs.");
        return true;
    }

    /**
     * Handles the list subcommand
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return True if the command was handled
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // List available mob types
        sender.sendMessage(ChatColor.GREEN + "Available power mob types:");
        for (String id : this.plugin.getConfigManager().getPowerMobs().keySet()) {
            PowerMobConfig config = this.plugin.getConfigManager().getPowerMob(id);
            sender.sendMessage(ChatColor.GOLD + "- " + id + ChatColor.GRAY + " (" +
                    config.getEntityType() + ", Health: " + config.getMinHealth() + " - " + config.getMaxHealth() + ")");
        }

        // List active power mobs
        sender.sendMessage(ChatColor.GREEN + "Active power mobs:");
        List<PowerMob> activeMobs = this.plugin.getPowerMobManager().getPowerMobs().values().stream()
                .filter(PowerMob::isValid)
                .toList();
        if (activeMobs.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "There are no active power mobs.");
            return true;
        }
        for (PowerMob mob : activeMobs) {
            LivingEntity entity = mob.getEntity();
            Location loc = entity.getLocation();
            long remainingDespawnMs = this.plugin.getSpawnTimerManager().getRemainingDespawnTime(mob);
            String despawnInfo;
            if (remainingDespawnMs < 0) {
                despawnInfo = "no timer";
            } else {
                long remainingSeconds = remainingDespawnMs / 1000;
                despawnInfo = remainingSeconds + "s left";
            }

            sender.sendMessage(
                    ChatColor.GOLD + "- " + mob.getId() +
                            ChatColor.GRAY + " (" + ChatColor.DARK_GREEN + "World: " + ChatColor.GRAY + loc.getWorld().getName() +
                            " - " + ChatColor.DARK_GREEN + "Type: " + ChatColor.GRAY + entity.getType() +
                            " - " + ChatColor.DARK_GREEN + "Position: " + ChatColor.GRAY + loc.getBlockX() + "x, " + loc.getBlockY() + "y, " + loc.getBlockZ() + "z" +
                            " - " + ChatColor.DARK_GREEN + "Health: " + ChatColor.GRAY + String.format("%.2f", entity.getHealth()) +
                            ChatColor.GRAY + " - " + ChatColor.DARK_GREEN + "Despawn: " + ChatColor.GRAY + despawnInfo +
                            ChatColor.GRAY + ")"
            );
        }

        return true;
    }

    /**
     * Handles the info subcommand
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return True if the command was handled
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.info")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob info <mob-id>");
            return true;
        }

        String mobId = args[1];

        // Check if the mob ID is valid
        PowerMobConfig config = this.plugin.getConfigManager().getPowerMob(mobId);
        if (config == null) {
            sender.sendMessage(ChatColor.RED + "Invalid mob ID: " + mobId);
            return true;
        }

        // Display mob info
        sender.sendMessage(ChatColor.GREEN + "Information for mob " + mobId + ":");
        sender.sendMessage(ChatColor.GOLD + "Type: " + ChatColor.WHITE + config.getEntityType());
        sender.sendMessage(ChatColor.GOLD + "Name: " + ChatColor.translateAlternateColorCodes('&', config.getName()));
        sender.sendMessage(ChatColor.GOLD + "Health: " + ChatColor.WHITE + config.getMinHealth() + "-" + config.getMaxHealth());
        sender.sendMessage(ChatColor.GOLD + "Damage Multiplier: " + ChatColor.WHITE + config.getMinDamageMultiplier() + "-" + config.getMaxDamageMultiplier());
        sender.sendMessage(ChatColor.GOLD + "Damage Weight: " + ChatColor.WHITE + config.getDamageWeight());
        sender.sendMessage(ChatColor.GOLD + "Speed Multiplier: " + ChatColor.WHITE + config.getMinSpeedMultiplier() + "-" + config.getMaxSpeedMultiplier());
        sender.sendMessage(ChatColor.GOLD + "Speed Weight: " + ChatColor.WHITE + config.getSpeedWeight());
        // List abilities
        String abilities = config.getPossibleAbilities().keySet().stream().map(a -> ChatColor.WHITE + a).collect(Collectors.joining(", "));
        sender.sendMessage(ChatColor.GOLD + "Abilities: " + ChatColor.WHITE + abilities);
        // List equipment
        for (String type : config.getPossibleEquipment().keySet()) {
            if (!config.getPossibleEquipment().get(type).isEmpty()) {
                sender.sendMessage(ChatColor.GOLD + type + ": " + ChatColor.WHITE + config.getPossibleEquipment().get(type).get(0).getItem());
            }
        }

        // List active instances
        List<PowerMob> activeMobs = this.plugin.getPowerMobManager().getPowerMobs().values().stream()
                .filter(mob -> mob.isValid() && mob.getId().equals(mobId))
                .collect(Collectors.toList());

        sender.sendMessage(ChatColor.GOLD + "Active instances: " + ChatColor.WHITE + activeMobs.size());

        return true;
    }

    /**
     * Handles the reload subcommand
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return True if the command was handled
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Reloading PowerMobs configuration...");

        try {
            this.plugin.getConfigManager().reloadConfig();
            this.plugin.getAbilityManager().loadAbilities();
            this.plugin.getSpawnBlockerManager().reloadBlockers();
            this.plugin.getSpawnKeyManager().reloadKeys();
            this.plugin.getEquipmentManager().reloadEquipment();
            this.plugin.getItemEffectManager().reloadEffects();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error reloading configuration: " + e.getMessage());
            this.plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Handles the remove subcommand
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return True if the command was handled
     */
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.remove")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob remove <all|radius|type> [radius|type]");
            return true;
        }

        String mode = args[1].toLowerCase();

        switch (mode) {
            case "all":
                // Remove all power mobs
                int removed = 0;
                for (PowerMob mob : new ArrayList<>(this.plugin.getPowerMobManager().getPowerMobs().values())) {
                    if (mob.isValid()) {
                        mob.getEntity().remove();
                        mob.remove();
                        this.plugin.getPowerMobManager().unregisterPowerMob(mob);
                        removed++;
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Removed " + removed + " power mobs.");
                return true;

            case "radius":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /powermob remove radius <radius>");
                    return true;
                }

                int radius;
                try {
                    radius = Integer.parseInt(args[2]);
                    if (radius < 1 || radius > 200) {
                        sender.sendMessage(ChatColor.RED + "Radius must be between 1 and 200.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid radius: " + args[2]);
                    return true;
                }

                Location location = player.getLocation();

                int removedInRadius = 0;
                for (Entity entity : player.getWorld().getEntities()) {
                    if (entity instanceof LivingEntity && entity.getLocation().distance(location) <= radius) {
                        PowerMob mob = PowerMob.getFromEntity(this.plugin, (LivingEntity) entity);
                        if (mob != null && mob.isValid()) {
                            mob.getEntity().remove();
                            mob.remove();
                            this.plugin.getPowerMobManager().unregisterPowerMob(mob);
                            removedInRadius++;
                        }
                    }
                }

                sender.sendMessage(ChatColor.GREEN + "Removed " + removedInRadius + " power mobs within " + radius + " blocks.");
                return true;

            case "type":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /powermob remove type <mob-id>");
                    return true;
                }

                String typeId = args[2];
                int removedOfType = 0;

                for (PowerMob mob : new ArrayList<>(this.plugin.getPowerMobManager().getPowerMobs().values())) {
                    if (mob.isValid() && mob.getId().equals(typeId)) {
                        mob.getEntity().remove();
                        mob.remove();
                        this.plugin.getPowerMobManager().unregisterPowerMob(mob);
                        removedOfType++;
                    }
                }

                sender.sendMessage(ChatColor.GREEN + "Removed " + removedOfType + " " + typeId + " mobs.");
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown mode: " + mode);
                sender.sendMessage(ChatColor.RED + "Usage: /powermob remove <all|radius|type> [radius|type]");
                return true;
        }
    }


    /**
     * Handles the command to open the inventory GUI config.
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return True if the command was handled
     */
    private boolean handleConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.config")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (plugin.getGuiManager().getCurrentPlayer() != null) {
            sender.sendMessage(ChatColor.RED + "A player is already using the configuration GUI.");
            return true;
        }
        this.plugin.getGuiManager().setCurrentPlayer(new PlayerSessionData(player.getUniqueId()));
        this.plugin.getGuiManager().openMainMenu(player);
        return true;
    }

    /**
     * Handles the timers subcommand with scope-based structure
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return True if the command was handled
     */
    private boolean handleTimers(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.timers")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (!this.plugin.getConfigManager().isSpawnTimersEnabled()) {
            sender.sendMessage(ChatColor.YELLOW + "Spawn timers are currently disabled in the configuration.");
            return true;
        }

        if (args.length < 2) {
            // Show summary when no subcommand is provided
            sender.sendMessage(ChatColor.GREEN + "=== Spawn Timer Summary ===");
            sender.sendMessage(this.plugin.getSpawnTimerManager().getDebugInfo());
            sender.sendMessage(ChatColor.GRAY + "Usage:");
            sender.sendMessage(ChatColor.GRAY + "  /powermob timers view <scope> [mobId] [world] [x y z]");
            sender.sendMessage(ChatColor.GRAY + "  /powermob timers reset <scope> [mobId|all] [world] [x y z]");
            sender.sendMessage(ChatColor.GRAY + "Scopes: global, location");
            return true;
        }

        String subcommand = args[1].toLowerCase();

        switch (subcommand) {
            case "view":
                return handleTimersView(sender, args);
            case "reset":
                return handleTimersReset(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown timers subcommand: " + subcommand);
                sender.sendMessage(ChatColor.GRAY + "Available: view, reset");
                return true;
        }
    }

    /**
     * Handles the timers view subcommand
     */
    private boolean handleTimersView(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob timers view <scope> [mobId] [world] [x y z]");
            sender.sendMessage(ChatColor.GRAY + "Scopes: global, location");
            return true;
        }

        String scope = args[2].toLowerCase();

        switch (scope) {
            case "global":
                return handleGlobalTimersView(sender, args);
            case "location":
                return handleLocationTimersView(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Invalid scope: " + scope);
                sender.sendMessage(ChatColor.GRAY + "Available scopes: global, location");
                return true;
        }
    }

    /**
     * Handles the timers reset subcommand
     */
    private boolean handleTimersReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob timers reset <scope> [mobId|all] [world] [x y z]");
            sender.sendMessage(ChatColor.GRAY + "Scopes: global, location");
            return true;
        }

        String scope = args[2].toLowerCase();

        switch (scope) {
            case "global":
                return handleGlobalTimersReset(sender, args);
            case "location":
                return handleLocationTimersReset(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Invalid scope: " + scope);
                sender.sendMessage(ChatColor.GRAY + "Available scopes: global, location");
                return true;
        }
    }

    /**
     * Handles viewing global timers
     */
    private boolean handleGlobalTimersView(CommandSender sender, String[] args) {
        if (this.plugin.getConfigManager().isSpawnLocationBased()) {
            sender.sendMessage(ChatColor.YELLOW + "Global timers are disabled - location-based timers are active.");
            return true;
        }

        String filterMobId = null;
        if (args.length >= 4) {
            filterMobId = args[3];
            if (!filterMobId.equals("random") && this.plugin.getConfigManager().getPowerMob(filterMobId) == null) {
                sender.sendMessage(ChatColor.RED + "Invalid mob ID: " + filterMobId);
                return true;
            }
        }

        sender.sendMessage(ChatColor.GREEN + "=== Global Spawn Timers ===");
        if (filterMobId != null) {
            sender.sendMessage(ChatColor.GRAY + "Filtered by mob type: " + ChatColor.WHITE + filterMobId);
        }

        Map<String, SpawnContext> globalTimers = this.plugin.getSpawnTimerManager().getGlobalTimers();

        if (globalTimers.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No active global timers.");
            return true;
        }

        long currentTime = System.currentTimeMillis();
        int displayed = 0;

        for (Map.Entry<String, SpawnContext> entry : globalTimers.entrySet()) {
            String mobId = entry.getKey();
            SpawnContext context = entry.getValue();

            // Apply filter if specified
            if (filterMobId != null && !mobId.equals(filterMobId)) {
                continue;
            }

            long remaining = this.plugin.getSpawnTimerManager().getRemainingCooldown(mobId, null);
            long remainingSeconds = remaining / 1000;

            if (remaining > 0) {
                sender.sendMessage(ChatColor.GOLD + "• " + mobId + ChatColor.GRAY + " - " +
                        ChatColor.RED + remainingSeconds + "s remaining" +
                        ChatColor.GRAY + " (spawned " + formatTime(currentTime - context.getSpawnTime()) + " ago)");
            } else {
                sender.sendMessage(ChatColor.GOLD + "• " + mobId + ChatColor.GRAY + " - " +
                        ChatColor.GREEN + "Ready to spawn" +
                        ChatColor.GRAY + " (spawned " + formatTime(currentTime - context.getSpawnTime()) + " ago)");
            }
            displayed++;
        }

        if (displayed == 0 && filterMobId != null) {
            sender.sendMessage(ChatColor.GRAY + "No global timers found for mob type: " + filterMobId);
        }

        return true;
    }

    /**
     * Handles viewing location timers
     */
    private boolean handleLocationTimersView(CommandSender sender, String[] args) {
        if (!this.plugin.getConfigManager().isSpawnLocationBased()) {
            sender.sendMessage(ChatColor.YELLOW + "Location-based timers are disabled - global timers are active.");
            return true;
        }

        String filterMobId = null;
        String worldName = null;
        int x = Integer.MIN_VALUE, y = Integer.MIN_VALUE, z = Integer.MIN_VALUE;

        // Parse arguments: [mobId] [world] [x y z]
        if (args.length >= 4) {
            filterMobId = args[3];
            if (!filterMobId.equals("random") && this.plugin.getConfigManager().getPowerMob(filterMobId) == null) {
                sender.sendMessage(ChatColor.RED + "Invalid mob ID: " + filterMobId);
                return true;
            }
        }

        if (args.length >= 5) {
            worldName = args[4];
        }

        if (args.length >= 8) {
            try {
                x = Integer.parseInt(args[5]);
                y = Integer.parseInt(args[6]);
                z = Integer.parseInt(args[7]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid coordinates. Use integers for x, y, z.");
                return true;
            }
        }

        sender.sendMessage(ChatColor.GREEN + "=== Location-Based Spawn Timers ===");
        if (filterMobId != null) {
            sender.sendMessage(ChatColor.GRAY + "Filtered by mob type: " + ChatColor.WHITE + filterMobId);
        }
        if (worldName != null) {
            sender.sendMessage(ChatColor.GRAY + "Filtered by world: " + ChatColor.WHITE + worldName);
        }
        if (x != Integer.MIN_VALUE) {
            sender.sendMessage(ChatColor.GRAY + "Filtered by location: " + ChatColor.WHITE + worldName + " (" + x + ", " + y + ", " + z + ")");
        }
        sender.sendMessage(ChatColor.GRAY + "Distance check: " + this.plugin.getConfigManager().getLocationDistance() + " blocks");

        Map<String, SpawnContext> locationTimers = this.plugin.getSpawnTimerManager().getLocationTimers();

        if (locationTimers.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No active location timers.");
            return true;
        }

        long currentTime = System.currentTimeMillis();
        int displayed = 0;

        for (Map.Entry<String, SpawnContext> entry : locationTimers.entrySet()) {
            String locationKey = entry.getKey();
            SpawnContext context = entry.getValue();
            String mobId = context.getMobConfigId();

            // Apply mob filter if specified
            if (filterMobId != null && !mobId.equals(filterMobId)) {
                continue;
            }

            Location loc = context.getLocation();

            // Apply world filter if specified
            if (worldName != null && !loc.getWorld().getName().equals(worldName)) {
                continue;
            }

            // Apply location filter if specified
            if (x != Integer.MIN_VALUE) {
                if (loc.getBlockX() != x || loc.getBlockY() != y || loc.getBlockZ() != z) {
                    continue;
                }
            }

            long remaining = this.plugin.getSpawnTimerManager().getRemainingCooldown(mobId, loc);
            long remainingSeconds = remaining / 1000;

            String locationString = String.format("%s (%d, %d, %d)",
                    loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            if (remaining > 0) {
                sender.sendMessage(ChatColor.GOLD + "• " + mobId + ChatColor.GRAY + " at " + locationString);
                sender.sendMessage(ChatColor.GRAY + "  └ " + ChatColor.RED + remainingSeconds + "s remaining" +
                        ChatColor.GRAY + " (spawned " + formatTime(currentTime - context.getSpawnTime()) + " ago)");
            } else {
                sender.sendMessage(ChatColor.GOLD + "• " + mobId + ChatColor.GRAY + " at " + locationString);
                sender.sendMessage(ChatColor.GRAY + "  └ " + ChatColor.GREEN + "Ready to spawn" +
                        ChatColor.GRAY + " (spawned " + formatTime(currentTime - context.getSpawnTime()) + " ago)");
            }

            displayed++;
        }

        if (displayed == 0) {
            if (filterMobId != null || worldName != null || x != Integer.MIN_VALUE) {
                sender.sendMessage(ChatColor.GRAY + "No location timers found matching the specified filters.");
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "Showing " + displayed + " location timer(s).");
        }

        return true;
    }

    /**
     * Handles resetting global timers
     */
    private boolean handleGlobalTimersReset(CommandSender sender, String[] args) {
        if (this.plugin.getConfigManager().isSpawnLocationBased()) {
            sender.sendMessage(ChatColor.YELLOW + "Global timers are disabled - location-based timers are active.");
            return true;
        }

        String target = "all";
        if (args.length >= 4) {
            target = args[3];
        }

        if (target.equals("all")) {
            this.plugin.getSpawnTimerManager().getGlobalTimers().clear();
            sender.sendMessage(ChatColor.GREEN + "All global timers have been reset.");
        } else {
            // Validate mob ID
            if (!target.equals("random") && this.plugin.getConfigManager().getPowerMob(target) == null) {
                sender.sendMessage(ChatColor.RED + "Invalid mob ID: " + target);
                return true;
            }

            if (this.plugin.getSpawnTimerManager().getGlobalTimers().remove(target) != null) {
                sender.sendMessage(ChatColor.GREEN + "Global timer for " + target + " has been reset.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No global timer found for " + target + ".");
            }
        }

        return true;
    }

    /**
     * Handles resetting location timers
     */
    private boolean handleLocationTimersReset(CommandSender sender, String[] args) {
        if (!this.plugin.getConfigManager().isSpawnLocationBased()) {
            sender.sendMessage(ChatColor.YELLOW + "Location-based timers are disabled - global timers are active.");
            return true;
        }

        String target = "all";
        String worldName = null;
        int x = Integer.MIN_VALUE, y = Integer.MIN_VALUE, z = Integer.MIN_VALUE;

        // Parse arguments: [mobId|all] [world] [x y z]
        if (args.length >= 4) {
            target = args[3];
        }

        if (args.length >= 5) {
            worldName = args[4];
        }

        if (args.length >= 8) {
            try {
                x = Integer.parseInt(args[5]);
                y = Integer.parseInt(args[6]);
                z = Integer.parseInt(args[7]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid coordinates. Use integers for x, y, z.");
                return true;
            }
        }

        if (target.equals("all")) {
            // Reset all location timers (optionally filtered by location)
            Map<String, SpawnContext> locationTimers = this.plugin.getSpawnTimerManager().getLocationTimers();
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, SpawnContext> entry : locationTimers.entrySet()) {
                Location loc = entry.getValue().getLocation();
                boolean matches = worldName == null || loc.getWorld().getName().equals(worldName);

                if (x != Integer.MIN_VALUE) {
                    if (loc.getBlockX() != x || loc.getBlockY() != y || loc.getBlockZ() != z) {
                        matches = false;
                    }
                }

                if (matches) {
                    toRemove.add(entry.getKey());
                }
            }

            for (String key : toRemove) {
                locationTimers.remove(key);
            }

            if (toRemove.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No location timers found matching the specified filters.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Reset " + toRemove.size() + " location timer(s).");
            }
        } else {
            // Reset timers for specific mob ID (optionally filtered by location)
            if (!target.equals("random") && this.plugin.getConfigManager().getPowerMob(target) == null) {
                sender.sendMessage(ChatColor.RED + "Invalid mob ID: " + target);
                return true;
            }

            Map<String, SpawnContext> locationTimers = this.plugin.getSpawnTimerManager().getLocationTimers();
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, SpawnContext> entry : locationTimers.entrySet()) {
                SpawnContext context = entry.getValue();
                Location loc = context.getLocation();

                if (!context.getMobConfigId().equals(target)) {
                    continue;
                }

                boolean matches = worldName == null || loc.getWorld().getName().equals(worldName);

                if (x != Integer.MIN_VALUE) {
                    if (loc.getBlockX() != x || loc.getBlockY() != y || loc.getBlockZ() != z) {
                        matches = false;
                    }
                }

                if (matches) {
                    toRemove.add(entry.getKey());
                }
            }

            for (String key : toRemove) {
                locationTimers.remove(key);
            }

            if (toRemove.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No location timers found for " + target + " matching the specified filters.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Reset " + toRemove.size() + " location timer(s) for " + target + ".");
            }
        }

        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.delete")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to delete mob types.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob delete <mob-id>");
            return true;
        }

        String mobId = args[1];
        if ("random".equalsIgnoreCase(mobId)) {
            sender.sendMessage(ChatColor.RED + "Cannot delete the special 'random' type.");
            return true;
        }

        // Validate exists in config (in-memory map)
        if (this.plugin.getConfigManager().getPowerMob(mobId) == null) {
            sender.sendMessage(ChatColor.RED + "Unknown mob id: " + mobId);
            return true;
        }

        // Centralized removal/persist
        boolean removedFromConfig = this.plugin.getConfigManager().removePowerMob(mobId);
        if (!removedFromConfig) {
            sender.sendMessage(ChatColor.RED + "Failed to remove '" + mobId + "' from mobsconfig.yml.");
            return true;
        }

        // Clear timers for this id
        this.plugin.getSpawnTimerManager().getGlobalTimers().remove(mobId);
        this.plugin.getSpawnTimerManager().getLocationTimers().entrySet().removeIf(e ->
                mobId.equals(e.getValue().getMobConfigId()));

        // Despawn active instances
        int removed = 0;
        for (PowerMob mob : new ArrayList<>(this.plugin.getPowerMobManager().getPowerMobs().values())) {
            if (mob.isValid() && mobId.equals(mob.getId())) {
                mob.getEntity().remove();
                mob.remove();
                this.plugin.getPowerMobManager().unregisterPowerMob(mob);
                removed++;
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Deleted mob type '" + mobId + "' from config.");
        if (removed > 0) {
            sender.sendMessage(ChatColor.GRAY + "Also removed " + removed + " active instance(s) and cleared timers.");
        }
        return true;
    }

    /**
     * Formats milliseconds into a human-readable time string
     *
     * @param milliseconds Time in milliseconds
     * @return Formatted time string
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;

        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            return hours + "h " + remainingMinutes + "m";
        }

    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.getitem")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob give <item-id> [amount] [player]");
            return true;
        }

        String itemId = args[1];
        int amount = 1;
        Player target;

        // amount
        if (args.length >= 3) {
            try {
                amount = Math.min(64, Math.max(1, Integer.parseInt(args[2])));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
                return true;
            }
        }

        // player
        if (args.length >= 4) {
            target = sender.getServer().getPlayerExact(args[3]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[3]);
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ChatColor.RED + "Console must specify a player: /powermob give <item-id> [amount] <player>");
            return true;
        }

        ItemStack item = this.plugin.getEquipmentManager().getEquipment(itemId);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Unknown item id: " + itemId);
            return true;
        }
        item.setAmount(amount);

        var inv = target.getInventory();
        HashMap<Integer, ItemStack> leftovers = inv.addItem(item);
        if (!leftovers.isEmpty()) {
            // drop leftovers at feet
            for (ItemStack stack : leftovers.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.WHITE + amount + ChatColor.GREEN + " of " + ChatColor.GOLD + itemId + ChatColor.GREEN + " to " + target.getName());
        return true;
    }

    /**
     * Handles spawn blocker related commands
     */
    private boolean handleSpawnBlockerCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob spawnblocker <give|reload|list|info>");
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "give":
                return handleSpawnBlockerGive(sender, args);
            case "reload":
                return handleSpawnBlockerReload(sender);
            case "list":
                return handleSpawnBlockerList(sender);
            case "info":
                return handleSpawnBlockerInfo(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown spawn blocker command: " + subCommand);
                return true;
        }
    }

    private boolean handleSpawnBlockerGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.spawnblocker.give")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to give spawn blockers!");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob spawnblocker give <player> <blocker_id> [amount]");
            return true;
        }

        String playerName = args[2];
        String blockerId = args[3];
        int amount = 1;

        if (args.length >= 5) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[4]);
                return true;
            }
        }

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return true;
        }

        SpawnBlockerManager.SpawnBlockerConfig config = plugin.getSpawnBlockerManager().getSpawnBlockerConfig(blockerId);
        if (config == null) {
            sender.sendMessage(ChatColor.RED + "Unknown spawn blocker: " + blockerId);
            return true;
        }

        if (!config.type().equals("CUSTOM")) {
            sender.sendMessage(ChatColor.RED + "Cannot give vanilla spawn blockers as items!");
            return true;
        }

        for (int i = 0; i < amount; i++) {
            if (!plugin.getSpawnBlockerManager().giveSpawnBlockerItem(target, blockerId)) {
                sender.sendMessage(ChatColor.RED + "Failed to give spawn blocker item!");
                return true;
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + blockerId + " to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "You received " + amount + "x spawn blocker: " +
                config.name().replace("&", "§"));
        return true;
    }

    private boolean handleSpawnBlockerReload(CommandSender sender) {
        if (!sender.hasPermission("powermobs.spawnblocker.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload spawn blockers!");
            return true;
        }

        plugin.getSpawnBlockerManager().loadBlockers();
        sender.sendMessage(ChatColor.GREEN + "Spawn blockers reloaded!");
        return true;
    }


    private boolean handleSpawnBlockerList(CommandSender sender) {
        if (!sender.hasPermission("powermobs.spawnblocker.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to list spawn blockers!");
            return true;
        }

        // Collect active blockers by world, deduplicated
        Map<String, LinkedHashSet<SpawnBlockerManager.SpawnBlockerData>> byWorld = new TreeMap<>();
        for (World world : plugin.getServer().getWorlds()) {
            Map<SpawnBlockerManager.ChunkCoord, Set<SpawnBlockerManager.SpawnBlockerData>> map =
                    plugin.getSpawnBlockerManager().getActiveBlockers(world);
            if (map.isEmpty()) continue;

            LinkedHashSet<SpawnBlockerManager.SpawnBlockerData> unique = new LinkedHashSet<>();
            for (Set<SpawnBlockerManager.SpawnBlockerData> set : map.values()) {
                unique.addAll(set); // SpawnBlockerData equals/hashCode dedupe by (blockerId, location)
            }
            if (!unique.isEmpty()) {
                byWorld.put(world.getName(), unique);
            }
        }

        int total = byWorld.values().stream().mapToInt(Set::size).sum();
        if (total == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No active spawn blockers found in loaded worlds.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Active Spawn Blockers (grouped by world):");

        for (Map.Entry<String, LinkedHashSet<SpawnBlockerManager.SpawnBlockerData>> entry : byWorld.entrySet()) {
            String worldName = entry.getKey();
            List<SpawnBlockerManager.SpawnBlockerData> list = new ArrayList<>(entry.getValue());

            // Sort by Y, then X, then Z for consistent display
            list.sort(Comparator
                    .comparingInt((SpawnBlockerManager.SpawnBlockerData d) -> d.location().getBlockY())
                    .thenComparingInt(d -> d.location().getBlockX())
                    .thenComparingInt(d -> d.location().getBlockZ()));

            sender.sendMessage(ChatColor.GOLD + worldName + ChatColor.GRAY + " (" + list.size() + ")");

            for (SpawnBlockerManager.SpawnBlockerData data : list) {
                Location loc = data.location();
                SpawnBlockerManager.SpawnBlockerConfig cfg = data.config();
                String locStr = String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

                sender.sendMessage(ChatColor.YELLOW + "  • " + ChatColor.WHITE + data.blockerId()
                        + ChatColor.GRAY + " at " + locStr
                        + ChatColor.GRAY + " [" + cfg.type() + ", " + cfg.material()
                        + ", chunk range: " + cfg.chunkRange() + "]");
            }
        }

        sender.sendMessage(ChatColor.GRAY + "Total active spawn blockers: " + ChatColor.WHITE + total);
        return true;
    }

    private boolean handleSpawnBlockerInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.spawnblocker.info")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view spawn blocker info!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob spawnblocker info <blocker_id>");
            return true;
        }

        String blockerId = args[2];
        SpawnBlockerManager.SpawnBlockerConfig config = plugin.getSpawnBlockerManager().getSpawnBlockerConfig(blockerId);
        if (config == null) {
            sender.sendMessage(ChatColor.RED + "Unknown spawn blocker: " + blockerId);
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "=== Spawn Blocker Info: " + blockerId + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + config.type());
        sender.sendMessage(ChatColor.YELLOW + "Material: " + ChatColor.WHITE + config.material());
        sender.sendMessage(ChatColor.YELLOW + "Chunk Range: " + ChatColor.WHITE + config.chunkRange() + " blocks");
        sender.sendMessage(ChatColor.YELLOW + "Enabled: " + (config.enabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));

        if (!config.name().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Name: " + config.name().replace("&", "§"));
        }

        if (!config.description().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + config.description());
        }

        if (config.id() != null) {
            sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + config.id());
        }

        return true;
    }

    private List<String> getSpawnBlockerTabCompletions(String[] args) {
        if (args.length == 2) {
            return Arrays.asList("give", "reload", "list", "info");
        } else if (args.length == 3) {
            switch (args[1].toLowerCase()) {
                case "give":
                    return null; // Player names
                case "info":
                    return new ArrayList<>(plugin.getSpawnBlockerManager().getSpawnBlockerIds());
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            return plugin.getSpawnBlockerManager().getSpawnBlockerIds().stream()
                    .filter(id -> {
                        SpawnBlockerManager.SpawnBlockerConfig config = plugin.getSpawnBlockerManager().getSpawnBlockerConfig(id);
                        return config != null && config.type().equals("CUSTOM");
                    })
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendStatsHelp(sender);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "view":
                return handleStatsView(sender, args);
            case "topkills":
                return handleStatsTopKills(sender, args);
            case "topallkills":
                return handleStatsTopAllKills(sender, args);
            case "topmaxdamage":
                return handleStatsTopMaxDamage(sender, args);
            case "topallmaxdamage":
                return handleStatsTopAllMaxDamage(sender, args);
            case "topdamage":
                return handleStatsTopDamage(sender, args);
            case "topalldamage":
                return handleStatsTopAllDamage(sender, args);
            case "topdeaths":
                return handleStatsTopDeaths(sender, args);
            case "topalldeaths":
                return handleStatsTopAllDeaths(sender, args);
            case "rank":
                return handleStatsRank(sender, args);
            case "clearplayerdata":
                return handleStatsClearPlayerData(sender, args);
            case "clearpowermobdata":
                return handleStatsClearPowerMobData(sender, args);
            case "clearplayerpowermobdata":
                return handleStatsClearPlayerPowerMobData(sender, args);
            case "flush":
                return handleStatsFlush(sender);
            case "reloadcache":
                return handleStatsReloadCache(sender);
            default:
                sendStatsHelp(sender);
                return true;
        }
    }

    private boolean handleStatsView(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.view")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view stats.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats view <player> [powermob-id]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        UUID playerUuid = target.getUniqueId();
        StatsManager statsManager = plugin.getStatsManager();

        statsManager.loadSpecificPlayerStats(playerUuid).whenComplete((statsMap, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load stats data: " + throwable.getMessage());
                return;
            }

            String targetName = target.getName() != null ? target.getName() : playerUuid.toString();
            sender.sendMessage(ChatColor.GREEN + "=== PowerMobs Stats Data for " + targetName + " ===");

            if (args.length >= 4) {
                String powerMobId = args[3];
                CachedStats.MobStats stats = statsMap.get(powerMobId);
                if (stats == null) {
                    sender.sendMessage(ChatColor.GRAY + "No stats data recorded for power mob: " + powerMobId);
                    return;
                }

                sender.sendMessage(ChatColor.YELLOW + "Power Mob: " + ChatColor.WHITE + powerMobId);
                sender.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + stats.kills);
                sender.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + stats.deaths);
                sender.sendMessage(ChatColor.YELLOW + "Max Damage: " + ChatColor.WHITE + (int) stats.maxDamage);
                sender.sendMessage(ChatColor.YELLOW + "Total Damage: " + ChatColor.WHITE + (int) stats.totalDamage);
                return;
            }

            if (statsMap.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats data recorded.");
                return;
            }

            for (Map.Entry<String, CachedStats.MobStats> entry : statsMap.entrySet()) {
                CachedStats.MobStats stats = entry.getValue();
                sender.sendMessage(ChatColor.GOLD + entry.getKey() + ChatColor.GRAY
                        + " | Kills: " + ChatColor.WHITE + stats.kills
                        + ChatColor.GRAY + " | Deaths: " + ChatColor.WHITE + stats.deaths
                        + ChatColor.GRAY + " | Max Dmg: " + ChatColor.WHITE + (int) stats.maxDamage
                        + ChatColor.GRAY + " | Total Dmg: " + ChatColor.WHITE + (int) stats.totalDamage);
            }
        });

        return true;
    }

    private boolean handleStatsTopKills(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.top")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view leaderboards.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats topkills <mob-id> [limit]");
            return true;
        }

        String mobId = args[2];
        int limit = parseLimit(args, 3);
        plugin.getStatsManager().getTopKillerToMob(mobId, limit).whenComplete((rows, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load leaderboard.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "=== Top killers for " + mobId + " ===");
            if (rows.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats found.");
                return;
            }

            int rank = 1;
            for (StatsManager.LeaderboardEntry row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.playerUuid());
                String name = player.getName() != null ? player.getName() : row.playerUuid().toString();
                sender.sendMessage(ChatColor.GOLD + "#" + rank++
                        + ChatColor.YELLOW + " " + name
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + (int) row.score());
            }
        });
        return true;
    }

    private boolean handleStatsTopAllKills(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.top")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view leaderboards.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats topallkills [limit]");
            return true;
        }

        int limit = parseLimit(args, 2);
        plugin.getStatsManager().getTopKillerAcrossAllMobs(limit).whenComplete((rows, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load leaderboard.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "=== Top killers across all Power Mobs ===");
            if (rows.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats found.");
                return;
            }

            int rank = 1;
            for (StatsManager.LeaderboardEntry row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.playerUuid());
                String name = player.getName() != null ? player.getName() : row.playerUuid().toString();
                sender.sendMessage(ChatColor.GOLD + "#" + rank++
                        + ChatColor.YELLOW + " " + name
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + (int) row.score());
            }
        });
        return true;
    }

    private boolean handleStatsTopMaxDamage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.top")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view leaderboards.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats topmaxdamage <mob-id> [limit]");
            return true;
        }

        String mobId = args[2];
        int limit = parseLimit(args, 3);
        plugin.getStatsManager().getTopMaxDamageToMob(mobId, limit).whenComplete((rows, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load leaderboard.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "=== Top max damage for " + mobId + " ===");
            if (rows.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats found.");
                return;
            }

            int rank = 1;
            for (StatsManager.LeaderboardEntry row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.playerUuid());
                String name = player.getName() != null ? player.getName() : row.playerUuid().toString();
                sender.sendMessage(ChatColor.GOLD + "#" + rank++
                        + ChatColor.YELLOW + " " + name
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + (int) row.score());
            }
        });
        return true;
    }

    private boolean handleStatsTopAllMaxDamage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.top")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view leaderboards.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats topallmaxdamage [limit]");
            return true;
        }

        int limit = parseLimit(args, 2);
        plugin.getStatsManager().getTopMaxDamageAcrossAllMobs(limit).whenComplete((rows, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load leaderboard.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "=== Top max damage across all Power Mobs ===");
            if (rows.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats found.");
                return;
            }

            int rank = 1;
            for (StatsManager.LeaderboardEntry row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.playerUuid());
                String name = player.getName() != null ? player.getName() : row.playerUuid().toString();
                sender.sendMessage(ChatColor.GOLD + "#" + rank++
                        + ChatColor.YELLOW + " " + name
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + (int) row.score());
            }
        });
        return true;
    }

    private boolean handleStatsTopDamage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.top")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view leaderboards.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats topkills <mob-id> [limit]");
            return true;
        }

        String mobId = args[2];
        int limit = parseLimit(args, 3);
        plugin.getStatsManager().getTopDamageToMob(mobId, limit).whenComplete((rows, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load leaderboard.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "=== Top Total Damage Across All Power Mobs ===");
            if (rows.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats found.");
                return;
            }

            int rank = 1;
            for (StatsManager.LeaderboardEntry row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.playerUuid());
                String name = player.getName() != null ? player.getName() : row.playerUuid().toString();
                sender.sendMessage(ChatColor.GOLD + "#" + rank++
                        + ChatColor.YELLOW + " " + name
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + (int) row.score());
            }
        });
        return true;
    }

    private boolean handleStatsTopAllDamage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.top")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view leaderboards.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats topalltotaldamage [limit]");
            return true;
        }

        int limit = parseLimit(args, 2);
        plugin.getStatsManager().getTopDamageAcrossAllMobs(limit).whenComplete((rows, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load leaderboard.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "=== Top total damage across all Power Mobs ===");
            if (rows.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats found.");
                return;
            }

            int rank = 1;
            for (StatsManager.LeaderboardEntry row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.playerUuid());
                String name = player.getName() != null ? player.getName() : row.playerUuid().toString();
                sender.sendMessage(ChatColor.GOLD + "#" + rank++
                        + ChatColor.YELLOW + " " + name
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + (int) row.score());
            }
        });
        return true;
    }

    private boolean handleStatsTopDeaths(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.top")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view leaderboards.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats topdeaths <mob-id> [limit]");
            return true;
        }

        String mobId = args[2];
        int limit = parseLimit(args, 3);
        plugin.getStatsManager().getTopDeathsToMob(mobId, limit).whenComplete((rows, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load leaderboard.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "=== Top Deaths to " + mobId + " ===");
            if (rows.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats found.");
                return;
            }

            int rank = 1;
            for (StatsManager.LeaderboardEntry row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.playerUuid());
                String name = player.getName() != null ? player.getName() : row.playerUuid().toString();
                sender.sendMessage(ChatColor.GOLD + "#" + rank++
                        + ChatColor.YELLOW + " " + name
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + (int) row.score());
            }
        });
        return true;
    }

    private boolean handleStatsTopAllDeaths(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.top")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view leaderboards.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats topalldeaths [limit]");
            return true;
        }

        int limit = parseLimit(args, 2);
        plugin.getStatsManager().getTopDeathsAcrossAllMobs(limit).whenComplete((rows, throwable) -> {
            if (throwable != null) {
                sender.sendMessage(ChatColor.RED + "Failed to load leaderboard.");
                return;
            }

            sender.sendMessage(ChatColor.GREEN + "=== Top deaths across all Power Mobs ===");
            if (rows.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No stats found.");
                return;
            }

            int rank = 1;
            for (StatsManager.LeaderboardEntry row : rows) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(row.playerUuid());
                String name = player.getName() != null ? player.getName() : row.playerUuid().toString();
                sender.sendMessage(ChatColor.GOLD + "#" + rank++
                        + ChatColor.YELLOW + " " + name
                        + ChatColor.GRAY + " - " + ChatColor.WHITE + (int) row.score());
            }
        });
        return true;
    }

    private boolean handleStatsRank(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.rank")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view stat ranks.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats rank <player> <kills|deaths|damage|maxdamage>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        UUID playerUuid = target.getUniqueId();
        String stat = args[3].toLowerCase(Locale.ROOT);
        String name = target.getName() != null ? target.getName() : playerUuid.toString();

        switch (stat) {
            case "kills":
                plugin.getStatsManager().getPlayerKillsRank(playerUuid).whenComplete((rank, throwable) -> {
                    if (throwable != null) {
                        sender.sendMessage(ChatColor.RED + "Failed to load kills rank.");
                        return;
                    }
                    sender.sendMessage(ChatColor.GREEN + "=== PowerMobs Rank Data for " + name + " ===");
                    sender.sendMessage(ChatColor.YELLOW + "Kills Rank: " + ChatColor.WHITE + rank);
                });
                return true;

            case "deaths":
                plugin.getStatsManager().getPlayerDeathsRank(playerUuid).whenComplete((rank, throwable) -> {
                    if (throwable != null) {
                        sender.sendMessage(ChatColor.RED + "Failed to load deaths rank.");
                        return;
                    }
                    sender.sendMessage(ChatColor.GREEN + "=== PowerMobs Rank Data for " + name + " ===");
                    sender.sendMessage(ChatColor.YELLOW + "Deaths Rank: " + ChatColor.WHITE + rank);
                });
                return true;

            case "damage":
                plugin.getStatsManager().getPlayerDamageRank(playerUuid).whenComplete((rank, throwable) -> {
                    if (throwable != null) {
                        sender.sendMessage(ChatColor.RED + "Failed to load damage rank.");
                        return;
                    }
                    sender.sendMessage(ChatColor.GREEN + "=== PowerMobs Rank Data for " + name + " ===");
                    sender.sendMessage(ChatColor.YELLOW + "Damage Rank: " + ChatColor.WHITE + rank);
                });
                return true;
            case "maxdamage":
                plugin.getStatsManager().getPlayerMaxDamageRank(playerUuid).whenComplete((rank, throwable) -> {
                    if (throwable != null) {
                        sender.sendMessage(ChatColor.RED + "Failed to load max damage rank.");
                        return;
                    }
                    sender.sendMessage(ChatColor.GREEN + "=== PowerMobs Rank Data for " + name + " ===");
                    sender.sendMessage(ChatColor.YELLOW + "Max Damage Rank: " + ChatColor.WHITE + rank);
                });
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid stat type: " + stat);
                sender.sendMessage(ChatColor.RED + "Usage: /powermob stats rank <player> <kills|deaths|damage|maxdamage>");
                return true;
        }
    }

    private boolean handleStatsClearPlayerData(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.clearplayerdata")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to clear player stats data.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats clearplayerdata <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        CachedStats.deletePlayer(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "Cleared all PowerMobs stats data for " + target.getName() + ".");
        return true;
    }

    private boolean handleStatsClearPowerMobData(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.clearpowermobdata")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to clear power mob stats data.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats clearpowermobdata <powermob-id>");
            return true;
        }

        String powerMobId = args[2];
        plugin.getStatsManager().clearAllSpecificMobData(powerMobId);
        sender.sendMessage(ChatColor.GREEN + "Cleared all PowerMobs stats data for power mob id '" + powerMobId + "'.");
        return true;
    }

    private boolean handleStatsClearPlayerPowerMobData(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermobs.stats.clearplayerpowermobdata")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to clear player power mob stats data.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /powermob stats clearplayerpowermobdata <player> <powermob-id>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        String powerMobId = args[3];

        plugin.getStatsManager().clearSpecificMobData(target.getUniqueId(), powerMobId);
        sender.sendMessage(ChatColor.GREEN + "Cleared PowerMobs stats data for " + target.getName()
                + " on power mob id '" + powerMobId + "'.");
        return true;
    }

    private boolean handleStatsFlush(CommandSender sender) {
        if (!sender.hasPermission("powermobs.stats.flush")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to flush stats.");
            return true;
        }

        plugin.getStatsManager().saveAllActiveCaches(true);
        sender.sendMessage(ChatColor.GREEN + "Triggered asynchronous stats flush.");
        return true;
    }

    private boolean handleStatsReloadCache(CommandSender sender) {
        if (!sender.hasPermission("powermobs.stats.reloadcache")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload stats cache.");
            return true;
        }

        plugin.getStatsManager().loadGlobalMobTotalsIntoCache();
        sender.sendMessage(ChatColor.GREEN + "Triggered stats cache reload from database.");
        return true;
    }

    private int parseLimit(String[] args, int index) {
        if (args.length <= index) {
            return 10;
        }

        try {
            return Math.max(1, Math.min(100, Integer.parseInt(args[index])));
        } catch (NumberFormatException exception) {
            return 10;
        }
    }

    private void sendStatsHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "PowerMobs Stats Commands:");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats view <player> [mob-id]" + ChatColor.GRAY + " - View PowerMobs stats data");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats topkills <mob-id> [limit]" + ChatColor.GRAY + " - View top kill stats data for a power mob");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats topdeaths <mob-id> [limit]" + ChatColor.GRAY + " - View top death stats data for a power mob");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats topmaxdamage <mob-id> [limit]" + ChatColor.GRAY + " - View top max damage stats data for a power mob");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats topdamage <mob-id> [limit]" + ChatColor.GRAY + " - View top total damage stats for a power mob");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats topallkills [limit]" + ChatColor.GRAY + " - View top all time kill stats data against power mobs");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats topalldeaths [limit]" + ChatColor.GRAY + " - View top all time death stats data against power mobs");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats topallmaxdamage [limit]" + ChatColor.GRAY + " - View top all time max damage stats data against power mobs");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats topalldamage [limit]" + ChatColor.GRAY + " - View top all time total damage stats against power mobs");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats rank <player> <kills|deaths|maxdamage|damage>" + ChatColor.GRAY + " - View rank data for a specific stat");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats clearplayerdata <player>" + ChatColor.GRAY + " - Clear all player stats data");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats clearpowermobdata <mob-id>" + ChatColor.GRAY + " - Clear all power mob stats data");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats clearplayerpowermobdata <player> <mob-id>" + ChatColor.GRAY + " - Clear one player's power mob stats data");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats flush" + ChatColor.GRAY + " - Flush pending stats data to the database");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats reloadcache" + ChatColor.GRAY + " - Reload stats cache data from the database");
    }

    /**
     * Sends help information to a command sender
     *
     * @param sender The command sender
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "PowerMobs Commands:");
        sender.sendMessage(ChatColor.GOLD + "/powermob spawn <mob-id> [count]" + ChatColor.GRAY + " - Spawn power mobs");
        sender.sendMessage(ChatColor.GOLD + "/powermob list" + ChatColor.GRAY + " - List available mob types and active mobs");
        sender.sendMessage(ChatColor.GOLD + "/powermob info <mob-id>" + ChatColor.GRAY + " - Show information about a mob type");
        sender.sendMessage(ChatColor.GOLD + "/powermob timers" + ChatColor.GRAY + " - Show timer summary");
        sender.sendMessage(ChatColor.GOLD + "/powermob timers view <scope> [mobId] [world] [x y z]" + ChatColor.GRAY + " - View spawn timers");
        sender.sendMessage(ChatColor.GOLD + "/powermob timers reset <scope> [mobId|all] [world] [x y z]" + ChatColor.GRAY + " - Reset spawn timers");
        sender.sendMessage(ChatColor.GOLD + "/powermob give <item-id> [amount] [player]" + ChatColor.GRAY + " - Gives a custom item");
        sender.sendMessage(ChatColor.GOLD + "/powermob reload" + ChatColor.GRAY + " - Reload the configuration");
        sender.sendMessage(ChatColor.GOLD + "/powermob remove <all|radius|type> [radius|type]" + ChatColor.GRAY + " - Remove power mobs");
        sender.sendMessage(ChatColor.GOLD + "/powermob config" + ChatColor.GRAY + " - Open the configuration GUI");
        sender.sendMessage(ChatColor.GOLD + "/powermob spawnblocker <give|reload|list|info>" + ChatColor.GRAY + " - Manage spawn blockers");
        sender.sendMessage(ChatColor.GOLD + "/powermob populate <abilities|items|mobs|keys|blockers>" + ChatColor.GRAY + " - Populate the config with default data that isn't already there or configured");
        sender.sendMessage(ChatColor.GOLD + "/powermob update" + ChatColor.GRAY + " - Updates items to the latest configuration setup");
        sender.sendMessage(ChatColor.GOLD + "/powermob delete <mobID>" + ChatColor.GRAY + " - Deletes and de-registers a power mob");
        sender.sendMessage(ChatColor.GOLD + "/powermob stats <view|topkills|topallkills|topdeaths|topalldeaths|topdamage|topalldamage|topmaxdamage|topallmaxdamage|rank|clearplayerdata|clearpowermobdata|clearplayerpowermobdata|flush|reloadcache>" + ChatColor.GRAY + " - View and manage stats database data");
        sender.sendMessage(ChatColor.GOLD + "/powermob help" + ChatColor.GRAY + " - Show this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("spawn");
            completions.add("list");
            completions.add("info");
            completions.add("timers");
            completions.add("give");
            completions.add("reload");
            completions.add("remove");
            completions.add("config");
            completions.add("spawnblocker");
            completions.add("populate");
            completions.add("update");
            completions.add("delete");
            completions.add("stats");
            completions.add("help");

            return filterCompletions(completions, args[0]);
        } else if (args[0].equalsIgnoreCase("spawnblocker")) {
            List<String> comps = getSpawnBlockerTabCompletions(args);
            if (comps == null) return null;
            return filterCompletions(comps, args[args.length - 1]);
        } else if (args[0].equalsIgnoreCase("stats")) {
            if (args.length == 2) {
                return filterCompletions(List.of(
                        "view",
                        "topkills",
                        "topallkills",
                        "topdeaths",
                        "topalldeaths",
                        "topdamage",
                        "topalldamage",
                        "topmaxdamage",
                        "topallmaxdamage",
                        "rank",
                        "clearplayerdata",
                        "clearpowermobdata",
                        "clearplayerpowermobdata",
                        "flush",
                        "reloadcache"
                ), args[1]);
            } else if (args.length == 3) {
                switch (args[1].toLowerCase()) {
                    case "view":
                    case "rank":
                    case "clearplayerdata":
                    case "clearplayerpowermobdata":
                        return filterCompletions(
                                this.plugin.getServer().getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .collect(Collectors.toList()),
                                args[2]
                        );
                    case "topkills":
                    case "topmaxdamage":
                    case "topdeaths":
                    case "clearpowermobdata":
                        return filterCompletions(
                                new ArrayList<>(this.plugin.getConfigManager().getPowerMobs().keySet()),
                                args[2]
                        );
                    case "topdamage":
                        return filterCompletions(List.of("10", "25", "50", "100"), args[2]);
                }
            } else if (args.length == 4) {
                switch (args[1].toLowerCase()) {
                    case "view":
                    case "clearplayerpowermobdata":
                        return filterCompletions(
                                new ArrayList<>(this.plugin.getConfigManager().getPowerMobs().keySet()),
                                args[3]
                        );
                    case "topkills":
                    case "topmaxdamage":
                    case "topdeaths":
                        return filterCompletions(List.of("10", "25", "50", "100"), args[3]);
                    case "rank":
                        return filterCompletions(List.of("kills", "deaths", "damage"), args[3]);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("info")) {
                List<String> completions = new ArrayList<>(this.plugin.getConfigManager().getPowerMobs().keySet());
                if (args[0].equalsIgnoreCase("spawn")) {
                    completions.add("random");
                }

                return filterCompletions(completions, args[1]);
            } else if (args[0].equalsIgnoreCase("timers")) {
                List<String> completions = new ArrayList<>();
                completions.add("view");
                completions.add("reset");
                return filterCompletions(completions, args[1]);
            } else if (args[0].equalsIgnoreCase("remove")) {
                List<String> completions = new ArrayList<>();
                completions.add("all");
                completions.add("radius");
                completions.add("type");

                return filterCompletions(completions, args[1]);
            } else if (args[0].equalsIgnoreCase("give")) {
                // Suggest available custom item IDs
                List<String> itemIds = new ArrayList<>(this.plugin.getEquipmentManager().getAllEquipment().keySet());
                return filterCompletions(itemIds, args[1]);
            } else if (args[0].equalsIgnoreCase("populate")) {
                List<String> completions = new ArrayList<>();
                completions.add("abilities");
                completions.add("items");
                completions.add("mobs");
                completions.add("keys");
                completions.add("blockers");
                completions.add("main");
                return filterCompletions(completions, args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("timers") &&
                    (args[1].equalsIgnoreCase("view") || args[1].equalsIgnoreCase("reset"))) {
                List<String> completions = new ArrayList<>();
                completions.add("global");
                completions.add("location");
                return filterCompletions(completions, args[2]);
            } else if (args[0].equalsIgnoreCase("remove") && args[1].equalsIgnoreCase("type")) {
                List<String> mobIds = this.plugin.getPowerMobManager().getPowerMobs().values().stream()
                        .filter(PowerMob::isValid)
                        .map(PowerMob::getId)
                        .distinct()
                        .collect(Collectors.toList());

                return filterCompletions(mobIds, args[2]);
            } else if (args[0].equalsIgnoreCase("remove") && args[1].equalsIgnoreCase("radius")) {
                List<String> completions = new ArrayList<>();
                completions.add("10");
                completions.add("25");
                completions.add("50");
                completions.add("100");

                return filterCompletions(completions, args[2]);
            } else if (args[0].equalsIgnoreCase("give")) {
                // Suggest common stack sizes
                List<String> amounts = Arrays.asList("1", "8", "16", "32", "64");
                return filterCompletions(amounts, args[2]);
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("timers") &&
                    (args[1].equalsIgnoreCase("view") || args[1].equalsIgnoreCase("reset"))) {
                // Tab complete mob IDs for timer commands
                List<String> completions = new ArrayList<>(this.plugin.getConfigManager().getPowerMobs().keySet());
                completions.add("random");
                if (args[1].equalsIgnoreCase("reset")) {
                    completions.add("all");
                }
                return filterCompletions(completions, args[3]);
            } else if (args[0].equalsIgnoreCase("give")) {
                // Suggest online player names
                List<String> names = this.plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return filterCompletions(names, args[3]);
            }

        }

        return Collections.emptyList();

    }

    /**
     * Filters completions based on the current input
     *
     * @param completions The list of possible completions
     * @param current     The current input
     * @return The filtered completions
     */
    private List<String> filterCompletions(List<String> completions, String current) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList());
    }
}