package com.custommobs.UI.framework;

import com.custommobs.UI.GUIPage;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Abstract implementation of GUIPage with common functionality
 */
@Getter
public abstract class AbstractGUIPage implements GUIPage {

    protected final GUIPageManager pageManager;
    protected final Inventory inventory;
    protected final int size;
    protected final String title;

    /**
     * Create a new GUI page
     * @param pageManager The page manager
     * @param size The inventory size (must be a multiple of 9)
     * @param title The inventory title
     */
    public AbstractGUIPage(GUIPageManager pageManager, int size, String title) {
        this.pageManager = pageManager;
        this.size = size;
        this.title = title;
        this.inventory = Bukkit.createInventory(pageManager, size, title);
    }


    /**
     * Creates a GUI item with a custom name and lore
     */
    protected ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);

            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a standard back button
     * @param slot The slot to place the button
     * @param text Custom text (or null for default)
     */
    protected void addBackButton(int slot, String text) {
        ItemStack backButton = createGuiItem(Material.BARRIER,
                text != null ? text : ChatColor.RED + "Back");
        inventory.setItem(slot, backButton);
    }

    /**
     * Create a standard navigation button for next/previous pages
     * @param slot The slot to place the button
     * @param next True for next, false for previous
     */
    protected void addNavigationButton(int slot, boolean next) {
        ItemStack navButton = createGuiItem(Material.ARROW,
                ChatColor.YELLOW + (next ? "Next Page" : "Previous Page"));
        inventory.setItem(slot, navButton);
    }
}

