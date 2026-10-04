package fr.noltox.hcplugins.customplayerglowing.storage;

import fr.noltox.hcplugins.customplayerglowing.testsupport.TestPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlowSelectionStoreTest {

    private final GlowSelectionStore store = new GlowSelectionStore(TestPlayer.plugin());

    @Test
    void selectionIsStoredInPlayerPdcAndReadByANewStore() {
        var player = new TestPlayer();
        store.select(player.player(), "heaven");

        assertEquals("heaven", player.pdc().get(new NamespacedKey("hcglowing", "selected_profile"),
                PersistentDataType.STRING));
        assertEquals("heaven", new GlowSelectionStore(TestPlayer.plugin()).selected(player.player()));
    }

    @Test
    void playersHaveIndependentSelections() {
        var first = new TestPlayer();
        var second = new TestPlayer();
        store.select(first.player(), "heaven");
        store.select(second.player(), "rainbow");
        store.clear(first.player());

        assertNull(store.selected(first.player()));
        assertEquals("rainbow", store.selected(second.player()));
    }

    @Test
    void clearingSelectionPreservesUnrelatedData() {
        var player = new TestPlayer();
        var other = new NamespacedKey("otherplugin", "level");
        player.pdc().set(other, PersistentDataType.INTEGER, 42);
        store.select(player.player(), "heaven");
        store.clear(player.player());
        store.clear(player.player());

        assertNull(store.selected(player.player()));
        assertEquals(42, player.pdc().get(other, PersistentDataType.INTEGER));
    }

    @Test
    void selectingTheSameProfileDoesNotRewritePdc() {
        var player = new TestPlayer();
        store.select(player.player(), "heaven");
        store.select(player.player(), "heaven");
        assertEquals(1, player.writes());
    }

    @Test
    void snapshotIsImmutableAndContainsOnlySuppliedSelectedPlayers() {
        var first = new TestPlayer();
        var unselected = new TestPlayer();
        var offline = new TestPlayer();
        store.select(first.player(), "heaven");
        store.select(offline.player(), "rainbow");

        var snapshot = store.snapshot(List.of(first.player(), unselected.player()));
        assertEquals(Map.of(first.player().getUniqueId(), "heaven"), snapshot);
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        store.select(first.player(), "pink");
        assertEquals("heaven", snapshot.get(first.player().getUniqueId()));
    }

    @Test
    void failedReloadRestoresSelectionsAndAbsenceWithoutTouchingOtherPlayersOrKeys() {
        var first = new TestPlayer();
        var initiallyUnselected = new TestPlayer();
        var offline = new TestPlayer();
        var online = List.of(first.player(), initiallyUnselected.player());
        var other = new NamespacedKey("otherplugin", "level");
        first.pdc().set(other, PersistentDataType.INTEGER, 42);
        store.select(first.player(), "heaven");
        store.select(offline.player(), "rainbow");
        var previous = store.snapshot(online);
        store.clear(first.player());
        store.select(initiallyUnselected.player(), "pink");

        store.restoreAfterFailedReload(online, previous);
        assertEquals("heaven", store.selected(first.player()));
        assertNull(store.selected(initiallyUnselected.player()));
        assertEquals("rainbow", store.selected(offline.player()));
        assertEquals(42, first.pdc().get(other, PersistentDataType.INTEGER));
    }

    @Test
    void rejectsMissingOrBlankSelectionWithoutChangingExistingData() {
        var player = new TestPlayer();
        store.select(player.player(), "heaven");
        assertThrows(NullPointerException.class, () -> store.select(player.player(), null));
        assertThrows(IllegalArgumentException.class, () -> store.select(player.player(), " "));
        assertEquals("heaven", store.selected(player.player()));
    }
}
