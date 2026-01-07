package com.powermobs.UI.framework;

import com.powermobs.UI.GUIManager;
import com.powermobs.UI.GUIPage;
import com.powermobs.UI.chat.ChatInputType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract implementation of GUIPage with common functionality
 */
@Getter
public abstract class AbstractGUIPage implements GUIPage {

    protected final GUIPageManager pageManager;
    protected final GUIManager guiManager;
    protected final int size;
    protected final String unsavedWarning = ChatColor.RED + "UNSAVED CHANGES WILL BE LOST";
    protected Inventory inventory;
    protected String title;

    /**
     * Create a new GUI page
     *
     * @param pageManager The page manager
     * @param size        The inventory size (must be a multiple of 9)
     * @param title       The inventory title
     */
    public AbstractGUIPage(GUIPageManager pageManager, GUIManager guiManager, int size, String title) {
        this.pageManager = pageManager;
        this.guiManager = guiManager;
        this.size = size;
        this.title = title;
        this.inventory = Bukkit.createInventory(pageManager, size, title);
    }

    public static String formatDecimal(double value) {
        BigDecimal bd = new BigDecimal(Double.toString(value)).stripTrailingZeros();
        String s = bd.toPlainString();
        if (s.startsWith(".")) s = "0" + s;
        if (s.equals("-0")) s = "0";
        return s;
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
     * Creates a GUI item with a custom name and lore
     */
    protected ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a standard back button
     *
     * @param slot The slot to place the button
     * @param text Custom text (or null for default)
     */
    protected void addBackButton(int slot, String text) {
        ItemStack backButton = createGuiItem(Material.BARRIER,
                text != null ? text : ChatColor.RED + "Back");
        inventory.setItem(slot, backButton);
    }

    /**
     * Create a standard back button
     *
     * @param slot    The slot to place the button
     * @param text    Custom text (or null for default)
     * @param warning Display warning of losing unsaved changes
     */
    protected void addBackButton(int slot, String text, boolean warning) {
        String warningText = "";
        if (warning) {
            warningText = unsavedWarning;
        }
        ItemStack backButton = createGuiItem(Material.BARRIER,
                text != null ? text : ChatColor.RED + "Back",
                warningText);
        inventory.setItem(slot, backButton);
    }

    /**
     * Create a standard navigation button for next/previous pages
     *
     * @param slot The slot to place the button
     * @param next True for next, false for previous
     */
    protected void addNavigationButton(int slot, boolean next) {
        ItemStack navButton = createGuiItem(Material.ARROW,
                ChatColor.YELLOW + (next ? "Next Page" : "Previous Page"));
        inventory.setItem(slot, navButton);
    }

    protected void startChatInput(Player player, ChatInputType inputType, com.powermobs.UI.chat.ChatInputCallback callback) {
        player.closeInventory();
        pageManager.getPlugin().getGuiManager().getChatInputHandler().startInputSession(player, inputType, callback);
    }

    /**
     * Updates the inventory with a new title
     */
    protected void updateInventoryTitle(String newTitle) {
        if (!this.title.equals(newTitle)) {
            ItemStack[] contents = inventory.getContents();
            this.title = newTitle;
            this.inventory = Bukkit.createInventory(pageManager, size, newTitle);
            this.inventory.setContents(contents);
        }
    }

    /**
     * Formats material names for better display
     */
    protected String formatMaterialName(String name) {
        String[] words = name.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(word.charAt(0))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }
}

