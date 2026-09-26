package fr.noltox.hcplugins.customplayerglowing.support;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/** Manages dynamic permissions while preserving permissions owned by other plugins. */
public final class DynamicPermissionRegistry {

    private final PluginManager pluginManager;
    private final Logger logger;
    private final PermissionDefault defaultValue;
    private final Map<String, Permission> ownedPermissions = new HashMap<>();

    public DynamicPermissionRegistry(
            PluginManager pluginManager,
            Logger logger,
            PermissionDefault defaultValue
    ) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    }

    public void synchronize(Collection<String> requestedPermissions) {
        requirePrimaryThread();
        Set<String> desiredPermissions = new HashSet<>(requestedPermissions);
        if (desiredPermissions.size() != requestedPermissions.size()) {
            throw new IllegalArgumentException("La liste des permissions dynamiques contient un doublon.");
        }
        desiredPermissions.forEach(this::validatePermission);

        Map<String, Permission> createdPermissions = new HashMap<>();
        try {
            for (String node : desiredPermissions) {
                Permission existing = pluginManager.getPermission(node);
                Permission owned = ownedPermissions.get(node);
                if (owned != null && existing == owned) {
                    if (owned.getDefault() != defaultValue) {
                        owned.setDefault(defaultValue);
                        logger.warning(() -> "La permission dynamique '" + node
                                + "' avait été modifiée ; sa valeur par défaut a été restaurée.");
                    }
                } else if (existing == null) {
                    Permission created = new Permission(node, defaultValue);
                    pluginManager.addPermission(created);
                    createdPermissions.put(node, created);
                }
            }
        } catch (RuntimeException exception) {
            createdPermissions.forEach(this::removeIfOwned);
            throw exception;
        }

        ownedPermissions.putAll(createdPermissions);
        ownedPermissions.entrySet().removeIf(entry -> {
            if (desiredPermissions.contains(entry.getKey())) {
                return false;
            }
            removeIfOwned(entry.getKey(), entry.getValue());
            return true;
        });
    }

    public void unregisterAll() {
        requirePrimaryThread();
        ownedPermissions.forEach(this::removeIfOwned);
        ownedPermissions.clear();
    }

    private void validatePermission(String node) {
        if (node == null || node.isBlank()) {
            throw new IllegalArgumentException("Une permission dynamique est vide.");
        }
        Permission existing = pluginManager.getPermission(node);
        Permission owned = ownedPermissions.get(node);
        if (existing != null && existing != owned && existing.getDefault() != defaultValue) {
            throw new IllegalArgumentException("La permission dynamique '" + node
                    + "' est déjà enregistrée avec la valeur par défaut " + existing.getDefault()
                    + " ; elle doit être " + defaultValue + ".");
        }
    }

    private void removeIfOwned(String node, Permission permission) {
        if (pluginManager.getPermission(node) == permission) {
            pluginManager.removePermission(permission);
        }
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Les permissions dynamiques doivent être modifiées sur le thread serveur.");
        }
    }
}
