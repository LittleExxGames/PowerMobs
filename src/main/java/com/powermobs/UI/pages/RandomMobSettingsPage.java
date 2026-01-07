package com.powermobs.UI.pages;

import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.EditingType;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.config.RandomMobConfig;
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

/**
 * Page for configuring random mob settings
 */
public class RandomMobSettingsPage extends AbstractGUIPage {

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
    private RandomMobConfig tempMobConfig;
    private int highlightedEntity = 0;
    public RandomMobSettingsPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, 54, ChatColor.DARK_PURPLE + "Random Mob Settings");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Random mob settings.", "ui");
        String editingWarning = ChatColor.RED + "YOU HAVE UNSAVED CHANGES. NAVIGATING HERE WILL RESET CHANGES";
        if (!guiManager.getCurrentPlayer().isEditing()) {
            RandomMobConfig config = pageManager.getPlugin().getConfigManager().getRandomMobConfig();
            tempMobConfig = new RandomMobConfig(config);
        }
        boolean hasChanges = hasUnsavedChanges();
        // Enabled/Disabled toggle
        Material toggleMaterial = tempMobConfig.isEnabled() ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        String toggleText = tempMobConfig.isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
        ItemStack toggleButton = createGuiItem(toggleMaterial,
                ChatColor.GOLD + "Random Mobs: " + toggleText,
                ChatColor.GRAY + "Click to toggle random mob spawning");
        inventory.setItem(2, toggleButton);

        // Allowed mob types
        List<String> entities = new java.util.ArrayList<>();
        entities.add(ChatColor.GRAY + "Right click to cycle");
        entities.add(ChatColor.GRAY + "Left click to enable/disable");
        for (int i = -3; i <= 3; i++) {
            int index = (highlightedEntity + i + HOSTILE_ENTITY_TYPES.size()) % HOSTILE_ENTITY_TYPES.size();
            EntityType entity = HOSTILE_ENTITY_TYPES.get(index);
            String active = (tempMobConfig.getAllowedTypes().contains(entity)) ? ChatColor.DARK_GREEN + "✓ " : ChatColor.RED + "✗ ";
            if (i == 0) {
                // Current/highlighted entity
                entities.add(active + ChatColor.YELLOW + "► " + ChatColor.GOLD + entity.name() + ChatColor.YELLOW + " ◄");
            } else {
                entities.add(active + ChatColor.BLUE + entity.name());
            }
        }
        ItemStack mobTypesItem = createGuiItem(Material.ZOMBIE_SPAWN_EGG,
                ChatColor.GREEN + "Allowed Mob Types",
                entities);
        inventory.setItem(3, mobTypesItem);

        // Ability possibilities
        List<String> abilityDisplay = new ArrayList<>();
        abilityDisplay.add(ChatColor.GRAY + "Active Possibilities: " + tempMobConfig.getPossibleAbilities().size());
        abilityDisplay.add(ChatColor.GRAY + "Selectable Abilities: " + pageManager.getPlugin().getAbilityManager().getAbilities().size());
        abilityDisplay.add(ChatColor.GRAY + "Click to configure");
        if (hasChanges) {
            abilityDisplay.add(editingWarning);
        }
        ItemStack abilityItem = createGuiItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + "Ability Possibilities",
                abilityDisplay);
        inventory.setItem(4, abilityItem);

        // Ability range
        ItemStack abilityRangeItem = createGuiItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + "Ability Count Range",
                ChatColor.GRAY + "Range: " + tempMobConfig.getMinAbilities() + " - " + tempMobConfig.getMaxAbilities(),
                ChatColor.GRAY + "Active Possibilities: " + tempMobConfig.getPossibleAbilities().size(),
                ChatColor.GRAY + "Click to configure the range");
        inventory.setItem(5, abilityRangeItem);

        // Ability weight
        ItemStack abilityWeightItem = createGuiItem(Material.BLAZE_POWDER,
                ChatColor.LIGHT_PURPLE + "Ability Weight",
                ChatColor.GRAY + "Weight: " + tempMobConfig.getAbilitiesWeight(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(6, abilityWeightItem);

        // Health multiplier range
        ItemStack healthItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Health Multiplier",
                ChatColor.GRAY + "Range: " + tempMobConfig.getMinHealthMultiplier() + " - " + tempMobConfig.getMaxHealthMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(11, healthItem);

        // Health weight
        ItemStack healthWeightItem = createGuiItem(Material.RED_DYE,
                ChatColor.RED + "Health Weight",
                ChatColor.GRAY + "Weight: " + tempMobConfig.getHealthWeight(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(12, healthWeightItem);

        // Drop possibilities
        List<String> dropDisplay = new ArrayList<>();
        dropDisplay.add(ChatColor.GRAY + "Current Possibilities: " + tempMobConfig.getDrops().size());
        dropDisplay.add(ChatColor.GRAY + "Click to configure");
        if (hasChanges) {
            dropDisplay.add(editingWarning);
        }
        ItemStack dropItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drop Possibilities",
                dropDisplay);
        inventory.setItem(13, dropItem);

        // Drop range
        ItemStack dropRangeItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drop Count Range",
                ChatColor.GRAY + "Range: " + tempMobConfig.getMinDrops() + " - " + tempMobConfig.getMaxDrops(),
                ChatColor.GRAY + "Possible Drops: " + tempMobConfig.getDrops().size(),
                ChatColor.GRAY + "Click to configure the range");
        inventory.setItem(14, dropRangeItem);

        // Drop weight
        ItemStack dropWeightItem = createGuiItem(Material.CHEST,
                ChatColor.YELLOW + "Drop Weight",
                ChatColor.GRAY + "Weight: " + tempMobConfig.getDropWeight(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(15, dropWeightItem);

        // Weapon chance
        ItemStack weaponChanceItem = createGuiItem(Material.WOODEN_SWORD,
                ChatColor.DARK_GREEN + "Weapon Chance",
                ChatColor.GOLD + "Chance: " + formatDecimal(tempMobConfig.getWeaponChance())
        );
        inventory.setItem(19, weaponChanceItem);

        // Offhand chance
        ItemStack offhandChanceItem = createGuiItem(Material.SHIELD,
                ChatColor.DARK_GREEN + "Offhand Chance",
                ChatColor.GOLD + "Chance: " + formatDecimal(tempMobConfig.getOffhandChance())
        );
        inventory.setItem(20, offhandChanceItem);

        // Helmet chance
        ItemStack helmetChanceItem = createGuiItem(Material.LEATHER_HELMET,
                ChatColor.DARK_GREEN + "Helmet Chance",
                ChatColor.GOLD + "Chance: " + formatDecimal(tempMobConfig.getHelmetChance())
        );
        inventory.setItem(21, helmetChanceItem);

        // Chestplate chance
        ItemStack chestplateChanceItem = createGuiItem(Material.LEATHER_CHESTPLATE,
                ChatColor.DARK_GREEN + "Chestplate Chance",
                ChatColor.GOLD + "Chance: " + formatDecimal(tempMobConfig.getChestplateChance())
        );
        inventory.setItem(22, chestplateChanceItem);

        // Leggings chance
        ItemStack leggingsChanceItem = createGuiItem(Material.LEATHER_LEGGINGS,
                ChatColor.DARK_GREEN + "Leggings Chance",
                ChatColor.GOLD + "Chance: " + formatDecimal(tempMobConfig.getLeggingsChance())
        );
        inventory.setItem(23, leggingsChanceItem);

        // Boots chance
        ItemStack bootsChanceItem = createGuiItem(Material.LEATHER_BOOTS,
                ChatColor.DARK_GREEN + "Boots Chance",
                ChatColor.GOLD + "Chance: " + formatDecimal(tempMobConfig.getBootsChance())
        );
        inventory.setItem(24, bootsChanceItem);

        // Prefix settings
        List<String> prefixDisplay = new ArrayList<>();
        prefixDisplay.add(ChatColor.GRAY + "Click to manage prefix settings");
        if (hasChanges) {
            prefixDisplay.add(editingWarning);
        }
        ItemStack prefixItem = createGuiItem(Material.ANVIL,
                ChatColor.AQUA + "Prefix Settings",
                prefixDisplay);
        inventory.setItem(25, prefixItem);

        // Possible weapons
        List<String> weaponsDisplay = new ArrayList<>();
        weaponsDisplay.add(ChatColor.GRAY + "Click to display and edit the list of possible weapons");
        weaponsDisplay.add(ChatColor.GRAY + "Weapon Count: " + tempMobConfig.getPossibleEquipment().get("possible-weapons").size());
        if (hasChanges) {
            weaponsDisplay.add(editingWarning);
        }
        ItemStack weaponsButton = createGuiItem(Material.DIAMOND_SWORD,
                ChatColor.GREEN + "Weapon Possibilities",
                weaponsDisplay);
        inventory.setItem(28, weaponsButton);

        // Possible offhands
        List<String> offhandDisplay = new ArrayList<>();
        offhandDisplay.add(ChatColor.GRAY + "Click to display and edit the list of possible offhands");
        offhandDisplay.add(ChatColor.GRAY + "Offhand Count: " + tempMobConfig.getPossibleEquipment().get("possible-offhands").size());
        if (hasChanges) {
            offhandDisplay.add(editingWarning);
        }
        ItemStack offhandsButton = createGuiItem(Material.SHIELD,
                ChatColor.GREEN + "Offhand Possibilities",
                offhandDisplay);
        inventory.setItem(29, offhandsButton);

        // Possible helmets
        List<String> helmetDisplay = new ArrayList<>();
        helmetDisplay.add(ChatColor.GRAY + "Click to display and edit the list of possible helmets");
        helmetDisplay.add(ChatColor.GRAY + "Helmet Count: " + tempMobConfig.getPossibleEquipment().get("possible-helmets").size());
        if (hasChanges) {
            helmetDisplay.add(editingWarning);
        }
        ItemStack helmetsButton = createGuiItem(Material.DIAMOND_HELMET,
                ChatColor.GREEN + "Helmet Possibilities",
                helmetDisplay);
        inventory.setItem(30, helmetsButton);

        // Possible chestplates
        List<String> chestplateDisplay = new ArrayList<>();
        chestplateDisplay.add(ChatColor.GRAY + "Click to display and edit the list of possible chestplates");
        chestplateDisplay.add(ChatColor.GRAY + "Chestplate Count: " + tempMobConfig.getPossibleEquipment().get("possible-chestplates").size());
        if (hasChanges) {
            chestplateDisplay.add(editingWarning);
        }
        ItemStack chestplatesButton = createGuiItem(Material.DIAMOND_CHESTPLATE,
                ChatColor.GREEN + "Chestplate Possibilities",
                chestplateDisplay);
        inventory.setItem(31, chestplatesButton);

        // Possible leggings
        List<String> leggingsDisplay = new ArrayList<>();
        leggingsDisplay.add(ChatColor.GRAY + "Click to display and edit the list of possible leggings");
        leggingsDisplay.add(ChatColor.GRAY + "Leggings Count: " + tempMobConfig.getPossibleEquipment().get("possible-leggings").size());
        if (hasChanges) {
            leggingsDisplay.add(editingWarning);
        }
        ItemStack leggingsButton = createGuiItem(Material.DIAMOND_LEGGINGS,
                ChatColor.GREEN + "Legging Possibilities",
                leggingsDisplay
        );
        inventory.setItem(32, leggingsButton);

        // Possible boots
        List<String> bootsDisplay = new ArrayList<>();
        bootsDisplay.add(ChatColor.GRAY + "Click to display and edit the list of possible boots");
        bootsDisplay.add(ChatColor.GRAY + "Boots Count: " + tempMobConfig.getPossibleEquipment().get("possible-boots").size());
        if (hasChanges) {
            bootsDisplay.add(editingWarning);
        }
        ItemStack bootsButton = createGuiItem(Material.DIAMOND_BOOTS,
                ChatColor.GREEN + "Boot Possibilities",
                bootsDisplay);
        inventory.setItem(33, bootsButton);

        // Suffix settings
        List<String> suffixDisplay = new ArrayList<>();
        suffixDisplay.add(ChatColor.GRAY + "Click to manage suffix settings");
        if (hasChanges) {
            suffixDisplay.add(editingWarning);
        }
        ItemStack suffixItem = createGuiItem(Material.ANVIL,
                ChatColor.AQUA + "Suffix Settings",
                suffixDisplay);
        inventory.setItem(34, suffixItem);

        // Damage multiplier range
        ItemStack damageItem = createGuiItem(Material.IRON_SWORD,
                ChatColor.GOLD + "Damage Multiplier",
                ChatColor.GRAY + "Range: " + tempMobConfig.getMinDamageMultiplier() + " - " + tempMobConfig.getMaxDamageMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(38, damageItem);

        // Damage weight
        ItemStack damageWeightItem = createGuiItem(Material.IRON_SWORD,
                ChatColor.GOLD + "Damage Weight",
                ChatColor.GRAY + "Weight: " + tempMobConfig.getDamageWeight(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(39, damageWeightItem);

        // Experience Chance
        ItemStack experienceChanceItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience Chance",
                ChatColor.GOLD + "Chance: " + formatDecimal(tempMobConfig.getExperienceChance())
        );
        inventory.setItem(40, experienceChanceItem);

        // Experience range
        ItemStack expItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience Range",
                ChatColor.GRAY + "Range: " + tempMobConfig.getExperienceMinAmount() + " - " + tempMobConfig.getExperienceMaxAmount(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(41, expItem);

        // Experience weight
        ItemStack expWeightItem = createGuiItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.DARK_GREEN + "Experience Weight",
                ChatColor.GRAY + "Weight: " + tempMobConfig.getExperienceWeight(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(42, expWeightItem);

        // Speed multiplier range
        ItemStack speedItem = createGuiItem(Material.FEATHER,
                ChatColor.WHITE + "Speed Multiplier",
                ChatColor.GRAY + "Range: " + tempMobConfig.getMinSpeedMultiplier() + " - " + tempMobConfig.getMaxSpeedMultiplier(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(47, speedItem);

        // Speed weight
        ItemStack speedWeightItem = createGuiItem(Material.FEATHER,
                ChatColor.WHITE + "Speed Weight",
                ChatColor.GRAY + "Weight: " + tempMobConfig.getSpeedWeight(),
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(48, speedWeightItem);

        // Glow option
        String toggle = tempMobConfig.isGlowing() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
        ItemStack glowItem = createGuiItem(Material.GLOWSTONE_DUST,
                ChatColor.GOLD + "Glowing On Spawn",
                ChatColor.GRAY + "Status: " + toggle,
                ChatColor.GRAY + "Click to toggle");
        inventory.setItem(49, glowItem);

        // Glow time
        ItemStack glowTimeItem = createGuiItem(Material.CLOCK,
                ChatColor.GOLD + "Glow duration",
                ChatColor.GRAY + "Time: " + tempMobConfig.getMinGlowTime() + " - " + tempMobConfig.getMaxGlowTime() + " seconds",
                ChatColor.GRAY + "Click to adjust");
        inventory.setItem(50, glowTimeItem);

        // Spawn condition
        List<String> spawnDisplay = new ArrayList<>();
        spawnDisplay.add(ChatColor.GRAY + "Click to configure");
        if (hasChanges) {
            spawnDisplay.add(editingWarning);
        }
        ItemStack spawnConditionItem = createGuiItem(Material.CLOCK,
                ChatColor.AQUA + "Spawn Configuration",
                spawnDisplay);
        inventory.setItem(51, spawnConditionItem);

        // Save button
        ItemStack saveButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save the current configuration"
        );
        inventory.setItem(45, saveButton);

        // Back button
        List<String> backDisplay = new ArrayList<>();
        if (hasChanges) {
            backDisplay.add(editingWarning);
        }
        ItemStack backButton = createGuiItem(Material.BARRIER,
                ChatColor.RED + "Back to Main Menu",
                backDisplay);
        inventory.setItem(53, backButton);
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        PlayerSessionData session = guiManager.getCurrentPlayer();
        switch (slot) {
            case 2: // Random mob toggle
                tempMobConfig.setEnabled(!tempMobConfig.isEnabled());
                session.setEditing(true);
                build();
                break;
            case 3: // Possible entities
                if (clickType == ClickType.RIGHT) {
                    if (highlightedEntity == HOSTILE_ENTITY_TYPES.size() - 1) {
                        highlightedEntity = 0;
                    } else {
                        highlightedEntity++;
                    }
                    build();
                } else if (clickType == ClickType.LEFT) {
                    EntityType entity = HOSTILE_ENTITY_TYPES.get(highlightedEntity);
                    if (tempMobConfig.getAllowedTypes().contains(entity)) {
                        tempMobConfig.getAllowedTypes().remove(entity);
                    } else {
                        tempMobConfig.getAllowedTypes().add(entity);
                    }
                    session.setEditing(true);
                    build();
                }
                break;
            case 4: // Ability possibilities
                session.setEditing(false);
                session.setItemEditType(EditingType.ABILITY);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 5: // Ability range
                startChatInput(player, ChatInputType.RANGE, (value, p) -> {
                    updateAbilityRange(value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 6: // Ability weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setAbilitiesWeight((Integer) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 11: // Health multiplier
                startChatInput(player, ChatInputType.MULTIPLIER, (value, p) -> {
                    updateHealth(value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 12: // Health weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setHealthWeight((Integer) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 13: // Drop possibilities
                session.setEditing(false);
                session.setItemEditType(EditingType.DROPS_CONFIG);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 14: // Drop range
                startChatInput(player, ChatInputType.DROP_AMOUNT, (value, p) -> {
                    updateDropRange(value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 15: // Drop weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setDropWeight((Integer) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 19: // Weapon chance
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempMobConfig.setWeaponChance((double) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 20: // Offhand chance
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempMobConfig.setOffhandChance((double) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 21: // Helmet chance
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempMobConfig.setHelmetChance((double) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 22: // Chestplate chance
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempMobConfig.setChestplateChance((double) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 23: // Leggings chance
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempMobConfig.setLeggingsChance((double) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 24: // Boots chance
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempMobConfig.setBootsChance((double) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 25: // Prefix settings
                session.setEditing(false);
                session.setItemEditType(EditingType.PREFIX);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 28: // Possible weapons
                session.setEditing(false);
                session.setItemEditType(EditingType.MAINHAND);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 29: // Possible offhands
                session.setEditing(false);
                session.setItemEditType(EditingType.OFFHAND);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 30: // Possible helmets
                session.setEditing(false);
                session.setItemEditType(EditingType.HELMET);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 31: // Possible chestplates
                session.setEditing(false);
                session.setItemEditType(EditingType.CHESTPLATE);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 32: // Possible leggings
                session.setEditing(false);
                session.setItemEditType(EditingType.LEGGINGS);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 33: // Possible boots
                session.setEditing(false);
                session.setItemEditType(EditingType.BOOTS);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 34: // Suffix settings
                session.setEditing(false);
                session.setItemEditType(EditingType.SUFFIX);
                pageManager.navigateTo("bulk_item_selection", true, player);
                break;
            case 38: // Damage multiplier range
                startChatInput(player, ChatInputType.MULTIPLIER, (value, p) -> {
                    updateDamage(value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 39: // Damage weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setDamageWeight((Integer) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 40: // Experience chance
                startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                    tempMobConfig.setExperienceChance((double) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 41: // Experience range
                startChatInput(player, ChatInputType.DROP_AMOUNT, (value, p) -> {
                    updateExperienceAmount(value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 42: // Experience weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setExperienceWeight((Integer) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 47: // Speed multiplier range
                startChatInput(player, ChatInputType.MULTIPLIER, (value, p) -> {
                    updateSpeed(value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 48: // Speed weight
                startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                    tempMobConfig.setSpeedWeight((Integer) value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 49: // Glow toggle
                tempMobConfig.setGlowing(!tempMobConfig.isGlowing());
                session.setEditing(true);
                build();
                break;
            case 50: // Glow time
                startChatInput(player, ChatInputType.GLOW_TIME, (value, p) -> {
                    updateGlowTime(value);
                    session.setEditing(true);
                    pageManager.navigateTo("random_mob_settings", false, p);
                });
                break;
            case 51: // Spawn conditions
                session.setEditing(false);
                pageManager.navigateTo("mob_spawn_conditions", true, player);
                break;
            case 45: // Save
                session.setEditing(false);
                saveConfig();
                build();
                break;
            case 53: // Back button
                pageManager.navigateBack(player);
                break;
        }
        // Handle other clicks
        return true;
    }

    private void updateHealth(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            double min = Double.parseDouble(parts[0].trim());
            double max = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : min;
            tempMobConfig.setMinHealthMultiplier(min);
            tempMobConfig.setMaxHealthMultiplier(max);
        } else if (value instanceof Double) {
            // Single value
            double amount = (Double) value;
            tempMobConfig.setMinHealthMultiplier(amount);
            tempMobConfig.setMaxHealthMultiplier(amount);
        }
    }

    private void updateDropRange(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            tempMobConfig.setMinDrops(min);
            tempMobConfig.setMaxDrops(max);
        } else if (value instanceof Integer) {
            // Single value
            int amount = (Integer) value;
            tempMobConfig.setMinDrops(amount);
            tempMobConfig.setMaxDrops(amount);
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

    private void updateAbilityRange(Object value) {
        if (value instanceof String range) {
            // Range format like "1-3"
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
            tempMobConfig.setMinAbilities(min);
            tempMobConfig.setMaxAbilities(max);
        } else if (value instanceof Integer) {
            // Single value
            int amount = (Integer) value;
            tempMobConfig.setMinAbilities(amount);
            tempMobConfig.setMaxAbilities(amount);
        }
    }

    private boolean hasUnsavedChanges() {
        RandomMobConfig original = pageManager.getPlugin().getConfigManager().getRandomMobConfig();

        // Convert both to their config map representations and compare
        Map<String, Object> originalMap = original.toConfigMap();
        Map<String, Object> tempMap = tempMobConfig.toConfigMap();

        return !originalMap.equals(tempMap);
    }

    private void saveConfig() {
        pageManager.getPlugin().getConfigManager().saveRandomMob(tempMobConfig.toConfigMap());
    }
}
