package fr.noltox.hcplugins.customplayerglowing.config;

import fr.noltox.hcplugins.core.api.glow.GlowProfiles;
import fr.noltox.hcplugins.core.api.config.BukkitYaml;
import fr.noltox.hcplugins.core.api.message.MiniMessages;
import fr.noltox.hcplugins.customplayerglowing.permission.Permissions;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Immutable, fully validated runtime configuration. */
public record GlowConfiguration(Map<String, GlowPattern> glowings, Messages messages, DialogText dialog) {

    private static final Pattern GLOWING_ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    public GlowConfiguration {
        glowings = Collections.unmodifiableMap(new LinkedHashMap<>(glowings));
    }

    public static GlowConfiguration load(File file) {
        YamlConfiguration configuration = BukkitYaml.load(file.toPath());
        ConfigurationSection section = configuration.getConfigurationSection("glowings");
        if (section == null || section.getKeys(false).isEmpty()) {
            throw invalid("La section 'glowings' doit contenir au moins un cosmétique.");
        }

        Map<String, GlowPattern> glowings = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            if (!GLOWING_ID.matcher(id).matches()) {
                throw invalid("L'identifiant '" + id
                        + "' doit uniquement contenir des minuscules, chiffres, tirets ou underscores.");
            }
            String path = "glowings." + id;
            if (section.getConfigurationSection(id) == null) {
                throw invalid("Le cosmétique '" + path + "' doit être une section YAML.");
            }
            String profileId = requiredString(configuration, path + ".profile");
            GlowProfiles.Profile profile = GlowProfiles.find(profileId)
                    .orElseThrow(() -> invalid("Le profil '" + profileId + "' de '" + path
                            + "' n'existe pas dans HCResourcePack."));
            glowings.put(id, new GlowPattern(
                    id,
                    Permissions.cosmetic(id),
                    profile,
                    component(configuration, path + ".button-name"),
                    component(configuration, path + ".tooltip")
            ));
        }

        Messages messages = new Messages(
                component(configuration, "messages.players-only"),
                component(configuration, "messages.no-permission"),
                component(configuration, "messages.usage"),
                component(configuration, "messages.reload-success"),
                component(configuration, "messages.reload-failure"),
                component(configuration, "messages.selection-saved"),
                component(configuration, "messages.selection-disabled"),
                component(configuration, "messages.selection-invalidated"),
                component(configuration, "messages.save-failure")
        );
        DialogText dialog = new DialogText(
                component(configuration, "dialog.title"),
                component(configuration, "dialog.description"),
                component(configuration, "dialog.selected-suffix"),
                component(configuration, "dialog.disable-button-name"),
                component(configuration, "dialog.disable-tooltip"),
                component(configuration, "dialog.close-button-name"),
                component(configuration, "dialog.close-tooltip")
        );
        return new GlowConfiguration(glowings, messages, dialog);
    }

    private static String requiredString(YamlConfiguration configuration, String path) {
        Object value = configuration.get(path);
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalid("La clé '" + path + "' doit être un texte non vide.");
        }
        return text;
    }

    private static Component component(YamlConfiguration configuration, String path) {
        String text = requiredString(configuration, path);
        try {
            return MiniMessages.parseStrict(text);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Le MiniMessage de la clé '" + path + "' est invalide.", exception);
        }
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException("Configuration invalide : " + message);
    }

    public GlowPattern glowing(String id) {
        return glowings.get(id);
    }

    public record GlowPattern(
            String id,
            String permission,
            GlowProfiles.Profile profile,
            Component buttonName,
            Component tooltip
    ) {
    }

    public record Messages(
            Component playersOnly,
            Component noPermission,
            Component usage,
            Component reloadSuccess,
            Component reloadFailure,
            Component selectionSaved,
            Component selectionDisabled,
            Component selectionInvalidated,
            Component saveFailure
    ) {
    }

    public record DialogText(
            Component title,
            Component description,
            Component selectedSuffix,
            Component disableButtonName,
            Component disableTooltip,
            Component closeButtonName,
            Component closeTooltip
    ) {
    }
}
