package fr.noltox.hcplugins.customplayerglowing.placeholder;

import fr.noltox.hcplugins.placeholdersextra.api.PlaceholderProvider;
import org.bukkit.OfflinePlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Contributes the active legacy color code used by TAB's scoreboard team.
 */
public final class PlayerGlowPlaceholderProvider implements PlaceholderProvider {

    private final Function<UUID, String> colorProvider;

    public PlayerGlowPlaceholderProvider(Function<UUID, String> colorProvider) {
        this.colorProvider = Objects.requireNonNull(colorProvider, "colorProvider");
    }

    @Override
    public String resolve(OfflinePlayer player, String placeholder) {
        if (!"color".equalsIgnoreCase(placeholder)) {
            return null;
        }
        if (player == null) {
            return "";
        }
        return colorProvider.apply(player.getUniqueId());
    }
}
