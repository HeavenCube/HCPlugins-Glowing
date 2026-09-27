package fr.noltox.hcplugins.customplayerglowing.storage;

import fr.noltox.hcplugins.core.api.config.BukkitYaml;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persists one selected glowing ID per player in a dedicated YAML file.
 */
public final class GlowSelectionStore {

    private static final String SELECTIONS_PATH = "selections";

    private final Plugin plugin;
    private final File dataFile;
    private Map<UUID, String> selections = new LinkedHashMap<>();

    public GlowSelectionStore(Plugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!dataFile.exists()) {
            try {
                write(Map.of());
                selections = new LinkedHashMap<>();
                return;
            } catch (IOException exception) {
                throw new IllegalStateException("Impossible de créer data.yml.", exception);
            }
        }

        YamlConfiguration data = BukkitYaml.load(dataFile.toPath());

        Map<UUID, String> loaded = new LinkedHashMap<>();
        var section = data.getConfigurationSection(SELECTIONS_PATH);
        if (data.contains(SELECTIONS_PATH) && section == null) {
            throw new IllegalStateException("La clé 'selections' de data.yml doit être une section YAML.");
        }
        if (section != null) {
            for (String rawUuid : section.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalStateException("UUID invalide dans data.yml : " + rawUuid, exception);
                }
                Object rawSelection = section.get(rawUuid);
                if (!(rawSelection instanceof String selection) || selection.isBlank()) {
                    throw new IllegalStateException("Sélection invalide dans data.yml pour " + rawUuid + ".");
                }
                loaded.put(uuid, selection);
            }
        }
        selections = loaded;
    }

    public String selected(UUID uuid) {
        return selections.get(uuid);
    }

    public Map<UUID, String> snapshot() {
        return Map.copyOf(selections);
    }

    public boolean select(UUID uuid, String glowingId) {
        Map<UUID, String> replacement = new LinkedHashMap<>(selections);
        replacement.put(uuid, glowingId);
        return persistAndReplace(replacement, "Impossible de sauvegarder le glow de " + uuid + ".");
    }

    public boolean clear(UUID uuid) {
        if (!selections.containsKey(uuid)) {
            return true;
        }
        Map<UUID, String> replacement = new LinkedHashMap<>(selections);
        replacement.remove(uuid);
        return persistAndReplace(replacement, "Impossible de supprimer le glow de " + uuid + ".");
    }

    /**
     * Removes an unusable selection from memory even when the disk write fails.
     */
    public void invalidate(UUID uuid) {
        if (!selections.containsKey(uuid)) {
            return;
        }
        Map<UUID, String> replacement = new LinkedHashMap<>(selections);
        replacement.remove(uuid);
        selections = replacement;
        try {
            write(replacement);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, exception,
                    () -> "La sélection invalide de " + uuid + " n'a pas pu être retirée de data.yml.");
        }
    }

    public boolean replaceAll(Map<UUID, String> replacement) {
        return persistAndReplace(
                new LinkedHashMap<>(replacement),
                "Impossible de mettre à jour les sélections dans data.yml."
        );
    }

    /**
     * Restores runtime consistency after a failed reload, even if the disk rollback cannot complete.
     */
    public boolean restoreAfterFailedReload(Map<UUID, String> replacement) {
        Map<UUID, String> restored = new LinkedHashMap<>(replacement);
        selections = restored;
        try {
            write(restored);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Impossible de restaurer les sélections précédentes dans data.yml.", exception);
            return false;
        }
    }

    private boolean persistAndReplace(Map<UUID, String> replacement, String errorMessage) {
        try {
            write(replacement);
            selections = replacement;
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, errorMessage, exception);
            return false;
        }
    }

    private void write(Map<UUID, String> replacement) throws IOException {
        Path dataDirectory = plugin.getDataFolder().toPath();
        Files.createDirectories(dataDirectory);
        Path temporaryFile = Files.createTempFile(dataDirectory, "player-glows-", ".yml.tmp");
        try {
            YamlConfiguration data = new YamlConfiguration();
            data.createSection(SELECTIONS_PATH);
            replacement.forEach((uuid, glowingId) ->
                    data.set(SELECTIONS_PATH + "." + uuid, glowingId));
            data.save(temporaryFile.toFile());
            try {
                Files.move(
                        temporaryFile,
                        dataFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
