package com.custommobs.commands;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.config.CustomMobConfig;
import com.custommobs.mobs.CustomMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command handler for the custommob command
 */
@RequiredArgsConstructor
public class CustomMobCommand implements CommandExecutor, TabCompleter {

    private final CustomMobsPlugin plugin;
    
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
            case "help":
                sendHelp(sender);
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
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommobs.spawn")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /custommob spawn <mob-id> [count]");
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
        CustomMobConfig config = this.plugin.getConfigManager().getCustomMob(mobId);
        if (config == null && !mobId.equals("random")) {
            sender.sendMessage(ChatColor.RED + "Invalid mob ID: " + mobId);
            return true;
        }
        
        // Get the entity type
        EntityType entityType = mobId.equals("random") ? 
            EntityType.ZOMBIE : // Default for random mobs
            config.getEntityType();
        
        // Get the player
        Player player = (Player) sender;
        Location location = player.getTargetBlock(null, 100).getLocation().add(0, 1, 0);
        
        // Spawn the mobs
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            try {
                // Spawn the entity
                LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);
                
                // Create the custom mob
                CustomMob customMob = this.plugin.getCustomMobManager().createAndRegisterCustomMob(entity, mobId);
                if (customMob != null) {
                    spawned++;
                }
            } catch (Exception e) {
                this.plugin.getLogger().severe("Error spawning custom mob: " + e.getMessage());
            }
        }
        
        sender.sendMessage(ChatColor.GREEN + "Spawned " + spawned + " custom mobs.");
        return true;
    }
    
    /**
     * Handles the list subcommand
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommobs.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // List available mob types
        sender.sendMessage(ChatColor.GREEN + "Available custom mob types:");
        for (String id : this.plugin.getConfigManager().getCustomMobs().keySet()) {
            CustomMobConfig config = this.plugin.getConfigManager().getCustomMob(id);
            sender.sendMessage(ChatColor.GOLD + "- " + id + ChatColor.GRAY + " (" + 
                config.getEntityType() + ", Health: " + config.getHealth() + ")");
        }
        
        // List active custom mobs
        sender.sendMessage(ChatColor.GREEN + "Active custom mobs:");
        Map<String, Integer> counts = this.plugin.getCustomMobManager().getCustomMobs().values().stream()
            .filter(CustomMob::isValid)
            .collect(Collectors.groupingBy(CustomMob::getId, Collectors.summingInt(m -> 1)));
        
        for (String id : counts.keySet()) {
            sender.sendMessage(ChatColor.GOLD + "- " + id + ": " + ChatColor.WHITE + counts.get(id));
        }
        
        return true;
    }
    
    /**
     * Handles the info subcommand
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommobs.info")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /custommob info <mob-id>");
            return true;
        }
        
        String mobId = args[1];
        
        // Check if the mob ID is valid
        CustomMobConfig config = this.plugin.getConfigManager().getCustomMob(mobId);
        if (config == null) {
            sender.sendMessage(ChatColor.RED + "Invalid mob ID: " + mobId);
            return true;
        }
        
        // Display mob info
        sender.sendMessage(ChatColor.GREEN + "Information for mob " + mobId + ":");
        sender.sendMessage(ChatColor.GOLD + "Type: " + ChatColor.WHITE + config.getEntityType());
        sender.sendMessage(ChatColor.GOLD + "Name: " + ChatColor.translateAlternateColorCodes('&', config.getName()));
        sender.sendMessage(ChatColor.GOLD + "Health: " + ChatColor.WHITE + config.getHealth());
        sender.sendMessage(ChatColor.GOLD + "Damage Multiplier: " + ChatColor.WHITE + config.getDamageMultiplier());
        sender.sendMessage(ChatColor.GOLD + "Speed Multiplier: " + ChatColor.WHITE + config.getSpeedMultiplier());
        
        // List abilities
        sender.sendMessage(ChatColor.GOLD + "Abilities:");
        for (String ability : config.getAbilities()) {
            sender.sendMessage(ChatColor.GRAY + "- " + ability);
        }
        
        // List equipment
        sender.sendMessage(ChatColor.GOLD + "Equipment:");
        for (String slot : config.getEquipment().keySet()) {
            sender.sendMessage(ChatColor.GRAY + "- " + slot + ": " + config.getEquipment().get(slot));
        }
        
        // List active instances
        List<CustomMob> activeMobs = this.plugin.getCustomMobManager().getCustomMobs().values().stream()
            .filter(mob -> mob.isValid() && mob.getId().equals(mobId))
            .collect(Collectors.toList());
            
        sender.sendMessage(ChatColor.GOLD + "Active instances: " + ChatColor.WHITE + activeMobs.size());
        
        return true;
    }
    
    /**
     * Handles the reload subcommand
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommobs.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Reloading CustomMobs configuration...");
        
        try {
            this.plugin.getConfigManager().reloadConfig();
            this.plugin.getAbilityManager().loadAbilities();
            this.plugin.getEquipmentManager().loadEquipment();
            
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
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("custommobs.remove")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /custommob remove <all|radius|type> [radius|type]");
            return true;
        }
        
        String mode = args[1].toLowerCase();
        
        switch (mode) {
            case "all":
                // Remove all custom mobs
                int removed = 0;
                for (CustomMob mob : new ArrayList<>(this.plugin.getCustomMobManager().getCustomMobs().values())) {
                    if (mob.isValid()) {
                        mob.getEntity().remove();
                        mob.remove();
                        this.plugin.getCustomMobManager().unregisterCustomMob(mob);
                        removed++;
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Removed " + removed + " custom mobs.");
                return true;
                
            case "radius":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
                
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /custommob remove radius <radius>");
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
                
                Player player = (Player) sender;
                Location location = player.getLocation();
                
                int removedInRadius = 0;
                for (Entity entity : player.getWorld().getEntities()) {
                    if (entity instanceof LivingEntity && entity.getLocation().distance(location) <= radius) {
                        CustomMob mob = CustomMob.getFromEntity(this.plugin, (LivingEntity) entity);
                        if (mob != null && mob.isValid()) {
                            mob.getEntity().remove();
                            mob.remove();
                            this.plugin.getCustomMobManager().unregisterCustomMob(mob);
                            removedInRadius++;
                        }
                    }
                }
                
                sender.sendMessage(ChatColor.GREEN + "Removed " + removedInRadius + " custom mobs within " + radius + " blocks.");
                return true;
                
            case "type":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /custommob remove type <mob-id>");
                    return true;
                }
                
                String typeId = args[2];
                int removedOfType = 0;
                
                for (CustomMob mob : new ArrayList<>(this.plugin.getCustomMobManager().getCustomMobs().values())) {
                    if (mob.isValid() && mob.getId().equals(typeId)) {
                        mob.getEntity().remove();
                        mob.remove();
                        this.plugin.getCustomMobManager().unregisterCustomMob(mob);
                        removedOfType++;
                    }
                }
                
                sender.sendMessage(ChatColor.GREEN + "Removed " + removedOfType + " " + typeId + " mobs.");
                return true;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown mode: " + mode);
                sender.sendMessage(ChatColor.RED + "Usage: /custommob remove <all|radius|type> [radius|type]");
                return true;
        }
    }
    
    /**
     * Sends help information to a command sender
     * 
     * @param sender The command sender
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "CustomMobs Commands:");
        sender.sendMessage(ChatColor.GOLD + "/custommob spawn <mob-id> [count]" + ChatColor.GRAY + " - Spawn custom mobs");
        sender.sendMessage(ChatColor.GOLD + "/custommob list" + ChatColor.GRAY + " - List available mob types and active mobs");
        sender.sendMessage(ChatColor.GOLD + "/custommob info <mob-id>" + ChatColor.GRAY + " - Show information about a mob type");
        sender.sendMessage(ChatColor.GOLD + "/custommob reload" + ChatColor.GRAY + " - Reload the configuration");
        sender.sendMessage(ChatColor.GOLD + "/custommob remove <all|radius|type> [radius|type]" + ChatColor.GRAY + " - Remove custom mobs");
        sender.sendMessage(ChatColor.GOLD + "/custommob help" + ChatColor.GRAY + " - Show this help message");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("spawn");
            completions.add("list");
            completions.add("info");
            completions.add("reload");
            completions.add("remove");
            completions.add("help");
            
            return filterCompletions(completions, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("info")) {
                List<String> completions = new ArrayList<>(this.plugin.getConfigManager().getCustomMobs().keySet());
                if (args[0].equalsIgnoreCase("spawn")) {
                    completions.add("random");
                }
                
                return filterCompletions(completions, args[1]);
            } else if (args[0].equalsIgnoreCase("remove")) {
                List<String> completions = new ArrayList<>();
                completions.add("all");
                completions.add("radius");
                completions.add("type");
                
                return filterCompletions(completions, args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("remove") && args[1].equalsIgnoreCase("type")) {
                List<String> mobIds = this.plugin.getCustomMobManager().getCustomMobs().values().stream()
                    .filter(CustomMob::isValid)
                    .map(CustomMob::getId)
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
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Filters completions based on the current input
     * 
     * @param completions The list of possible completions
     * @param current The current input
     * @return The filtered completions
     */
    private List<String> filterCompletions(List<String> completions, String current) {
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(current.toLowerCase()))
            .collect(Collectors.toList());
    }
}