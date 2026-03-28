package com.militopia.config;

public enum UnitType {
    // Land
    RECRUIT,
    RANGER,
    SNIPER,
    TANK,
    JUGGERNAUT,
    // Air
    RECON_DRONE,
    SUICIDE_DRONE,
    APACHE,
    B2,
    // Sea
    GUNBOAT,
    DESTROYER,
    CARRIER,
    SUBMARINE;

    /** Returns the UnitType for the given key string, or null if not found. */
    public static UnitType fromKey(String key) {
        if (key == null) return null;
        try {
            return valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
