package com.powermobs.UI.pages;

import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.config.*;
import com.powermobs.utils.WorldCatalog;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class MobSpawnConditionsPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private String selectedMobId;
    private int currentPage = 0;
    private SpawnCondition tempConfig = null;

    private List<String> worldOrder = new ArrayList<>();
    private Map<String, Boolean> worldActiveMap = new LinkedHashMap<>();
    private Set<String> knownWorldNames = new LinkedHashSet<>(); // NEW: Bukkit + Multiverse known worlds
    private int selectedWorldIndex = 0;

    private int selectedBoundingBoxIndex = 0;
    private boolean delBoundingBox = false;

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
            selectedWorldIndex = 0;
            selectedBoundingBoxIndex = 0;
        }
        boolean hasChanges = !tempConfig.toConfigMap().equals(mob.getSpawnCondition().toConfigMap());

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

        List<BoundingBox> boxes = tempConfig.getBoundingBoxes();
        List<String> boundingBoxes = new ArrayList<>();

        if (boxes == null || boxes.isEmpty()) {
            tempConfig.setBoundingBoxes(new ArrayList<>());
            tempConfig.getBoundingBoxes().add(new BoundingBox());
            boxes = tempConfig.getBoundingBoxes();
            pageManager.getPlugin().debug("Unable to find bounding boxes. Creating default.", "ui");
        }
            if (selectedBoundingBoxIndex < 0) {
                selectedBoundingBoxIndex = 0;
            }
        if (selectedBoundingBoxIndex >= boxes.size()) {
            selectedBoundingBoxIndex = boxes.size() - 1;
        }

        int visibleCount = Math.min(5, boxes.size());
        int startIndex = (boxes.size() <= visibleCount) ? 0 : selectedBoundingBoxIndex - 2;
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (startIndex > boxes.size() - visibleCount) {
            startIndex = boxes.size() - visibleCount;
        }

        for (int i = 0; i < visibleCount; i++) {
            int index = startIndex + i;
            BoundingBox box = boxes.get(index);

            List<String> boxLines = new ArrayList<>();
            boxLines.add(ChatColor.GRAY + "X: " + ChatColor.WHITE + box.getXPair());
            boxLines.add(ChatColor.GRAY + "Y: " + ChatColor.WHITE + box.getYPair());
            boxLines.add(ChatColor.GRAY + "Z: " + ChatColor.WHITE + box.getZPair());

            if (index == selectedBoundingBoxIndex) {
                boundingBoxes.add(ChatColor.YELLOW + "► " + ChatColor.GOLD + "Bounding Box " + (index + 1) + ChatColor.YELLOW + " ◄");
                boundingBoxes.addAll(boxLines);
            } else {
                boundingBoxes.add(ChatColor.BLUE + "Bounding Box " + (index + 1));
                boundingBoxes.addAll(boxLines);
            }

            if (i != visibleCount - 1) {
                boundingBoxes.add(ChatColor.DARK_GRAY + " ");
            }
        }

        if (boxes.size() > visibleCount) {
            boundingBoxes.add(ChatColor.GRAY + "Showing " + visibleCount + " of " + boxes.size());
        }

        if (delBoundingBox) {
            boundingBoxes.add(ChatColor.RED + "DELETE?");
        }

        boundingBoxes.add(ChatColor.GRAY + "Left click: delete");
        boundingBoxes.add(ChatColor.GRAY + "Right click: cycle selection");

        ItemStack boxDisplay = createGuiItem(
                Material.COMPASS,
                ChatColor.YELLOW + "--Bounding Boxes--",
                boundingBoxes
        );
        inventory.setItem(13, boxDisplay);

        ItemStack addBoxDisplay = createGuiItem(
                Material.LODESTONE,
                ChatColor.GREEN + "Click to add a bounding box"
        );
        inventory.setItem(22, addBoxDisplay);

        buildWorldSelectorItem();

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

        // Save button
        ItemStack saveButton = createGuiItem(
                Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save spawn condition changes"
        );
        inventory.setItem(45, saveButton);

        // Back button
        addBackButton(53, ChatColor.RED + "Back to Mob List", hasChanges);
    }

    private void buildWorldSelectorItem() {
        refreshWorldLists();

        if (selectedWorldIndex < 0) selectedWorldIndex = 0;
        if (!worldOrder.isEmpty() && selectedWorldIndex >= worldOrder.size()) selectedWorldIndex = 0;

        Set<String> selected = tempConfig.getWorlds(); // null = all worlds allowed

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Left-click: toggle world");
        lore.add(ChatColor.GRAY + "Right-click: cycle selection");

        if (worldOrder.isEmpty()) {
            lore.add(ChatColor.RED + "No worlds found.");
            inventory.setItem(10, createGuiItem(
                    Material.END_PORTAL_FRAME,
                    ChatColor.LIGHT_PURPLE + "Spawnable Worlds",
                    lore
            ));
            return;
        }

        String current = worldOrder.get(selectedWorldIndex);
        lore.add(ChatColor.YELLOW + "Selected: " + ChatColor.WHITE + current);

        for (String worldName : worldOrder) {
            boolean isKnown = knownWorldNames.contains(worldName);
            boolean isActive = Boolean.TRUE.equals(worldActiveMap.get(worldName));
            boolean allowed = (selected == null) || selected.contains(worldName);

            if (!isKnown) {
                // Unknown / not real world => show dark gray and indicate inactive
                String prefix = allowed ? (ChatColor.DARK_GRAY + "✓ ") : (ChatColor.DARK_GRAY + "✗ ");
                lore.add(prefix + ChatColor.DARK_GRAY + worldName + " (inactive)");
                continue;
            }

            ChatColor lineColor = isActive ? ChatColor.GRAY : ChatColor.DARK_GRAY;
            String prefix = allowed ? (ChatColor.DARK_GREEN + "✓ ") : (ChatColor.RED + "✗ ");
            String suffix = isActive ? "" : (ChatColor.DARK_GRAY + " (inactive)");

            ChatColor nameColor = worldName.equals(current) ? ChatColor.GREEN : lineColor;
            lore.add(prefix + nameColor + worldName + suffix);
        }

        inventory.setItem(10, createGuiItem(
                Material.END_PORTAL_FRAME,
                ChatColor.LIGHT_PURPLE + "Spawnable Worlds",
                lore
        ));
    }

    private void refreshWorldLists() {
        // Known worlds = Bukkit + Multiverse (even if inactive/unloaded)
        worldActiveMap = loadWorldActiveMap();
        knownWorldNames = new LinkedHashSet<>(worldActiveMap.keySet());

        worldOrder = new ArrayList<>(knownWorldNames);

        // Append unknown worlds found in config so user can see them (inactive, not enableable)
        Set<String> selected = tempConfig != null ? tempConfig.getWorlds() : null;
        if (selected != null) {
            for (String w : selected) {
                if (!knownWorldNames.contains(w) && !worldActiveMap.containsKey(w)) {
                    worldActiveMap.put(w, false);
                    worldOrder.add(w);
                }
            }
        }
    }

    private Map<String, Boolean> loadWorldActiveMap() {
        return new LinkedHashMap<>(WorldCatalog.getKnownWorldActiveMap());
    }

    private void toggleSelectedWorld() {
        if (worldOrder.isEmpty()) return;

        String worldName = worldOrder.get(selectedWorldIndex);
        boolean isKnown = knownWorldNames.contains(worldName);

        Set<String> worlds = tempConfig.getWorlds();
        if (worlds == null) {
            // Convert "all enabled" into an explicit set of known worlds (unknown worlds excluded)
            worlds = new LinkedHashSet<>(knownWorldNames);
            tempConfig.setWorlds(worlds);
        }

        if (!isKnown) {
            // Unknown worlds cannot be enabled; allow removal if present
            worlds.remove(worldName);
            return;
        }

        if (worlds.contains(worldName)) {
            worlds.remove(worldName);
        } else {
            worlds.add(worldName);
        }
    }

    private void pruneUnknownWorldsBeforeSave() {
        if (tempConfig == null || tempConfig.getWorlds() == null) return;

        Set<String> known = loadWorldActiveMap().keySet();
        tempConfig.getWorlds().removeIf(w -> !known.contains(w));
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
            if (worldOrder.isEmpty()) return true;

            if (clickType == ClickType.RIGHT) {
                selectedWorldIndex = (selectedWorldIndex + 1) % worldOrder.size();
                build();
                return true;
            }
            if (clickType == ClickType.LEFT) {
                toggleSelectedWorld();
                build();
                return true;
            }

        }
        if (slot == 13) {
            if (clickType == ClickType.RIGHT) {
                selectedBoundingBoxIndex++;
                if (selectedBoundingBoxIndex >= tempConfig.getBoundingBoxes().size()) {
                    selectedBoundingBoxIndex = 0;
                }
                delBoundingBox = false;            }
            if (clickType == ClickType.LEFT) {
                if (delBoundingBox) {
                    tempConfig.getBoundingBoxes().remove(selectedBoundingBoxIndex);
                    selectedBoundingBoxIndex = 0;
                }
                delBoundingBox = !delBoundingBox;
            }
            build();
            return true;
        }

        if (slot == 16) {
            startChatInput(player, ChatInputType.TIMES, (value, p) -> {
                updateTimes(value);
                pageManager.navigateTo("mob_spawn_conditions", false, p);
            });
        }

        if (slot == 22) {
            startChatInput(player, ChatInputType.COORDINATES, (value, p) -> {
                updateCoordinates(value);
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
        pruneUnknownWorldsBeforeSave();
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

    private void updateCoordinates(Object input) {
        String[] parts = ((String) input).split(":");
        String[] x = parts[0].split(",");
        String[] y = parts[1].split(",");
        String[] z = parts[2].split(",");
        int minX, maxX, minY, maxY, minZ, maxZ;

        if (x[0].equalsIgnoreCase("infinity")) {
            minX = Integer.MIN_VALUE;
        } else {
            minX = Integer.parseInt(x[0]);
        }
        if (x[1].equalsIgnoreCase("infinity")) {
            maxX = Integer.MAX_VALUE;
        } else {
            maxX = Integer.parseInt(x[1]);
        }
        if (y[0].equalsIgnoreCase("infinity")) {
            minY = Integer.MIN_VALUE;
        } else {
            minY = Integer.parseInt(y[0]);
        }
        if (y[1].equalsIgnoreCase("infinity")) {
            maxY = Integer.MAX_VALUE;
        } else {
            maxY = Integer.parseInt(y[1]);
        }
        if (z[0].equalsIgnoreCase("infinity")) {
            minZ = Integer.MIN_VALUE;
        } else {
            minZ = Integer.parseInt(z[0]);
        }
        if (z[1].equalsIgnoreCase("infinity")) {
            maxZ = Integer.MAX_VALUE;
        } else {
            maxZ = Integer.parseInt(z[1]);
        }

        tempConfig.getBoundingBoxes().add(new BoundingBox(minX, maxX, minY, maxY, minZ, maxZ));
    }

}
