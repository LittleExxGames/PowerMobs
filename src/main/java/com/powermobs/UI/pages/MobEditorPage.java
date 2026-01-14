package com.powermobs.UI.pages;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.EditingType;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.config.PowerManager;
import com.powermobs.config.PowerMobConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MobEditorPage extends AbstractGUIPage {
    private static final String title = ChatColor.DARK_PURPLE + "Mob Editor";
    private static final List<EntityType> HOSTILE_ENTITY_TYPES = Arrays.asList(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.CREEPER,
            EntityType.SPIDER,
            EntityType.ENDERMAN,
            EntityType.WITCH,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.STRAY,
            EntityType.PILLAGER,
            EntityType.EVOKER,
            EntityType.VINDICATOR,
            EntityType.ILLUSIONER,
            EntityType.GUARDIAN,
            EntityType.ELDER_GUARDIAN,
            EntityType.SHULKER,
            EntityType.SLIME,
            EntityType.MAGMA_CUBE,
            EntityType.BLAZE,
            EntityType.GHAST,
            EntityType.PHANTOM,
            EntityType.WITHER_SKELETON,
            EntityType.WITHER,
            EntityType.ZOGLIN,
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.HOGLIN,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.RAVAGER,
            EntityType.WARDEN
    );
    private String selectedMobId;
    private PowerMobConfig tempMobConfig;
    private int highlightedEntity = 0;
    public MobEditorPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, 54, title);
    }

    public void build() {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PlayerSessionData player = guiManager.getCurrentPlayer();
        String mobId = player.getSelectedMobId();
        if (!player.isEditing()) {
            tempMobConfig = new PowerMobConfig(plugin.getConfigManager().getPowerMob(mobId));
            player.setEditing(true);
            highlightedEntity = 0;
        }
        if (tempMobConfig == null) {
            plugin.debug("No mob config found for mob " + mobId, "ui");
            pageManager.navigateBack();
            return;
        }
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Mob Editor.", "ui");
        selectedMobId = mobId;
        boolean hasChanges = hasUnsavedChanges();

        // Mob info display
        Material icon = guiManager.getMaterialForEntityType(tempMobConfig.getEntityType());
        ItemStack infoItem = createGuiItem(icon,
                ChatColor.GREEN + mobId,
                ChatColor.GRAY + "Type: " + tempMobConfig.getEntityType(),
                ChatColor.GRAY + "Name: " + ChatColor.translateAlternateColorCodes('&', tempMobConfig.getName()));
        inventory.setItem(4, infoItem);

        // Delete mob
        ItemStack deleteItem = createGuiItem(Material.LAVA_BUCKET,
                ChatColor.RED + "Delete Mob",
                ChatColor.GRAY + "Permanently delete this mob",
                ChatColor.DARK_RED + "This cannot be undone!");
        inventory.setItem(8, deleteItem);

        // Health amount
        ItemStack healthItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Health: " + tempMobConfig.getMinHealth() + "-" + tempMobConfig.getMaxHealth(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(10, healthItem);

        // Health weight
        ItemStack healthWeightItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Health Weight: " + tempMobConfig.getHealthWeight(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(11, healthWeightItem);

        // Damage multiplier
        ItemStack damageItem = createGuiItem(Material.IRON_SWORD,
                ChatColor.GOLD + "Damage Multiplier: " + tempMobConfig.getMinDamageMultiplier() + "-" + tempMobConfig.getMaxDamageMultiplier(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(12, damageItem);

        // Damage weight
        ItemStack damageWeightItem = createGuiItem(Material.IRON_SWORD,
                ChatColor.GOLD + "Damage Multiplier Weight: " + tempMobConfig.getDamageWeight(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(13, damageWeightItem);

        // Speed multiplier
        ItemStack speedItem = createGuiItem(Material.FEATHER,
                ChatColor.WHITE + "Speed Multiplier: " + tempMobConfig.getMinSpeedMultiplier() + "-" + tempMobConfig.getMaxSpeedMultiplier(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(14, speedItem);

        // Speed weight
        ItemStack speedWeightItem = createGuiItem(Material.FEATHER,
                ChatColor.WHITE + "Speed Multiplier Weight: " + tempMobConfig.getSpeedWeight(),
                ChatColor.GRAY + "Click to modify");
        inventory.setItem(15, speedWeightItem);

        // Entity display
        List<String> entities = new ArrayList<>();
        entities.add(ChatColor.GRAY + "Right Click - Cycle through hostile mobs");
        entities.add(ChatColor.GRAY + "Left Click - Select entity type");
        // Get the current entity type and find its index in the list
        EntityType currentEntityType = tempMobConfig.getEntityType();
        // Display 7 entities: 3 before, current (highlighted), 3 after
        for (int i = -3; i <= 3; i++) {
            int index = (highlightedEntity + i + HOSTILE_ENTITY_TYPES.size()) % HOSTILE_ENTITY_TYPES.size();
            EntityType entity = HOSTILE_ENTITY_TYPES.get(index);
            String active = "";
            if (entity == currentEntityType) {
                active = ChatColor.GREEN + "✓ ";
            }
            if (i == 0) {
                // Current/highlighted entity
                entities.add(active + ChatColor.YELLOW + "► " + ChatColor.GOLD + entity.name() + ChatColor.YELLOW + " ◄");
            } else {
                entities.add(active + ChatColor.BLUE + entity.name());
            }
        }

        ItemStack entitiesDisplay = createGuiItem(
                guiManager.getMaterialForEntityType(currentEntityType),
                ChatColor.LIGHT_PURPLE + "Current: " + currentEntityType.name(),
                entities
        );
        inventory.setItem(16, entitiesDisplay);

        // Glow option
        String toggle = tempMobConfig.isGlowing() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
        ItemStack glowItem = createGuiItem(Material.GLOWSTONE_DUST,
                ChatColor.GOLD + "Glowing On Spawn",
                ChatColor.GRAY + "Enabled: " + toggle,
                ChatColor.GRAY + "Click to toggle");
        inventory.setItem(19, glowItem);

        // Glow time
        ItemStack glowTimeItem = createGuiItem(Material.CLOCK,
                ChatColor.GOLD + "Glow duration",
                ChatColor.GRAY + "Time: " + tempMobConfig.getMinGlowTime() + " - " + tempMobConfig.getMaxGlowTime() + " seconds",
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(20, glowTimeItem);

        // Drop possibilities
        List<String> dropDisplay = new ArrayList<>();
        dropDisplay.add(ChatColor.GRAY + "Current Possibilities: " + tempMobConfig.getDrops().size());
        if (hasChanges) {
            dropDisplay.add(ChatColor.GRAY + "Click to configure - " + unsavedWarning);
        } else {
            dropDisplay.add(ChatColor.GRAY + "Click to configure");
        }
        ItemStack dropItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drop Possibilities",
                dropDisplay);
        inventory.setItem(21, dropItem);

        // Drop range
        ItemStack dropRangeItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drop Count Range",
                ChatColor.GRAY + "Range: " + tempMobConfig.getMinDrops() + " - " + tempMobConfig.getMaxDrops(),
                ChatColor.GRAY + "Possible Drops: " + tempMobConfig.getDrops().size(),
                ChatColor.GRAY + "Click to configure the range");
        inventory.setItem(22, dropRangeItem);

        // Drop weight
        ItemStack dropWeightItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drop Weight",
                ChatColor.GRAY + "Weight: " + tempMobConfig.getDropWeight(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(23, dropWeightItem);

        // Abilities
        List<String> abilityDisplay = new ArrayList<>();
        abilityDisplay.add(ChatColor.GRAY + "Current Possibilities: " + tempMobConfig.getPossibleAbilities().size());
        if (hasChanges) {
            abilityDisplay.add(ChatColor.GRAY + "Click to configure - " + unsavedWarning);
        } else {
            abilityDisplay.add(ChatColor.GRAY + "Click to configure");
        }
        ItemStack abilitiesItem = createGuiItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + "Abilities",
                abilityDisplay);
        inventory.setItem(28, abilitiesItem);

        // Equipment
        List<String> equipmentDisplay = new ArrayList<>();
        equipmentDisplay.add(ChatColor.GRAY + "Current equipment:");
        for (String slot : tempMobConfig.getPossibleEquipment().keySet()) {
            if (!tempMobConfig.getPossibleEquipment().get(slot).isEmpty()) {
                String name = tempMobConfig.getPossibleEquipment().get(slot).get(0).getItem();
                equipmentDisplay.add(ChatColor.GRAY + slot + ": " + name);
            }
        }
        if (hasChanges) {
            equipmentDisplay.add(ChatColor.GRAY + "Click to configure - " + unsavedWarning);
        } else {
            equipmentDisplay.add(ChatColor.GRAY + "Click to configure");
        }

        ItemStack equipmentItem = createGuiItem(Material.DIAMOND_CHESTPLATE,
                ChatColor.BLUE + "Equipment",
                equipmentDisplay);
        inventory.setItem(31, equipmentItem);

        // Spawn conditions
        List<String> spawnDisplay = new ArrayList<>();
        if (hasChanges) {
            spawnDisplay.add(ChatColor.GRAY + "Configure where and when this mob can spawn - " + unsavedWarning);
        } else {
            spawnDisplay.add(ChatColor.GRAY + "Configure where and when this mob can spawn");
        }
        ItemStack spawnItem = createGuiItem(Material.CLOCK,
                ChatColor.GREEN + "Spawn Conditions",
                spawnDisplay);
        inventory.setItem(34, spawnItem);

        // Experience Chance
        ItemStack experienceChanceItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience",
                ChatColor.GOLD + "Chance: " + tempMobConfig.getExperienceChance()
        );
        inventory.setItem(39, experienceChanceItem);

        // Experience amount
        ItemStack experienceAmountItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience",
                ChatColor.GOLD + "Amount: " + tempMobConfig.getExperienceMinAmount() + " - " + tempMobConfig.getExperienceMaxAmount()
        );
        inventory.setItem(40, experienceAmountItem);

        // Experience weight
        ItemStack experienceWeightItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience",
                ChatColor.GOLD + "Weight: " + tempMobConfig.getExperienceWeight()
        );
        inventory.setItem(41, experienceWeightItem);

        // Test spawn
        List<String> testDisplay = new ArrayList<>();
        testDisplay.add(ChatColor.GRAY + "Spawn this mob at your location");
        if (hasChanges) {
            testDisplay.add(ChatColor.RED + "UNABLE TO SPAWN WITH UNSAVED SETTINGS");
        }
        ItemStack testItem = createGuiItem(Material.EGG,
                ChatColor.YELLOW + "Test Spawn",
                testDisplay);
        inventory.setItem(49, testItem);

        // Save button
        ItemStack saveButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save all changes to this mob");
        inventory.setItem(45, saveButton);

        // Back button
        addBackButton(53, ChatColor.RED + "Back to Mob List", hasChanges);
    }

    public boolean handleClick(Player player, int slot, ClickType clickType) {
        PlayerSessionData session = guiManager.getCurrentPlayer();
        switch (slot) {
            case 4: // Change name
                startChatInput(player, ChatInputType.STRING_NAMES, (value, p) -> {
                    tempMobConfig.setName((String) value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 8: // Delete mob
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "To delete a mob, use the command: /powermob delete " + selectedMobId);
                break;
            case 10: // Health
                startChatInput(player, ChatInputType.HEALTH, (value, p) -> {
                    updateHealth(value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 11: // Health weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setHealthWeight((Integer) value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 12: // Damage
                startChatInput(player, ChatInputType.MULTIPLIER, (value, p) -> {
                    updateDamage(value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 13: // Damage weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setDamageWeight((Integer) value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 14: // Speed
                startChatInput(player, ChatInputType.MULTIPLIER, (value, p) -> {
                    updateSpeed(value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 15: // Speed weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setSpeedWeight((Integer) value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 16: // Entity type
                if (clickType == ClickType.RIGHT) {
                    highlightedEntity++;
                    if (highlightedEntity >= HOSTILE_ENTITY_TYPES.size()) {
                        highlightedEntity = 0;
                    }
                    build();
                } else if (clickType == ClickType.LEFT) {
                    tempMobConfig.setEntityType(HOSTILE_ENTITY_TYPES.get(highlightedEntity));
                    build();
                }
                break;
            case 19: // Glow toggle
                tempMobConfig.setGlowing(!tempMobConfig.isGlowing());
                build();
                break;
            case 20: // Glow time
                startChatInput(player, ChatInputType.GLOW_TIME, (value, p) -> {
                    updateGlowTime(value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 21: // Drops
                session.setEditing(false);
                session.resetDropEditing();
                session.setItemEditType(EditingType.DROPS_CONFIG);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 22: // Drop Range
                startChatInput(player, ChatInputType.DROP_AMOUNT, (value, p) -> {
                    updateDropRange(value);
                    session.setEditing(true);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 23: // Drop weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setDropWeight((Integer) value);
                    session.setEditing(true);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 28: // Abilities
                session.setEditing(false);
                session.setItemEditType(EditingType.ABILITY);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 31: // Equipment
                session.setEditing(false);
                pageManager.navigateTo("mob_equipment", true, player);
                break;
            case 34: // Spawn conditions
                session.setEditing(false);
                pageManager.navigateTo("mob_spawn_conditions", true, player);
                break;
            case 39: // Experience chacne
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempMobConfig.setExperienceChance((Double) value);
                    pageManager.navigateTo("drop_settings", false, p);
                });
                break;
            case 40: // Experience amount
                startChatInput(player, ChatInputType.DROP_AMOUNT, (value, p) -> {
                    updateExperienceAmount(value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 41: // Experience weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setExperienceWeight((Integer) value);
                    pageManager.navigateTo("mob_editor", false, p);
                });
                break;
            case 45: //Save changes
                session.setEditing(false);
                PowerManager configManager = pageManager.getPlugin().getConfigManager();
                boolean saved = configManager.savePowerMob(selectedMobId, tempMobConfig.toConfigMap());
                if (saved) {
                    build();
                }
                break;
            case 49: // Test spawn
                if (!hasUnsavedChanges()) {
                    session.setEditing(false);
                    player.closeInventory();
                    player.performCommand("powermob spawn " + selectedMobId);
                }
                break;
            case 53: // Back to mob list
                session.setEditing(false);
                pageManager.navigateBack(player);
                break;
        }
        return true;
    }

    private void updateHealth(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            tempMobConfig.setMinHealth(min);
            tempMobConfig.setMaxHealth(max);
        } else if (value instanceof Integer) {
            // Single value
            int amount = (Integer) value;
            tempMobConfig.setMinHealth(amount);
            tempMobConfig.setMaxHealth(amount);
        }
    }

    private void updateDamage(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            double min = Double.parseDouble(parts[0].trim());
            double max = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : min;
            tempMobConfig.setMinDamageMultiplier(min);
            tempMobConfig.setMaxDamageMultiplier(max);
        } else if (value instanceof Double) {
            // Single value
            double amount = (Double) value;
            tempMobConfig.setMinDamageMultiplier(amount);
            tempMobConfig.setMaxDamageMultiplier(amount);
        }
    }

    private void updateSpeed(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            double min = Double.parseDouble(parts[0].trim());
            double max = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : min;
            tempMobConfig.setMinSpeedMultiplier(min);
            tempMobConfig.setMaxSpeedMultiplier(max);
        } else if (value instanceof Double) {
            // Single value
            double amount = (Double) value;
            tempMobConfig.setMinSpeedMultiplier(amount);
            tempMobConfig.setMaxSpeedMultiplier(amount);
        }
    }

    private void updateGlowTime(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            tempMobConfig.setMinGlowTime(min);
            tempMobConfig.setMaxGlowTime(max);
        } else if (value instanceof Integer) {
            // Single value
            int amount = (Integer) value;
            tempMobConfig.setMinGlowTime(amount);
            tempMobConfig.setMaxGlowTime(amount);
        }
    }

    private void updateDropRange(Object value) {
        String settings = (String) value;
        String[] parts = settings.split("-");
        int dropMin;
        int dropMax;
        if (parts.length == 2) {
            dropMin = Integer.parseInt(parts[0].trim());
            dropMax = Integer.parseInt(parts[1].trim());
        } else {
            dropMin = Integer.parseInt(parts[0].trim());
            dropMax = dropMin;
        }
        tempMobConfig.setMinDrops(dropMin);
        tempMobConfig.setMaxDrops(dropMax);
    }

    private void updateExperienceAmount(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            tempMobConfig.setExperienceMinAmount(min);
            tempMobConfig.setExperienceMaxAmount(max);
        } else if (value instanceof Integer) {
            // Single value
            int amount = (Integer) value;
            tempMobConfig.setExperienceMinAmount(amount);
            tempMobConfig.setExperienceMaxAmount(amount);
        }
    }

    private boolean hasUnsavedChanges() {
        PowerMobConfig original = pageManager.getPlugin().getConfigManager().getPowerMob(selectedMobId);

        // Convert both to their config map representations and compare
        Map<String, Object> originalMap = original.toConfigMap();
        Map<String, Object> tempMap = tempMobConfig.toConfigMap();

        return !originalMap.equals(tempMap);
    }
}
