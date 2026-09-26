package fr.noltox.hcplugins.customplayerglowing.listener;

import fr.noltox.hcplugins.customplayerglowing.service.GlowEngine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.function.Supplier;

/**
 * Connects player lifecycle events to the current configuration generation.
 */
public final class PlayerGlowListener implements Listener {

    private final Supplier<GlowEngine> glowEngineSupplier;

    public PlayerGlowListener(Supplier<GlowEngine> glowEngineSupplier) {
        this.glowEngineSupplier = glowEngineSupplier;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        glowEngineSupplier.get().playerJoined(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        glowEngineSupplier.get().playerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChanged(PlayerChangedWorldEvent event) {
        glowEngineSupplier.get().queueResynchronization(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        glowEngineSupplier.get().queueResynchronization(event.getPlayer());
    }
}
