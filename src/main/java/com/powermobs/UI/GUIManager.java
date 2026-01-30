package com.powermobs.UI;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.UI.chat.ChatInputHandler;
import com.powermobs.UI.framework.GUIPageManager;
import com.powermobs.UI.framework.PlayerSessionData;
import com.powermobs.UI.pages.*;
import com.powermobs.UI.pages.abilities.AbilityConfigGUIPage;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;


/**
 * Central manager for all GUI functionality
 */
@Getter
@Setter
public class GUIManager {

    private final PowerMobsPlugin plugin;
    private final GUIPageManager pageManager;
    private final ChatInputHandler chatInputHandler;
    private PlayerSessionData currentPlayer;


    public GUIManager(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        this.pageManager = new GUIPageManager(plugin);
        this.chatInputHandler = new ChatInputHandler(plugin);
        // Register all pages
        registerPages();
    }

    /**
     * Register all GUI pages
     */
    private void registerPages() {
        // Main pages
        pageManager.registerPage("main_menu", new MainMenuPage(pageManager, this));
        pageManager.registerPage("mob_list", new MobListPage(pageManager, this));
        pageManager.registerPage("mob_editor", new MobEditorPage(pageManager, this));
        pageManager.registerPage("create_mob", new CreateMobPage(pageManager, this));
        pageManager.registerPage("random_mob_settings", new RandomMobSettingsPage(pageManager, this));

        pageManager.registerPage("bulk_item_selection", new BulkItemSelectionPage(pageManager, this));
        pageManager.registerPage("drop_settings", new DropSettingsPage(pageManager, this));
        pageManager.registerPage("item_drop_config", new ItemDropConfigurationPage(pageManager, this));
        pageManager.registerPage("item_selection", new ItemSelectionPage(pageManager, this));
        pageManager.registerPage("mob_equipment", new MobEquipmentPage(pageManager, this));
        pageManager.registerPage("mob_equipment_item_settings", new MobEquipmentItemSettingsPage(pageManager, this));
        pageManager.registerPage("mob_spawn_conditions", new MobSpawnConditionsPage(pageManager, this));

        pageManager.registerPage("ability_config", new AbilityConfigGUIPage(pageManager, this));
    }

    /**
     * Open the main menu for a player
     *
     * @param player The player to show the GUI to
     */
    public void openMainMenu(Player player) {
        pageManager.navigateTo("main_menu", false, player);
    }

    /**
     * Handle click events from the inventory listener
     *
     * @param player The player who clicked
     * @param slot   The slot that was clicked
     * @return True if the event should be canceled
     */
    public boolean handleClick(Player player, int slot, ClickType clickType) {
        return pageManager.handleClick(player, slot, clickType);
    }

    /**
     * Gets a material that represents an entity type
     */
    public Material getMaterialForEntityType(EntityType type) {
        return switch (type) {
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case CREEPER -> Material.CREEPER_HEAD;
            case SPIDER -> Material.SPIDER_EYE;
            case ENDERMAN -> Material.ENDER_PEARL;
            case SLIME -> Material.SLIME_BALL;
            case WITCH -> Material.BREWING_STAND;
            case HUSK -> Material.SAND;
            case DROWNED -> Material.TRIDENT;
            case STRAY -> Material.BLUE_ICE;
            case PILLAGER -> Material.CROSSBOW;
            case EVOKER -> Material.TOTEM_OF_UNDYING;
            case VINDICATOR -> Material.IRON_AXE;
            case ILLUSIONER -> Material.GLASS;
            case GUARDIAN -> Material.PRISMARINE_SHARD;
            case ELDER_GUARDIAN -> Material.SPONGE;
            case SHULKER -> Material.SHULKER_BOX;
            case MAGMA_CUBE -> Material.MAGMA_CREAM;
            case BLAZE -> Material.BLAZE_ROD;
            case GHAST -> Material.GHAST_TEAR;
            case PHANTOM -> Material.PHANTOM_MEMBRANE;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            case WITHER -> Material.NETHER_STAR;
            case ZOGLIN -> Material.ROTTEN_FLESH;
            case ZOMBIFIED_PIGLIN -> Material.GOLDEN_SWORD;
            case HOGLIN -> Material.COOKED_PORKCHOP;
            case PIGLIN -> Material.GOLD_INGOT;
            case PIGLIN_BRUTE -> Material.GOLD_BLOCK;
            case RAVAGER -> Material.SADDLE;
            case WARDEN -> Material.ECHO_SHARD;
            default -> Material.SPAWNER;
        };
    }

}

