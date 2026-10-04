package fr.noltox.hcplugins.customplayerglowing.service;

import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration;
import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration.GlowPattern;
import fr.noltox.hcplugins.customplayerglowing.storage.GlowSelectionStore;
import fr.noltox.hcplugins.core.api.task.DeferredUpdates;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns vanilla glowing state while TAB transports the selected carrier as the player's team color.
 * Visual animation is entirely evaluated by HCPack-CustomAssets on the GPU.
 */
public final class GlowEngine {

    private static final long PERMISSION_AUDIT_PERIOD_TICKS = 20L;

    private final Plugin plugin;
    private final GlowConfiguration configuration;
    private final GlowSelectionStore selectionStore;
    private final GlowOwnership ownership = new GlowOwnership();
    private final Map<UUID, String> tabColorCodes = new ConcurrentHashMap<>();
    private final DeferredUpdates<UUID> resynchronizations;
    private final List<UUID> auditPlayers = new ArrayList<>();
    private BukkitTask permissionAudit;
    private boolean active;

    public GlowEngine(Plugin plugin, GlowConfiguration configuration, GlowSelectionStore selectionStore) {
        this(plugin, configuration, selectionStore, Map.of());
    }

    public GlowEngine(
            Plugin plugin,
            GlowConfiguration configuration,
            GlowSelectionStore selectionStore,
            Map<UUID, Boolean> previousGlowingStates
    ) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.selectionStore = selectionStore;
        resynchronizations = new DeferredUpdates<>(plugin, playerId -> {
            Player player = Bukkit.getPlayer(playerId);
            if (active && plugin.isEnabled() && player != null && player.isOnline()) {
                synchronize(player, true);
            }
        });
        ownership.restore(previousGlowingStates);
    }

    public void start() {
        if (active) {
            return;
        }
        active = true;
        Bukkit.getOnlinePlayers().forEach(player -> synchronize(player, false));
        permissionAudit = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::auditPermissions,
                PERMISSION_AUDIT_PERIOD_TICKS,
                PERMISSION_AUDIT_PERIOD_TICKS
        );
    }

    public void playerJoined(Player player) {
        synchronize(player, true);
    }

    public void playerQuit(Player player) {
        deactivate(player);
    }

    public void queueResynchronization(Player player) {
        if (!active) {
            return;
        }
        resynchronizations.request(player.getUniqueId());
    }

    public void synchronize(Player player) {
        synchronize(player, true);
    }

    public SelectionResult select(Player player, String glowingId) {
        GlowPattern pattern = configuration.glowing(glowingId);
        if (pattern == null || !player.hasPermission(pattern.permission())) {
            return SelectionResult.NOT_ALLOWED;
        }
        selectionStore.select(player, glowingId);
        activate(player, pattern);
        return SelectionResult.APPLIED;
    }

    public void disable(Player player) {
        selectionStore.clear(player);
        deactivate(player);
    }

    public void shutdown() {
        active = false;
        resynchronizations.close();
        if (permissionAudit != null) {
            permissionAudit.cancel();
            permissionAudit = null;
        }
        for (UUID playerId : ownership.playerIds()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                restore(player);
            }
        }
        ownership.clear();
        tabColorCodes.clear();
        auditPlayers.clear();
    }

    /** Returns the legacy carrier code consumed as the final color in TAB's tagprefix. */
    public String tabColorCode(UUID playerId) {
        return tabColorCodes.getOrDefault(playerId, "");
    }

    public Map<UUID, Boolean> previousGlowingStates() {
        return ownership.previousGlowingStates();
    }

    private void synchronize(Player player, boolean notifyInvalidation) {
        String selectedId = selectionStore.selected(player);
        if (selectedId == null) {
            deactivate(player);
            return;
        }
        GlowPattern pattern = configuration.glowing(selectedId);
        if (pattern == null || !player.hasPermission(pattern.permission())) {
            deactivate(player);
            selectionStore.clear(player);
            if (notifyInvalidation) {
                player.sendMessage(configuration.messages().selectionInvalidated());
            }
            return;
        }
        activate(player, pattern);
    }

    private void activate(Player player, GlowPattern pattern) {
        UUID playerId = player.getUniqueId();
        boolean glowing = player.isGlowing();
        ownership.activate(playerId, glowing, pattern.id());
        if (!glowing) {
            player.setGlowing(true);
        }
        char carrier = pattern.profile().carrier().legacyCode();
        String current = tabColorCodes.get(playerId);
        if (current == null || current.charAt(1) != carrier) {
            tabColorCodes.put(playerId, "&" + carrier);
        }
    }

    private void deactivate(Player player) {
        restore(player);
        tabColorCodes.remove(player.getUniqueId());
    }

    private void restore(Player player) {
        Boolean previous = ownership.deactivate(player.getUniqueId());
        if (previous != null) {
            player.setGlowing(previous);
        }
    }

    private void auditPermissions() {
        // Arbitrary contextual permissions/Bukkit attachments have no guaranteed change event.
        // A reusable snapshot allows synchronize() to remove ownership while iterating.
        ownership.copyPlayerIdsTo(auditPlayers);
        try {
            for (UUID playerId : auditPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    synchronize(player, true);
                }
            }
        } finally {
            auditPlayers.clear();
        }
    }

    public enum SelectionResult {
        APPLIED,
        NOT_ALLOWED
    }

    /** Pure ownership state kept separate so restoration semantics are directly testable. */
    static final class GlowOwnership {

        private final Map<UUID, OwnedGlow> active = new HashMap<>();

        void activate(UUID playerId, boolean currentGlowing, String profileId) {
            OwnedGlow previous = active.get(playerId);
            if (previous == null) {
                active.put(playerId, new OwnedGlow(currentGlowing, profileId));
            } else if (!previous.profileId().equals(profileId)) {
                active.put(playerId, new OwnedGlow(previous.previousGlowing(), profileId));
            }
        }

        Boolean deactivate(UUID playerId) {
            OwnedGlow removed = active.remove(playerId);
            return removed == null ? null : removed.previousGlowing();
        }

        Set<UUID> playerIds() {
            return Set.copyOf(active.keySet());
        }

        void copyPlayerIdsTo(List<UUID> target) {
            target.clear();
            for (UUID playerId : active.keySet()) {
                target.add(playerId);
            }
        }

        Map<UUID, Boolean> previousGlowingStates() {
            Map<UUID, Boolean> result = new HashMap<>();
            active.forEach((playerId, state) -> result.put(playerId, state.previousGlowing()));
            return Map.copyOf(result);
        }

        void restore(Map<UUID, Boolean> previousGlowingStates) {
            previousGlowingStates.forEach((playerId, previous) ->
                    active.put(playerId, new OwnedGlow(previous, "reload")));
        }

        void clear() {
            active.clear();
        }

        record OwnedGlow(boolean previousGlowing, String profileId) {
        }
    }
}
