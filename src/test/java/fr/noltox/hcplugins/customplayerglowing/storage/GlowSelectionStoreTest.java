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
