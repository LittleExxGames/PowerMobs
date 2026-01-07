package com.powermobs.UI.framework;

import com.powermobs.PowerMobsPlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class PlayerSessionData {
    private UUID playerUUID;
    private PowerMobType type;
    private String selectedMobId;
    private EditingType itemEditType = EditingType.NONE;
    private boolean isEditing;
    private boolean cancelAction;
    private ItemStack selectedItem; // used for item displaying to show selections
    private String selectedItemType; // "vanilla", "custom", and "ability"
    private String selectedItemId; // Stores the currently selected item Id for moving between pages
    private Set<String> activeItems = new LinkedHashSet<>();

    public PlayerSessionData(UUID uuid) {
        playerUUID = uuid;
    }

    public Player getPlayer() {
        return PowerMobsPlugin.getInstance().getServer().getPlayer(playerUUID);
    }

    public void resetDropEditing() {
        this.itemEditType = EditingType.NONE;
        this.isEditing = false;
        this.selectedItem = new ItemStack(org.bukkit.Material.AIR);
        this.selectedItemId = null;
    }

    /**
     * Sets the selected mob ID and defines the mob type to be custom.
     *
     * @param id
     */
    public void setSelectedMobId(String id) {
        this.selectedMobId = id;
        this.type = PowerMobType.CUSTOM;
    }

    /**
     * Sets the type of mob currently being edited while affecting related values.
     *
     * @param type
     */
    public void setType(PowerMobType type) {
        this.type = type;
        if (PowerMobType.RANDOM.equals(type)) {
            this.selectedMobId = null;
        }
    }

    /**
     * Clears the selected id and item object.
     */
    public void clearItemSelection() {
        this.selectedItemId = null;
        this.selectedItem = null;
    }

}
