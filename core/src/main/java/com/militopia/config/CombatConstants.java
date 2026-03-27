package com.militopia.config;

/**
 * Named constants for all magic numbers used in combat resolution.
 * Centralizing these makes tuning easier and avoids silent bugs from
 * scattered literals.
 */
public class CombatConstants {

    private CombatConstants() {}

    // -----------------------------------------------------------------------
    // Jump animation (Juggernaut)
    // -----------------------------------------------------------------------
    public static final float JUMP_ARC_HEIGHT       = 50f;
    public static final float JUMP_DURATION         = 0.7f;

    // -----------------------------------------------------------------------
    // Attack animations
    // -----------------------------------------------------------------------
    public static final float LUNGE_MELEE_DURATION  = 0.4f;
    public static final float LUNGE_RANGED_DURATION = 0.15f;
    public static final float LUNGE_MELEE_DISTANCE  = 1.5f;   // multiplier on iso delta
    public static final float LUNGE_RANGED_RECOIL   = -0.1f;  // small pop-back for ranged

    // -----------------------------------------------------------------------
    // Muzzle flash offsets
    // -----------------------------------------------------------------------
    public static final float MUZZLE_OFFSET_X_RIGHT =  3.5f;
    public static final float MUZZLE_OFFSET_X_LEFT  = -3.5f;
    public static final float MUZZLE_OFFSET_Y       = -2.5f;

    // -----------------------------------------------------------------------
    // Terrain defense bonuses
    // -----------------------------------------------------------------------
    public static final int TERRAIN_BONUS_MOUNTAIN  = 3;
    public static final int TERRAIN_BONUS_TREE      = 1;

    // -----------------------------------------------------------------------
    // Ability constants
    // -----------------------------------------------------------------------
    public static final int DIG_IN_DEFENSE_BONUS    = 3;
    public static final int MAX_RANGE_ATTACK_PENALTY = 1;
    public static final int SHORE_BOMBARDMENT_BONUS = 5;   // Destroyer vs Land

    // -----------------------------------------------------------------------
    // Nuke ability
    // -----------------------------------------------------------------------
    public static final int NUKE_RADIUS             = 1;
    public static final int NUKE_DAMAGE             = 15;
    public static final int NUKE_COOLDOWN_TURNS     = 3;
}
