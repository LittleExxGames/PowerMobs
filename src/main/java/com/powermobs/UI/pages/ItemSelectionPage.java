package com.powermobs.UI.pages;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.EditingType;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.mobs.abilities.Ability;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class ItemSelectionPage extends AbstractGUIPage {
    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private final CustomItemType selectedItemType = CustomItemType.CUSTOM;
    private int pageNumber = 0;
    private boolean showingCustomItems = false;
    private SelectionMode selectionMode = SelectionMode.ITEMS;
    public ItemSelectionPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.BLUE + "Item Selection List");
    }

    @Override
    public void build() {
        inventory.clear();

        // Determine selection mode based on editing type
        PlayerSessionData session = guiManager.getCurrentPlayer();
        if (session.getItemEditType() == EditingType.ABILITY) {
            selectionMode = SelectionMode.ABILITIES;
            updateInventoryTitle(ChatColor.LIGHT_PURPLE + "Ability Selection List");
        } else {
            selectionMode = SelectionMode.ITEMS;
            updateInventoryTitle(ChatColor.BLUE + "Item Selection List");
        }

        pageManager.getPlugin().debug("Building GUI for " + selectionMode.name() + " selection.", "ui");

        if (selectionMode == SelectionMode.ABILITIES) {
            buildAbilitySelection();
        } else {
            buildItemSelection();
        }
    }

    private void buildItemSelection() {
        List<Object> items;

        if (showingCustomItems) {
            // Get custom items from config/manager
            items = new ArrayList<>(getCustomItems());
        } else {
            // Get vanilla materials
            items = Arrays.stream(Material.values())
                    .filter(material -> material.isItem() && !material.isAir())
                    .sorted(Comparator.comparing(Enum::name))
                    .collect(Collectors.toList());
        }

        // Setup paging if needed
        int startIndex = pageNumber * 45;
        int endIndex = Math.min(startIndex + 45, items.size());

        int slot = 0;
        if (showingCustomItems) {
            List<Map.Entry<String, ItemStack>> customItems = new ArrayList<>(pageManager.getPlugin().getEquipmentManager().getAllEquipment().entrySet());
            customItems.removeIf(entry -> guiManager.getCurrentPlayer().getActiveItems().contains(entry.getKey()));
            pageManager.getPlugin().debug("Number of custom items found: " + customItems.size(), "ui");
            endIndex = Math.min(endIndex, customItems.size());
            for (int i = startIndex; i < endIndex; i++) {
                Map.Entry<String, ItemStack> entry = customItems.get(i);
                String customItemId = entry.getKey();
                ItemStack customItem = entry.getValue().clone();
                ItemMeta meta = customItem.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "(" + customItemId + ")" + ChatColor.BLUE + " " + meta.getDisplayName());
                customItem.setItemMeta(meta);
                inventory.setItem(slot, customItem);
                slot++;
            }
        } else {
            for (int i = startIndex; i < endIndex; i++) {
                Object item = items.get(i);
                // Display vanilla material
                Material material = (Material) item;
                ItemStack displayItem = createGuiItem(
                        material,
                        ChatColor.WHITE + formatMaterialName(material.name())
                );
                inventory.setItem(slot, displayItem);
                slot++;
            }
        }

        // Add navigation buttons
        if (pageNumber > 0) {
            addNavigationButton(45, false);
        }

        if ((pageNumber + 1) * 45 < items.size()) {
            addNavigationButton(53, true);
        }

        // Toggle custom/vanilla items button
        ItemStack toggleButton = createGuiItem(
                showingCustomItems ? Material.CRAFTING_TABLE : Material.ENDER_CHEST,
                showingCustomItems ? ChatColor.AQUA + "Show Vanilla Items" : ChatColor.LIGHT_PURPLE + "Show Custom Items",
                ChatColor.GRAY + "Click to toggle between",
                ChatColor.GRAY + "vanilla and custom items"
        );
        inventory.setItem(47, toggleButton);

        // Back button
        addBackButton(49, ChatColor.RED + "Back to Main Menu");

        // Display currently selected item
        inventory.setItem(51, guiManager.getCurrentPlayer().getSelectedItem());
    }

    private void buildAbilitySelection() {
        // Get all available abilities
        Map<String, Ability> allAbilities = pageManager.getPlugin().getAbilityManager().getAbilities();

        // Filter out abilities that are already active for this mob
        Set<String> activeAbilities = guiManager.getCurrentPlayer().getActiveItems();
        List<Map.Entry<String, Ability>> availableAbilities = allAbilities.entrySet().stream()
                .filter(entry -> !activeAbilities.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        pageManager.getPlugin().debug("Number of available abilities: " + availableAbilities.size(), "ui");

        // Setup paging
        int startIndex = pageNumber * 45;
        int endIndex = Math.min(startIndex + 45, availableAbilities.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Ability> entry = availableAbilities.get(i);
            String abilityId = entry.getKey();
            Ability ability = entry.getValue();

            // Create display item for ability
            ItemStack displayItem = createAbilityDisplayItem(abilityId, ability);
            inventory.setItem(slot, displayItem);
            slot++;
        }

        // Add navigation buttons
        if (pageNumber > 0) {
            addNavigationButton(45, false);
        }

        if ((pageNumber + 1) * 45 < availableAbilities.size()) {
            addNavigationButton(53, true);
        }

        // Info button showing ability selection mode
        ItemStack infoButton = createGuiItem(
                Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + "Ability Selection Mode",
                ChatColor.GRAY + "Select abilities for your mob",
                ChatColor.GRAY + "Already active abilities are filtered out"
        );
        inventory.setItem(47, infoButton);

        // Back button
        addBackButton(49, ChatColor.RED + "Back");
    }

    private ItemStack createAbilityDisplayItem(String abilityId, Ability ability) {
        // Use different materials based on ability type/name for visual variety
        Material iconMaterial = ability.getMaterial();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Ability ID: " + ChatColor.WHITE + abilityId);

        // Add ability description if available
        if (ability.getDescription() != null && !ability.getDescription().isEmpty()) {
            lore.add(ChatColor.GRAY + "Description:");
            lore.add(ChatColor.YELLOW + ability.getDescription());
        }

        // Add ability status if available
        if (ability.getStatus() != null && !ability.getStatus().isEmpty()) {
            lore.add(ChatColor.WHITE + "=== Config ===");
            for (String status : ability.getStatus()) {
                lore.add(ChatColor.GRAY + status);
            }
        }


        lore.add("");
        lore.add(ChatColor.GREEN + "Click to select this ability");

        return createGuiItem(
                iconMaterial,
                ChatColor.LIGHT_PURPLE + ability.getTitle(),
                lore
        );
    }

    private List<String> getCustomItems() {
        List<String> customItems = new ArrayList<>();

        // Get custom weapons
        Map<String, ItemStack> weapons = pageManager.getPlugin().getEquipmentManager().getWeapons();
        if (weapons != null) {
            customItems.addAll(weapons.keySet());
        }

        // Get custom armor
        Map<String, ItemStack> armor = pageManager.getPlugin().getEquipmentManager().getArmor();
        if (armor != null) {
            customItems.addAll(armor.keySet());
        }

        // Get custom unique items
        Map<String, ItemStack> uniques = pageManager.getPlugin().getEquipmentManager().getUniques();
        if (uniques != null) {
            customItems.addAll(uniques.keySet());
        }

        return customItems;
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        if (slot < 45 && inventory.getItem(slot) != null) {
            if (selectionMode == SelectionMode.ABILITIES) {
                return handleAbilitySelection(player, slot, clickType);
            } else {
                return handleItemSelection(player, slot, clickType);
            }
        } else if (slot == 45 && pageNumber > 0) {
            // Previous page
            pageNumber--;
            build();
        } else if (slot == 53 && inventory.getItem(53) != null) {
            // Next page
            pageNumber++;
            build();
        } else if (slot == 47) {
            if (selectionMode == SelectionMode.ITEMS) {
                // Toggle between vanilla and custom items
                showingCustomItems = !showingCustomItems;
                pageNumber = 0; // Reset to first page when switching views
                build();
            }
            // For abilities, slot 47 is just an info button - no action needed
        } else if (slot == 49) {
            this.guiManager.getCurrentPlayer().setCancelAction(true);
            pageManager.navigateBack(player);
        }
        return true;
    }

    private boolean handleItemSelection(Player player, int slot, ClickType clickType) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PlayerSessionData session = guiManager.getCurrentPlayer();
        String itemId = "";
        ItemStack clickedItem = inventory.getItem(slot);

        if (showingCustomItems) {
            try {
                PersistentDataContainer dataContainer = clickedItem.getItemMeta().getPersistentDataContainer();
                NamespacedKey key = new NamespacedKey(PowerMobsPlugin.getInstance(), "custom-id");
                itemId = dataContainer.get(key, PersistentDataType.STRING);
            } catch (Exception e) {
                plugin.debug("Error getting custom item ID PersistentDataContainer: " + e.getMessage(), "ui");
            }
            if (itemId != null) {
                // Store the selected custom item
                session.setSelectedItemType("custom");
            }
        } else {
            // For vanilla items, use the material directly
            Material material = clickedItem.getType();
            itemId = material.name();
            session.setSelectedItemType("vanilla");
        }

        plugin.debug("Selected item ID: " + itemId, "ui");
        session.setSelectedItemId(itemId);
        session.setSelectedItem(clickedItem.clone());
        session.getActiveItems().clear();
        session.setEditing(true);
        if (isEquipmentSlot(session.getItemEditType())) {
            plugin.debug("Equipment selection complete, navigating to MobEquipmentItemSettingsPage", "ui");
            pageManager.navigateBack(player);
        } else {
            pageManager.navigateBack(player);
        }
        return true;
    }

    private boolean handleAbilitySelection(Player player, int slot, ClickType clickType) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PlayerSessionData session = guiManager.getCurrentPlayer();

        // Get all available abilities (filtered)
        Map<String, Ability> allAbilities = plugin.getAbilityManager().getAbilities();
        Set<String> activeAbilities = session.getActiveItems();
        List<String> availableAbilityIds = allAbilities.keySet().stream()
                .filter(id -> !activeAbilities.contains(id))
                .sorted()
                .toList();

        // Calculate which ability was clicked
        int abilityIndex = pageNumber * 45 + slot;
        if (abilityIndex >= availableAbilityIds.size()) {
            return false;
        }

        String selectedAbilityId = availableAbilityIds.get(abilityIndex);
        Ability selectedAbility = allAbilities.get(selectedAbilityId);

        plugin.debug("Selected ability ID: " + selectedAbilityId, "ui");

        // Store the selected ability information in a way that BulkItemSelectionPage can handle
        session.setSelectedItemId(selectedAbilityId);
        session.setSelectedItemType("ability"); // New type for abilities

        // Create a display item for the ability (similar to how items are handled)
        ItemStack abilityDisplayItem = createAbilityDisplayItem(selectedAbilityId, selectedAbility);
        session.setSelectedItem(abilityDisplayItem);

        // Clear active items and mark as editing for consistency with item selection
        session.getActiveItems().clear();
        session.setEditing(true);

        plugin.debug("Ability selection complete, navigating back to bulk selection page", "ui");
        pageManager.navigateBack(player);
        return true;
    }

    private boolean isEquipmentSlot(EditingType type) {
        return type == EditingType.HELMET || type == EditingType.CHESTPLATE || type == EditingType.LEGGINGS ||
                type == EditingType.BOOTS || type == EditingType.MAINHAND || type == EditingType.OFFHAND;
    }

    public enum CustomItemType {
        CUSTOM,
        WEAPONS,
        ARMOR,
        UNIQUES
    }

    public enum SelectionMode {
        ITEMS,     // Original item selection mode
        ABILITIES  // New ability selection mode
    }
}
