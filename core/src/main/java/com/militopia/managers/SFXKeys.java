package com.militopia.managers;

/**
 * Central constants for all SFX file paths.
 * AudioManager.playSFX() prepends "audio/sfx/" automatically — pass filename only.
 */
public final class SFXKeys {

    private SFXKeys() {}

    // ── UI ───────────────────────────────────────────────────────────────────
    public static final String UI_HOVER            = "ui_hover.WAV";
    public static final String UI_CLICK_CONFIRM    = "ui_click_confirm.WAV";
    public static final String UI_CLICK_CANCEL     = "ui_click_cancel.WAV";
    public static final String UI_PANEL_OPEN       = "ui_panel_open.WAV";
    public static final String UI_PANEL_CLOSE      = "ui_panel_close.WAV";
    public static final String UI_ERROR            = "ui_error.WAV";
    public static final String UI_NOTIFICATION     = "ui_notification.WAV";
    public static final String UI_TOGGLE           = "ui_toggle.WAV";

    // ── Tiles ─────────────────────────────────────────────────────────────────
    public static final String TILE_CLICK          = "tile_click.WAV";
    public static final String TILE_DESELECT       = "tile_deselect.WAV";
    public static final String TILE_MOVE_RANGE     = "tile_move_range.WAV";
    public static final String TILE_ATTACK_RANGE   = "tile_attack_range.WAV";

    // ── Unit selection ────────────────────────────────────────────────────────
    public static final String UNIT_SELECT         = "unit_select.WAV";
    public static final String UNIT_DESELECT       = "unit_deselect.WAV";
    public static final String UNIT_CANT_MOVE      = "unit_cant_move.WAV";

    // ── Movement ──────────────────────────────────────────────────────────────
    public static final String MOVE_LAND_LIGHT              = "move_land_light.WAV";
    public static final String MOVE_LAND_HEAVY_TANK         = "move_land_heavy_tank.WAV";
    public static final String MOVE_LAND_HEAVY_JUGGERNAUT   = "move_land_heavy_juggernaut.WAV";
    public static final String MOVE_WATER                   = "move_water.WAV";
    public static final String MOVE_AIR_B2                  = "move_air.WAV";
    public static final String MOVE_AIR_HELICOPTER          = "move_air_helicopter.WAV";
    public static final String MOVE_AIR_DRONE               = "move_air_drone.WAV";

    // ── Attacks ───────────────────────────────────────────────────────────────
    public static final String ATTACK_KNIFE          = "attack_recruit_knife.WAV";
    public static final String ATTACK_AK47           = "attack_ranger_ak47.WAV";
    public static final String ATTACK_AWP            = "attack_sniper_awp.WAV";
    public static final String ATTACK_TANK           = "attack_tank_cannon.WAV";
    public static final String ATTACK_JUGGERNAUT     = "attack_juggernaut.WAV";
    public static final String ATTACK_B2             = "attack_b2_bomb.WAV";
    public static final String ATTACK_SUBMARINE      = "attack_submarine.WAV";
    public static final String ATTACK_APACHE         = "attack_apache.WAV";
    public static final String ATTACK_SUICIDE_DRONE  = "attack_suicide_drone.WAV";
    public static final String ATTACK_GUNBOAT        = "attack_gunboat.WAV";
    public static final String ATTACK_DESTROYER      = "attack_destroyer.WAV";
    public static final String ATTACK_CARRIER        = "attack_carrier.WAV";

    // ── Hits & blocks ─────────────────────────────────────────────────────────
    public static final String HIT_MAN             = "hit_man.WAV";
    public static final String HIT_MACHINE         = "hit_machine.WAV";
    public static final String BLOCKED_MAN         = "blocked_man.WAV";
    public static final String BLOCKED_MACHINE     = "blocked_machine.WAV";

    // ── Deaths ────────────────────────────────────────────────────────────────
    public static final String DEATH_MAN           = "death_man.WAV";
    public static final String DEATH_MACHINE       = "death_machine.WAV";
    public static final String DEATH_AIR           = "death_air.WAV";
    public static final String DEATH_NAVAL         = "death_naval.WAV";

    // ── Structures & Actions ──────────────────────────────────────────────────
    public static final String STRUCTURE_CAPTURE   = "structure_capture.WAV";
    public static final String UNIT_DEPLOY         = "unit_deploy.WAV";
    public static final String ACTION_HUNT         = "action_hunt.WAV";
    public static final String ACTION_DEMOLISH     = "action_demolish.WAV";
    public static final String ACTION_SCAVENGE     = "action_scavenge.WAV";
    public static final String ACTION_BUILD        = "action_build.WAV";
    public static final String ACTION_CUT_TREE     = "action_cut_tree.WAV";

    // ── Economy ───────────────────────────────────────────────────────────────
    public static final String RESOURCE_COLLECT    = "resource_collect.WAV";
    public static final String RESOURCE_INSUFFICIENT = "resource_insufficient.WAV";

    // ── Turn flow ─────────────────────────────────────────────────────────────
    public static final String TURN_END_PLAYER     = "turn_end_player.WAV";
    public static final String TURN_START_ENEMY    = "turn_start_enemy.WAV";
    public static final String TURN_START_PLAYER   = "turn_start_player.WAV";

    // ── Fog of War ────────────────────────────────────────────────────────────
    public static final String FOG_REVEAL          = "fog_reveal.WAV";

    // ── Game end ──────────────────────────────────────────────────────────────
    public static final String VICTORY             = "victory.WAV";
    public static final String DEFEAT              = "defeat.WAV";
}
