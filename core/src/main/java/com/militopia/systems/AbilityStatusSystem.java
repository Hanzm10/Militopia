package com.militopia.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.militopia.components.AbilitiesComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.map.MapGenerator;

/**
 * Handles turn-based status updates for unit and structure abilities.
 * This should be called once per turn flip (during Player turnover).
 */
public class AbilityStatusSystem extends EntitySystem {

    private final MapGenerator.GameMap gameMap;
    private Engine engine;

    public AbilityStatusSystem(MapGenerator.GameMap map) {
        this.gameMap = map;
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.engine = engine;
    }

    /**
     * Executes turn-start updates for the given player.
     * 
     * @param currentPlayerID The ID of the player whose turn is starting.
     */
    public void onTurnStart(int currentPlayerID) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(StatsComponent.class, GridPositionComponent.class, AbilitiesComponent.class).get());

        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            AbilitiesComponent abilities = e.getComponent(AbilitiesComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);

            if (stats.owner == currentPlayerID) {
                // --- TURN START: My units ---

                // RANGER: Overwatch Reset
                if (stats.unitTypeKey.equals("RANGER")) {
                    abilities.isOverwatchActive = true;
                }

                // SUBMARINE: Nuke Cooldown
                if (abilities.nukeCooldown > 0) {
                    abilities.nukeCooldown--;
                }

                // APACHE: Fuel Gauge
                if (stats.unitTypeKey.equals("APACHE")) {
                    if (abilities.fuel > 0) {
                        abilities.fuel--;
                    }
                    if (abilities.fuel == 0) {
                        // CRASH
                        stats.currentHP = 0; // Triggers death later
                    }
                }

                // CARRIER / HOSPITAL / SOLAR: Adjacency Effects
                checkAdjacencyEffects(e, stats, pos, currentPlayerID);
            }
        }
    }

    private void checkAdjacencyEffects(Entity entity, StatsComponent stats, GridPositionComponent pos, int owner) {
        ImmutableArray<Entity> neighbors = engine.getEntitiesFor(
                Family.all(StatsComponent.class, GridPositionComponent.class).get());

        boolean isCarrier = stats.unitTypeKey.equals("CARRIER");
        boolean isHospital = stats.name.contains("Hospital");
        boolean isSolar = stats.name.contains("Solar");

        if (!isCarrier && !isHospital && !isSolar)
            return;

        for (Entity neighbor : neighbors) {
            if (neighbor == entity)
                continue;
            StatsComponent nStats = neighbor.getComponent(StatsComponent.class);
            GridPositionComponent nPos = neighbor.getComponent(GridPositionComponent.class);

            if (nStats.owner == owner && chebyshev(pos.x, pos.y, nPos.x, nPos.y) <= 1) {
                // CARRIER: Heal & Refuel Air
                if (isCarrier && nStats.moveType == StatsComponent.MoveType.AIR) {
                    heal(nStats, 2);
                    AbilitiesComponent nAbilities = neighbor.getComponent(AbilitiesComponent.class);
                    if (nAbilities != null)
                        nAbilities.fuel = nAbilities.fuelMax;
                }
                // HOSPITAL: Heal
                if (isHospital) {
                    heal(nStats, 2);
                }
            }
        }
    }

    private void heal(StatsComponent stats, int amount) {
        stats.currentHP = Math.min(stats.maxHP, stats.currentHP + amount);
    }

    private int chebyshev(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }
}
