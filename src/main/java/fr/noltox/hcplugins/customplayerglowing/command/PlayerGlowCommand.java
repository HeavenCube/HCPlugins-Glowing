package fr.noltox.hcplugins.customplayerglowing.command;

import fr.noltox.hcplugins.core.api.command.CoreCommand;
import fr.noltox.hcplugins.core.api.message.CoreTranslations;
import fr.noltox.hcplugins.customplayerglowing.config.GlowConfiguration;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles the canonical command and the plugin-owned /glow shortcut.
 */
public final class PlayerGlowCommand implements CoreCommand, BasicCommand {

    private final Supplier<GlowConfiguration> configurationSupplier;
    private final Consumer<Player> dialogOpener;
    private final BooleanSupplier configurationReloader;
    private final Plugin plugin;
    private final CoreTranslations translations;

    public PlayerGlowCommand(
            Plugin plugin,
            CoreTranslations translations,
            Supplier<GlowConfiguration> configurationSupplier,
            Consumer<Player> dialogOpener,
            BooleanSupplier configurationReloader
    ) {
        this.plugin = plugin;
        this.translations = translations;
        this.configurationSupplier = configurationSupplier;
        this.dialogOpener = dialogOpener;
        this.configurationReloader = configurationReloader;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            open(source.getSender());
        } else if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            reload(source.getSender());
        } else {
            source.getSender().sendMessage(configurationSupplier.get().messages().usage());
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (!source.getSender().isOp() || args.length > 1) {
            return List.of();
        }
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return "reload".startsWith(prefix) ? List.of("reload") : List.of();
    }

    private void open(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(translations.playersOnly());
            return;
        }
        dialogOpener.accept(player);
    }

    private void reload(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage(translations.operatorOnly());
            return;
        }
        long started = System.nanoTime();
        if (configurationReloader.getAsBoolean()) {
            sender.sendMessage(translations.reloadSuccess(plugin, System.nanoTime() - started));
        } else {
            sender.sendMessage(translations.reloadFailure(plugin));
        }
    }
}
