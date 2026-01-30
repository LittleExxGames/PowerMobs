package com.powermobs.UI.pages.abilities;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.GUIManager;
import com.powermobs.UI.chat.ChatInputType;
import com.powermobs.UI.framework.AbstractGUIPage;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.UI.framework.PowerMobType;
import com.powermobs.utils.HostileEntityTypes;
import com.powermobs.config.IPowerMobConfig;
import com.powermobs.mobs.abilities.Ability;
import com.powermobs.mobs.abilities.AbilityConfigField;
import com.powermobs.mobs.abilities.AbilityConfigValueType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AbilityConfigGUIPage extends AbstractGUIPage {

    private static final int INVENTORY_SIZE = 54; // 6 rows of inventory
    private String selectedMobId;
    private String abilityId;
    private Map<String, Object> originConfig;
    private Map<String, Object> currentConfig;

    private Map<String, Object> defaultConfig;
    private Map<String, AbilityConfigField> schema;

    private final Map<Integer, String> slotToKey = new LinkedHashMap<>();
    private final Map<String, Integer> highlightedEntityByKey = new HashMap<>();

    public AbilityConfigGUIPage(GUIPageManager pageManager, GUIManager guiManager) {
        super(pageManager, guiManager, INVENTORY_SIZE, ChatColor.BLUE + "Ability Config");
    }
    @Override
    public void build() {
        inventory.clear();
        slotToKey.clear();

        PowerMobsPlugin plugin = pageManager.getPlugin();
        plugin.debug("Building GUI for Ability Configuration.", "ui");

        PlayerSessionData playerSession = plugin.getGuiManager().getCurrentPlayer();
        if (playerSession.isCancelAction()) {
            playerSession.setCancelAction(false);
            pageManager.navigateBack(playerSession.getPlayer());
            return;
        }

        abilityId = playerSession.getSelectedItemId();
        selectedMobId = playerSession.getSelectedMobId();

        IPowerMobConfig mobConfig = (playerSession.getType() == PowerMobType.RANDOM)
                ? plugin.getConfigManager().getRandomMobConfig()
                : plugin.getConfigManager().getPowerMob(selectedMobId);


        if (mobConfig == null) {
            plugin.getLogger().severe("Invalid mob config while building ability configuration page.");
            pageManager.navigateBack(playerSession.getPlayer());
            return;
        }

        Ability ability = plugin.getAbilityManager().getAbility(abilityId);
        if (ability == null) {
            plugin.getLogger().severe("Invalid ability while building ability configuration page: " + abilityId);
            pageManager.navigateBack(playerSession.getPlayer());
            return;
        }

        // Only (re)load when first opened (editing=false) or ability changed
        if (!playerSession.isEditing()) {

            schema = ability.getConfigSchema() != null ? new LinkedHashMap<>(ability.getConfigSchema()) : new LinkedHashMap<>();
            defaultConfig = buildDefaults(plugin, abilityId, schema); // per-field defaults

            Map<String, Map<String, Object>> possible = mobConfig.getPossibleAbilities();
            Map<String, Object> overrides = (possible != null) ? possible.get(abilityId) : null;
            if (overrides == null) {
                overrides = Collections.emptyMap(); // no overrides yet => use defaults
            }

            originConfig = new LinkedHashMap<>();
            for (String key : defaultConfig.keySet()) {
                Object v = overrides.containsKey(key) ? overrides.get(key) : defaultConfig.get(key);
                originConfig.put(key, v);
            }

            currentConfig = new LinkedHashMap<>(originConfig);

            playerSession.setEditing(true);
        }

        ItemStack abilityItem =  createGuiItem(ability.getMaterial(), ChatColor.WHITE + ability.getTitle(),ChatColor.LIGHT_PURPLE + ability.getDescription());
        inventory.setItem(13, abilityItem);

        List<String> keys = new ArrayList<>(defaultConfig.keySet());
        keys.sort(String.CASE_INSENSITIVE_ORDER);

        int configCount = 1;
        for (String key : keys) {
            AbilityConfigField field = schema.get(key);
            AbilityConfigValueType type = field.type();
            Object currentVal = currentConfig.get(key);

            ItemStack configItem;
            if (type == AbilityConfigValueType.ENTITY_TYPE || type == AbilityConfigValueType.ENTITY_TYPE_LIST) {
                configItem = buildEntitySelectorItem(key, type);
            } else {
                List<String> lore = new ArrayList<>();
                if (field != null && field.description() != null && !field.description().isBlank()) {
                    lore.add(ChatColor.GRAY + "~" + ChatColor.WHITE + field.description() + ChatColor.GRAY + "~");
                }
                lore.add(ChatColor.GREEN + "Current: " + ChatColor.WHITE + Objects.toString(currentVal));
                lore.add(ChatColor.YELLOW + "Left-click: edit");
                lore.add(ChatColor.YELLOW + "Right-click: reset to default");

                configItem = createGuiItem(
                        Material.PAPER,
                        ChatColor.YELLOW + key,
                        lore);
            }

            int slot = configPosition(configCount, 31, keys.size());
            inventory.setItem(slot, configItem);
            slotToKey.put(slot, key);
            configCount++;
        }

        ItemStack saveButton = createGuiItem(Material.EMERALD,
                ChatColor.GREEN + "Save Changes",
                ChatColor.GRAY + "Save the current configuration");
        inventory.setItem(45, saveButton);

        addBackButton(53, ChatColor.RED + "Back", !originConfig.equals(currentConfig));
    }

    @Override
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        if (slot == 45) {
            return save(player);
        }
        if (slot == 53) {
            guiManager.getCurrentPlayer().setEditing(false);
            pageManager.navigateBack(player);
            return true;
        }

        String key = slotToKey.get(slot);
        if (key == null) {
            return false;
        }

        AbilityConfigField field = schema.get(key);
        AbilityConfigValueType type = field.type();

        if (type == AbilityConfigValueType.ENTITY_TYPE || type == AbilityConfigValueType.ENTITY_TYPE_LIST) {
            if (type == AbilityConfigValueType.ENTITY_TYPE) {
                if (clickType == ClickType.RIGHT) {
                    int cursor = highlightedEntityByKey.getOrDefault(key, 0);
                    cursor = (cursor + 1) % HostileEntityTypes.values().length;
                    highlightedEntityByKey.put(key, cursor);
                    build();
                    player.updateInventory();
                    return true;
                }
                if (clickType == ClickType.LEFT) {
                    int cursor = highlightedEntityByKey.getOrDefault(key, 0);
                    HostileEntityTypes picked = HostileEntityTypes.values()[cursor];
                    currentConfig.put(key, picked.name());
                    build();
                    player.updateInventory();
                    return true;
                }
                return true;
            }
            if (clickType == ClickType.LEFT) {
                int cursor = highlightedEntityByKey.getOrDefault(key, 0);
                cursor = (cursor + 1) % HostileEntityTypes.values().length;
                highlightedEntityByKey.put(key, cursor);
                build();
                player.updateInventory();
                return true;
            }

            if (clickType == ClickType.RIGHT) {
                int cursor = highlightedEntityByKey.getOrDefault(key, 0);
                HostileEntityTypes picked = HostileEntityTypes.values()[cursor];

                List<String> list = new ArrayList<>();
                Object v = currentConfig.get(key);
                if (v instanceof List<?> existing) {
                    for (Object o : existing) {
                        if (o != null) list.add(o.toString().trim().toUpperCase(Locale.ROOT));
                    }
                }

                String name = picked.name();
                if (list.contains(name)) list.remove(name);
                else list.add(name);

                currentConfig.put(key, list);
                build();
                player.updateInventory();
                return true;
            }

            return true;
        }

        if (clickType.isRightClick()) {
            currentConfig.put(key, defaultConfig.get(key));
            build();
            player.updateInventory();
            return true;
        }
        if (!clickType.isLeftClick()) return true;

        switch (type) {
            case STRING -> startChatInput(player, ChatInputType.ABILITY_STRING, (value, p) -> {
                currentConfig.put(key, value);
                pageManager.navigateTo("ability_config", false, p);
            });
            case BOOLEAN -> startChatInput(player, ChatInputType.ABILITY_BOOLEAN, (value, p) -> {
                currentConfig.put(key, value);
                pageManager.navigateTo("ability_config", false, p);
            });
            case INT -> startChatInput(player, ChatInputType.ABILITY_INT, (value, p) -> {
                currentConfig.put(key, value);
                pageManager.navigateTo("ability_config", false, p);
            });
            case DOUBLE -> startChatInput(player, ChatInputType.ABILITY_DOUBLE, (value, p) -> {
                currentConfig.put(key, value);
                pageManager.navigateTo("ability_config", false, p);
            });
            case CHANCE -> startChatInput(player, ChatInputType.CHANCE, (value, p) -> {
                currentConfig.put(key, value);
                pageManager.navigateTo("ability_config", false, p);
            });
            default -> {
                // no-op
            }
        }

        return false;
    }

    private boolean save(Player player) {
        PowerMobsPlugin plugin = pageManager.getPlugin();
        PlayerSessionData session = plugin.getGuiManager().getCurrentPlayer();

        IPowerMobConfig mobConfig = (session.getType() == PowerMobType.RANDOM)
                ? plugin.getConfigManager().getRandomMobConfig()
                : plugin.getConfigManager().getPowerMob(selectedMobId);

        if (mobConfig == null) {
            plugin.getLogger().severe("Invalid mob config while saving ability configuration page.");
            return true;
        }

        Map<String, Map<String, Object>> possible = mobConfig.getPossibleAbilities();
        if (possible == null) {
            plugin.getLogger().severe("possibleAbilities was null while saving ability configuration page.");
            return true;
        }

        // If everything equals defaults, save ability only (empty map => use defaults).
        boolean allDefaults = true;
        for (String key : defaultConfig.keySet()) {
            if (!Objects.equals(currentConfig.get(key), defaultConfig.get(key))) {
                allDefaults = false;
                break;
            }
        }

        if (allDefaults) {
            possible.put(abilityId, Collections.emptyMap());
        } else {
            // Populate full config (defaults + edited values), so partial becomes complete.
            Map<String, Object> full = new LinkedHashMap<>();
            for (String key : defaultConfig.keySet()) {
                full.put(key, currentConfig.getOrDefault(key, defaultConfig.get(key)));
            }
            possible.put(abilityId, full);
        }

        if (session.getType() == PowerMobType.RANDOM) {
            plugin.getConfigManager().saveRandomMob(mobConfig.toConfigMap());
        } else {
            plugin.getConfigManager().savePowerMob(selectedMobId, mobConfig.toConfigMap());
        }

        session.setEditing(false);
        pageManager.navigateBack(player);
        return true;
    }

    private ItemStack buildEntitySelectorItem(String key, AbilityConfigValueType type) {
        int cursor = highlightedEntityByKey.getOrDefault(key, 0);
        if (cursor < 0 || cursor >= HostileEntityTypes.values().length) cursor = 0;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + type.name());

        if (type == AbilityConfigValueType.ENTITY_TYPE) {
            lore.add(ChatColor.GRAY + "Right click to scroll");
            lore.add(ChatColor.GRAY + "Left click to select");
        } else {
            lore.add(ChatColor.GRAY + "Left click to scroll");
            lore.add(ChatColor.GRAY + "Right click to enable/disable");
        }

        // current selection(s)
        String selectedSingle = Objects.toString(currentConfig.get(key), "").trim().toUpperCase(Locale.ROOT);
        Set<String> selectedList = new HashSet<>();
        if (type == AbilityConfigValueType.ENTITY_TYPE_LIST) {
            Object v = currentConfig.get(key);
            if (v instanceof List<?> list) {
                for (Object o : list) {
                    if (o != null) selectedList.add(o.toString().trim().toUpperCase(Locale.ROOT));
                }
            }
        }

        for (int i = -3; i <= 3; i++) {
            int idx = (cursor + i + HostileEntityTypes.values().length) % HostileEntityTypes.values().length;
            HostileEntityTypes entity = HostileEntityTypes.values()[idx];

            boolean active = (type == AbilityConfigValueType.ENTITY_TYPE)
                    ? entity.name().equals(selectedSingle)
                    : selectedList.contains(entity.name());

            String mark = active ? (ChatColor.DARK_GREEN + "✓ ") : (ChatColor.RED + "✗ ");
            if (i == 0) {
                lore.add(mark + ChatColor.YELLOW + "► " + ChatColor.GOLD + entity.name() + ChatColor.YELLOW + " ◄");
            } else {
                lore.add(mark + ChatColor.BLUE + entity.name());
            }
        }

        Material icon = (type == AbilityConfigValueType.ENTITY_TYPE)
                ? Material.ZOMBIE_SPAWN_EGG
                : Material.SKELETON_SPAWN_EGG;

        return createGuiItem(icon, ChatColor.YELLOW + key, lore);
    }

    private Map<String, Object> buildDefaults(PowerMobsPlugin plugin, String abilityId, Map<String, AbilityConfigField> schema) {
        Map<String, Object> out = new LinkedHashMap<>();
        ConfigurationSection defaultsSection = plugin.getConfigManager()
                .getAbilitiesConfigManager()
                .getConfig()
                .getConfigurationSection("abilities." + abilityId);

        if (defaultsSection != null) {
            out.putAll(defaultsSection.getValues(false));
        }

        // Fill any missing keys from schema defaults (fallback only)
        if (schema != null && !schema.isEmpty()) {
            for (AbilityConfigField f : schema.values()) {
                out.putIfAbsent(f.key(), f.defaultValue());
            }
        }

        return out;
    }

    private String nextEntityTypeName(String currentName) {
        EntityType current = null;
        if (currentName != null && !currentName.isBlank()) {
            try {
                current = EntityType.valueOf(currentName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) { }
        }

        List<EntityType> options = Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .filter(EntityType::isSpawnable)
                .toList();

        if (options.isEmpty()) {
            return EntityType.ZOMBIE.name();
        }

        int idx = (current == null) ? -1 : options.indexOf(current);
        int nextIdx = (idx + 1) % options.size();
        return options.get(nextIdx).name();
    }

    private int configPosition(int count, int startPos, int totalItems) {
        final int perRow = 5;

        int index = count - 1;
        int row = index / perRow;
        int idxInRow = index % perRow;

        int remaining = totalItems - (row * perRow);
        int rowSize = Math.min(perRow, Math.max(remaining, 0));

        int[] offsets = switch (rowSize) {
            case 1 -> new int[]{0};                  // ooooxoooo
            case 2 -> new int[]{-1, 1};              // oooxoxooo
            case 3 -> new int[]{-2, 0, 2};           // ooxoxoxoo
            case 4 -> new int[]{-3, -1, 1, 3};       // oxoxoxoxo
            case 5 -> new int[]{-2, -1, 0, 1, 2};    // ooxxxxxoo
            default -> new int[]{0};
        };

        return startPos + (9 * row) + offsets[idxInRow];
    }

}
