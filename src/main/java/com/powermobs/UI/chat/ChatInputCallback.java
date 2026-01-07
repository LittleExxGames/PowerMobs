package com.powermobs.UI.chat;

import org.bukkit.entity.Player;

/**
 * Callback interface for chat input results
 */
@FunctionalInterface
public interface ChatInputCallback {
    /**
     * Called when valid input is received
     *
     * @param value  The validated input value
     * @param player The player who provided the input
     */
    void onInputReceived(Object value, Player player);
}