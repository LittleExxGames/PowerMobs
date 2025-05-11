package com.custommobs.UI;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.config.ConfigManager;
import com.custommobs.config.CustomMobConfig;
import com.custommobs.config.RandomMobConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConfigGUI implements InventoryHolder {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private final CustomMobsPlugin plugin;
    private final Inventory inventory;
    private GUIPage currentPage;
    private String selectedMobId;
    private int pageNumber = 0;
    public ConfigGUI(CustomMobsPlugin plugin) {
        this.plugin = plugin;
        this.currentPage = GUIPage.MAIN_MENU;
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, ChatColor.DARK_PURPLE + "CustomMobs Configuration");

        // Initialize with main menu
        updateMainMenu();
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Updates the inventory with main menu items
     */
    public void updateMainMenu() {
        inventory.clear();

        // Custom Mobs List button
        ItemStack mobListButton = createGuiItem(Material.ZOMBIE_HEAD,
                ChatColor.GREEN + "Custom Mobs List",
                ChatColor.GRAY + "View and edit all custom mobs");
        inventory.setItem(20, mobListButton);

        // Create New Mob button
        ItemStack createMobButton = createGuiItem(Material.DRAGON_EGG,
                ChatColor.GOLD + "Create New Custom Mob",
                ChatColor.GRAY + "Create a brand new custom mob");
        inventory.setItem(22, createMobButton);

        // Random Mob Settings button
        ItemStack randomMobButton = createGuiItem(Material.COMPARATOR,
                ChatColor.AQUA + "Random Mob Settings",
                ChatColor.GRAY + "Configure random mob generation");
        inventory.setItem(24, randomMobButton);

        // Help button
        ItemStack helpButton = createGuiItem(Material.BOOK,
                ChatColor.YELLOW + "Help",
                ChatColor.GRAY + "Information about using this GUI");
        inventory.setItem(40, helpButton);

        // Reload Config button
        ItemStack reloadButton = createGuiItem(Material.REDSTONE,
                ChatColor.RED + "Reload Configuration",
                ChatColor.GRAY + "Reload all configurations from disk");
        inventory.setItem(49, reloadButton);

        currentPage = GUIPage.MAIN_MENU;
    }

    /**
     * Updates the inventory with the custom mobs list
     */
    public void updateMobList() {
        inventory.clear();

        // Get all custom mobs
        Map<String, CustomMobConfig> mobs = plugin.getConfigManager().getCustomMobs();

        // Setup paging if needed
        int startIndex = pageNumber * 45;
        int endIndex = Math.min(startIndex + 45, mobs.size());

        // Add mob entries
        int slot = 0;
        List<String> mobIds = new ArrayList<>(mobs.keySet());
        for (int i = startIndex; i < endIndex; i++) {
            if (i >= mobIds.size()) break;

            String mobId = mobIds.get(i);
            CustomMobConfig mobConfig = mobs.get(mobId);

            Material icon = getMaterialForEntityType(mobConfig.getEntityType());

            ItemStack mobItem = createGuiItem(icon,
                    ChatColor.GREEN + mobId,
                    ChatColor.GRAY + "Type: " + mobConfig.getEntityType(),
                    ChatColor.GRAY + "Health: " + mobConfig.getHealth(),
                    ChatColor.GRAY + "Click to edit this mob");

            inventory.setItem(slot, mobItem);
            slot++;
        }

        // Add navigation buttons
        if (pageNumber > 0) {
            ItemStack prevButton = createGuiItem(Material.ARROW,
                    ChatColor.YELLOW + "Previous Page");
            inventory.setItem(45, prevButton);
        }

        if ((pageNumber + 1) * 45 < mobs.size()) {
            ItemStack nextButton = createGuiItem(Material.ARROW,
                    ChatColor.YELLOW + "Next Page");
            inventory.setItem(53, nextButton);
        }

        // Back button
        ItemStack backButton = createGuiItem(Material.BARRIER,
                ChatColor.RED + "Back to Main Menu");
        inventory.setItem(49, backButton);

        currentPage = GUIPage.MOB_LIST;
    }

    /**
     * Updates the inventory with random mob settings
     */
    public void updateRandomMobSettings() {
        inventory.clear();

        RandomMobConfig config = plugin.getConfigManager().getRandomMobConfig();

        // Enabled/Disabled toggle
        Material toggleMaterial = config.isEnabled() ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        String toggleText = config.isEnabled() ? "Enabled" : "Disabled";
        ItemStack toggleButton = createGuiItem(toggleMaterial,
                ChatColor.GOLD + "Random Mobs: " + ChatColor.GREEN + toggleText,
                ChatColor.GRAY + "Click to toggle random mob generation");
        inventory.setItem(4, toggleButton);

        // Spawn chance slider
        ItemStack chanceItem = createGuiItem(Material.CLOCK,
                ChatColor.AQUA + "Spawn Chance: " + config.getChance(),
                ChatColor.GRAY + "Chance of a random mob spawning",
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(10, chanceItem);

        // Health multiplier range
        ItemStack healthItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Health Multiplier",
                ChatColor.GRAY + "Min: " + config.getMinHealthMultiplier(),
                ChatColor.GRAY + "Max: " + config.getMaxHealthMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(12, healthItem);

        // Damage multiplier range
        ItemStack damageItem = createGuiItem(Material.IRON_SWORD,
                ChatColor.GOLD + "Damage Multiplier",
                ChatColor.GRAY + "Min: " + config.getMinDamageMultiplier(),
                ChatColor.GRAY + "Max: " + config.getMaxDamageMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(14, damageItem);

        // Speed multiplier range
        ItemStack speedItem = createGuiItem(Material.FEATHER,
                ChatColor.WHITE + "Speed Multiplier",
                ChatColor.GRAY + "Min: " + config.getMinSpeedMultiplier(),
                ChatColor.GRAY + "Max: " + config.getMaxSpeedMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(16, speedItem);

        // Ability settings
        ItemStack abilityItem = createGuiItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + "Ability Settings",
                ChatColor.GRAY + "Min Abilities: " + config.getMinAbilities(),
                ChatColor.GRAY + "Max Abilities: " + config.getMaxAbilities(),
                ChatColor.GRAY + "Possible Abilities: " + config.getPossibleAbilities().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(28, abilityItem);

        // Equipment settings
        ItemStack equipmentItem = createGuiItem(Material.DIAMOND_CHESTPLATE,
                ChatColor.BLUE + "Equipment Settings",
                ChatColor.GRAY + "Weapon Chance: " + config.getWeaponChance(),
                ChatColor.GRAY + "Armor Chances: " +
                        String.format("H:%.1f/C:%.1f/L:%.1f/B:%.1f",
                                config.getHelmetChance(),
                                config.getChestplateChance(),
                                config.getLeggingsChance(),
                                config.getBootsChance()),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(30, equipmentItem);

        // Drop settings
        ItemStack dropItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drop Settings",
                ChatColor.GRAY + "Min Drops: " + config.getMinDrops(),
                ChatColor.GRAY + "Max Drops: " + config.getMaxDrops(),
                ChatColor.GRAY + "Possible Drops: " + config.getDrops().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(32, dropItem);

        // Allowed mob types
        ItemStack mobTypesItem = createGuiItem(Material.ZOMBIE_SPAWN_EGG,
                ChatColor.GREEN + "Allowed Mob Types",
                ChatColor.GRAY + "Configured Types: " + config.getAllowedTypes().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(34, mobTypesItem);

        // Back button
        ItemStack backButton = createGuiItem(Material.BARRIER,
                ChatColor.RED + "Back to Main Menu");
        inventory.setItem(49, backButton);

        currentPage = GUIPage.RANDOM_MOB_SETTINGS;
    }

    /**
     * Opens the mob editor for a specific mob
     *
     * @param mobId The ID of the mob to edit
     */
    public void openMobEditor(String mobId) {
        inventory.clear();

        CustomMobConfig mobConfig = plugin.getConfigManager().getCustomMob(mobId);
        if (mobConfig == null) {
            updateMobList();
            return;
        }

        selectedMobId = mobId;

        // Mob info display
        Material icon = getMaterialForEntityType(mobConfig.getEntityType());
        ItemStack infoItem = createGuiItem(icon,
                ChatColor.GREEN + mobId,
                ChatColor.GRAY + "Type: " + mobConfig.getEntityType(),
                ChatColor.GRAY + "Name: " + ChatColor.translateAlternateColorCodes('&', mobConfig.getName()));
        inventory.setItem(4, infoItem);

        // Health setting
        ItemStack healthItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Health: " + mobConfig.getHealth(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(10, healthItem);

        // Damage multiplier
        ItemStack damageItem = createGuiItem(Material.IRON_SWORD,
                ChatColor.GOLD + "Damage Multiplier: " + mobConfig.getDamageMultiplier(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(12, damageItem);

        // Speed multiplier
        ItemStack speedItem = createGuiItem(Material.FEATHER,
                ChatColor.WHITE + "Speed Multiplier: " + mobConfig.getSpeedMultiplier(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(14, speedItem);

        // Entity type
        ItemStack typeItem = createGuiItem(Material.ZOMBIE_SPAWN_EGG,
                ChatColor.AQUA + "Entity Type: " + mobConfig.getEntityType(),
                ChatColor.GRAY + "Click to change");
        inventory.setItem(16, typeItem);

        // Abilities
        ItemStack abilitiesItem = createGuiItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + "Abilities",
                ChatColor.GRAY + "Current abilities: " + mobConfig.getAbilities().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(28, abilitiesItem);

        // Equipment
        ItemStack equipmentItem = createGuiItem(Material.DIAMOND_CHESTPLATE,
                ChatColor.BLUE + "Equipment",
                ChatColor.GRAY + "Current equipment: " + mobConfig.getEquipment().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(30, equipmentItem);

        // Drops
        ItemStack dropsItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drops",
                ChatColor.GRAY + "Drop count: " + mobConfig.getMinDrops() + "-" + mobConfig.getMaxDrops(),
                ChatColor.GRAY + "Possible drops: " + mobConfig.getDrops().size(),
                ChatColor.GRAY + "Click to configure");
        inventory.setItem(32, dropsItem);

        // Spawn conditions
        ItemStack spawnItem = createGuiItem(Material.SPAWNER,
                ChatColor.GREEN + "Spawn Conditions",
                ChatColor.GRAY + "Configure where and when",
                ChatColor.GRAY + "this mob can spawn");
        inventory.setItem(34, spawnItem);

        // Test spawn
        ItemStack testItem = createGuiItem(Material.EGG,
                ChatColor.YELLOW + "Test Spawn",
                ChatColor.GRAY + "Spawn this mob at your location");
        inventory.setItem(40, testItem);

        // Delete mob
        ItemStack deleteItem = createGuiItem(Material.LAVA_BUCKET,
                ChatColor.RED + "Delete Mob",
                ChatColor.GRAY + "Permanently delete this mob",
                ChatColor.DARK_RED + "This cannot be undone!");
        inventory.setItem(45, deleteItem);

        // Back button
        ItemStack backButton = createGuiItem(Material.BARRIER,
                ChatColor.RED + "Back to Mob List");
        inventory.setItem(49, backButton);

        // Save button
        ItemStack saveButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save all changes to this mob");
        inventory.setItem(53, saveButton);

        currentPage = GUIPage.MOB_EDITOR;
    }

    /**
     * Opens the create mob interface
     */
    public void openCreateMobInterface() {
        inventory.clear();

        // Title
        ItemStack titleItem = createGuiItem(Material.DRAGON_EGG,
                ChatColor.GOLD + "Create New Custom Mob",
                ChatColor.GRAY + "Configure your new mob");
        inventory.setItem(4, titleItem);

        // Mob ID
        ItemStack idItem = createGuiItem(Material.NAME_TAG,
                ChatColor.GREEN + "Set Mob ID",
                ChatColor.GRAY + "Current: " + (selectedMobId != null ? selectedMobId : "Not set"),
                ChatColor.GRAY + "Click to set a unique identifier");
        inventory.setItem(10, idItem);

        // Entity Type
        ItemStack typeItem = createGuiItem(Material.ZOMBIE_SPAWN_EGG,
                ChatColor.AQUA + "Entity Type",
                ChatColor.GRAY + "Choose the base entity type");
        inventory.setItem(12, typeItem);

        // Display Name
        ItemStack nameItem = createGuiItem(Material.OAK_SIGN,
                ChatColor.WHITE + "Set Display Name",
                ChatColor.GRAY + "Set the name shown above the mob");
        inventory.setItem(14, nameItem);

        // Health
        ItemStack healthItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Set Health",
                ChatColor.GRAY + "Default: 20.0");
        inventory.setItem(16, healthItem);

        // More settings options can go here...

        // Create button
        ItemStack createButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Create Mob",
                ChatColor.GRAY + "Save and create this custom mob");
        inventory.setItem(40, createButton);

        // Back button
        ItemStack backButton = createGuiItem(Material.BARRIER,
                ChatColor.RED + "Back to Main Menu");
        inventory.setItem(36, backButton);

        currentPage = GUIPage.CREATE_MOB;
    }

    /**
     * Creates a GUI item with a custom name and lore
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);

            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Gets a material that represents an entity type
     */
    private Material getMaterialForEntityType(EntityType type) {
        switch (type) {
            case ZOMBIE:
                return Material.ZOMBIE_HEAD;
            case SKELETON:
                return Material.SKELETON_SKULL;
            case CREEPER:
                return Material.CREEPER_HEAD;
            case SPIDER:
                return Material.SPIDER_EYE;
            case ENDERMAN:
                return Material.ENDER_PEARL;
            case SLIME:
                return Material.SLIME_BALL;
            case WITCH:
                return Material.BREWING_STAND;
            // Add more mappings as needed
            default:
                return Material.SPAWNER;
        }
    }

    /**
     * Handle clicks on the GUI
     *
     * @param player The player who clicked
     * @param slot   The slot that was clicked
     * @return True if the event should be canceled
     */
    public boolean handleClick(Player player, int slot) {
        // Handle clicks based on current page
        switch (currentPage) {
            case MAIN_MENU:
                return handleMainMenuClick(player, slot);
            case MOB_LIST:
                return handleMobListClick(player, slot);
            case MOB_EDITOR:
                return handleMobEditorClick(player, slot);
            case CREATE_MOB:
                return handleCreateMobClick(player, slot);
            case RANDOM_MOB_SETTINGS:
                return handleRandomMobSettingsClick(player, slot);
            default:
                return true;
        }
    }

    /**
     * Handle clicks on the main menu
     */
    private boolean handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 20: // Custom Mobs List
                updateMobList();
                break;
            case 22: // Create New Mob
                openCreateMobInterface();
                break;
            case 24: // Random Mob Settings
                updateRandomMobSettings();
                break;
            case 40: // Help
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "=== CustomMobs GUI Help ===");
                player.sendMessage(ChatColor.YELLOW + "This GUI allows you to manage all your custom mobs.");
                player.sendMessage(ChatColor.YELLOW + "- View and edit existing mobs");
                player.sendMessage(ChatColor.YELLOW + "- Create new custom mobs");
                player.sendMessage(ChatColor.YELLOW + "- Configure random mob generation");
                player.sendMessage(ChatColor.YELLOW + "Use the tabs to navigate between sections.");
                break;
            case 49: // Reload Config
                player.closeInventory();
                player.performCommand("custommob reload");
                break;
        }
        return true;
    }

    /**
     * Handle clicks on the mob list
     */
    private boolean handleMobListClick(Player player, int slot) {
        if (slot < 45) {
            // A mob was clicked - open its editor
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                openMobEditor(name);
            }
        } else if (slot == 45 && pageNumber > 0) {
            // Previous page
            pageNumber--;
            updateMobList();
        } else if (slot == 53 && inventory.getItem(53) != null) {
            // Next page
            pageNumber++;
            updateMobList();
        } else if (slot == 49) {
            // Back to main menu
            updateMainMenu();
        }
        return true;
    }

    /**
     * Handle clicks on the mob editor
     */
    private boolean handleMobEditorClick(Player player, int slot) {
        switch (slot) {
            case 49: // Back to mob list
                updateMobList();
                break;
            case 40: // Test spawn
                player.closeInventory();
                player.performCommand("custommob spawn " + selectedMobId);
                break;
            case 45: // Delete mob
                // This would require implementation of a confirmation GUI or command
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "To delete a mob, use the command: /custommob delete " + selectedMobId);
                break;
            // Other editor actions would be implemented here
        }
        return true;
    }

    /**
     * Handle clicks on the create mob interface
     */
    private boolean handleCreateMobClick(Player player, int slot) {
        switch (slot) {
            case 36: // Back to main menu
                updateMainMenu();
                break;
            case 40: // Create mob
                player.closeInventory();
                // This would need a full implementation of mob creation logic
                player.sendMessage(ChatColor.YELLOW + "Mob creation through GUI not yet implemented.");
                break;
            // Other creation actions would be implemented here
        }
        return true;
    }

    /**
     * Handle clicks on the random mob settings
     */
    private boolean handleRandomMobSettingsClick(Player player, int slot) {
        switch (slot) {
            case 49: // Back to main menu
                updateMainMenu();
                break;
            // Other random mob settings actions would be implemented here
        }
        return true;
    }

    // GUI page types
    public enum GUIPage {
        MAIN_MENU,
        MOB_LIST,
        MOB_EDITOR,
        CREATE_MOB,
        RANDOM_MOB_SETTINGS
    }
}