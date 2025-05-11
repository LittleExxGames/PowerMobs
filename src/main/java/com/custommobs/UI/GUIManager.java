package com.custommobs.UI;

import com.custommobs.CustomMobsPlugin;
import com.custommobs.UI.framework.GUIPageManager;
import com.custommobs.UI.pages.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

/**
 * Central manager for all GUI functionality
 */
@Getter
@Setter
public class GUIManager {

    private final CustomMobsPlugin plugin;
    private final GUIPageManager pageManager;
    private String selectedMobId;
    private Object editingObject;

    public GUIManager(CustomMobsPlugin plugin) {
        this.plugin = plugin;
        this.pageManager = new GUIPageManager(plugin);

        // Register all pages
        registerPages();
    }

    /**
     * Register all GUI pages
     */
    private void registerPages() {
        // Main pages
        pageManager.registerPage("main_menu", new MainMenuPage(pageManager));
        pageManager.registerPage("mob_list", new MobListPage(pageManager));
        pageManager.registerPage("mob_editor", new MobEditorPage(pageManager));
        pageManager.registerPage("create_mob", new CreateMobPage(pageManager));
        pageManager.registerPage("random_mob_settings", new RandomMobSettingsPage(pageManager));

        // Subpages for mob editor
        // These would be implemented similarly to the pages above
        //pageManager.registerPage("mob_abilities", new MobAbilitiesPage(pageManager));
        //pageManager.registerPage("mob_equipment", new MobEquipmentPage(pageManager));
        //pageManager.registerPage("mob_drops", new MobDropsPage(pageManager));
        //pageManager.registerPage("mob_spawn_conditions", new MobSpawnConditionsPage(pageManager));

        // Sub-pages for ability, equipment, and drop editors
        // These would be implemented as needed
    }

    /**
     * Open the main menu for a player
     * @param player The player to show the GUI to
     */
    public void openMainMenu(Player player) {
        pageManager.navigateTo("main_menu", false, player);
        //player.openInventory(pageManager.getInventory());
    }

    /**
     * Handle click events from the inventory listener
     * @param player The player who clicked
     * @param slot The slot that was clicked
     * @return True if the event should be canceled
     */
    public boolean handleClick(Player player, int slot) {
        return pageManager.handleClick(player, slot);
    }


}

