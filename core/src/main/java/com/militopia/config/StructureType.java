package com.militopia.config;

public enum StructureType {
    BASE("BASE", "Base"),
    TOWN("TOWN", "Town"),
    OIL_DERRICK("OIL_DERRICK", "Oil Derrick"),
    SOLAR("SOLAR", "Solar Array"),
    RADAR("RADAR", "Radar Station"),
    SIGNAL_JAMMER("JAMMER", "Signal Jammer"),
    MUNITION_FACTORY("MUNITION_FACTORY", "Munition Factory"),
    NUCLEAR_PLANT("NUCLEAR", "Nuclear Plant"),
    PORT("PORT", "Port"),
    AIRPORT("AIRPORT", "Airport");

    private final String key;
    private final String displayName;

    StructureType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Returns the StructureType for the given key string, or null if not found. */
    public static StructureType fromKey(String key) {
        if (key == null) return null;
        for (StructureType t : values()) {
            if (t.key.equalsIgnoreCase(key)) return t;
        }
        return null;
    }

    /** Returns the StructureType whose displayName matches the given string, or null if not found. */
    public static StructureType fromDisplayName(String name) {
        if (name == null) return null;
        for (StructureType t : values()) {
            if (t.displayName.equalsIgnoreCase(name)) return t;
        }
        // Partial match for names like "Player's 1st Base"
        String lower = name.toLowerCase();
        for (StructureType t : values()) {
            if (lower.contains(t.displayName.toLowerCase())) return t;
        }
        return null;
    }
}
