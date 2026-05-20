package com.powermobs.UI.pages;

import com.powermobs.UI.GUIManager;
import com.powermobs.UI.framework.*;
import com.powermobs.config.IPowerMobConfig;
import com.powermobs.config.SpawnCondition;
import org.bukkit.*;
import org.bukkit.generator.structure.Structure;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.structure.StructureManager;

import java.util.*;

public class SpawnConditionBulkListPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private static final int ITEMS_PER_PAGE = 45; // Reserve 9 slots for navigation
    private final List<Structure> structures = new ArrayList<>();
    private SpawnCondition originConfig;
    private SpawnCondition tempConfig;
    private final Map<Integer, Integer> structureChunkSelection = new LinkedHashMap<>();
    private int pageNumber = 0;
    private String selectedMobId = null;
    private boolean unsavedChanges = false;

    public SpawnConditionBulkListPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.BLUE + "Spawn Condition Bulk List Selection");
    }


    @Override
    public void build() {
        inventory.clear();
        PlayerSessionData player = guiManager.getCurrentPlayer();
        pageManager.getPlugin().debug("Building GUI for Spawn Condition Bulk List.", "ui");

        selectedMobId = guiManager.getCurrentPlayer().getSelectedMobId();
        IPowerMobConfig mob;
        if (selectedMobId != null) {
            mob = pageManager.getPlugin().getConfigManager().getPowerMob(selectedMobId);
        } else {
            mob = pageManager.getPlugin().getConfigManager().getRandomMobConfig();
        }
        if (!player.isEditing()) {
            player.setSelectedItemId(null);
            pageManager.getPlugin().debug("Retrieving list source data", "ui");
            originConfig = mob.getSpawnCondition();
            tempConfig = new SpawnCondition(originConfig);
            player.setEditing(true);
        }
        unsavedChanges = (!originConfig.toConfigMap().equals(tempConfig.toConfigMap()));
        structures.clear();
        buildStructureItems();

        addNavigationControls();

        addActionButtons();
    }

    private void buildStructureItems() {
        inventory.clear();

        List<Structure> sorted = getSortedStructure();
        this.structures.clear();
        this.structures.addAll(sorted);
        List<List<Structure>> chunks = chunkStructure(sorted, 10);

        List<ItemStack> chunkItems = new ArrayList<>(chunks.size());

        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            List<Structure> chunk = chunks.get(chunkIndex);
            int selectedIndex = structureChunkSelection.getOrDefault(chunkIndex, 0);
            if (selectedIndex >= chunk.size()) {
                selectedIndex = 0;
            }

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Left click: toggle selected structure");
            lore.add(ChatColor.GRAY + "Right click: cycle selection");
            lore.add(ChatColor.DARK_GRAY + " ");

            for (int i = 0; i < chunk.size(); i++) {
                Structure type = chunk.get(i);
                NamespacedKey key = Registry.STRUCTURE.getKey(type);
                boolean enabled = tempConfig.getStructures() != null && tempConfig.getStructures().contains(type);

                ChatColor color = enabled ? ChatColor.DARK_GREEN : ChatColor.RED;
                String marker = enabled ? "✓ " : "✗ ";
                String selector = i == selectedIndex ? ChatColor.YELLOW + "► " : ChatColor.DARK_GRAY + "  ";
                String keyText = key == null ? "<unknown>" : key.toString();

                lore.add(selector + color + marker + ChatColor.WHITE + keyText);
            }

            int start = chunkIndex * 10 + 1;
            int end = start + chunk.size() - 1;
            String title = ChatColor.LIGHT_PURPLE + "Structures " + start + "-" + end;

            chunkItems.add(createGuiItem(Material.BRICKS, title, lore));
        }

        int startIndex = pageNumber * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int globalIndex = startIndex + i;
            if (globalIndex < chunkItems.size()) {
                inventory.setItem(i, chunkItems.get(globalIndex));
            } else {
                inventory.setItem(i, null);
            }
        }
    }

    private List<Structure> getSortedStructure() {
        Comparator<Structure> comparator = Comparator
                .comparingInt((Structure type) -> {
                    NamespacedKey key = Registry.STRUCTURE.getKey(type);
                    return key != null && "minecraft".equalsIgnoreCase(key.getNamespace()) ? 0 : 1;
                })
                .thenComparing(type -> {
                    NamespacedKey key = Registry.STRUCTURE.getKey(type);
                    return key == null ? "" : key.toString();
                });

        return Registry.STRUCTURE.stream()
                .sorted(comparator)
                .toList();
    }

    private List<List<Structure>> chunkStructure(List<Structure> sorted, int chunkSize) {
        List<List<Structure>> chunks = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i += chunkSize) {
            chunks.add(new ArrayList<>(sorted.subList(i, Math.min(i + chunkSize, sorted.size()))));
        }
        return chunks;
    }

//    private void displayItems() {
//        int startIndex = pageNumber * ITEMS_PER_PAGE;
//        int slot = 0;
//        List<String> ids = new ArrayList<>(availableItems.keySet());
//        for (int i = startIndex; i < Math.min(startIndex + ITEMS_PER_PAGE, availableItems.size()); i++) {
//            String id = ids.get(i);
//            ItemStack item = availableItems.get(id).clone();
//            inventory.setItem(slot++, item);
//        }
//    }
//
//    private void displayBiomeGroups(SpawnCondition spawnCondition) {
//        BiomeGroupManager biomeGroupManager = spawnCondition.getBiomeGroupManager();
//        Set<String> groupNames = biomeGroupManager.getGroupNames();
//
//        int slot = 28;
//        int maxSlotsPerRow = 7;
//        int currentRow = 0;
//        int currentCol = 0;
//
//        for (String groupName : groupNames) {
//            if (slot >= 45) break; // Don't go beyond row 5 (slots 45-53 reserved)
//
//            Set<Biome> biomesInGroup = biomeGroupManager.getDefaultBiomesForGroup(groupName);
//            List<String> biomes = new ArrayList<>();
//            biomes.add(ChatColor.GRAY + "Left-click to toggle");
//            biomes.add(ChatColor.GRAY + "Right-click to cycle biomes");
//            for (Biome biome : biomesInGroup) {
//                String biomeList = ChatColor.GRAY + "";
//                if (biomeGroupManager.isEnabledBiome(groupName, biome)) {
//                    biomeList = biomeList + ChatColor.DARK_GREEN + "✓ ";
//                } else {
//                    biomeList = biomeList + ChatColor.RED + "✗ ";
//                }
//                if (biomeGroupManager.isSelectedBiome(groupName, biome)) {
//                    biomeList = biomeList + ChatColor.GREEN + biome.name();
//                } else {
//                    biomeList = biomeList + ChatColor.GRAY + biome.name();
//                }
//                biomes.add(biomeList);
//            }
//
//            Material material = biomeGroupManager.getGroupMaterial(groupName);
//            ItemStack biomeGroupItem = createGuiItem(
//                    material,
//                    ChatColor.YELLOW + "~~~~~" + groupName + "~~~~~",
//                    biomes
//            );
//            inventory.setItem(slot, biomeGroupItem);
//
//            currentCol++;
//            if (currentCol >= maxSlotsPerRow) {
//                currentCol = 0;
//                currentRow++;
//                slot = 28 + (currentRow * 9);
//            } else {
//                slot++;
//            }
//        }
//    }


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
        int totalItems = (this.structures.size() + 9) / 10;
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

        // Back button
        addBackButton(52, ChatColor.RED + "Back", unsavedChanges);
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        if (slot >= 45) {
            return handleNavigationClick(player, slot, clickType);
        }
        return handleStructureClick(slot, clickType);
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
                int totalItems = structureChunkSelection.size();
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
                    originConfig = null;
                    tempConfig = null;
                    build();
                }
                return true;
            case 52: // Back
                structureChunkSelection.clear();
                originConfig = null;
                tempConfig = null;
                pageNumber = 0;
                structures.clear();
                guiManager.getCurrentPlayer().setEditing(false);
                pageManager.navigateBack(player);
                return true;
        }
        return false;
    }

    private boolean handleStructureClick( int slot, ClickType clickType) {
        int chunkIndex = (pageNumber * ITEMS_PER_PAGE) + slot;


        List<List<Structure>> chunks = chunkStructure(getSortedStructure(), 10);
        if (chunkIndex < 0 || chunkIndex >= chunks.size()) return false;
        List<Structure> chunk = chunks.get(chunkIndex);

        int selectedIndex = structureChunkSelection.getOrDefault(chunkIndex, 0);
        if (selectedIndex >= chunk.size()) selectedIndex = 0;

        if (clickType == ClickType.RIGHT) {
            selectedIndex = (selectedIndex + 1) % chunk.size();
            structureChunkSelection.put(chunkIndex, selectedIndex);
            build();
            return true;
        }

        if (clickType == ClickType.LEFT) {
            Structure selected = chunk.get(selectedIndex);
            Set<Structure> enabled = tempConfig.getStructures();
            if (enabled == null) {
                enabled = new LinkedHashSet<>();
                tempConfig.setStructures(enabled);
            }

            if (enabled.contains(selected)) enabled.remove(selected);
            else enabled.add(selected);

            unsavedChanges = true;
            build();
            return true;
        }

        return false;
    }


    private void saveConfig() {
        IPowerMobConfig mob;
        PlayerSessionData session = guiManager.getCurrentPlayer();
        if (session.getType().equals(PowerMobType.RANDOM)) {
            mob = pageManager.getPlugin().getConfigManager().getRandomMobConfig();
        } else {
            mob = pageManager.getPlugin().getConfigManager().getPowerMob(selectedMobId);
        }
        mob.setSpawnCondition(tempConfig);
        if (session.getType().equals(PowerMobType.RANDOM)) {
            pageManager.getPlugin().getConfigManager().saveRandomMob(mob.toConfigMap());
        } else {
            pageManager.getPlugin().getConfigManager().savePowerMob(selectedMobId, mob.toConfigMap());
        }
    }
}