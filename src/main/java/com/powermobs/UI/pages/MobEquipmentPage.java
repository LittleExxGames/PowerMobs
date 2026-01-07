package com.powermobs.UI.pages;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.EditingType;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.config.EquipmentItemConfig;
import com.powermobs.config.PowerMobConfig;
import com.powermobs.mobs.equipment.EnchantmentConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Page for managing mob equipment
 */
public class MobEquipmentPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private String selectedMobId;
    private Map<String, List<EquipmentItemConfig>> mobEquipment;
    private Map<String, List<EquipmentItemConfig>> originEquipment;

    public MobEquipmentPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.BLUE + "Mob Equipment");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Mob Equipment.", "ui");

        PowerMobsPlugin plugin = pageManager.getPlugin();
        PlayerSessionData session = guiManager.getCurrentPlayer();
        selectedMobId = session.getSelectedMobId();
        PowerMobConfig mobConfig = plugin.getConfigManager().getPowerMob(selectedMobId);

        if (mobConfig == null) {
            pageManager.navigateBack();
            return;
        }

        Map<String, List<EquipmentItemConfig>> latestSaved =
                new HashMap<>(mobConfig.getPossibleEquipment());

        if (originEquipment == null || !originEquipment.equals(latestSaved)) {
            originEquipment = latestSaved;
        }
        if (mobEquipment == null || !originEquipment.equals(latestSaved)) {
            mobEquipment = new HashMap<>(originEquipment);
        }

        session.setItemEditType(EditingType.EQUIPMENT);
        boolean unsavedChanges = !mobEquipment.equals(originEquipment);

        // Equipment slots display
        // Helmet slot
        ItemStack helmetDisplay = createEquipmentSlot("helmet", Material.IRON_HELMET,
                getFirstEquipment("possible-helmets"), "Head/Helmet");
        inventory.setItem(13, helmetDisplay);

        // Chestplate slot
        ItemStack chestDisplay = createEquipmentSlot("chestplate", Material.IRON_CHESTPLATE,
                getFirstEquipment("possible-chestplates"), "Chest/Chestplate");
        inventory.setItem(22, chestDisplay);

        // Leggings slot
        ItemStack leggingsDisplay = createEquipmentSlot("leggings", Material.IRON_LEGGINGS,
                getFirstEquipment("possible-leggings"), "Legs/Leggings");
        inventory.setItem(31, leggingsDisplay);

        // Boots slot
        ItemStack bootsDisplay = createEquipmentSlot("boots", Material.IRON_BOOTS,
                getFirstEquipment("possible-boots"), "Feet/Boots");
        inventory.setItem(40, bootsDisplay);

        // Main hand slot
        ItemStack mainHandDisplay = createEquipmentSlot("mainhand", Material.IRON_SWORD,
                getFirstEquipment("possible-weapons"), "Main Hand");
        inventory.setItem(21, mainHandDisplay);

        // Off hand slot
        ItemStack offHandDisplay = createEquipmentSlot("offhand", Material.SHIELD,
                getFirstEquipment("possible-offhands"), "Off Hand");
        inventory.setItem(23, offHandDisplay);

        // Save button
        ItemStack saveButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Save Equipment",
                ChatColor.GRAY + "Save equipment changes");
        inventory.setItem(45, saveButton);

        // Clear Equipment button
        ItemStack clearButton = createGuiItem(Material.BUCKET,
                ChatColor.RED + "Clear All Equipment",
                ChatColor.GRAY + "Remove all equipment from this mob");
        inventory.setItem(49, clearButton);

        // Back button
        addBackButton(53, ChatColor.RED + "Back to Mob Editor", unsavedChanges);
    }

    private EquipmentItemConfig getFirstEquipment(String key) {
        List<EquipmentItemConfig> list = mobEquipment.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }


    private ItemStack createEquipmentSlot(String slot, Material defaultMaterial, EquipmentItemConfig equipment, String slotName) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        String equipmentId = (equipment != null ? equipment.getItem() : null);

        // No equipment or empty id – simple “empty” display
        if (equipmentId == null || equipmentId.isEmpty()) {
            return createGuiItem(
                    defaultMaterial,
                    ChatColor.BLUE + slotName,
                    ChatColor.YELLOW + "Click to configure",
                    ChatColor.GRAY + "Current: " + ChatColor.RED + "Empty"
            );
        }

        // Try to build a real item stack that looks like it would in-hand
        ItemStack displayItem = null;
        Material material = getVanillaMaterial(equipmentId);
        boolean isVanilla = (material != null);

        if (isVanilla) {
            // Vanilla item
            displayItem = new ItemStack(material);
        } else {
            // Custom item from EquipmentManager
            ItemStack customItem = plugin.getEquipmentManager().getEquipment(equipmentId);
            if (customItem != null) {
                displayItem = customItem.clone();
            }
        }

        if (displayItem == null) {
            // Fallback if the ID is invalid
            return createGuiItem(
                    defaultMaterial,
                    ChatColor.BLUE + slotName,
                    ChatColor.YELLOW + "Click to configure",
                    ChatColor.GRAY + "Current: " + ChatColor.RED + "Unknown ID " + equipmentId
            );
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta == null) {
            meta = plugin.getServer().getItemFactory().getItemMeta(displayItem.getType());
        }
        String originalName = (meta != null && meta.hasDisplayName()) ? meta.getDisplayName() : null;
        List<String> originalLore = (meta != null && meta.hasLore())
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();

        meta.setDisplayName(ChatColor.BLUE + slotName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Click to configure");
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.GRAY + "id: " + equipmentId + ChatColor.DARK_GRAY);

        lore.add("");

        if (originalName != null && !originalName.isEmpty()) {
            lore.add(originalName);
        }

        if (!originalLore.isEmpty()) {
            lore.addAll(originalLore);
        }

        if (equipment.getEnchantments() != null && !equipment.getEnchantments().isEmpty()) {
            lore.add("");
            lore.add(ChatColor.LIGHT_PURPLE + "Equipment Enchantments:");
            for (EnchantmentConfig ench : equipment.getEnchantments()) {
                if (ench.getMinLevel() == ench.getMaxLevel()) {
                    lore.add(ChatColor.GREEN + ench.getType()
                            + ChatColor.GRAY + " Lv " + ench.getMinLevel()
                            + ChatColor.DARK_GRAY + " (w=" + ench.getWeight() + ")");
                } else {
                    lore.add(ChatColor.GREEN + ench.getType()
                            + ChatColor.GRAY + " Lv " + ench.getMinLevel() + "-" + ench.getMaxLevel()
                            + ChatColor.DARK_GRAY + " (w=" + ench.getWeight() + ")");
                }
            }
        }

        if (!isVanilla) {
            Map<org.bukkit.enchantments.Enchantment, Integer> liveEnchants = displayItem.getEnchantments();
            if (!liveEnchants.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.LIGHT_PURPLE + "Equipment Enchantments:");
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : liveEnchants.entrySet()) {
                    org.bukkit.enchantments.Enchantment ench = entry.getKey();
                    int level = entry.getValue();
                    lore.add(ChatColor.GREEN + ench.getKey().getKey().toUpperCase()
                            + ChatColor.GRAY + " Lv " + level);
                }
            }
        }

        lore.add("");
        lore.add(ChatColor.RED + "Right click to remove");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        displayItem.setItemMeta(meta);
        return displayItem;
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

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {

        PlayerSessionData session = guiManager.getCurrentPlayer();
        switch (slot) {
            case 13: {
                if (clickType == ClickType.RIGHT) {
                    mobEquipment.remove("possible-helmets");
                    session.setItemEditType(EditingType.EQUIPMENT);
                    build();
                    break;
                }
                session.setItemEditType(EditingType.HELMET);
                session.setSelectedItem(inventory.getItem(slot));
                EquipmentItemConfig cfg = null;
                List<EquipmentItemConfig> list = mobEquipment.get("possible-helmets");
                if (list != null && !list.isEmpty()) {
                    cfg = list.get(0);
                }
                session.setSelectedItemId(cfg != null ? cfg.getItem() : null);
                session.setEditing(false);
                originEquipment = null;
                mobEquipment = null;
                pageManager.navigateTo("mob_equipment_item_settings", true, player);
                break;
            }
            case 22: {
                if (clickType == ClickType.RIGHT) {
                    mobEquipment.remove("possible-chestplates");
                    session.setItemEditType(EditingType.EQUIPMENT);
                    build();
                    break;
                }
                session.setItemEditType(EditingType.CHESTPLATE);
                session.setSelectedItem(inventory.getItem(slot));
                EquipmentItemConfig cfg = null;
                List<EquipmentItemConfig> list = mobEquipment.get("possible-chestplates");
                if (list != null && !list.isEmpty()) {
                    cfg = list.get(0);
                }
                session.setSelectedItemId(cfg != null ? cfg.getItem() : null);
                session.setEditing(false);
                originEquipment = null;
                mobEquipment = null;
                pageManager.navigateTo("mob_equipment_item_settings", true, player);
                break;
            }
            case 31: {
                if (clickType == ClickType.RIGHT) {
                    mobEquipment.remove("possible-leggings");
                    session.setItemEditType(EditingType.EQUIPMENT);
                    build();
                    break;
                }
                session.setItemEditType(EditingType.LEGGINGS);
                session.setSelectedItem(inventory.getItem(slot));
                EquipmentItemConfig cfg = null;
                List<EquipmentItemConfig> list = mobEquipment.get("possible-leggings");
                if (list != null && !list.isEmpty()) {
                    cfg = list.get(0);
                }
                session.setSelectedItemId(cfg != null ? cfg.getItem() : null);
                session.setEditing(false);
                originEquipment = null;
                mobEquipment = null;
                pageManager.navigateTo("mob_equipment_item_settings", true, player);
                break;
            }
            case 40: {
                if (clickType == ClickType.RIGHT) {
                    mobEquipment.remove("possible-boots");
                    session.setItemEditType(EditingType.EQUIPMENT);
                    build();
                    break;
                }
                session.setItemEditType(EditingType.BOOTS);
                session.setSelectedItem(inventory.getItem(slot));
                EquipmentItemConfig cfg = null;
                List<EquipmentItemConfig> list = mobEquipment.get("possible-boots");
                if (list != null && !list.isEmpty()) {
                    cfg = list.get(0);
                }
                session.setSelectedItemId(cfg != null ? cfg.getItem() : null);
                session.setEditing(false);
                originEquipment = null;
                mobEquipment = null;
                pageManager.navigateTo("mob_equipment_item_settings", true, player);
                break;
            }
            case 21: {
                if (clickType == ClickType.RIGHT) {
                    mobEquipment.remove("possible-weapons");
                    session.setItemEditType(EditingType.EQUIPMENT);
                    build();
                    break;
                }
                session.setItemEditType(EditingType.MAINHAND);
                session.setSelectedItem(inventory.getItem(slot));
                EquipmentItemConfig cfg = null;
                List<EquipmentItemConfig> list = mobEquipment.get("possible-weapons");
                if (list != null && !list.isEmpty()) {
                    cfg = list.get(0);
                }
                session.setSelectedItemId(cfg != null ? cfg.getItem() : null);
                session.setEditing(false);
                originEquipment = null;
                mobEquipment = null;
                pageManager.navigateTo("mob_equipment_item_settings", true, player);
                break;
            }
            case 23: {
                if (clickType == ClickType.RIGHT) {
                    mobEquipment.remove("possible-offhands");
                    session.setItemEditType(EditingType.EQUIPMENT);
                    build();
                    break;
                }
                session.setItemEditType(EditingType.OFFHAND);
                session.setSelectedItem(inventory.getItem(slot));
                EquipmentItemConfig cfg = null;
                List<EquipmentItemConfig> list = mobEquipment.get("possible-offhands");
                if (list != null && !list.isEmpty()) {
                    cfg = list.get(0);
                }
                session.setSelectedItemId(cfg != null ? cfg.getItem() : null);
                session.setEditing(false);
                originEquipment = null;
                mobEquipment = null;
                pageManager.navigateTo("mob_equipment_item_settings", true, player);
                break;
            }
            case 45:
                saveEquipment(player);
                build();
                session.setItemEditType(EditingType.EQUIPMENT);
                session.setEditing(false);
                break;
            case 49:
                clearAllEquipment(player);
                session.setItemEditType(EditingType.EQUIPMENT);
                build();
                break;
            case 53:
                mobEquipment = null;
                session.setEditing(false);
                session.setItemEditType(EditingType.NONE);
                pageManager.navigateBack(player);
                break;
            default:
        }

        return true;
    }


    private void clearAllEquipment(Player player) {
        if (mobEquipment != null) {
            mobEquipment.clear();
        }
        player.sendMessage(ChatColor.GREEN + "All equipment cleared (not saved yet)!");
    }

    private void saveEquipment(Player player) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PowerMobConfig mobConfig = plugin.getConfigManager().getPowerMob(selectedMobId);

        mobConfig.getPossibleEquipment().clear();
        mobConfig.getPossibleEquipment().putAll(mobEquipment);

        plugin.getConfigManager().savePowerMob(selectedMobId, mobConfig.toConfigMap());
        originEquipment = null;
        mobEquipment = null;
        player.sendMessage(ChatColor.GREEN + "Equipment saved successfully!");
    }

}

