package fr.noltox.hcplugins.customplayerglowing.permission;

/**
 * Permission nodes owned by the glow module.
 */
public final class Permissions {

    public static final String COSMETIC_PREFIX = "hcplugins.glowing.cosmetic.";

    private Permissions() {
    }

    public static String cosmetic(String id) {
        return COSMETIC_PREFIX + id;
    }
}
