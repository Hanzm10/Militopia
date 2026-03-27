package com.militopia.components;

import com.badlogic.ashley.core.Component;

/**
 * Stores status and state for unique unit/structure abilities.
 */
public class AbilitiesComponent implements Component {

    // --- State flags ---
    public boolean isDiggingIn = false; // Recruit
    public boolean hasUsedDigIn = false; // Recruit
    public boolean isOverwatchActive = false; // Ranger (must be manually activated)
    public boolean isCloaked = false; // Sniper / Wraith / Submarine / B2
    public boolean pendingSkirmishMove = false; // Gunboat: 1-tile move after attacking

    // --- Resources / Cooldowns ---
    public int fuel = -1; // Apache (-1 = N/A)
    public int nukeCooldown = 0; // Submarine

    // --- Ability Specific Data ---
    public int fuelMax = 5;

    public AbilitiesComponent() {
    }

    /** Convenience for initializing with fuel */
    public AbilitiesComponent(int initialFuel) {
        this.fuel = initialFuel;
        this.fuelMax = initialFuel;
    }
}
