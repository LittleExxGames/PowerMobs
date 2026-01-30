package com.powermobs.UI.pages;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.*;
import com.powermobs.config.EquipmentItemConfig;
import com.powermobs.config.IPowerMobConfig;
import com.powermobs.config.PowerMobConfig;
import com.powermobs.config.RandomMobConfig;
import com.powermobs.mobs.abilities.Ability;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.equipment.CustomDropConfig;
import com.powermobs.mobs.equipment.EnchantmentConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BulkItemSelectionPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private static final int ITEMS_PER_PAGE = 45; // Reserve 9 slots for navigation
    private final Map<String, ItemStack> availableItems = new LinkedHashMap<>();
    private final Map<String, ItemStack> originItems = new LinkedHashMap<>();
    private EditingType editingType = EditingType.NONE;
    private ItemFilterType itemSelectionType = ItemFilterType.VANILLA;
    private int pageNumber = 0;
    private String selectedMobId = null;
    private boolean unsavedChanges = false;
    private String delete = "";

    public BulkItemSelectionPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.BLUE + "Bulk Item Selection");
    }


    public void setItemFilterType(ItemFilterType itemSelectionType) {
        this.itemSelectionType = itemSelectionType;
        this.pageNumber = 0;
    }

    @Override
    public void build() {
        inventory.clear();
        PlayerSessionData player = guiManager.getCurrentPlayer();
        editingType = player.getItemEditType();
        updateInventoryTitle();
        pageManager.getPlugin().debug("Building GUI for Bulk Item selection list. Type: " + editingType + ", filter: " + itemSelectionType, "ui");

        selectedMobId = guiManager.getCurrentPlayer().getSelectedMobId();
        IPowerMobConfig mobConfig;
        if (selectedMobId != null) {
            mobConfig = pageManager.getPlugin().getConfigManager().getPowerMob(selectedMobId);
        } else {
            mobConfig = pageManager.getPlugin().getConfigManager().getRandomMobConfig();
        }
        if (!player.isEditing()) {
            loadCurrentList(mobConfig);
            player.setSelectedItemId(null);
            pageManager.getPlugin().debug("Retrieving list source data", "ui");
            delete = "";
            originItems.clear();
            originItems.putAll(availableItems);
            player.setEditing(true);
        }
        if (player.getSelectedItemId() != null) {
            addItem(player.getSelectedItemId(), player.getSelectedItemType());
            player.setSelectedItemId(null);
        }
        unsavedChanges = (!originItems.equals(availableItems));
        // Display current page items
        displayItems();

        // Add navigation controls
        addNavigationControls();

        // Add action buttons
        addActionButtons();
    }

    /**
     * Updates the inventory title based on the current editing type
     */
    private void updateInventoryTitle() {
        String newTitle = getPageTitle();
        updateInventoryTitle(newTitle); // Use the new method from AbstractGUIPage
    }

    /**
     * Gets the appropriate page title based on editing type
     */
    private String getPageTitle() {
        return switch (editingType) {
            case PREFIX -> ChatColor.BLUE + "Edit Name Prefixes";
            case SUFFIX -> ChatColor.BLUE + "Edit Name Suffixes";
            case MAINHAND -> ChatColor.BLUE + "Edit Weapons";
            case OFFHAND -> ChatColor.BLUE + "Edit Offhands";
            case HELMET -> ChatColor.BLUE + "Edit Helmets";
            case CHESTPLATE -> ChatColor.BLUE + "Edit Chestplates";
            case LEGGINGS -> ChatColor.BLUE + "Edit Leggings";
            case BOOTS -> ChatColor.BLUE + "Edit Boots";
            case ABILITY -> ChatColor.BLUE + "Edit Abilities";
            case DROPS_CONFIG -> ChatColor.BLUE + "Edit Item Drops";
            default -> ChatColor.BLUE + "Bulk Item Selection";
        };
    }

    private void loadCurrentList(IPowerMobConfig mobConfig) {
        availableItems.clear();
        Map<String, ItemStack> customItems = new LinkedHashMap<>(pageManager.getPlugin().getEquipmentManager().getAllEquipment());
        switch (editingType) {
            case PREFIX:
                for (String prefix : mobConfig.getNamePrefixes()) {
                    addTitleToCurrentList(prefix);
                }
                break;
            case SUFFIX:
                for (String suffix : mobConfig.getNameSuffixes()) {
                    addTitleToCurrentList(suffix);
                }
                break;
            case MAINHAND:
                for (EquipmentItemConfig availableItem : mobConfig.getPossibleEquipment().get("possible-weapons")) {
                    ItemStack displayItem = tryRetrieveItem(availableItem, customItems);
                    if (displayItem == null) continue;
                    addItemToCurrentList(availableItem.getItem(), displayItem);
                }
                break;
            case OFFHAND:
                for (EquipmentItemConfig availableItem : mobConfig.getPossibleEquipment().get("possible-offhands")) {
                    ItemStack displayItem = tryRetrieveItem(availableItem, customItems);
                    if (displayItem == null) continue;
                    addItemToCurrentList(availableItem.getItem(), displayItem);
                }
                break;
            case HELMET:
                for (EquipmentItemConfig availableItem : mobConfig.getPossibleEquipment().get("possible-helmets")) {
                    ItemStack displayItem = tryRetrieveItem(availableItem, customItems);
                    if (displayItem == null) continue;
                    addItemToCurrentList(availableItem.getItem(), displayItem);
                }
                break;
            case CHESTPLATE:
                for (EquipmentItemConfig availableItem : mobConfig.getPossibleEquipment().get("possible-chestplates")) {
                    ItemStack displayItem = tryRetrieveItem(availableItem, customItems);
                    if (displayItem == null) continue;
                    addItemToCurrentList(availableItem.getItem(), displayItem);
                }
                break;
            case LEGGINGS:
                for (EquipmentItemConfig availableItem : mobConfig.getPossibleEquipment().get("possible-leggings")) {
                    ItemStack displayItem = tryRetrieveItem(availableItem, customItems);
                    if (displayItem == null) continue;
                    addItemToCurrentList(availableItem.getItem(), displayItem);
                }
                break;
            case BOOTS:
                for (EquipmentItemConfig availableItem : mobConfig.getPossibleEquipment().get("possible-boots")) {
                    ItemStack displayItem = tryRetrieveItem(availableItem, customItems);
                    if (displayItem == null) continue;
                    addItemToCurrentList(availableItem.getItem(), displayItem);
                }
                break;
            case ABILITY:
                Map<String, Ability> allAbilities = pageManager.getPlugin().getAbilityManager().getAbilities();
                Map<String, Map<String, Object>> possibleAbilities = mobConfig.getPossibleAbilities();

                for (String abilityId : possibleAbilities.keySet()) {
                    Ability ability = allAbilities.get(abilityId);
                    if (ability == null) {
                        pageManager.getPlugin().getLogger().warning(
                                "Ability '" + abilityId + "' is not a valid ability. Please remove it from the config."
                        );
                        continue;
                    }
                    addAbility(abilityId, ability, possibleAbilities);
                }
                break;
            case DROPS_CONFIG: {
                for (CustomDropConfig drop : mobConfig.getDrops()) {
                    //Custom items
                    if (customItems.containsKey(drop.getItem())) {
                        ItemStack displayItem = customItems.get(drop.getItem()).clone();
                        ItemMeta meta = displayItem.getItemMeta();
                        List<String> lore = new ArrayList<>();
                        if (meta.hasLore()) {
                            lore.addAll(meta.getLore());
                        }
                        lore.add(ChatColor.GRAY + "Drop chance: " + drop.getChance());
                        lore.add(ChatColor.GRAY + "Drop amount: " + drop.getMinAmount() + " - " + drop.getMaxAmount());
                        lore.add(ChatColor.GRAY + "Drop weight: " + drop.getAmountWeight());
                        if (unsavedChanges) {
                            lore.add(ChatColor.GRAY + "Click to edit - " + ChatColor.RED + "UNSAVED DELETED DROPS CHANGES WILL BE LOST");
                        } else {
                            lore.add(ChatColor.GRAY + "Click to edit");
                        }
                        lore.add(ChatColor.YELLOW + "Right-click to delete");
                        if (delete.equals(drop.getItem())) {
                            lore.add(ChatColor.DARK_RED + "DELETE?");
                        }
                        meta.setLore(lore);
                        displayItem.setItemMeta(meta);
                        availableItems.put(drop.getItem(), displayItem);
                    } else {
                        //Vanilla items
                        try {
                            Material material;
                            if (drop.getItem().lastIndexOf("-") != -1) {
                                material = Material.valueOf(drop.getItem().substring(0, drop.getItem().lastIndexOf("-")).toUpperCase());
                            } else {
                                material = Material.valueOf(drop.getItem().toUpperCase());
                            }
                            ItemStack displayItem = new ItemStack(material);
                            ItemMeta meta = displayItem.getItemMeta();
                            meta.setDisplayName(ChatColor.GREEN + "(" + drop.getItem() + ") " + material.name());
                            List<String> lore = new ArrayList<>();
                            lore.add(ChatColor.GRAY + "Drop chance: " + drop.getChance());
                            lore.add(ChatColor.GRAY + "Drop amount: " + drop.getMinAmount() + " - " + drop.getMaxAmount());
                            lore.add(ChatColor.GRAY + "Drop weight: " + drop.getAmountWeight());
                            if (unsavedChanges) {
                                lore.add(ChatColor.GRAY + "Click to edit - " + ChatColor.RED + "UNSAVED DELETED DROPS CHANGES WILL BE LOST");
                            } else {
                                lore.add(ChatColor.GRAY + "Click to edit");
                            }
                            lore.add(ChatColor.YELLOW + "Right-click to delete");
                            if (delete.equals(drop.getItem())) {
                                lore.add(ChatColor.DARK_RED + "DELETE?");
                            }
                            meta.setLore(lore);
                            displayItem.setItemMeta(meta);
                            availableItems.put(drop.getItem(), displayItem);
                        } catch (IllegalArgumentException ex) {
                            Bukkit.getLogger().severe(drop.getItem() + " is not a valid item. Please remove it from being an item through the config or fix it.");
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private void addTitleToCurrentList(String value) {
        switch (editingType) {
            case PREFIX:
            case SUFFIX:
                ItemStack displayItem = new ItemStack(Material.PAPER);
                ItemMeta meta = displayItem.getItemMeta();
                meta.setDisplayName(value);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Right-click to remove");
                if (delete.equals(value)) {
                    lore.add(ChatColor.DARK_RED + "DELETE?");
                }
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
                availableItems.put(value, displayItem);
                break;
            default:
                break;
        }
    }

    private ItemStack tryRetrieveItem(EquipmentItemConfig itemConfig, Map<String, ItemStack> customItems) {
        String itemId = itemConfig.getItem();
        ItemStack displayItem =
                customItems.containsKey(itemId)
                        ? customItems.get(itemId).clone()
                        : null;

        if (displayItem == null) {
            String materialKey = itemId;
            int dash = materialKey.lastIndexOf("-");
            if (dash != -1) {
                materialKey = materialKey.substring(0, dash);
            }

            Material material = Material.getMaterial(materialKey.toUpperCase());
            if (material != null && material.isItem()) {
                displayItem = new ItemStack(material);
            }
        }
        if (displayItem == null) {
            Bukkit.getLogger().severe(itemId + " is not a valid item. Please remove it from being an item through the config or fix it.");
            return null;
        }
        ItemMeta meta = displayItem.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
        }
        for (EnchantmentConfig enchantment : itemConfig.getEnchantments()) {
            if (enchantment.getMinLevel() == enchantment.getMaxLevel()) {
                lore.add(ChatColor.GREEN + enchantment.getType() + " - Level: " + enchantment.getMinLevel());
            } else {
                lore.add(ChatColor.GREEN + enchantment.getType() + " - Range: " + enchantment.getMinLevel() + "-" + enchantment.getMaxLevel() + " - Weight: " + enchantment.getWeight());
            }
        }
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    private void addItemToCurrentList(String itemId, ItemStack item) {
        switch (editingType) {
            case MAINHAND:
            case OFFHAND:
            case HELMET:
            case CHESTPLATE:
            case LEGGINGS:
            case BOOTS:
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "(" + itemId + ") " + meta.getDisplayName());
                List<String> lore = new ArrayList<>();
                if (meta.hasLore()) {
                    lore.addAll(meta.getLore());
                }
                lore.add(ChatColor.YELLOW + "Right-click to remove");
                if (delete.equals(itemId)) {
                    lore.add(ChatColor.DARK_RED + "DELETE?");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
                availableItems.put(itemId, item);
                break;
            default:
                break;
        }
    }

    /**
     * Adds an item to the availableItems list
     */
    private void addItem(String itemId, String itemType) {
        ItemStack displayItem = null;
        if ("custom".equals(itemType)) {
            // Get custom item from equipment manager
            Map<String, ItemStack> customItems = pageManager.getPlugin().getEquipmentManager().getAllEquipment();
            if (customItems.containsKey(itemId)) {
                displayItem = customItems.get(itemId).clone();
            }
        } else if ("vanilla".equals(itemType)) {
            // Create vanilla item
            try {
                Material material = Material.valueOf(itemId.toUpperCase());
                displayItem = new ItemStack(material);
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.WHITE + formatMaterialName(material.name()));
                    displayItem.setItemMeta(meta);
                }
            } catch (IllegalArgumentException e) {
                pageManager.getPlugin().debug("Invalid material: " + itemId, "ui");
                return;
            }
        } else if ("ability".equals(itemType)) {
            IPowerMobConfig mobConfig;
            if (selectedMobId != null) {
                mobConfig = pageManager.getPlugin().getConfigManager().getPowerMob(selectedMobId);
            } else {
                mobConfig = pageManager.getPlugin().getConfigManager().getRandomMobConfig();
            }
            Ability ability = pageManager.getPlugin().getAbilityManager().getAbility(itemId);
            if (ability != null) {
                addAbility(itemId, ability, mobConfig.getPossibleAbilities());
            }
            return;
        }

        if (displayItem != null) {
            addItemToCurrentList(itemId, displayItem);
            pageManager.getPlugin().debug("Added equipment item: " + itemId + " (type: " + itemType + ")", "ui");
        }
    }

    private void displayItems() {
        int startIndex = pageNumber * ITEMS_PER_PAGE;
        int slot = 0;
        List<String> ids = new ArrayList<>(availableItems.keySet());
        for (int i = startIndex; i < Math.min(startIndex + ITEMS_PER_PAGE, availableItems.size()); i++) {
            String id = ids.get(i);
            ItemStack item = availableItems.get(id).clone();
            item = updateDelete(id, item);
            inventory.setItem(slot++, item);
        }
    }

    private void addAbility(String abilityId, Ability ability, Map<String, Map<String, Object>> possibleAbilities) {
        ItemStack displayItem = new ItemStack(ability.getMaterial());
        ItemMeta meta = displayItem.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + ability.getTitle());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Ability ID: " + ChatColor.WHITE + abilityId);
        lore.add(ChatColor.GRAY + "Description:");
        lore.add(ChatColor.YELLOW + ability.getDescription());

        List<String> resolvedStatus = getResolvedAbilityStatus(abilityId, ability, possibleAbilities);
        if (!resolvedStatus.isEmpty()) {
            lore.add(ChatColor.WHITE + "=== Config ===");
            for (String stat : resolvedStatus) {
                lore.add(ChatColor.GRAY + stat);
            }
        }
        // TODO Later add "Click to configure"

        lore.add(ChatColor.YELLOW + "Right-click to remove");
        if (delete.equals(abilityId)) {
            lore.add(ChatColor.DARK_RED + "DELETE?");
        }

        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        availableItems.put(abilityId, displayItem);
    }

    private List<String> getResolvedAbilityStatus(String abilityId, Ability ability, Map<String, Map<String, Object>> possibleAbilities) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        Map<String, Object> overrides = possibleAbilities.getOrDefault(abilityId, Map.of());

        Map<String, Object> resolved = new LinkedHashMap<>();

        if (ability != null && ability.getConfigSchema() != null) {
            for (var entry : ability.getConfigSchema().entrySet()) {
                AbilityConfigField field = entry.getValue();
                if (field != null) {
                    resolved.put(field.key(), field.defaultValue());
                }
            }
        }

        ConfigurationSection defaultsSection = plugin.getConfigManager()
                .getAbilitiesConfigManager()
                .getConfig()
                .getConfigurationSection("abilities." + abilityId);

        if (defaultsSection != null) {
            resolved.putAll(defaultsSection.getValues(true));
        }

        Map<String, Object> flattenedOverrides = new LinkedHashMap<>();
        flatten("", overrides, flattenedOverrides);
        resolved.putAll(flattenedOverrides);

        if (resolved.isEmpty()) {
            return List.of();
        }

        return resolved.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ": " + Objects.toString(e.getValue()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> source, Map<String, Object> out) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection section) {
                flatten(key, new LinkedHashMap<>(section.getValues(false)), out);
                continue;
            }
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> nested = new LinkedHashMap<>();
                for (Map.Entry<?, ?> nestedEntry : map.entrySet()) {
                    if (nestedEntry.getKey() != null) {
                        nested.put(String.valueOf(nestedEntry.getKey()), nestedEntry.getValue());
                    }
                }
                flatten(key, nested, out);
                continue;
            }

            out.put(key, value);
        }
    }

    private ItemStack updateDelete(String id, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (delete.equals(id)) {
            List<String> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.getLore());
            }
            lore.add(ChatColor.DARK_RED + "DELETE?");
            meta.setLore(lore);
        } else if (meta.hasLore()) {
            if (meta.getLore().get(meta.getLore().size() - 1).contains(ChatColor.DARK_RED + "DELETE?")) {
                meta.getLore().remove(meta.getLore().size() - 1);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private void addNavigationControls() {
        // Previous page button
        if (pageNumber > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Previous Page");
            prevPage.setItemMeta(meta);
            inventory.setItem(45, prevPage);
        }

        // Next page button
        int totalItems = availableItems.size();
        if ((pageNumber + 1) * ITEMS_PER_PAGE < totalItems) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Next Page");
            nextPage.setItemMeta(meta);
            inventory.setItem(53, nextPage);
        }

        // Page info
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta meta = pageInfo.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Page " + (pageNumber + 1));
        pageInfo.setItemMeta(meta);
        inventory.setItem(49, pageInfo);
    }

    private void addActionButtons() {
        // Save button
        ItemStack saveButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save the current configuration");
        inventory.setItem(46, saveButton);

        // Add new item button
        List<String> addNewInfo = new ArrayList<>();
        if (unsavedChanges && editingType == EditingType.DROPS_CONFIG) {
            addNewInfo.add(ChatColor.RED + "UNSAVED CHANGES WILL BE LOST");
        }
        ItemStack addButton = createGuiItem(Material.GREEN_CONCRETE,
                ChatColor.GREEN + "Add New", addNewInfo);
        inventory.setItem(49, addButton);

//        // Filter type button (for equipment)
//        if (editingType == EditingType.EQUIPMENT) {
//            ItemStack filterButton = new ItemStack(Material.HOPPER);
//            ItemMeta filterMeta = filterButton.getItemMeta();
//            filterMeta.setDisplayName(ChatColor.CYAN + "Filter: " + itemSelectionType.name());
//            filterButton.setItemMeta(filterMeta);
//            inventory.setItem(47, filterButton);
//        }

        // Back button
        addBackButton(52, ChatColor.RED + "Back", unsavedChanges);
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        if (slot >= 45) {
            return handleNavigationClick(player, slot, clickType);
        }
        return handleItemClick(player, slot, clickType);
    }

    private boolean handleNavigationClick(Player player, int slot, ClickType clickType) {
        switch (slot) {
            case 45: // Previous page
                if (pageNumber > 0) {
                    pageNumber--;
                    build();
                }
                return true;
            case 53: // Next page
                int totalItems = availableItems.size();
                if ((pageNumber + 1) * ITEMS_PER_PAGE < totalItems) {
                    pageNumber++;
                    build();
                }
                return true;
            case 46: // Save button
                if (unsavedChanges) {
                    saveConfig();
                    guiManager.getCurrentPlayer().setEditing(false);
                    unsavedChanges = false;
                    build();
                }
                return true;
            case 49: // Add new
                handleAddNew(player);
                return true;
//            case 47: // Filter (for equipment)
//                if (editingType == EditingType.EQUIPMENT) {
//                    cycleFilterType();
//                    build();
//                }
//                return true;
            case 52: // Back
                guiManager.getCurrentPlayer().setItemEditType(EditingType.NONE);
                delete = "";
                guiManager.getCurrentPlayer().setEditing(false);
                pageManager.navigateBack(player);
                return true;
        }
        return false;
    }

    private boolean handleItemClick(Player player, int slot, ClickType clickType) {
        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem == null) return false;

        PlayerSessionData sessionData = guiManager.getCurrentPlayer();
        int itemIndex = pageNumber * ITEMS_PER_PAGE + slot;
        List<String> itemIds = new ArrayList<>(availableItems.keySet());
        if (itemIndex >= itemIds.size()) return false;

        String id = itemIds.get(itemIndex);
        PowerMobsPlugin plugin = pageManager.getPlugin();
        plugin.debug("Selected Item: " + id + " at item index: " + itemIndex, "ui");

        if (clickType == ClickType.RIGHT) {
            if (id.equals(delete)) {
                availableItems.remove(id);
                plugin.debug("Deleted: " + id, "ui");
                delete = "";
                build();
            } else {
                delete = id;
                sessionData.setEditing(true);
                plugin.debug("Staged to delete: " + id, "ui");
                build();
            }
        } else {
            switch (editingType) {
                case PREFIX:
                case SUFFIX:
                    break;
                case MAINHAND:
                case OFFHAND:
                case HELMET:
                case CHESTPLATE:
                case LEGGINGS:
                case BOOTS:
                    if (clickType == ClickType.LEFT) {
                        sessionData.setItemEditType(editingType);
                        sessionData.setSelectedItem(inventory.getItem(slot));
                        sessionData.setEditing(false);
                        sessionData.setSelectedItemId(id);
                        pageManager.navigateTo("mob_equipment_item_settings", true, player);
                    }
                    break;
                case ABILITY:
                    if (clickType == ClickType.LEFT) {
                        sessionData.setSelectedItemId(id);
                        sessionData.setEditing(false);
                        pageManager.navigateTo("ability_config", true, player);
                    }
                    break;
                case DROPS_CONFIG:
                    if (clickType == ClickType.LEFT) {
                        sessionData.setEditing(false);
                        sessionData.setSelectedItemId(id);
                        pageManager.navigateTo("item_drop_config", true, player);
                    }
                    break;
                default:
                    break;
            }
        }

        return true;
    }

    private void handleAddNew(Player player) {
        PlayerSessionData session = guiManager.getCurrentPlayer();
        switch (editingType) {
            case PREFIX:
            case SUFFIX:
                startChatInput(player, ChatInputType.STRING_NAMES, (value, p) -> {
                    addTitleToCurrentList((String) value);
                    pageManager.navigateTo("bulk_item_selection", false, player);
                });
                break;
            case MAINHAND:
            case OFFHAND:
            case HELMET:
            case CHESTPLATE:
            case LEGGINGS:
            case BOOTS:
                session.setActiveItems(new LinkedHashSet<>(availableItems.keySet()));
                session.setItemEditType(editingType);
                session.setSelectedItem(null);
                session.setSelectedItemId(null);
                session.setSelectedItemType(null);
                session.setEditing(true);
                pageManager.navigateTo("mob_equipment_item_settings", true, player);
                break;
            case ABILITY:
                session.setActiveItems(new LinkedHashSet<>(availableItems.keySet()));
                session.setItemEditType(EditingType.ABILITY);
                pageManager.navigateTo("item_selection", true, player);
                break;
            case DROPS_CONFIG:
                session.setEditing(true);
                session.setActiveItems(new LinkedHashSet<>(availableItems.keySet()));
                pageManager.navigateTo("item_drop_config", true, player);
                break;
            default:
                break;
        }

    }

//    private void cycleFilterType() {
//        ItemFilterType[] types = ItemFilterType.values();
//        int currentIndex = Arrays.asList(types).indexOf(itemSelectionType);
//        itemSelectionType = types[(currentIndex + 1) % types.length];
//    }


    private void saveConfig() {
        IPowerMobConfig mob;
        PlayerSessionData session = guiManager.getCurrentPlayer();
        if (session.getType().equals(PowerMobType.RANDOM)) {
            mob = pageManager.getPlugin().getConfigManager().getRandomMobConfig();
        } else {
            mob = pageManager.getPlugin().getConfigManager().getPowerMob(selectedMobId);
        }
        switch (editingType) {
            case PREFIX:
                mob.getNamePrefixes().clear();
                mob.getNamePrefixes().addAll(availableItems.keySet());
                break;
            case SUFFIX:
                mob.getNameSuffixes().clear();
                mob.getNameSuffixes().addAll(availableItems.keySet());
                break;
            case MAINHAND:
                mob.getPossibleEquipment().get("possible-weapons").removeIf(item -> !availableItems.containsKey(item.getItem()));
                break;
            case OFFHAND:
                mob.getPossibleEquipment().get("possible-offhands").removeIf(item -> !availableItems.containsKey(item.getItem()));
                break;
            case HELMET:
                mob.getPossibleEquipment().get("possible-helmets").removeIf(item -> !availableItems.containsKey(item.getItem()));
                break;
            case CHESTPLATE:
                mob.getPossibleEquipment().get("possible-chestplates").removeIf(item -> !availableItems.containsKey(item.getItem()));
                break;
            case LEGGINGS:
                mob.getPossibleEquipment().get("possible-leggings").removeIf(item -> !availableItems.containsKey(item.getItem()));
                break;
            case BOOTS:
                mob.getPossibleEquipment().get("possible-boots").removeIf(item -> !availableItems.containsKey(item.getItem()));
                break;
            case ABILITY:
                Map<String, Map<String, Object>> possibleAbilities = mob.getPossibleAbilities();
                possibleAbilities.keySet().removeIf(abilityId -> !availableItems.containsKey(abilityId));

                for (String abilityId : availableItems.keySet()) {
                    possibleAbilities.computeIfAbsent(abilityId, k -> new LinkedHashMap<>());
                }
                break;
            case DROPS_CONFIG:
                mob.getDrops().removeIf(drop -> !availableItems.containsKey(drop.getItem()));
                break;
            default:
                break;
        }
        if (session.getType().equals(PowerMobType.RANDOM)) {
            pageManager.getPlugin().getConfigManager().saveRandomMob(mob.toConfigMap());
        } else {
            pageManager.getPlugin().getConfigManager().savePowerMob(selectedMobId, mob.toConfigMap());
        }
    }
}
