package com.powermobs.UI.pages;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.EditingType;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.config.PowerMobConfig;
import com.powermobs.mobs.equipment.CustomDropConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DropSettingsPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private final List<String> deletedIds = new ArrayList<>();
    private String selectedMobId;
    private int minDropCount = 0;
    private int maxDropCount = 0;
    private int dropWeight = 100;
    private double experienceChance = 0.0;
    private int minExperienceAmount = 0;
    private int maxExperienceAmount = 0;
    private int experienceWeight = 100;
    private Map<String, ItemStack> drops = new HashMap<>();
    private boolean deleting;
    private String deleteId = "";

    public DropSettingsPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.YELLOW + "Drop Settings");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Drop Settings.", "ui");

        PowerMobsPlugin plugin = pageManager.getPlugin();
        PlayerSessionData session = guiManager.getCurrentPlayer();
        selectedMobId = session.getSelectedMobId();
        PowerMobConfig mobConfig = plugin.getConfigManager().getPowerMob(selectedMobId);
        if (mobConfig == null) {
            plugin.debug("Selected mob ID is null. Returning to last page. Mod ID" + selectedMobId + " is invalid.", "ui");
            pageManager.navigateBack();
            return;
        }
        if (!session.isEditing()) {
            minDropCount = mobConfig.getMinDrops();
            maxDropCount = mobConfig.getMaxDrops();
            dropWeight = mobConfig.getDropWeight();
            experienceChance = mobConfig.getExperienceChance();
            minExperienceAmount = mobConfig.getExperienceMinAmount();
            maxExperienceAmount = mobConfig.getExperienceMaxAmount();
            experienceWeight = mobConfig.getExperienceWeight();
            deletedIds.clear();
            deleting = false;
            deleteId = "";
            session.setEditing(true);
        }
        boolean unsavedChanges = !matchesOrigin();

        // Drop range Settings
        ItemStack dropsRangeItem = createGuiItem(Material.HOPPER,
                ChatColor.AQUA + "Min to Max drops: " + minDropCount + " - " + maxDropCount,
                ChatColor.GOLD + "Drop Weight: " + dropWeight,
                ChatColor.GRAY + "Click to modify the range and weight");
        inventory.setItem(4, dropsRangeItem);

        // Info
        ItemStack info = createGuiItem(Material.PAPER,
                ChatColor.GOLD + "Info",
                ChatColor.GOLD + "Double right click to delete a drop",
                ChatColor.GOLD + "Left click to edit a drop",
                ChatColor.GRAY + "Saved changes only apply to deleted drops and drop count changes",
                ChatColor.GRAY + "Modified or added items are saved in the item modification screen",
                ChatColor.GRAY + "Changing pages reverts unsaved changes"
        );
        inventory.setItem(13, info);

        // Current Drops List (starts at row 3)
        List<String> ids;
        ids = mobConfig.getDrops().stream()
                .map(CustomDropConfig::getItem)
                .collect(Collectors.toList());
        drops = getDisplayItems(ids, plugin.getEquipmentManager().getAllEquipment());

        int startSlot = 27;
        int maxDisplayed = 9; // One row of drops
        // Display existing drops (if any)
        int index = 0; // Track the inventory slot position

        for (Map.Entry<String, ItemStack> entry : drops.entrySet()) {
            if (index >= maxDisplayed) break;
            ItemStack displayItem = drops.get(entry.getKey()).clone();
            CustomDropConfig drop = mobConfig.getDrop(entry.getKey());
            ItemMeta meta = displayItem.getItemMeta();
            List<String> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.getLore());
            }
            lore.add(ChatColor.GRAY + "Drop chance: " + drop.getChance());
            lore.add(ChatColor.GRAY + "Drop amount: " + drop.getMinAmount() + " - " + drop.getMaxAmount());
            lore.add(ChatColor.GRAY + "Drop weight: " + drop.getAmountWeight());
            if (unsavedChanges) {
                lore.add(ChatColor.GRAY + "Click to edit - " + ChatColor.RED + "UNSAVED DROP CHANGES WILL BE LOST");
            } else {
                lore.add(ChatColor.GRAY + "Click to edit");
            }
            lore.add(ChatColor.GRAY + "Right-click to delete");
            if (deleteId.equals(entry.getKey())) {
                lore.add(ChatColor.DARK_RED + "DELETE?");
            }
            meta.setLore(lore);
            displayItem.setItemMeta(meta);

            inventory.setItem(startSlot + index, displayItem);
            index++;
        }

        // Experience Chance
        ItemStack experienceChanceItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience",
                ChatColor.GOLD + "Chance: " + experienceChance
        );
        inventory.setItem(39, experienceChanceItem);

        // Experience amount
        ItemStack experienceAmountItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience",
                ChatColor.GOLD + "Amount: " + minExperienceAmount + " - " + maxExperienceAmount
        );
        inventory.setItem(40, experienceAmountItem);

        // Experience weight
        ItemStack experienceWeightItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience",
                ChatColor.GOLD + "Weight: " + experienceWeight
        );
        inventory.setItem(41, experienceWeightItem);

        // Create New Drop Button
        ItemStack createNewItem = createGuiItem(Material.DROPPER,
                ChatColor.GREEN + "Create New Drop",
                ChatColor.GRAY + "Click to add a new item drop");
        inventory.setItem(49, createNewItem);

        // Save Button
        ItemStack saveButton = createGuiItem(
                Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save the current configuration",
                ChatColor.GRAY + "(Saves drop count changes and deleted drops)"
        );
        inventory.setItem(45, saveButton);

        // Back button
        addBackButton(53, ChatColor.RED + "Back to Mob Editor", unsavedChanges);
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        if (slot == 4) {
            // Configure min and max drops
            player.closeInventory();
            startChatInput(player, ChatInputType.DROPS_CONFIG, (value, p) -> {
                updateDropSettings(value);
                pageManager.navigateTo("drop_settings", false, p);
            });
        } else if (slot == 53) {
            // Back to mob editor
            resetDropSettings();
            pageManager.navigateBack(player);
        } else if (slot == 45) {
            saveDropSettings(player);
            build();
        } else if (slot == 39) { // Experience chance
            startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                experienceChance = (Double) value;
                pageManager.navigateTo("drop_settings", false, p);
            });
        } else if (slot == 40) { // Experience amount
            startChatInput(player, ChatInputType.DROP_AMOUNT, (value, p) -> {
                updateExperienceAmount(value);
                pageManager.navigateTo("drop_settings", false, p);
            });
        } else if (slot == 41) { // Experience weight
            startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                experienceWeight = (Integer) value;
                pageManager.navigateTo("drop_settings", false, p);
            });
        } else if (slot == 49) {

            plugin.getGuiManager().getCurrentPlayer().setEditing(true);
            plugin.getGuiManager().getCurrentPlayer().setItemEditType(EditingType.DROP);

            pageManager.navigateTo("item_drop_config", true, player);
        } else if (slot >= 27 && slot < 36) {
            // A drop was clicked - edit this drop
            if (inventory.getItem(slot) == null) return true;
            int index = slot - 27;
            // Convert the Map keys into a list to access by index
            List<String> dropIds = new ArrayList<>(drops.keySet());

            if (index < dropIds.size()) {
                String dropId = dropIds.get(index); // Retrieve the drop ID
                if (clickType == ClickType.LEFT) {
                    if (deleting) {
                        deleting = false;
                        deleteId = "";
                        build();
                    } else {
                        plugin.debug("Drop with drop ID of " + dropId + " is a " + plugin.getGuiManager().getCurrentPlayer().getSelectedItemType() + " item.", "ui");
                        plugin.getGuiManager().getCurrentPlayer().setSelectedItemId(dropId);
                        plugin.getGuiManager().getCurrentPlayer().setEditing(false);
                        plugin.getGuiManager().getCurrentPlayer().setItemEditType(EditingType.DROP);
                        plugin.getGuiManager().getCurrentPlayer().setSelectedItem(inventory.getItem(slot).clone());
                        resetDropSettings();

                        pageManager.navigateTo("item_drop_config", true, player);
                    }
                } else {
                    if (deleting) {
                        drops.remove(dropId);
                        deletedIds.add(dropId);
                        deleteId = "";
                        deleting = false;
                        build();
                    } else {
                        deleting = true;
                        deleteId = dropId;
                        build();
                    }
                }
            }
        }
        return true;
    }

    private Map<String, ItemStack> getDisplayItems(List<String> items, Map<String, ItemStack> everyItem) {
        Map<String, ItemStack> displayItems = new HashMap<>();
        for (String item : items) {
            //Custom items
            if (deletedIds.contains(item)) continue;
            if (everyItem.containsKey(item)) {
                displayItems.put(item, everyItem.get(item));
            } else {
                //Vanilla items
                try {
                    Material material;
                    if (item.lastIndexOf("-") != -1) {
                        material = Material.valueOf(item.substring(0, item.lastIndexOf("-")).toUpperCase());
                    } else {
                        material = Material.valueOf(item.toUpperCase());
                    }
                    displayItems.put(item, new ItemStack(material));
                } catch (IllegalArgumentException ex) {
                    Bukkit.getLogger().severe(item + " is not a valid item. Please remove it from being an item through the config or fix it.");
                }
            }
        }
        return displayItems;
    }

    private void updateDropSettings(Object value) {
        String settings = (String) value;
        String[] parts = settings.split(":");
        String[] range = parts[0].split("-");
        int dropMin;
        int dropMax;
        int dropWeight = 100;
        if (range.length == 2) {
            dropMin = Integer.parseInt(range[0].trim());
            dropMax = Integer.parseInt(range[1].trim());
        } else {
            dropMin = Integer.parseInt(range[0].trim());
            dropMax = dropMin;
        }
        if (parts.length == 2) {
            dropWeight = Integer.parseInt(parts[1].trim());
        }
        minDropCount = dropMin;
        maxDropCount = dropMax;
        this.dropWeight = dropWeight;
    }

    private void updateExperienceAmount(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            minExperienceAmount = min;
            maxExperienceAmount = max;
        } else if (value instanceof Integer) {
            // Single value
            int amount = (Integer) value;
            minExperienceAmount = amount;
            maxExperienceAmount = amount;
        }
    }

    private void resetDropSettings() {
        minDropCount = 0;
        maxDropCount = 0;
        dropWeight = 100;
        experienceChance = 0.0;
        minExperienceAmount = 0;
        maxExperienceAmount = 0;
        experienceWeight = 100;
        deleting = false;
        deleteId = null;
        deletedIds.clear();
        drops.clear();
        guiManager.getCurrentPlayer().setEditing(false);
    }

    private boolean matchesOrigin() {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PowerMobConfig mobConfig = plugin.getConfigManager().getPowerMob(selectedMobId);
        return minDropCount == mobConfig.getMinDrops() &&
                maxDropCount == mobConfig.getMaxDrops() &&
                dropWeight == mobConfig.getDropWeight() &&
                Double.compare(experienceChance, mobConfig.getExperienceChance()) == 0 &&
                minExperienceAmount == mobConfig.getExperienceMinAmount() &&
                maxExperienceAmount == mobConfig.getExperienceMaxAmount() &&
                experienceWeight == mobConfig.getExperienceWeight() &&
                deletedIds.isEmpty();
    }

    private void saveDropSettings(Player player) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PowerMobConfig mob = new PowerMobConfig(plugin.getConfigManager().getPowerMob(selectedMobId));
        mob.setMinDrops(minDropCount);
        mob.setMaxDrops(maxDropCount);
        mob.setDropWeight(dropWeight);
        mob.setExperienceChance(experienceChance);
        mob.setExperienceMinAmount(minExperienceAmount);
        mob.setExperienceMaxAmount(maxExperienceAmount);
        mob.setExperienceWeight(experienceWeight);
        for (String deletedId : deletedIds) {
            mob.getDrops().remove(mob.getDrop(deletedId));
        }
        boolean saved = plugin.getConfigManager().savePowerMob(selectedMobId, mob.toConfigMap());
        if (!saved) {
            return;
        }
        guiManager.getCurrentPlayer().setEditing(false);
        resetDropSettings();
        player.sendMessage(ChatColor.GREEN + "Drop settings have been saved.");
    }
}
