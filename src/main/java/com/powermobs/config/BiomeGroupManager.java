package com.powermobs.config;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.*;

/**
 * Manages biome groups for spawn conditions
 */
@Getter
@Setter
public class BiomeGroupManager {

    private static final Map<String, Set<Biome>> DEFAULT_BIOME_GROUPS = new LinkedHashMap<>();
    private static final Map<String, Material> groupMaterial = new LinkedHashMap<>();

    static {
        // Forest Group
        Set<Biome> forestBiomes = new LinkedHashSet<>();
        forestBiomes.add(Biome.FOREST);
        forestBiomes.add(Biome.BIRCH_FOREST);
        forestBiomes.add(Biome.DARK_FOREST);
        forestBiomes.add(Biome.FLOWER_FOREST);
        forestBiomes.add(Biome.TAIGA);
        forestBiomes.add(Biome.OLD_GROWTH_BIRCH_FOREST);
        forestBiomes.add(Biome.OLD_GROWTH_PINE_TAIGA);
        forestBiomes.add(Biome.OLD_GROWTH_SPRUCE_TAIGA);
        forestBiomes.add(Biome.CHERRY_GROVE); //TODO: if needed, cast check for older versions
        forestBiomes.add(Biome.PALE_GARDEN);  //TODO: if needed, cast check for older versions
        DEFAULT_BIOME_GROUPS.put("Forest", forestBiomes);
        groupMaterial.put("Forest", Material.OAK_SAPLING);

        // Ocean Group
        Set<Biome> oceanBiomes = new LinkedHashSet<>();
        oceanBiomes.add(Biome.OCEAN);
        oceanBiomes.add(Biome.DEEP_OCEAN);
        oceanBiomes.add(Biome.WARM_OCEAN);
        oceanBiomes.add(Biome.LUKEWARM_OCEAN);
        oceanBiomes.add(Biome.COLD_OCEAN);
        oceanBiomes.add(Biome.DEEP_COLD_OCEAN);
        oceanBiomes.add(Biome.DEEP_LUKEWARM_OCEAN);
        oceanBiomes.add(Biome.FROZEN_OCEAN);
        oceanBiomes.add(Biome.DEEP_FROZEN_OCEAN);
        DEFAULT_BIOME_GROUPS.put("Ocean", oceanBiomes);
        groupMaterial.put("Ocean", Material.WATER_BUCKET);

        // Mountain Group
        Set<Biome> mountainBiomes = new LinkedHashSet<>();
        mountainBiomes.add(Biome.WINDSWEPT_HILLS);
        mountainBiomes.add(Biome.WINDSWEPT_FOREST);
        mountainBiomes.add(Biome.WINDSWEPT_GRAVELLY_HILLS);
        mountainBiomes.add(Biome.STONY_PEAKS);
        mountainBiomes.add(Biome.JAGGED_PEAKS);
        mountainBiomes.add(Biome.FROZEN_PEAKS);
        mountainBiomes.add(Biome.SNOWY_SLOPES);
        DEFAULT_BIOME_GROUPS.put("Mountain", mountainBiomes);
        groupMaterial.put("Mountain", Material.GRAVEL);

        // Desert Group
        Set<Biome> desertBiomes = new LinkedHashSet<>();
        desertBiomes.add(Biome.DESERT);
        desertBiomes.add(Biome.BADLANDS);
        desertBiomes.add(Biome.ERODED_BADLANDS);
        desertBiomes.add(Biome.WOODED_BADLANDS);
        DEFAULT_BIOME_GROUPS.put("Desert", desertBiomes);
        groupMaterial.put("Desert", Material.SAND);

        // Plains Group
        Set<Biome> plainsBiomes = new LinkedHashSet<>();
        plainsBiomes.add(Biome.PLAINS);
        plainsBiomes.add(Biome.SUNFLOWER_PLAINS);
        plainsBiomes.add(Biome.MEADOW);
        plainsBiomes.add(Biome.SAVANNA);
        plainsBiomes.add(Biome.SAVANNA_PLATEAU);
        plainsBiomes.add(Biome.WINDSWEPT_SAVANNA);
        DEFAULT_BIOME_GROUPS.put("Plains", plainsBiomes);
        groupMaterial.put("Plains", Material.GRASS_BLOCK);

        // Swamp Group
        Set<Biome> swampBiomes = new LinkedHashSet<>();
        swampBiomes.add(Biome.SWAMP);
        swampBiomes.add(Biome.MANGROVE_SWAMP);
        DEFAULT_BIOME_GROUPS.put("Swamp", swampBiomes);
        groupMaterial.put("Swamp", Material.MUD);

        // Underground Group
        Set<Biome> undergroundBiomes = new LinkedHashSet<>();
        undergroundBiomes.add(Biome.DEEP_DARK);
        undergroundBiomes.add(Biome.DRIPSTONE_CAVES);
        undergroundBiomes.add(Biome.LUSH_CAVES);
        DEFAULT_BIOME_GROUPS.put("Underground", undergroundBiomes);
        groupMaterial.put("Underground", Material.STONE);

        // Jungle biomes
        Set<Biome> jungleBiomes = new LinkedHashSet<>();
        jungleBiomes.add(Biome.JUNGLE);
        jungleBiomes.add(Biome.SPARSE_JUNGLE);
        jungleBiomes.add(Biome.BAMBOO_JUNGLE);
        DEFAULT_BIOME_GROUPS.put("Jungle", jungleBiomes);
        groupMaterial.put("Jungle", Material.JUNGLE_SAPLING);


        // Nether Group
        Set<Biome> netherBiomes = new LinkedHashSet<>();
        netherBiomes.add(Biome.NETHER_WASTES);
        netherBiomes.add(Biome.SOUL_SAND_VALLEY);
        netherBiomes.add(Biome.CRIMSON_FOREST);
        netherBiomes.add(Biome.WARPED_FOREST);
        netherBiomes.add(Biome.BASALT_DELTAS);
        DEFAULT_BIOME_GROUPS.put("Nether", netherBiomes);
        groupMaterial.put("Nether", Material.NETHERRACK);

        // End Group
        Set<Biome> endBiomes = new LinkedHashSet<>();
        endBiomes.add(Biome.THE_END);
        endBiomes.add(Biome.END_BARRENS);
        endBiomes.add(Biome.END_HIGHLANDS);
        endBiomes.add(Biome.END_MIDLANDS);
        endBiomes.add(Biome.SMALL_END_ISLANDS);
        DEFAULT_BIOME_GROUPS.put("End", endBiomes);
        groupMaterial.put("End", Material.END_STONE);

        // Cold Group
        Set<Biome> coldBiomes = new LinkedHashSet<>();
        coldBiomes.add(Biome.SNOWY_PLAINS);
        coldBiomes.add(Biome.ICE_SPIKES);
        coldBiomes.add(Biome.SNOWY_TAIGA);
        coldBiomes.add(Biome.GROVE);
        coldBiomes.add(Biome.SNOWY_BEACH);
        coldBiomes.add(Biome.FROZEN_RIVER);
        DEFAULT_BIOME_GROUPS.put("Cold", coldBiomes);
        groupMaterial.put("Cold", Material.ICE);

        //Misc
        Set<Biome> miscBiomes = new LinkedHashSet<>();
        miscBiomes.add(Biome.BEACH);
        miscBiomes.add(Biome.STONY_SHORE);
        miscBiomes.add(Biome.RIVER);
        miscBiomes.add(Biome.MUSHROOM_FIELDS);
        miscBiomes.add(Biome.THE_VOID);
        DEFAULT_BIOME_GROUPS.put("Misc", miscBiomes);
        groupMaterial.put("Misc", Material.MYCELIUM);
    }

    private Map<String, Set<Biome>> activeBiomesInGroups;
    private Map<String, Integer> selectedGroupBiome;

    public BiomeGroupManager() {
        this.activeBiomesInGroups = new LinkedHashMap<>();
        selectedGroupBiome = new LinkedHashMap<>();
        resetToDefaults();
    }

    public BiomeGroupManager(BiomeGroupManager copy) {
        this.activeBiomesInGroups = new LinkedHashMap<>();
        selectedGroupBiome = new LinkedHashMap<>();
        for (Map.Entry<String, Set<Biome>> entry : copy.activeBiomesInGroups.entrySet()) {
            this.activeBiomesInGroups.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        this.selectedGroupBiome.putAll(copy.selectedGroupBiome);
    }

    public void cycleBiomeChoice(String groupName) {
        Integer biomeIndex = selectedGroupBiome.getOrDefault(groupName, 0);
        biomeIndex++;
        if (biomeIndex >= getDefaultBiomesForGroup(groupName).size()) {
            biomeIndex = 0;
        }
        selectedGroupBiome.put(groupName, biomeIndex);
    }

    public boolean isSelectedBiome(String groupName, Biome biome) {
        Integer biomeIndex = selectedGroupBiome.getOrDefault(groupName, 0);
        Set<Biome> biomes = getDefaultBiomesForGroup(groupName);
        int at = 0;
        for (Biome b : biomes) {
            if (at == biomeIndex) {
                return b == biome;
            }
            at++;
        }
        return false;
    }

    public void toggleSelectedBiome(String groupName) {
        Integer biomeIndex = selectedGroupBiome.getOrDefault(groupName, 0);
        Set<Biome> biomes = getDefaultBiomesForGroup(groupName);
        Set<Biome> activeBiomes = getBiomesInGroup(groupName);
        int at = 0;
        for (Biome b : biomes) {
            if (at == biomeIndex) {
                if (!activeBiomes.contains(b)) {
                    activeBiomes.add(b);
                } else {
                    activeBiomes.remove(b);
                }
                break;
            }
            at++;
        }
    }

    public Set<Biome> getAllActiveBiomes() {
        Set<Biome> biomes = new LinkedHashSet<>();
        for (Set<Biome> groupBiomes : activeBiomesInGroups.values()) {
            biomes.addAll(groupBiomes);
        }
        return biomes;
    }

    public Material getGroupMaterial(String groupName) {
        return groupMaterial.getOrDefault(groupName, Material.BARRIER);
    }

    public Boolean isEnabledBiome(String groupName, Biome biome) {
        return getBiomesInGroup(groupName).contains(biome);
    }

    public Map<String, Set<Biome>> getBiomeGroups() {
        return new LinkedHashMap<>(DEFAULT_BIOME_GROUPS);
    }

    public Set<String> getGroupNames() {
        return DEFAULT_BIOME_GROUPS.keySet();
    }

    public Set<Biome> getBiomesInGroup(String groupName) {
        return activeBiomesInGroups.getOrDefault(groupName, new LinkedHashSet<>());
    }

    public void addBiomeToGroup(String groupName, Biome biome) {
        activeBiomesInGroups.computeIfAbsent(groupName, k -> new LinkedHashSet<>()).add(biome);
    }

    public void removeBiomeFromGroup(String groupName, Biome biome) {
        Set<Biome> group = activeBiomesInGroups.get(groupName);
        if (group != null) {
            group.remove(biome);
        }
    }

    public void resetGroupToDefaults(String groupName) {
        Set<Biome> defaultBiomes = DEFAULT_BIOME_GROUPS.get(groupName);
        if (defaultBiomes != null) {
            activeBiomesInGroups.put(groupName, new LinkedHashSet<>(defaultBiomes));
        }
    }

    public void resetToDefaults() {
        activeBiomesInGroups.clear();
        selectedGroupBiome.clear();
        for (Map.Entry<String, Set<Biome>> entry : DEFAULT_BIOME_GROUPS.entrySet()) {
            activeBiomesInGroups.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
    }

    public Set<Biome> getDefaultBiomesForGroup(String groupName) {
        return new LinkedHashSet<>(DEFAULT_BIOME_GROUPS.getOrDefault(groupName, new LinkedHashSet<>()));
    }

    public Set<Biome> getAllBiomesFromGroups(Set<String> groupNames) {
        Set<Biome> allBiomes = new LinkedHashSet<>();
        for (String groupName : groupNames) {
            allBiomes.addAll(getBiomesInGroup(groupName));
        }
        return allBiomes;
    }

    public Map<String, Object> toConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, Set<Biome>> entry : activeBiomesInGroups.entrySet()) {
            List<String> biomeNames = new ArrayList<>();
            for (Biome biome : entry.getValue()) {
                biomeNames.add(biome.name());
            }
            map.put(entry.getKey(), biomeNames);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public void fromConfigMap(Map<String, Object> configMap) {
        activeBiomesInGroups.clear();
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String groupName = entry.getKey();
            Set<Biome> biomes = new LinkedHashSet<>();

            if (entry.getValue() instanceof List) {
                List<String> biomeNames = (List<String>) entry.getValue();
                for (String biomeName : biomeNames) {
                    try {
                        Biome biome = Biome.valueOf(biomeName.toUpperCase());
                        biomes.add(biome);
                    } catch (IllegalArgumentException e) {
                        // Skip invalid biome names
                    }
                }
            }
            activeBiomesInGroups.put(groupName, biomes);
        }
    }
}
