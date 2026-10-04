package fr.noltox.hcplugins.customplayerglowing.storage;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S5960")
class GlowSelectionStoreTest {

    @TempDir
    Path directory;

    @Test
    void deferredInvalidationWriteCannotDeleteALaterSelection() {
        GlowSelectionStore store = store();
        store.load();
        UUID player = UUID.randomUUID();
        assertTrue(store.select(player, "gold"));
        assertTrue(store.invalidateWithoutSaving(player));
        assertNull(store.selected(player));
        assertTrue(store.select(player, "rainbow"));
        store.saveInvalidations();
        GlowSelectionStore reloaded = store();
        reloaded.load();
        assertEquals("rainbow", reloaded.selected(player));
    }

    @Test
    void batchInvalidationPersistsOnlyAffectedSelections() {
        GlowSelectionStore store = store();
        store.load();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID kept = UUID.randomUUID();
        assertTrue(store.replaceAll(Map.of(first, "gold", second, "rainbow", kept, "gold")));
        store.invalidateAll(java.util.List.of(first, second, UUID.randomUUID()));
        assertEquals(Map.of(kept, "gold"), store.snapshot());
        GlowSelectionStore reloaded = store();
        reloaded.load();
        assertEquals(store.snapshot(), reloaded.snapshot());
    }

    @Test
    void emptyAuditDoesNotTouchDisk() throws Exception {
        GlowSelectionStore store = store();
        store.load();
        var oldTime = java.nio.file.attribute.FileTime.fromMillis(1_000L);
        Files.setLastModifiedTime(dataFile(), oldTime);
        store.invalidateAll(java.util.List.of());
        store.invalidateAll(java.util.List.of(UUID.randomUUID()));
        assertEquals(oldTime, Files.getLastModifiedTime(dataFile()));
    }

    @Test
    void selectionsSurviveReloadAndCanBeCleared() {
        GlowSelectionStore store = store();
        store.load();
        UUID playerId = UUID.randomUUID();
        assertTrue(store.select(playerId, "gold"));

        GlowSelectionStore reloaded = store();
        reloaded.load();
        assertEquals("gold", reloaded.selected(playerId));
        assertTrue(reloaded.clear(playerId));
        store.load();
        assertTrue(store.snapshot().isEmpty());
    }

    @Test
    void selectingTheSameProfileDoesNotRewriteTheDataFile() throws Exception {
        GlowSelectionStore store = store();
        store.load();
        UUID playerId = UUID.randomUUID();
        assertTrue(store.select(playerId, "gold"));
        var oldTime = java.nio.file.attribute.FileTime.fromMillis(1_000L);
        Files.setLastModifiedTime(dataFile(), oldTime);
        assertTrue(store.select(playerId, "gold"));
        assertEquals(oldTime, Files.getLastModifiedTime(dataFile()));
    }

    @Test
    void malformedSelectionsPreserveFileAndLoadedState() throws Exception {
        GlowSelectionStore store = store();
        store.load();
        UUID playerId = UUID.randomUUID();
        assertTrue(store.select(playerId, "gold"));
        Path file = dataFile();
        for (String malformed : new String[]{"selections: invalid\n", "selections: [gold]\n"}) {
            Files.writeString(file, malformed);
            assertThrows(IllegalStateException.class, store::load);
            assertEquals(Map.of(playerId, "gold"), store.snapshot());
            assertEquals(malformed, Files.readString(file));
        }
    }

    @Test
    void duplicateSelectionsPreserveFileAndLoadedState() throws Exception {
        GlowSelectionStore store = store();
        store.load();
        UUID playerId = UUID.randomUUID();
        assertTrue(store.select(playerId, "gold"));
        Path file = dataFile();
        String duplicate = "selections:\n  " + playerId + ": gold\n  " + playerId + ": green\n";
        Files.writeString(file, duplicate);

        assertThrows(IllegalStateException.class, store::load);
        assertEquals(Map.of(playerId, "gold"), store.snapshot());
        assertEquals(duplicate, Files.readString(file));
    }

    private GlowSelectionStore store() {
        Plugin plugin = (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getDataFolder")) {
                        return directory.resolve("HCGlowing").toFile();
                    }
                    if (method.getName().equals("getName")) {
                        return "HCGlowing";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return new GlowSelectionStore(plugin);
    }

    private Path dataFile() {
        return directory.resolve("HCPlugins/HCGlowing/data.yml");
    }
}
