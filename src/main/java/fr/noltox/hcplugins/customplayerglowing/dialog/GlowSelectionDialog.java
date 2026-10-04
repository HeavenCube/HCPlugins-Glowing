package fr.noltox.hcplugins.customplayerglowing.dialog;

import fr.noltox.hcplugins.core.api.HCPluginsCore;
import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration;
import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration.GlowPattern;
import fr.noltox.hcplugins.customplayerglowing.service.GlowEngine;
import fr.noltox.hcplugins.customplayerglowing.service.GlowEngine.SelectionResult;
import fr.noltox.hcplugins.customplayerglowing.storage.GlowSelectionStore;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the native Paper dialog for one immutable configuration generation.
 */
public final class GlowSelectionDialog {

    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(2))
            .build();

    private final Plugin plugin;
    private final GlowConfiguration configuration;
    private final GlowSelectionStore selectionStore;
    private final GlowEngine glowEngine;
    private boolean active = true;

    public GlowSelectionDialog(
            Plugin plugin,
            GlowConfiguration configuration,
            GlowSelectionStore selectionStore,
            GlowEngine glowEngine
    ) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.selectionStore = selectionStore;
        this.glowEngine = glowEngine;
    }

    public void open(Player player) {
        if (!active || !player.isOnline()) {
            return;
        }
        glowEngine.synchronize(player);
        String selectedId = selectionStore.selected(player);
        List<ActionButton> actions = new ArrayList<>(configuration.glowings().size() + 1);
        for (GlowPattern pattern : configuration.glowings().values()) {
            Component label = pattern.buttonName();
            if (pattern.id().equals(selectedId)) {
                label = label.append(Component.space()).append(configuration.dialog().selectedSuffix());
            }
            DialogAction action = player.hasPermission(pattern.permission())
                    ? DialogAction.customClick(
                    (response, audience) -> scheduleSelection(audience, pattern.id()),
                    CALLBACK_OPTIONS
            )
                    : null;
            actions.add(ActionButton.create(label, pattern.tooltip(), 200, action));
        }

        actions.add(ActionButton.create(
                configuration.dialog().disableButtonName(),
                configuration.dialog().disableTooltip(),
                200,
                DialogAction.customClick(
                        (response, audience) -> scheduleDisable(audience),
                        CALLBACK_OPTIONS
                )
        ));

        ActionButton closeButton = ActionButton.create(
                configuration.dialog().closeButtonName(),
                configuration.dialog().closeTooltip(),
                120,
                DialogAction.customClick(
                        (response, audience) -> scheduleClose(audience),
                        CALLBACK_OPTIONS
                )
        );
        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(configuration.dialog().title())
                        .externalTitle(configuration.dialog().title())
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(List.of(DialogBody.plainMessage(configuration.dialog().description(), 360)))
                        .build())
                .type(DialogType.multiAction(actions, closeButton, 2)));
        player.showDialog(dialog);
    }

    public void shutdown() {
        active = false;
    }

    private void scheduleSelection(Object audience, String glowingId) {
        if (active && audience instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> select(player, glowingId));
        }
    }

    private void select(Player player, String glowingId) {
        if (!active || !player.isOnline()) {
            return;
        }
        SelectionResult result = glowEngine.select(player, glowingId);
        switch (result) {
            case APPLIED -> {
                player.sendMessage(configuration.messages().selectionSaved());
                player.closeDialog();
            }
            case NOT_ALLOWED -> player.sendMessage(HCPluginsCore.translations(plugin).noPermission());
        }
    }

    private void scheduleDisable(Object audience) {
        if (active && audience instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> disable(player));
        }
    }

    private void disable(Player player) {
        if (!active || !player.isOnline()) {
            return;
        }
        glowEngine.disable(player);
        player.sendMessage(configuration.messages().selectionDisabled());
        open(player);
    }

    private void scheduleClose(Object audience) {
        if (active && audience instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (active && player.isOnline()) {
                    player.closeDialog();
                }
            });
        }
    }
}
