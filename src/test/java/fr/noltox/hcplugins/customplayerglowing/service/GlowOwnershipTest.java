package fr.noltox.hcplugins.customplayerglowing.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlowOwnershipTest {

    @Test
    void changingProfileKeepsTheOriginalGlowingState() {
        var ownership = new GlowEngine.GlowOwnership();
        UUID playerId = UUID.randomUUID();

        ownership.activate(playerId, false, "pink-static");
        ownership.activate(playerId, true, "rainbow");

        assertFalse(ownership.deactivate(playerId));
        assertNull(ownership.deactivate(playerId));
    }

    @Test
    void cleanupRestoresAnInitiallyActiveVanillaGlow() {
        var ownership = new GlowEngine.GlowOwnership();
        UUID playerId = UUID.randomUUID();
        ownership.activate(playerId, true, "pink-static");

        assertTrue(ownership.deactivate(playerId));
        assertEquals(0, ownership.playerIds().size());
    }

    @Test
    void reloadCarriesOriginalStateIntoTheReplacementGeneration() {
        var previous = new GlowEngine.GlowOwnership();
        UUID playerId = UUID.randomUUID();
        previous.activate(playerId, true, "pink-static");

        var replacement = new GlowEngine.GlowOwnership();
        replacement.restore(previous.previousGlowingStates());
        replacement.activate(playerId, false, "rainbow");

        assertTrue(replacement.deactivate(playerId));
    }
}
