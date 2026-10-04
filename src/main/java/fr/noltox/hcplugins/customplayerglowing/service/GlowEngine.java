package fr.noltox.hcplugins.customplayerglowing.service;

import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration;
import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration.GlowPattern;
import fr.noltox.hcplugins.customplayerglowing.storage.GlowSelectionStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (active && plugin.isEnabled() && player.isOnline()) {
                synchronize(player, true);
            }
        });
    }

    public void synchronize(Player player) {
        synchronize(player, true);
    }

    public SelectionResult select(Player player, String glowingId) {
        GlowPattern pattern = configuration.glowing(glowingId);
        if (pattern == null || !player.hasPermission(pattern.permission())) {
            return SelectionResult.NOT_ALLOWED;
        }
        if (!selectionStore.select(player.getUniqueId(), glowingId)) {
            return SelectionResult.SAVE_FAILED;
        }
        activate(player, pattern);
        return SelectionResult.APPLIED;
    }

    public boolean disable(Player player) {
        if (!selectionStore.clear(player.getUniqueId())) {
            return false;
        }
        deactivate(player);
        return true;
    }

    public void shutdown() {
        active = false;
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
    }

    /** Returns the legacy carrier code consumed as the final color in TAB's tagprefix. */
    public String tabColorCode(UUID playerId) {
        return tabColorCodes.getOrDefault(playerId, "");
    }

    public Map<UUID, Boolean> previousGlowingStates() {
        return ownership.previousGlowingStates();
    }

    private void synchronize(Player player, boolean notifyInvalidation) {
        UUID playerId = player.getUniqueId();
        String selectedId = selectionStore.selected(playerId);
        if (selectedId == null) {
            deactivate(player);
            return;
        }
        GlowPattern pattern = configuration.glowing(selectedId);
        if (pattern == null || !player.hasPermission(pattern.permission())) {
            deactivate(player);
            selectionStore.invalidate(playerId);
            if (notifyInvalidation) {
                player.sendMessage(configuration.messages().selectionInvalidated());
            }
            return;
        }
        activate(player, pattern);
    }

    private void activate(Player player, GlowPattern pattern) {
        UUID playerId = player.getUniqueId();
        ownership.activate(playerId, player.isGlowing(), pattern.id());
        if (!player.isGlowing()) {
            player.setGlowing(true);
        }
        tabColorCodes.put(playerId, "&" + pattern.profile().carrier().legacyCode());
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
        for (UUID playerId : ownership.playerIds()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                synchronize(player, true);
            }
        }
    }

    public enum SelectionResult {
        APPLIED,
        NOT_ALLOWED,
        SAVE_FAILED
    }

    /** Pure ownership state kept separate so restoration semantics are directly testable. */
    static final class GlowOwnership {

        private final Map<UUID, OwnedGlow> active = new HashMap<>();

        void activate(UUID playerId, boolean currentGlowing, String profileId) {
            active.compute(playerId, (ignored, previous) -> new OwnedGlow(
                    previous == null ? currentGlowing : previous.previousGlowing(),
                    profileId
            ));
        }

        Boolean deactivate(UUID playerId) {
            OwnedGlow removed = active.remove(playerId);
            return removed == null ? null : removed.previousGlowing();
        }

        Set<UUID> playerIds() {
            return Set.copyOf(active.keySet());
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
