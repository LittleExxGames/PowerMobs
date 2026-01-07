package com.powermobs.UI.pages;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.*;
import com.powermobs.config.EquipmentItemConfig;
import com.powermobs.config.IPowerMobConfig;
import com.powermobs.config.PowerManager;
import com.powermobs.mobs.equipment.EnchantmentConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MobEquipmentItemSettingsPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private String selectedMobId;
    private String selectedItemId; //Should match with the tempItem item value
    private EquipmentItemConfig currentItem;
    private EquipmentItemConfig tempItem;

    public MobEquipmentItemSettingsPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.YELLOW + "Equipment Item Configuration");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Equipment Item Configuration.", "ui");

        PowerMobsPlugin plugin = pageManager.getPlugin();
        PlayerSessionData playerSession = plugin.getGuiManager().getCurrentPlayer();
        if (playerSession.isCancelAction()) {
            playerSession.setCancelAction(false);
            pageManager.navigateBack(playerSession.getPlayer());
            return;
        }

        selectedMobId = playerSession.getSelectedMobId();
        IPowerMobConfig mobConfig = (playerSession.getType() == PowerMobType.RANDOM)
                ? plugin.getConfigManager().getRandomMobConfig()
                : plugin.getConfigManager().getPowerMob(selectedMobId);

        String equipmentSlotKey = getEditingType(playerSession.getItemEditType());
        if (mobConfig == null || equipmentSlotKey == null) {
            plugin.getLogger().severe("Invalid mob config or equipment slot while building item settings page.");
            pageManager.navigateBack(playerSession.getPlayer());
            return;
        }

        selectedItemId = playerSession.getSelectedItemId();
        List<EquipmentItemConfig> slotEquipment =
                mobConfig.getPossibleEquipment().computeIfAbsent(equipmentSlotKey, k -> new ArrayList<>());


        // 1) No item selected yet. brand new item.
        if (selectedItemId == null) {
            plugin.debug("No selectedItemId, navigating to item_selection for new equipment config.", "ui");
            currentItem = null;
            tempItem = null;
            pageManager.navigateTo("item_selection", true, playerSession.getPlayer());
            return;
        }

        // 2) First time entering this page for this selectedItemId
        if (tempItem == null && currentItem == null) {
            // Try to find an existing config with this ID
            currentItem = slotEquipment.stream()
                    .filter(cfg -> Objects.equals(cfg.getItem(), selectedItemId))
                    .findFirst()
                    .orElse(null);

            if (currentItem != null) {
                StringBuilder ench = new StringBuilder();
                for (EnchantmentConfig enchant : currentItem.getEnchantments()) {
                    ench.append(enchant.getType()).append(", ");
                }
                plugin.debug("Found existing equipment config for item: " + selectedItemId + " : " + currentItem.getItem() + " : enchants: " + ench, "ui");
                // Editing existing
                tempItem = new EquipmentItemConfig(currentItem); // deep copy
                plugin.debug("Editing existing equipment config: " + currentItem.getItem(), "ui");
            } else {
                // New config for this ID
                tempItem = new EquipmentItemConfig(selectedItemId, new ArrayList<>());
                plugin.debug("Creating new equipment config for item: " + selectedItemId, "ui");
            }
        } else if (tempItem != null && !Objects.equals(tempItem.getItem(), selectedItemId)) {
            // 3) User changed the selected item via ItemSelectionPage:
            plugin.debug("Item selection changed from " + tempItem.getItem() + " to " + selectedItemId, "ui");
            tempItem.setItem(selectedItemId);
        }
        // else: returning to page without selection changes -> keep currentItem & tempItem


        ItemStack displayItem = getDisplayItem();

        boolean shouldShowWarning = false;
        if (!isVanillaItem(tempItem.getItem())) {
            // For custom items, warn if another entry already uses this ID
            shouldShowWarning = slotEquipment.stream().anyMatch(cfg ->
                    cfg != currentItem && Objects.equals(cfg.getItem(), tempItem.getItem()));
        }

        if (shouldShowWarning) {
            ItemStack warningItem = createGuiItem(
                    Material.REDSTONE,
                    ChatColor.RED + "The selected item is already being equipped!",
                    ChatColor.RED + "***CHANGES WILL OVERWRITE THE DUPLICATE ITEM***"
            );
            inventory.setItem(3, warningItem);
            inventory.setItem(5, warningItem);
        }
        inventory.setItem(4, displayItem);


        // Enchantment Settings (if applicable)
        boolean isEnchantable = canHaveEnchantments();

        if (isEnchantable) {
            List<String> enchantLore = new ArrayList<>();

            for (EnchantmentConfig enchant : tempItem.getEnchantments()) {
                plugin.debug("Displaying enchant: " + enchant.getType()
                        + " min=" + enchant.getMinLevel()
                        + " max=" + enchant.getMaxLevel()
                        + " weight=" + enchant.getWeight(), "ui");

                String line;
                if (enchant.getMinLevel() == enchant.getMaxLevel()) {
                    line = ChatColor.GREEN + enchant.getType()
                            + " - Level: " + enchant.getMinLevel()
                            + " - Weight: " + enchant.getWeight();
                } else {
                    line = ChatColor.GREEN + enchant.getType()
                            + " - Level: " + enchant.getMinLevel() + "-"
                            + enchant.getMaxLevel()
                            + " - Weight: " + enchant.getWeight();
                }
                enchantLore.add(line);
            }

            if (enchantLore.isEmpty()) {
                enchantLore.add(ChatColor.DARK_GRAY + "No enchantments set");
            }

            ItemStack singleEnchantItem = createGuiItem(
                    Material.ENCHANTED_BOOK,
                    ChatColor.LIGHT_PURPLE + "Enchantments",
                    ChatColor.GRAY + "Configure enchantments - Click to modify"
            );
            ItemMeta meta = singleEnchantItem.getItemMeta();
            if (meta != null) {
                List<String> fullLore = new ArrayList<>();
                fullLore.add(ChatColor.GRAY + "Configure enchantments - Click to modify");
                fullLore.addAll(enchantLore);
                meta.setLore(fullLore);
                singleEnchantItem.setItemMeta(meta);
            }
            inventory.setItem(40, singleEnchantItem);
        }

        // Save/Cancel Buttons
        ItemStack saveButton = createGuiItem(
                Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save the current configuration"
        );
        inventory.setItem(45, saveButton);

        // Back button
        List<String> backDisplay = new ArrayList<>();
        if (currentItem == null) {
            backDisplay.add(ChatColor.RED + "UNSAVED CHANGES WILL BE LOST");
        } else if (!tempItem.toConfigMap().equals(currentItem.toConfigMap())) {
            backDisplay.add(ChatColor.RED + "UNSAVED CHANGES WILL BE LOST");
        }
        ItemStack backButton = createGuiItem(Material.BARRIER,
                ChatColor.RED + "Back",
                backDisplay);
        inventory.setItem(53, backButton);
    }

    private ItemStack getDisplayItem() {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        // Handle case where no item is selected yet
        if (selectedItemId == null) {
            return createGuiItem(
                    Material.BARRIER,
                    ChatColor.RED + "No Item Selected",
                    ChatColor.GRAY + "Click to select an item"
            );
        }

        Material vanillaMaterial = getVanillaMaterial(selectedItemId);

        if (vanillaMaterial != null) {
            // It's a vanilla item
            return createGuiItem(
                    vanillaMaterial,
                    ChatColor.GREEN + "Current Item: " + formatMaterialName(vanillaMaterial.name()),
                    ChatColor.GRAY + "Click to select a different item"
            );
        } else {
            // Custom item
            // Try to get the custom item from equipment manager
            ItemStack customItem = plugin.getEquipmentManager().getEquipment(selectedItemId);
            if (customItem != null) {
                ItemStack displayItem = customItem.clone();
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + "Current Item: " + meta.getDisplayName());
                    displayItem.setItemMeta(meta);
                }
                return displayItem;
            } else {
                return createGuiItem(
                        Material.NAME_TAG,
                        ChatColor.GREEN + "Current Item: " + selectedItemId,
                        ChatColor.GRAY + "Custom item",
                        ChatColor.GRAY + "Click to select a different item"
                );
            }
        }
    }

    private boolean isVanillaItem(String itemId) {
        if (itemId == null) return false;

        // Extract base name (remove suffix like "-2")
        String baseName = itemId;
        int hyphenIndex = itemId.lastIndexOf("-");
        if (hyphenIndex != -1) {
            String suffix = itemId.substring(hyphenIndex + 1);
            try {
                // If suffix is a number, it's a vanilla duplicate format
                Integer.parseInt(suffix);
                baseName = itemId.substring(0, hyphenIndex);
            } catch (NumberFormatException e) {
                // Suffix is not a number, use full itemId as baseName
            }
        }

        // Check if base name is a valid Material
        try {
            Material.valueOf(baseName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Material getVanillaMaterial(String itemId) {
        if (!isVanillaItem(itemId)) return null;

        String baseName = itemId;
        int hyphenIndex = itemId.lastIndexOf("-");
        if (hyphenIndex != -1) {
            String suffix = itemId.substring(hyphenIndex + 1);
            try {
                Integer.parseInt(suffix);
                baseName = itemId.substring(0, hyphenIndex);
            } catch (NumberFormatException e) {
                // Keep full itemId
            }
        }

        try {
            return Material.valueOf(baseName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean canHaveEnchantments() {
        // If we have a current drop being edited
        if (tempItem != null) {
            // Use our centralized detection method
            if (isVanillaItem(tempItem.getItem())) {
                Material material = getVanillaMaterial(tempItem.getItem());
                return material != null && isEnchantableMaterial(material);
            } else {
                // Custom items don't need enchantment editing (pre-enchanted)
                return false;
            }
        }

        // For new items being created
        try {
            Material material = Material.valueOf(guiManager.getCurrentPlayer().getSelectedItemId().toUpperCase());
            return isEnchantableMaterial(material);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isEnchantableMaterial(Material material) {
        return material == Material.ENCHANTED_BOOK ||
                material.name().contains("_SWORD") ||
                material.name().contains("_AXE") ||
                material.name().contains("_PICKAXE") ||
                material.name().contains("_SHOVEL") ||
                material.name().contains("_HOE") ||
                material.name().contains("_HELMET") ||
                material.name().contains("_CHESTPLATE") ||
                material.name().contains("_LEGGINGS") ||
                material.name().contains("_BOOTS") ||
                material == Material.BOW ||
                material == Material.CROSSBOW ||
                material == Material.TRIDENT ||
                material == Material.FISHING_ROD ||
                material == Material.SHEARS ||
                material == Material.FLINT_AND_STEEL ||
                material == Material.CARROT_ON_A_STICK ||
                material == Material.WARPED_FUNGUS_ON_A_STICK ||
                material == Material.SHIELD ||
                material == Material.ELYTRA;
    }

    public String getEditingType(EditingType type) {
        return switch (type) {
            case HELMET -> "possible-helmets";
            case CHESTPLATE -> "possible-chestplates";
            case LEGGINGS -> "possible-leggings";
            case BOOTS -> "possible-boots";
            case MAINHAND -> "possible-weapons";
            case OFFHAND -> "possible-offhands";
            default -> null;
        };
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        PlayerSessionData session = guiManager.getCurrentPlayer();
        switch (slot) {
            case 4: // Display item - change item
                session.setSelectedItem(inventory.getItem(slot));
                pageManager.navigateTo("item_selection", true, player);
                return true;
            case 40: //Enchantments
                if (canHaveEnchantments()) {
                    startChatInput(player, ChatInputType.ENCHANTMENTS, (value, p) -> {
                        parseEnchantment(value);
                        pageManager.navigateTo("mob_equipment_item_settings", false, p);
                    });
                }
                return true;

            case 45: // Save Changes
                saveChanges(player);
                clearEquipmentInfo();
                session.setEditing(false);
                pageManager.navigateBack(player);
                return true;

            case 53: // Cancel
                clearEquipmentInfo();
                session.setEditing(false);
                pageManager.navigateBack(player);
                return true;
            default:
                return false;
        }

    }


    private void parseEnchantment(Object value) {
        String enchantment = (String) value;
        pageManager.getPlugin().debug(enchantment, "ui");
        String[] parts = enchantment.split(":");
        String type = parts[1];
        String proposition = parts[0];
        if (proposition.equals("remove")) {
            List<EnchantmentConfig> existingEnchants = tempItem.getEnchantments();
            existingEnchants.removeIf(enchant -> enchant.getType().equals(type));
        } else if (proposition.equals("add")) {
            String[] range = parts[2].split("-");
            int levelMin;
            int levelMax;
            int weight = 100;
            if (range.length == 2) {
                levelMin = Integer.parseInt(range[0].trim());
                levelMax = Integer.parseInt(range[1].trim());
            } else {
                levelMin = Integer.parseInt(range[0].trim());
                levelMax = levelMin;
            }
            if (parts.length > 3) {
                weight = Integer.parseInt(parts[3].trim());
            }
            List<EnchantmentConfig> existingEnchants = tempItem.getEnchantments();
            existingEnchants.removeIf(enchant -> enchant.getType().equals(type));
            EnchantmentConfig enchant = new EnchantmentConfig(type, levelMin, levelMax, weight);
            existingEnchants.add(enchant);
        }
    }


    public void clearEquipmentInfo() {
        guiManager.getCurrentPlayer().setSelectedItem(null);
        guiManager.getCurrentPlayer().setSelectedItemId(null);
        tempItem = null;
        currentItem = null;

    }

    private void saveChanges(Player player) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PowerManager configManager = plugin.getConfigManager();
        if (configManager.isSaveInProgress()) {
            plugin.debug("Saving already in progress", "save_and_load");
            return;
        }

        PlayerSessionData playerSession = guiManager.getCurrentPlayer();
        if (selectedItemId == null || tempItem == null) {
            player.sendMessage(ChatColor.RED + "No item selected to save!");
            return;
        }

        boolean isRandom = (playerSession.getType() == PowerMobType.RANDOM);
        if (!canHaveEnchantments()) {
            tempItem.getEnchantments().clear();
        }

        EquipmentItemConfig equipmentConfig = new EquipmentItemConfig(tempItem);

        String equipmentSlotKey = getEditingType(playerSession.getItemEditType());
        if (equipmentSlotKey == null) {
            plugin.debug(ChatColor.RED + "Invalid equipment slot for saving.", "save_and_load");
            return;
        }

        IPowerMobConfig mob = isRandom
                ? configManager.getRandomMobConfig()
                : configManager.getPowerMob(selectedMobId);

        Map<String, List<EquipmentItemConfig>> possible = mob.getPossibleEquipment();
        List<EquipmentItemConfig> existing = possible.get(equipmentSlotKey);
        List<EquipmentItemConfig> equipment =
                (existing != null) ? new ArrayList<>(existing) : new ArrayList<>();

        applyEquipmentChangeList(equipment, equipmentConfig, isVanillaItem(selectedItemId));

        // For PowerMobConfig, enforce max 1 entry per slot
        if (!isRandom && !equipment.isEmpty()) {
            equipment = new ArrayList<>(Collections.singletonList(equipment.get(equipment.size() - 1)));
        }

        possible.put(equipmentSlotKey, equipment);

        // Save
        if (isRandom) {
            configManager.saveRandomMob(mob.toConfigMap());
        } else {
            configManager.savePowerMob(selectedMobId, mob.toConfigMap());
        }

        plugin.debug("Equipment config saved for slot: " + equipmentSlotKey
                + " item: " + equipmentConfig.getItem(), "save_and_load");

        player.sendMessage(ChatColor.GREEN + "Equipment settings have been saved.");
    }

    /**
     * Applies add/update logic to a slot equipment list, handling vanilla duplicate IDs.
     */
    private void applyEquipmentChangeList(List<EquipmentItemConfig> equipment, EquipmentItemConfig equipmentConfig, boolean isVanilla) {
        if (!editingExisting()) {
            if (isVanilla) {
                String newEquipment = iterateVanillaDuplicates(equipment);
                equipmentConfig.setItem(newEquipment);
            }
            equipment.removeIf(cfg -> Objects.equals(cfg.getItem(), equipmentConfig.getItem()));
            equipment.add(equipmentConfig);
        } else {
            if (isVanilla) {
                String setEquipment = iterateVanillaDuplicates(equipment);
                equipmentConfig.setItem(setEquipment);
            }

            String originalId = currentItem.getItem();
            Iterator<EquipmentItemConfig> iterator = equipment.iterator();
            while (iterator.hasNext()) {
                EquipmentItemConfig existingEquipment = iterator.next();
                if (Objects.equals(existingEquipment.getItem(), originalId)) {
                    iterator.remove();
                    break;
                }
            }
            equipment.removeIf(cfg -> Objects.equals(cfg.getItem(), equipmentConfig.getItem()));
            equipment.add(equipmentConfig);
        }
    }

    private boolean editingExisting() {
        return currentItem != null;
    }

    public String iterateVanillaDuplicates(List<EquipmentItemConfig> equipment) {
        int duplicates = 0;
        List<String> itemIds = new ArrayList<>();
        String incoming;
        if (selectedItemId.lastIndexOf("-") != -1) {
            incoming = selectedItemId.substring(0, selectedItemId.lastIndexOf("-")).toUpperCase();
        } else {
            incoming = selectedItemId.toUpperCase();
        }
        for (EquipmentItemConfig existingEquipment : equipment) {
            itemIds.add(existingEquipment.getItem());
            String name;
            if (existingEquipment.getItem().lastIndexOf("-") != -1) {
                name = existingEquipment.getItem().substring(0, existingEquipment.getItem().lastIndexOf("-")).toUpperCase();
            } else {
                name = existingEquipment.getItem().toUpperCase();
            }
            if (name.equals(incoming)) {
                duplicates++;
            }
        }
        for (int i = 1; i <= duplicates + 1; i++) {
            String newItemId = incoming + "-" + i;
            if (!itemIds.contains(newItemId)) {
                return newItemId;
            }
        }
        return selectedItemId; // No duplicates found
    }
}
