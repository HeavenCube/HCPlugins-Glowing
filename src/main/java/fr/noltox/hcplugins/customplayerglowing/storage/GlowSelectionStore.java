package fr.noltox.hcplugins.customplayerglowing.storage;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores the selected cosmetic in the player's PDC, saved by Minecraft with player data.
 * Access on the server thread only; no player references, file I/O or offline-player reads.
 */
public final class GlowSelectionStore {

    private final NamespacedKey selectionKey;

    public GlowSelectionStore(Plugin plugin) {
        selectionKey = new NamespacedKey(plugin, "selected_profile");
    }

    public String selected(Player player) {
        return player.getPersistentDataContainer().get(selectionKey, PersistentDataType.STRING);
    }

    public void select(Player player, String glowingId) {
        Objects.requireNonNull(glowingId, "glowingId");
        if (glowingId.isBlank()) {
            throw new IllegalArgumentException("The selected cosmetic ID must not be blank.");
        }
        if (!glowingId.equals(selected(player))) {
            player.getPersistentDataContainer().set(selectionKey, PersistentDataType.STRING, glowingId);
        }
    }

    public void clear(Player player) {
        player.getPersistentDataContainer().remove(selectionKey);
    }

    /** Temporary reload snapshot of the supplied online players; unrelated PDC keys are untouched. */
    public Map<UUID, String> snapshot(Collection<? extends Player> players) {
        Map<UUID, String> selections = new HashMap<>();
        for (Player player : players) {
            String selected = selected(player);
            if (selected != null) {
                selections.put(player.getUniqueId(), selected);
            }
        }
        return Map.copyOf(selections);
    }

    /** Restores only this preference after a failed runtime reload, without forcing a player save. */
    public void restoreAfterFailedReload(Collection<? extends Player> players, Map<UUID, String> selections) {
        for (Player player : players) {
            String selected = selections.get(player.getUniqueId());
            if (selected == null) {
                clear(player);
            } else {
                select(player, selected);
            }
        }
    }
}
