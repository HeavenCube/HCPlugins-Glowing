package fr.noltox.hcplugins.customplayerglowing.service;

import fr.noltox.hcplugins.core.api.glow.GlowProfiles;
import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration;
import fr.noltox.hcplugins.customplayerglowing.storage.GlowSelectionStore;
import fr.noltox.hcplugins.customplayerglowing.testsupport.TestPlayer;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlowEngineTest {

    private static final String PERMISSION = "hcplugins.glowing.cosmetic.heaven";
    private final TestPlayer player = new TestPlayer();
    private final GlowSelectionStore store = new GlowSelectionStore(TestPlayer.plugin());
    private final GlowEngine engine = engine();

    @Test
    void allowedSelectionUpdatesPdcAndGlow() {
        player.allow(PERMISSION);
        assertEquals(GlowEngine.SelectionResult.APPLIED, engine.select(player.player(), "heaven"));
        assertEquals("heaven", store.selected(player.player()));
        assertTrue(player.player().isGlowing());
        assertFalse(engine.tabColorCode(player.player().getUniqueId()).isEmpty());
    }

    @Test
    void deniedSelectionDoesNotReplaceExistingPdcData() {
        store.select(player.player(), "rainbow");
        assertEquals(GlowEngine.SelectionResult.NOT_ALLOWED, engine.select(player.player(), "heaven"));
        assertEquals(GlowEngine.SelectionResult.NOT_ALLOWED, engine.select(player.player(), "absent"));
        assertEquals("rainbow", store.selected(player.player()));
        assertFalse(player.player().isGlowing());
    }

    @Test
    void permissionRevocationClearsPreferenceAndRestoresOwnedGlow() {
        player.allow(PERMISSION);
        engine.select(player.player(), "heaven");
        player.revoke(PERMISSION);
        engine.synchronize(player.player());

        assertNull(store.selected(player.player()));
        assertFalse(player.player().isGlowing());
        assertEquals("", engine.tabColorCode(player.player().getUniqueId()));
        assertEquals(1, player.messages().size());
    }

    @Test
    void explicitDisableClearsSelectionButPreservesExternalGlow() {
        player.player().setGlowing(true);
        player.allow(PERMISSION);
        engine.select(player.player(), "heaven");
        engine.disable(player.player());

        assertNull(store.selected(player.player()));
        assertTrue(player.player().isGlowing());
        assertEquals("", engine.tabColorCode(player.player().getUniqueId()));
    }

    @Test
    void quitKeepsPdcSelectionAndJoinRestoresGlow() {
        player.allow(PERMISSION);
        engine.select(player.player(), "heaven");
        engine.playerQuit(player.player());
        assertEquals("heaven", store.selected(player.player()));
        assertFalse(player.player().isGlowing());
        assertTrue(engine.previousGlowingStates().isEmpty());
        assertEquals("", engine.tabColorCode(player.player().getUniqueId()));

        engine().playerJoined(player.player());
        assertTrue(player.player().isGlowing());
        assertEquals(1, player.writes());
    }

    @Test
    void removedProfileIsClearedWhenPlayerJoins() {
        store.select(player.player(), "removed");
        engine.playerJoined(player.player());
        assertNull(store.selected(player.player()));
        assertFalse(player.player().isGlowing());
        assertEquals(1, player.messages().size());
    }

    private GlowEngine engine() {
        var empty = Component.empty();
        var pattern = new GlowConfiguration.GlowPattern("heaven", PERMISSION,
                GlowProfiles.find("heaven-gradient").orElseThrow(), empty, empty);
        var config = new GlowConfiguration(Map.of("heaven", pattern),
                new GlowConfiguration.Messages(empty, empty, empty, empty),
                new GlowConfiguration.DialogText(empty, empty, empty, empty, empty, empty, empty));
        return new GlowEngine(TestPlayer.plugin(), config, store);
    }
}
