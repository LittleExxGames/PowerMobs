package com.powermobs.UI.pages;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.UI.framework.PowerMobType;
import com.powermobs.config.IPowerMobConfig;
import com.powermobs.config.PowerManager;
import com.powermobs.mobs.equipment.CustomDropConfig;
import com.powermobs.mobs.equipment.EnchantmentConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ItemDropConfigurationPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private String selectedMobId;
    private String selectedItemId; //Should match with the tempItem item value
    // removed: private boolean isNewDrop;
    private CustomDropConfig currentItem;
    private CustomDropConfig tempItem;

    public ItemDropConfigurationPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.YELLOW + "Item Drop Configuration");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Item Drop Configuration.", "ui");

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

        if (mobConfig == null) {
            plugin.getLogger().severe("Invalid mob config while building item drop page.");
            pageManager.navigateBack(playerSession.getPlayer());
            return;
        }

        selectedItemId = playerSession.getSelectedItemId();

        // 1) No item selected yet. Brand new drop.
        if (selectedItemId == null) {
            plugin.debug("No selectedItemId, navigating to item_selection for new drop config.", "ui");
            currentItem = null;
            tempItem = null;
            pageManager.navigateTo("item_selection", true, playerSession.getPlayer());
            return;
        }

        // 2) First time entering this page for this selectedItemId
        if (tempItem == null && currentItem == null) {
            CustomDropConfig existing = mobConfig.getDrop(selectedItemId);
            if (existing != null) {
                // Editing existing
                currentItem = existing;
                tempItem = new CustomDropConfig(currentItem); // deep copy
                plugin.debug("Editing existing drop config: " + currentItem.getItem(), "ui");
            } else {
                // New config for this ID (defaults similar to previous code)
                plugin.debug("Creating new drop config for item: " + selectedItemId, "ui");
                tempItem = new CustomDropConfig(selectedItemId, 1, 1, 1, 100, new ArrayList<>());
            }
        } else if (tempItem != null && !Objects.equals(tempItem.getItem(), selectedItemId)) {
            // 3) User changed the selected item via ItemSelectionPage
            plugin.debug("Drop item selection changed from " + tempItem.getItem() + " to " + selectedItemId, "ui");
            tempItem.setItem(selectedItemId);
        }
        // else: returning to the page without selection changes -> keep currentItem & tempItem

        // Display Item (Center of top row)
        ItemStack displayItem = getDisplayItem();

        // Warning for conflicting custom-item drops
        boolean shouldShowWarning = false;
        if (!isVanillaItem(tempItem.getItem())) {
            CustomDropConfig existingDrop = mobConfig.getDrop(tempItem.getItem());
            shouldShowWarning = (existingDrop != null && existingDrop != currentItem);
        }

        if (shouldShowWarning) {
            ItemStack warningItem = createGuiItem(
                    Material.REDSTONE,
                    ChatColor.RED + "The selected item is already being dropped!",
                    ChatColor.RED + "***CHANGES WILL OVERWRITE THE DROP***"
            );
            inventory.setItem(3, warningItem);
            inventory.setItem(5, warningItem);
        }
        inventory.setItem(4, displayItem);

        // Drop Amount Settings
        ItemStack amountItem = createGuiItem(
                Material.ITEM_FRAME,
                ChatColor.AQUA + "Drop Amount: " +
                        (tempItem != null ? tempItem.getMinAmount() + "-" + tempItem.getMaxAmount() : "1"),
                ChatColor.GRAY + "Click to modify the amount",
                ChatColor.GRAY + "of items to drop"
        );
        inventory.setItem(20, amountItem);

        // Amount Weight Settings
        ItemStack amountWeightItem = createGuiItem(
                Material.ANVIL,
                ChatColor.GOLD + "Amount Weight: " +
                        (tempItem != null ? tempItem.getAmountWeight() : "100"),
                ChatColor.GRAY + "Click to set the weight (1-200)",
                ChatColor.GRAY + "1-99 weighs to lower values",
                ChatColor.GRAY + "100 is equal chance within the range",
                ChatColor.GRAY + "101-200 weighs to higher values"
        );
        inventory.setItem(22, amountWeightItem);

        // Drop Chance Settings
        ItemStack chanceItem = createGuiItem(
                Material.PAPER,
                ChatColor.YELLOW + "Drop Chance: " +
                        (tempItem != null ? formatDecimal(tempItem.getChance()) : "0.5"),
                ChatColor.GRAY + "Click to set chance (0-1)",
                ChatColor.GRAY + "0 = 0%, 1 = 100%"
        );
        inventory.setItem(24, chanceItem);

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
        String baseName = itemId;
        int hyphenIndex = itemId.lastIndexOf("-");
        if (hyphenIndex != -1) {
            String suffix = itemId.substring(hyphenIndex + 1);
            try {
                Integer.parseInt(suffix);
                baseName = itemId.substring(0, hyphenIndex);
            } catch (NumberFormatException e) {
                // Suffix is not a number, use full itemId as baseName
            }
        }
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
        if (tempItem != null) {
            if (isVanillaItem(tempItem.getItem())) {
                Material material = getVanillaMaterial(tempItem.getItem());
                return material != null && isEnchantableMaterial(material);
            } else {
                return false;
            }
        }
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

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        PlayerSessionData session = guiManager.getCurrentPlayer();
        switch (slot) {
            case 4: // Display item - change item
                session.setSelectedItem(inventory.getItem(slot));
                pageManager.navigateTo("item_selection", true, player);
                return true;

            case 20: // Drop Amount
                startChatInput(player, ChatInputType.DROP_AMOUNT, (value, p) -> {
                    updateDropAmount(value);
                    pageManager.navigateTo("item_drop_config", false, p);
                });
                return true;

            case 22: // Amount Weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempItem.setAmountWeight((Integer) value);
                    pageManager.navigateTo("item_drop_config", false, p);
                });
                return true;

            case 24: // Drop Chance
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempItem.setChance((Double) value);
                    pageManager.navigateTo("item_drop_config", false, p);
                });
                return true;

            case 40: //Enchantments
                if (canHaveEnchantments()) {
                    startChatInput(player, ChatInputType.ENCHANTMENTS, (value, p) -> {
                        parseEnchantment(value);
                        pageManager.navigateTo("item_drop_config", false, p);
                    });
                }
                return true;

            case 45: // Save Changes
                saveChanges(player);
                clearDropInfo();
                session.setEditing(false);
                pageManager.navigateBack(player);
                return true;

            case 53: // Cancel
                clearDropInfo();
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
        pageManager.getPlugin().debug("remove --- " + type, "ui");
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

    private void updateDropAmount(Object value) {
        if (value instanceof String range) {
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            tempItem.setMinAmount(min);
            tempItem.setMaxAmount(max);
        } else if (value instanceof Integer) {
            int amount = (Integer) value;
            tempItem.setMinAmount(amount);
            tempItem.setMaxAmount(amount);
        }
    }

    public void clearDropInfo() {
        guiManager.getCurrentPlayer().setSelectedItem(null);
        guiManager.getCurrentPlayer().setSelectedItemId(null);
        selectedMobId = null;
        tempItem = null;
        currentItem = null;
    }

    private void saveChanges(Player player) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PowerManager configManager = plugin.getConfigManager();
        if (configManager.isSaveInProgress()) {
            plugin.debug("Saving already in progress", "ui");
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
        CustomDropConfig dropConfig = new CustomDropConfig(tempItem);

        IPowerMobConfig mob = (isRandom)
                ? configManager.getRandomMobConfig()
                : configManager.getPowerMob(selectedMobId);
        List<CustomDropConfig> drops = mob.getDrops();

        boolean editingExisting = (currentItem != null);

        if (!editingExisting) {
            // New drop
            if (isVanillaItem(selectedItemId)) {
                String newDrop = iterateVanillaDuplicates(drops);
                dropConfig.setItem(newDrop);
            }
            // For both vanilla & custom, ensure uniqueness of final ID
            drops.removeIf(cfg -> Objects.equals(cfg.getItem(), dropConfig.getItem()));
            drops.add(dropConfig);
        } else {
            // Editing existing drop
            if (isVanillaItem(selectedItemId)) {
                String newDrop = iterateVanillaDuplicates(drops);
                dropConfig.setItem(newDrop);
            }
            String originalId = currentItem.getItem();
            Iterator<CustomDropConfig> iterator = drops.iterator();
            while (iterator.hasNext()) {
                CustomDropConfig existingDrop = iterator.next();
                if (Objects.equals(existingDrop.getItem(), originalId)) {
                    iterator.remove();
                    break;
                }
            }
            // Remove any config with the new ID to avoid duplicates
            drops.removeIf(cfg -> Objects.equals(cfg.getItem(), dropConfig.getItem()));
            drops.add(dropConfig);
        }

        if (isRandom) {
            configManager.saveRandomMob(mob.toConfigMap());
        } else {
            configManager.savePowerMob(selectedMobId, mob.toConfigMap());
        }
        player.sendMessage(ChatColor.GREEN + "Drop settings have been saved.");
    }

    public String iterateVanillaDuplicates(List<CustomDropConfig> drops) {
        int duplicates = 0;
        List<String> itemIds = new ArrayList<>();
        String incoming;
        if (selectedItemId.lastIndexOf("-") != -1) {
            incoming = selectedItemId.substring(0, selectedItemId.lastIndexOf("-")).toUpperCase();
        } else {
            incoming = selectedItemId.toUpperCase();
        }
        for (CustomDropConfig existingDrop : drops) {
            itemIds.add(existingDrop.getItem());
            String name;
            if (existingDrop.getItem().lastIndexOf("-") != -1) {
                name = existingDrop.getItem()
                        .substring(0, existingDrop.getItem().lastIndexOf("-"))
                        .toUpperCase();
            } else {
                name = existingDrop.getItem().toUpperCase();
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
