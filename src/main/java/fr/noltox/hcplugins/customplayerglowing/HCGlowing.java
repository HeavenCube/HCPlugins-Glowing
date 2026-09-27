package fr.noltox.hcplugins.customplayerglowing;

import fr.noltox.hcplugins.core.api.HCPluginsCore;
import fr.noltox.hcplugins.core.api.command.CoreCommandRegistration;
import fr.noltox.hcplugins.customplayerglowing.command.PlayerGlowCommand;
import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration;
import fr.noltox.hcplugins.customplayerglowing.dialog.GlowSelectionDialog;
import fr.noltox.hcplugins.customplayerglowing.listener.PlayerGlowListener;
import fr.noltox.hcplugins.customplayerglowing.placeholder.PlayerGlowPlaceholderProvider;
import fr.noltox.hcplugins.customplayerglowing.service.GlowEngine;
import fr.noltox.hcplugins.customplayerglowing.storage.GlowSelectionStore;
import fr.noltox.hcplugins.core.api.permission.DynamicPermissionRegistry;
import fr.noltox.hcplugins.placeholdersextra.api.HCPlaceholders;
import fr.noltox.hcplugins.placeholdersextra.api.PlaceholderProviderRegistration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public final class HCGlowing extends JavaPlugin {

    private File configurationFile;
    private GlowSelectionStore selectionStore;
    private PlaceholderProviderRegistration placeholderProvider;
    private CoreCommandRegistration commandRegistration;
    private DynamicPermissionRegistry cosmeticPermissions;
    private volatile RuntimeComponents runtime;

    @Override
    public void onEnable() {
        configurationFile = new File(getDataFolder(), "config.yml");
        try {
            ensureConfigurationFile();
            GlowConfiguration configuration = GlowConfiguration.load(configurationFile);
            cosmeticPermissions = new DynamicPermissionRegistry(
                    getServer().getPluginManager(),
                    getLogger(),
                    PermissionDefault.FALSE
            );
            synchronizeCosmeticPermissions(configuration);
            selectionStore = new GlowSelectionStore(this);
            selectionStore.load();

            SanitizedSelections sanitized = sanitizeSelections(configuration);
            if (!sanitized.selections().equals(selectionStore.snapshot())
                    && !selectionStore.replaceAll(sanitized.selections())) {
                throw new IllegalStateException("Impossible d'assainir les sélections persistées.");
            }

            runtime = RuntimeComponents.create(this, configuration, selectionStore);
            Bukkit.getPluginManager().registerEvents(new PlayerGlowListener(this::glowEngine), this);
            placeholderProvider = HCPlaceholders.require(this).register(
                    this,
                    "glow",
                    new PlayerGlowPlaceholderProvider(this::tabColorCode)
            );
            registerCommands();
            runtime.start();
            notifyInvalidated(sanitized.invalidOnlinePlayers(), configuration);
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE,
                    "Impossible d'initialiser HCGlowing. Le plugin va être désactivé.", exception);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        cleanup("les commandes", () -> {
            if (commandRegistration != null) {
                commandRegistration.close();
            }
        });
        cleanup("le provider de placeholders", () -> {
            if (placeholderProvider != null) {
                placeholderProvider.close();
            }
        });
        cleanup("le moteur de glow", () -> {
            if (runtime != null) {
                runtime.shutdown();
            }
        });
        cleanup("les permissions dynamiques", () -> {
            if (cosmeticPermissions != null) {
                cosmeticPermissions.unregisterAll();
            }
        });
    }

    private void registerCommands() {
        PlayerGlowCommand commands = new PlayerGlowCommand(
                this::configuration,
                this::openDialog,
                this::reloadRuntime
        );
        commandRegistration = HCPluginsCore.require(this).register(
                this,
                "glowing",
                "Cosmétiques de glow",
                List.of(),
                commands
        );
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("glow", "Cosmétiques de glow", commands)
        );
    }

    /**
     * Validates a complete replacement before touching the active runtime generation.
     */
    private boolean reloadRuntime() {
        RuntimeComponents previous = runtime;
        Map<UUID, String> previousSelections = selectionStore.snapshot();
        RuntimeComponents replacement;
        Map<UUID, Boolean> previousGlowingStates = previous.glowEngine().previousGlowingStates();
        GlowConfiguration replacementConfiguration;
        SanitizedSelections sanitized;
        try {
            replacementConfiguration = GlowConfiguration.load(configurationFile);
            synchronizeCosmeticPermissions(replacementConfiguration);
            sanitized = sanitizeSelections(replacementConfiguration);
            replacement = RuntimeComponents.create(
                    this,
                    replacementConfiguration,
                    selectionStore,
                    previousGlowingStates
            );
            if (!sanitized.selections().equals(previousSelections)
                    && !selectionStore.replaceAll(sanitized.selections())) {
                synchronizeCosmeticPermissions(previous.configuration());
                return false;
            }
        } catch (RuntimeException exception) {
            restoreCosmeticPermissions(previous.configuration(), exception);
            getLogger().log(Level.SEVERE,
                    "Impossible de recharger config.yml ; la configuration précédente reste active.", exception);
            return false;
        }

        previous.shutdown();
        runtime = replacement;
        try {
            replacement.start();
            notifyInvalidated(sanitized.invalidOnlinePlayers(), replacementConfiguration);
            return true;
        } catch (RuntimeException activationException) {
            replacement.shutdown();
            restoreCosmeticPermissions(previous.configuration(), activationException);
            if (!selectionStore.restoreAfterFailedReload(previousSelections)) {
                getLogger().severe("data.yml n'a pas pu être restauré après l'échec du rechargement.");
            }

            RuntimeComponents recovered = RuntimeComponents.create(
                    this,
                    previous.configuration(),
                    selectionStore,
                    previousGlowingStates
            );
            runtime = recovered;
            try {
                recovered.start();
            } catch (RuntimeException recoveryException) {
                activationException.addSuppressed(recoveryException);
                getLogger().log(Level.SEVERE,
                        "La configuration précédente n'a pas pu être réactivée ; le plugin va être désactivé.",
                        activationException);
                Bukkit.getPluginManager().disablePlugin(this);
                return false;
            }
            getLogger().log(Level.SEVERE,
                    "La nouvelle configuration n'a pas pu être activée ; la configuration précédente a été restaurée.",
                    activationException);
            return false;
        }
    }

    private SanitizedSelections sanitizeSelections(GlowConfiguration configuration) {
        Map<UUID, String> validSelections = new LinkedHashMap<>();
        Set<UUID> invalidOnlinePlayers = new LinkedHashSet<>();
        selectionStore.snapshot().forEach((uuid, glowingId) -> {
            GlowConfiguration.GlowPattern pattern = configuration.glowing(glowingId);
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (pattern != null && (onlinePlayer == null || onlinePlayer.hasPermission(pattern.permission()))) {
                validSelections.put(uuid, glowingId);
            } else if (onlinePlayer != null) {
                invalidOnlinePlayers.add(uuid);
            }
        });
        return new SanitizedSelections(Map.copyOf(validSelections), Set.copyOf(invalidOnlinePlayers));
    }

    private void notifyInvalidated(Set<UUID> invalidated, GlowConfiguration configuration) {
        invalidated.stream()
                .map(Bukkit::getPlayer)
                .filter(java.util.Objects::nonNull)
                .filter(Player::isOnline)
                .forEach(player -> player.sendMessage(configuration.messages().selectionInvalidated()));
    }

    private void ensureConfigurationFile() {
        if (!configurationFile.exists()) {
            saveResource("config.yml", false);
        }
    }

    private void synchronizeCosmeticPermissions(GlowConfiguration configuration) {
        cosmeticPermissions.synchronize(configuration.glowings().values().stream()
                .map(GlowConfiguration.GlowPattern::permission)
                .toList());
    }

    private void restoreCosmeticPermissions(GlowConfiguration configuration, RuntimeException failure) {
        try {
            synchronizeCosmeticPermissions(configuration);
        } catch (RuntimeException rollbackException) {
            failure.addSuppressed(rollbackException);
        }
    }

    private GlowConfiguration configuration() {
        return runtime.configuration();
    }

    private GlowEngine glowEngine() {
        return runtime.glowEngine();
    }

    private String tabColorCode(UUID playerId) {
        RuntimeComponents current = runtime;
        return current == null ? "" : current.glowEngine().tabColorCode(playerId);
    }

    private void openDialog(Player player) {
        runtime.dialog().open(player);
    }

    private void cleanup(String component, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | LinkageError exception) {
            getLogger().log(Level.WARNING, exception, () -> "Impossible d'arrêter proprement " + component + '.');
        }
    }

    private record SanitizedSelections(
            Map<UUID, String> selections,
            Set<UUID> invalidOnlinePlayers
    ) {
    }

    private record RuntimeComponents(
            GlowConfiguration configuration,
            GlowEngine glowEngine,
            GlowSelectionDialog dialog
    ) {

        private static RuntimeComponents create(
                JavaPlugin plugin,
                GlowConfiguration configuration,
                GlowSelectionStore selectionStore
        ) {
            return create(plugin, configuration, selectionStore, Map.of());
        }

        private static RuntimeComponents create(
                JavaPlugin plugin,
                GlowConfiguration configuration,
                GlowSelectionStore selectionStore,
                Map<UUID, Boolean> previousGlowingStates
        ) {
            GlowEngine glowEngine = new GlowEngine(
                    plugin,
                    configuration,
                    selectionStore,
                    previousGlowingStates
            );
            GlowSelectionDialog dialog = new GlowSelectionDialog(
                    plugin,
                    configuration,
                    selectionStore,
                    glowEngine
            );
            return new RuntimeComponents(configuration, glowEngine, dialog);
        }

        private void start() {
            glowEngine.start();
        }

        private void shutdown() {
            dialog.shutdown();
            glowEngine.shutdown();
        }
    }
}
