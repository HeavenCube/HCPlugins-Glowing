package fr.noltox.hcplugins.customplayerglowing.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlowOwnershipTest {

    @Test
    void auditSnapshotSurvivesOwnershipRemovalAndIsReusedWithoutStaleIds() {
        var ownership = new GlowEngine.GlowOwnership();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ownership.activate(first, false, "gold");
        ownership.activate(second, true, "gold");
        var buffer = new java.util.ArrayList<UUID>();
        ownership.copyPlayerIdsTo(buffer);
        for (UUID id : buffer) {
            ownership.deactivate(id);
        }
        assertEquals(2, buffer.size());
        ownership.copyPlayerIdsTo(buffer);
        assertTrue(buffer.isEmpty());
    }

    @Test
    void repeatedSynchronizationKeepsOriginalOwnership() {
        var ownership = new GlowEngine.GlowOwnership();
        UUID id = UUID.randomUUID();
        ownership.activate(id, false, "gold");
        ownership.activate(id, true, "gold");
        assertFalse(ownership.deactivate(id));
    }

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
