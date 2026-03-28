package com.militopia.config;

/**
 * Enum representing the wild animal species present in Militopia.
 *
 * Replaces fragile {@code rawName.contains("DEER")} / {@code type.name().contains("FISH")}
 * string checks with a single canonical lookup.
 */
public enum AnimalType {

    DEER,
    FISH,
    ZEBRA,
    HORSE;

    /**
     * Returns the AnimalType whose name is contained in {@code key} (case-insensitive),
     * or null if no animal type matches.
     *
     * <p>This mirrors the previous {@code rawName.contains("DEER")} semantics so that
     * keys like "ANIMAL_DEER" or "DEER" both resolve to {@link #DEER}.</p>
     *
     * @param key the string to test against each animal name (e.g. "ANIMAL_DEER", "FISH")
     * @return the matching {@link AnimalType}, or null if none matches
     */
    public static AnimalType fromKey(String key) {
        if (key == null) return null;
        String upper = key.toUpperCase();
        for (AnimalType a : values()) {
            if (upper.contains(a.name())) {
                return a;
            }
        }
        return null;
    }
}
