package fr.noltox.hcplugins.customplayerglowing.config;

import fr.noltox.hcglowprofiles.GlowProfiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlowConfigurationTest {

    @TempDir
    Path directory;

    @Test
    void bundledConfigurationPreservesOrderPermissionsAndProfiles() throws IOException {
        var configuration = load(defaults());
        assertEquals(List.of("heaven", "rainbow", "pink"), List.copyOf(configuration.glowings().keySet()));
        var heaven = configuration.glowing("heaven");
        assertEquals("hcplugins.glowing.cosmetic.heaven", heaven.permission());
        assertEquals("heaven-gradient", heaven.profile().id());
        assertEquals(GlowProfiles.EffectType.ANIMATED_GRADIENT, heaven.profile().effectType());
    }

    @Test
    void rejectsUnknownOrMissingProfiles() throws IOException {
        String defaults = defaults();
        assertThrows(IllegalStateException.class,
                () -> load(defaults.replace("profile: heaven-gradient", "profile: absent")));
        assertThrows(IllegalStateException.class,
                () -> load(defaults.replace("    profile: heaven-gradient\n", "")));
    }

    @Test
    void rejectsInvalidIdsAndNonSectionEntries() throws IOException {
        String defaults = defaults();
        assertThrows(IllegalStateException.class,
                () -> load(defaults.replace("  heaven:", "  Heaven:")));
        assertThrows(IllegalStateException.class,
                () -> load(defaults.replace("  heaven:\n", "  heaven: invalid\n")));
    }

    private GlowConfiguration load(String text) throws IOException {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, text);
        return GlowConfiguration.load(file.toFile());
    }

    private String defaults() throws IOException {
        try (var stream = getClass().getResourceAsStream("/config.yml")) {
            if (stream == null) {
                throw new IOException("Configuration embarquée absente.");
            }
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
