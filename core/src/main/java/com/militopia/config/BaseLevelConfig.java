package com.militopia.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BaseLevelConfig {

    public static class LevelData {

        public int level;
        public float maxXP;
        public int income; // The NEW income set for the base
        public int fundingBonus; // Immediate cash reward
        public int borderRadius; // Vision/Border radius
        public String[] unlockedUnits;
        public String[] unlockedStructures;

        public LevelData(int level, float maxXP, int income, int fundingBonus, int borderRadius,
                String[] units, String[] structs) {
            this.level = level;
            this.maxXP = maxXP;
            this.income = income;
            this.fundingBonus = fundingBonus;
            this.borderRadius = borderRadius;
            this.unlockedUnits = units;
            this.unlockedStructures = structs;
        }
    }

    private static final Map<Integer, LevelData> levels = new HashMap<>();

    static {
        // Level 1 (Initial)
        levels.put(1, new LevelData(1, 2000, 2, 5, GameConfig.BORDER_RADIUS,
                new String[] { "RECRUIT" },
                new String[] { "MUNITION_FACTORY" }));

        // Level 2
        levels.put(2, new LevelData(2, 3000, 3, 0, GameConfig.BORDER_RADIUS,
                new String[] { "RANGER", "RECON_DRONE", "GUNBOAT" },
                new String[] { "PORT", "HOSPITAL" }));

        // Level 3
        levels.put(3, new LevelData(3, 4500, 3, 10, GameConfig.BORDER_RADIUS,
                new String[] { "SNIPER", "SUICIDE_DRONE", "DESTROYER" },
                new String[] { "OIL_DERRICK", "RADAR" }));

        // Level 4 (Border Growth)
        levels.put(4, new LevelData(4, 6750, 3, 0, GameConfig.BORDER_RADIUS + 1,
                new String[] { "TANK", "APACHE", "CARRIER" },
                new String[] { "SOLAR", "JAMMER" }));

        // Level 5 — super unit is chosen via popup, not listed as a normal unlock
        levels.put(5, new LevelData(5, 10125, 3, 10, GameConfig.BORDER_RADIUS + 1,
                new String[] {},
                new String[] { "NUCLEAR" }));
    }

    public static LevelData getLevel(int level) {
        if (levels.containsKey(level)) {
            return levels.get(level);
        }
        // For Level 6+, calculate dynamically
        if (level > 5) {
            float prevMaxXP = levels.get(5).maxXP;
            for (int i = 6; i <= level; i++) {
                prevMaxXP *= 1.5f;
            }
            return new LevelData(level, prevMaxXP, 3, 10, GameConfig.BORDER_RADIUS + 1,
                    new String[] {},
                    new String[] {});
        }
        return levels.get(1);
    }

    /**
     * Collects all unlocked keys (units or structures) up to a maximum base level.
     */
    public static Set<String> getUnlockedForLevel(int maxLevel, boolean structs) {
        Set<String> set = new HashSet<>();
        for (int i = 1; i <= maxLevel; i++) {
            LevelData data = getLevel(i);
            String[] keys = structs ? data.unlockedStructures : data.unlockedUnits;
            if (keys != null) {
                for (String k : keys)
                    set.add(k);
            }
        }
        return set;
    }
}
