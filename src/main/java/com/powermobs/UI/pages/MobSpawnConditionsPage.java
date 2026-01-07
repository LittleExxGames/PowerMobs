package com.powermobs.UI.pages;

import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.config.BiomeGroupManager;
import com.powermobs.config.IPowerMobConfig;
import com.powermobs.config.PowerManager;
import com.powermobs.config.SpawnCondition;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MobSpawnConditionsPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private String selectedMobId;
    private int currentPage = 0;
    private SpawnCondition tempConfig = null;


    public MobSpawnConditionsPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.DARK_PURPLE + "Mob Spawn Conditions");
    }

    @Override
    public void build() {
        inventory.clear();
        pageManager.getPlugin().debug("Building GUI for Mob spawn conditions.", "ui");
        selectedMobId = guiManager.getCurrentPlayer().getSelectedMobId();
        IPowerMobConfig mob;
        if (selectedMobId != null) {
            mob = pageManager.getPlugin().getConfigManager().getPowerMob(selectedMobId);
        } else {
            mob = pageManager.getPlugin().getConfigManager().getRandomMobConfig();
        }
        if (tempConfig == null) {
            tempConfig = new SpawnCondition(mob.getSpawnCondition());
        }

        ItemStack spawnChanceDisplay = createGuiItem(Material.RECOVERY_COMPASS,
                ChatColor.WHITE + "Spawn Chance",
                ChatColor.GRAY + "Current spawn chance(after global chance)",
                ChatColor.WHITE + " " + formatDecimal(tempConfig.getSpawnChance()),
                ChatColor.GRAY + "Click to modify"
        );
        inventory.setItem(2, spawnChanceDisplay);

        // Spawn delay
        ItemStack spawnDelayDisplay = createGuiItem(Material.CLOCK,
                ChatColor.WHITE + "Spawn Delay",
                ChatColor.GRAY + "Current spawn delay",
                ChatColor.WHITE + " " + tempConfig.getMinSpawnDelay() + " - " + tempConfig.getMaxSpawnDelay() + " seconds",
                ChatColor.GRAY + "Click to modify"
        );
        inventory.setItem(3, spawnDelayDisplay);

        // Spawn delay weight
        ItemStack spawnDelayWeightDisplay = createGuiItem(Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                ChatColor.WHITE + "Spawn Delay Weight",
                ChatColor.GRAY + "Weight for the delay range chance",
                ChatColor.WHITE + " " + tempConfig.getSpawnDelayWeight(),
                ChatColor.GRAY + "Click to modify"
        );
        inventory.setItem(4, spawnDelayWeightDisplay);

        // Replace type
        String toggle = tempConfig.isReplaceTypeOnly() ? ChatColor.GREEN + "Same Type" : ChatColor.RED + "Any";
        ItemStack replaceTypeDisplay = createGuiItem(
                Material.SPAWNER,
                ChatColor.GOLD + "Replace any hostile mob",
                ChatColor.GRAY + "Status: " + toggle,
                ChatColor.YELLOW + "Click to toggle"
        );
        inventory.setItem(5, replaceTypeDisplay);

        // Time till despawn
        ItemStack despawnDisplay = createGuiItem(Material.CLOCK,
                ChatColor.WHITE + "Despawn Timer",
                ChatColor.GRAY + "The time(in seconds) until the power mob will despawn",
                ChatColor.WHITE + " " + tempConfig.getMinDespawnTime() + " - " + tempConfig.getMaxDespawnTime() + " seconds",
                ChatColor.GRAY + "Click to modify"
        );
        inventory.setItem(6, despawnDisplay);

        ItemStack xDisplay = createGuiItem(
                Material.COMPASS,
                ChatColor.YELLOW + "X axis",
                ChatColor.GRAY + "Current x spawnable range:",
                ChatColor.GREEN + " " + tempConfig.getMinX() + " - " + tempConfig.getMaxX(),
                ChatColor.GRAY + "Click to modify"
        );
        inventory.setItem(12, xDisplay);

        ItemStack yDisplay = createGuiItem(
                Material.COMPASS,
                ChatColor.YELLOW + "Y axis",
                ChatColor.GRAY + "Current y spawnable range:",
                ChatColor.GREEN + " " + tempConfig.getMinY() + " - " + tempConfig.getMaxY(),
                ChatColor.GRAY + "Click to modify"
        );
        inventory.setItem(13, yDisplay);

        ItemStack zDisplay = createGuiItem(
                Material.COMPASS,
                ChatColor.YELLOW + "Z axis",
                ChatColor.GRAY + "Current z spawnable range:",
                ChatColor.GREEN + " " + tempConfig.getMinZ() + " - " + tempConfig.getMaxZ(),
                ChatColor.GRAY + "Click to modify"
        );
        inventory.setItem(14, zDisplay);

        // Dimensions config
        List<String> dimensions = new ArrayList<>();
        dimensions.add(ChatColor.GRAY + "Click to modify");
        for (World.Environment dimension : tempConfig.getDimensions()) {
            dimensions.add(ChatColor.BLUE + dimension.name());
        }
        ItemStack dimensionsDisplay = createGuiItem(
                Material.END_PORTAL_FRAME,
                ChatColor.LIGHT_PURPLE + "Current spawnable dimensions:",
                dimensions
        );
        inventory.setItem(10, dimensionsDisplay);

        // Time conditions
        List<String> times = new ArrayList<>();
        times.add(ChatColor.GRAY + "Click to modify");
        for (SpawnCondition.TimeCondition time : tempConfig.getTimeConditions()) {
            times.add(ChatColor.BLUE + time.name());
        }
        ItemStack timeDisplay = createGuiItem(
                Material.DAYLIGHT_DETECTOR,
                ChatColor.LIGHT_PURPLE + "Current spawnable times:",
                times
        );
        inventory.setItem(16, timeDisplay);

        // Display biome groups
        displayBiomeGroups(tempConfig);

        // Back button
        addBackButton(53, ChatColor.RED + "Back to Mob Editor");

        // Save button
        ItemStack saveButton = createGuiItem(
                Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save spawn condition changes"
        );
        inventory.setItem(45, saveButton);

    }


    private void displayBiomeGroups(SpawnCondition spawnCondition) {
        BiomeGroupManager biomeGroupManager = spawnCondition.getBiomeGroupManager();
        Set<String> groupNames = biomeGroupManager.getGroupNames();

        int slot = 28;
        int maxSlotsPerRow = 7;
        int currentRow = 0;
        int currentCol = 0;

        for (String groupName : groupNames) {
            if (slot >= 45) break; // Don't go beyond row 5 (slots 45-53 reserved)

            Set<Biome> biomesInGroup = biomeGroupManager.getDefaultBiomesForGroup(groupName);
            List<String> biomes = new ArrayList<>();
            biomes.add(ChatColor.GRAY + "Left-click to toggle");
            biomes.add(ChatColor.GRAY + "Right-click to cycle biomes");
            for (Biome biome : biomesInGroup) {
                String biomeList = ChatColor.GRAY + "";
                if (biomeGroupManager.isEnabledBiome(groupName, biome)) {
                    biomeList = biomeList + ChatColor.DARK_GREEN + "✓ ";
                } else {
                    biomeList = biomeList + ChatColor.RED + "✗ ";
                }
                if (biomeGroupManager.isSelectedBiome(groupName, biome)) {
                    biomeList = biomeList + ChatColor.GREEN + biome.name();
                } else {
                    biomeList = biomeList + ChatColor.GRAY + biome.name();
                }
                biomes.add(biomeList);
            }

            Material material = biomeGroupManager.getGroupMaterial(groupName);
            ItemStack biomeGroupItem = createGuiItem(
                    material,
                    ChatColor.YELLOW + "~~~~~" + groupName + "~~~~~",
                    biomes
            );
            inventory.setItem(slot, biomeGroupItem);

            currentCol++;
            if (currentCol >= maxSlotsPerRow) {
                currentCol = 0;
                currentRow++;
                slot = 28 + (currentRow * 9);
            } else {
                slot++;
            }
        }
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        if (slot == 2) {
            startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                tempConfig.setSpawnChance((double) value);
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        if (slot == 3) {
            startChatInput(player, ChatInputType.SPAWN_DELAY, (value, p) -> {
                updateSpawnDelay(value);
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        if (slot == 4) {
            startChatInput(player, ChatInputType.WEIGHT, (value, p) -> {
                tempConfig.setSpawnDelayWeight((int) value);
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        if (slot == 5) {
            tempConfig.setReplaceTypeOnly(!tempConfig.isReplaceTypeOnly());
            build();
        }
        if (slot == 6) {
            startChatInput(player, ChatInputType.DESPAWN_TIME, (value, p) -> {
                updateDespawnTime(value);
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        if (slot == 10) {
            startChatInput(player, ChatInputType.DIMENSIONS, (value, p) -> {
                updateDimensions(value);
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        if (slot == 12) {
            startChatInput(player, ChatInputType.COORDINATES, (value, p) -> {
                updateCoordinates(value, "x");
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        if (slot == 13) {
            startChatInput(player, ChatInputType.COORDINATES, (value, p) -> {
                updateCoordinates(value, "y");
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        if (slot == 14) {
            startChatInput(player, ChatInputType.COORDINATES, (value, p) -> {
                updateCoordinates(value, "z");
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        if (slot == 16) {
            startChatInput(player, ChatInputType.TIMES, (value, p) -> {
                updateTimes(value);
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }
        // Handle biome group toggles (slots 28-44)
        if (slot >= 28 && slot < 45) {
            ItemStack clickedItem = inventory.getItem(slot);
            if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                String displayName = clickedItem.getItemMeta().getDisplayName();

                // Extract group name from display name
                String groupName = displayName.replace(ChatColor.YELLOW + "~~~~~", "").replace("~~~~~", "").replace(ChatColor.GRAY + "", "").trim();

                if (tempConfig.getBiomeGroupManager().getGroupNames().contains(groupName)) {

                    if (clickType == ClickType.RIGHT) {
                        tempConfig.getBiomeGroupManager().cycleBiomeChoice(groupName);
                        build();
                    } else if (clickType == ClickType.LEFT) {
                        tempConfig.getBiomeGroupManager().toggleSelectedBiome(groupName);
                        build();
                    }
                    return true;
                }
            }
        }

        // Save button
        if (slot == 45) {
            saveChanges(player);
            player.sendMessage(ChatColor.GREEN + "Spawn conditions saved!");
            return true;
        }

        // Back button
        if (slot == 53) {
            tempConfig = null;
            currentPage = 0;
            pageManager.navigateBack(player);
            return true;
        }

        return false;

    }

    private void updateSpawnDelay(Object input) {
        if (input instanceof Integer n) {;
            tempConfig.setMinSpawnDelay(n);
            tempConfig.setMaxSpawnDelay(n);
            return;
        }
        String delay = (String) input;
        String[] parts = delay.split("-");
        if (parts.length == 2) {
            tempConfig.setMinSpawnDelay(Integer.parseInt(parts[0]));
            tempConfig.setMaxSpawnDelay(Integer.parseInt(parts[1]));
        }
    }


    private void updateDimensions(Object input) {
        String config = (String) input;
        String[] parts = config.split(":");
        String o = parts[0].toLowerCase();
        String n = parts[1].toLowerCase();
        String e = parts[2].toLowerCase();
        if (o.equals("true") || o.equals("t")) {
            tempConfig.getDimensions().add(World.Environment.NORMAL);
        } else {
            tempConfig.getDimensions().remove(World.Environment.NORMAL);
        }
        if (n.equals("true") || n.equals("t")) {
            tempConfig.getDimensions().add(World.Environment.NETHER);
        } else {
            tempConfig.getDimensions().remove(World.Environment.NETHER);
        }
        if (e.equals("true") || e.equals("t")) {
            tempConfig.getDimensions().add(World.Environment.THE_END);
        } else {
            tempConfig.getDimensions().remove(World.Environment.THE_END);
        }
    }

    private void updateTimes(Object input) {
        String config = (String) input;
        String[] parts = config.split(":");
        String d = parts[0].toLowerCase();
        String n = parts[1].toLowerCase();
        if (d.equals("true") || d.equals("t")) {
            tempConfig.getTimeConditions().add(SpawnCondition.TimeCondition.DAY);
        } else {
            tempConfig.getTimeConditions().remove(SpawnCondition.TimeCondition.DAY);
        }
        if (n.equals("true") || n.equals("t")) {
            tempConfig.getTimeConditions().add(SpawnCondition.TimeCondition.NIGHT);
        } else {
            tempConfig.getTimeConditions().remove(SpawnCondition.TimeCondition.NIGHT);
        }
    }

    private void updateDespawnTime(Object input) {
        if (input instanceof Integer n) {;
            tempConfig.setMinDespawnTime(n);
            tempConfig.setMaxDespawnTime(n);
            return;
        }
        String delay = (String) input;
        String[] parts = delay.split("-");
        if (parts.length == 2) {
            tempConfig.setMinDespawnTime(Integer.parseInt(parts[0]));
            tempConfig.setMaxDespawnTime(Integer.parseInt(parts[1]));
        }
    }

    private void saveChanges(Player player) {
        IPowerMobConfig mob;
        PowerManager configManager = pageManager.getPlugin().getConfigManager();
        if (configManager.isSaveInProgress()) {
            return;
        }
        if (selectedMobId != null) {
            mob = configManager.getPowerMob(selectedMobId);
        } else {
            mob = configManager.getRandomMobConfig();
        }
        mob.setSpawnCondition(tempConfig);

        if (selectedMobId != null) {
            configManager.savePowerMob(selectedMobId, mob.toConfigMap());
        } else {
            configManager.saveRandomMob(mob.toConfigMap());
        }
        tempConfig = null;
        currentPage = 0;
        pageManager.navigateBack(player);
    }

    private void updateCoordinates(Object input, String axis) {
        String[] parts = ((String) input).split(":");
        String min = parts[0];
        String max = parts[1];
        switch (axis) {

            case "x":
                if (min.equalsIgnoreCase("infinity")) {
                    tempConfig.setMinX(Integer.MIN_VALUE);
                } else {
                    tempConfig.setMinX(Integer.parseInt(min));
                }
                if (max.equalsIgnoreCase("infinity")) {
                    tempConfig.setMaxX(Integer.MAX_VALUE);
                } else {
                    tempConfig.setMaxX(Integer.parseInt(max));
                }
                break;
            case "y":
                if (min.equalsIgnoreCase("infinity")) {
                    tempConfig.setMinY(Integer.MIN_VALUE);
                } else {
                    tempConfig.setMinY(Integer.parseInt(min));
                }
                if (max.equalsIgnoreCase("infinity")) {
                    tempConfig.setMaxY(Integer.MAX_VALUE);
                } else {
                    tempConfig.setMaxY(Integer.parseInt(max));
                }
                break;
            case "z":
                if (min.equalsIgnoreCase("infinity")) {
                    tempConfig.setMinZ(Integer.MIN_VALUE);
                } else {
                    tempConfig.setMinZ(Integer.parseInt(min));
                }
                if (max.equalsIgnoreCase("infinity")) {
                    tempConfig.setMaxZ(Integer.MAX_VALUE);
                } else {
                    tempConfig.setMaxZ(Integer.parseInt(max));
                }
                break;
        }
    }

}
