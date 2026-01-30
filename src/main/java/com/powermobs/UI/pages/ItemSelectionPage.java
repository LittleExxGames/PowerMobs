package com.powermobs.UI.pages;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.framework.*;
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
    private CustomItemType selectedItemType = CustomItemType.CUSTOM;
    private int pageNumber = 0;
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

            // Get custom items from config/manager
            if (selectedItemType.getGroup() == FilterGroup.CUSTOM)
            {
                items = new ArrayList<>(getCustomItems(selectedItemType));
            } else {
                // Get vanilla materials
                items = Arrays.stream(Material.values())
                        .filter(material -> material.isItem() && !material.isAir())
                        .filter(material -> switch (selectedItemType) {
                            case VANILLA_WEAPONS -> ItemCategories.VANILLA_WEAPONS.contains(material);
                            case VANILLA_ARMOR -> ItemCategories.VANILLA_ARMOR.contains(material);
                            case VANILLA_OTHER -> !ItemCategories.VANILLA_WEAPONS.contains(material)
                                    && !ItemCategories.VANILLA_ARMOR.contains(material);
                            case VANILLA -> true; // already filtered by isItem + !isAir

                            default -> true; // fallback for other filter types
                        })
                        .sorted(Comparator.comparing(Enum::name))
                        .collect(Collectors.toList());
            }
        // Setup paging if needed
        int startIndex = pageNumber * 45;
        int endIndex = Math.min(startIndex + 45, items.size());

        int slot = 0;
        if (selectedItemType.getGroup() == FilterGroup.CUSTOM) {
            Set<String> active = guiManager.getCurrentPlayer().getActiveItems();
            Map<String, ItemStack> allEquipment = pageManager.getPlugin().getEquipmentManager().getAllEquipment();

            List<String> filteredIds = items.stream()
                    .map(o -> (String) o)
                    .filter(id -> !active.contains(id))
                    .sorted()
                    .toList();

            pageManager.getPlugin().debug("Number of custom items found (filtered): " + filteredIds.size(), "ui");

            endIndex = Math.min(endIndex, filteredIds.size());
            for (int i = startIndex; i < endIndex; i++) {
                String customItemId = filteredIds.get(i);
                ItemStack base = allEquipment.get(customItemId);
                if (base == null) continue;

                ItemStack customItem = base.clone();
                ItemMeta meta = customItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + "(" + customItemId + ")" + ChatColor.BLUE + " " + meta.getDisplayName());
                    customItem.setItemMeta(meta);
                }

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

        String filterText;
        Material filterMaterial;
        switch (selectedItemType) {
            case VANILLA:
                filterText = "Vanilla Items";
                filterMaterial = Material.CRAFTING_TABLE;
                break;
            case VANILLA_ARMOR:
                filterText = "Vanilla Armor";
                filterMaterial = Material.NETHERITE_CHESTPLATE;
                break;
            case VANILLA_WEAPONS:
                filterText = "Vanilla Weapons";
                filterMaterial = Material.NETHERITE_SWORD;
                break;
            case VANILLA_OTHER:
                filterText = "Vanilla Other";
                filterMaterial = Material.BUNDLE;
                break;
            case CUSTOM:
                filterText = "Custom Items";
                filterMaterial = Material.NETHER_STAR;
                break;
            case ARMOR:
                filterText = "Custom Armor";
                filterMaterial = Material.TURTLE_HELMET;
                break;
            case WEAPONS:
                filterText = "Custom Weapons";
                filterMaterial = Material.TRIDENT;
                break;
            case UNIQUES:
                filterText = "Custom Uniques";
                filterMaterial = Material.DRAGON_BREATH;
                break;
            default:
                filterText = "Unknown";
                filterMaterial = Material.BARRIER;
        }

        // Toggle custom/vanilla items button
        ItemStack toggleButton = createGuiItem(filterMaterial, ChatColor.YELLOW + "Showing " + filterText, ChatColor.GRAY + "Click to toggle filter type");

        inventory.setItem(47, toggleButton);

        addBackButton(49, ChatColor.RED + "Back to Main Menu");

        inventory.setItem(51, guiManager.getCurrentPlayer().getSelectedItem());
    }

    private void buildAbilitySelection() {
        Map<String, Ability> allAbilities = pageManager.getPlugin().getAbilityManager().getAbilities();

        // Filter out abilities that are already active for this mob
        Set<String> activeAbilities = guiManager.getCurrentPlayer().getActiveItems();
        List<Map.Entry<String, Ability>> availableAbilities = allAbilities.entrySet().stream()
                .filter(entry -> !activeAbilities.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        pageManager.getPlugin().debug("Number of available abilities: " + availableAbilities.size(), "ui");

        int startIndex = pageNumber * 45;
        int endIndex = Math.min(startIndex + 45, availableAbilities.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Ability> entry = availableAbilities.get(i);
            String abilityId = entry.getKey();
            Ability ability = entry.getValue();

            ItemStack displayItem = createAbilityDisplayItem(abilityId, ability);
            inventory.setItem(slot, displayItem);
            slot++;
        }

        if (pageNumber > 0) {
            addNavigationButton(45, false);
        }

        if ((pageNumber + 1) * 45 < availableAbilities.size()) {
            addNavigationButton(53, true);
        }

        ItemStack infoButton = createGuiItem(
                Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + "Ability Selection Mode",
                ChatColor.GRAY + "Select abilities for your mob",
                ChatColor.GRAY + "Already active abilities are filtered out"
        );
        inventory.setItem(47, infoButton);

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

    private List<String> getCustomItems(CustomItemType type) {
        List<String> customItems = new ArrayList<>();

        Map<String, ItemStack> weapons = pageManager.getPlugin().getEquipmentManager().getWeapons();
        Map<String, ItemStack> armor = pageManager.getPlugin().getEquipmentManager().getArmor();
        Map<String, ItemStack> uniques = pageManager.getPlugin().getEquipmentManager().getUniques();

        switch (type) {
            case WEAPONS:
                if (weapons != null) customItems.addAll(weapons.keySet());
                break;
            case ARMOR:
                if (armor != null) customItems.addAll(armor.keySet());
                break;
            case UNIQUES:
                if (uniques != null) customItems.addAll(uniques.keySet());
                break;
            case CUSTOM:
            default:
                if (weapons != null) customItems.addAll(weapons.keySet());
                if (armor != null) customItems.addAll(armor.keySet());
                if (uniques != null) customItems.addAll(uniques.keySet());
                break;
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
            pageNumber--;
            build();
        } else if (slot == 53 && inventory.getItem(53) != null) {
            pageNumber++;
            build();
        } else if (slot == 47) {
            if (selectionMode == SelectionMode.ITEMS) {
                selectedItemType = selectedItemType.next();
                pageNumber = 0; // Reset to first page when switching views
                build();
            }
            // For abilities, slot 47 is just an info button - no action needed
        } else if (slot == 49) {
            PlayerSessionData session = guiManager.getCurrentPlayer();

            if (session.getSelectedItemId() == null){
                session.setCancelAction(true);
            }
            pageManager.navigateBack(player);
        }
        return true;
    }

    private boolean handleItemSelection(Player player, int slot, ClickType clickType) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PlayerSessionData session = guiManager.getCurrentPlayer();
        String itemId = "";
        ItemStack clickedItem = inventory.getItem(slot);

        if (selectedItemType.getGroup() == FilterGroup.CUSTOM) {
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
        session.setCancelAction(false);
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

        Map<String, Ability> allAbilities = plugin.getAbilityManager().getAbilities();
        Set<String> activeAbilities = session.getActiveItems();
        List<String> availableAbilityIds = allAbilities.keySet().stream()
                .filter(id -> !activeAbilities.contains(id))
                .sorted()
                .toList();

        int abilityIndex = pageNumber * 45 + slot;
        if (abilityIndex >= availableAbilityIds.size()) {
            return false;
        }

        String selectedAbilityId = availableAbilityIds.get(abilityIndex);
        plugin.debug("Selected ability ID: " + selectedAbilityId, "ui");

        // Store selection for AbilityConfigGUIPage
        session.setSelectedItemId(selectedAbilityId);
        session.setSelectedItemType("ability");

        session.setEditing(false);

        pageManager.navigateTo("ability_config", false, player);
        return true;
    }

    private boolean isEquipmentSlot(EditingType type) {
        return type == EditingType.HELMET || type == EditingType.CHESTPLATE || type == EditingType.LEGGINGS ||
                type == EditingType.BOOTS || type == EditingType.MAINHAND || type == EditingType.OFFHAND;
    }

    public enum CustomItemType {
        VANILLA(FilterGroup.VANILLA),
        VANILLA_ARMOR(FilterGroup.VANILLA),
        VANILLA_WEAPONS(FilterGroup.VANILLA),
        VANILLA_OTHER(FilterGroup.VANILLA),

        CUSTOM(FilterGroup.CUSTOM),
        ARMOR(FilterGroup.CUSTOM),
        WEAPONS(FilterGroup.CUSTOM),
        UNIQUES(FilterGroup.CUSTOM);

        private final FilterGroup group;

        CustomItemType(FilterGroup group) {
            this.group = group;
        }
        public FilterGroup getGroup() { return group; }

        public CustomItemType next() { CustomItemType[] values = CustomItemType.values(); return values[(this.ordinal() + 1) % values.length]; }
    }

    public enum FilterGroup {
        VANILLA,
        CUSTOM
    }

    public enum SelectionMode {
        ITEMS, 
        ABILITIES 
    }
}
