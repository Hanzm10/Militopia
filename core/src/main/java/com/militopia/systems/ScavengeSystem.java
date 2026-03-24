package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.math.MathUtils;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.utils.GameLogger;

/**
 * Handles the logic for scavenging ruins.
 * Extracted from SlideMenu to follow ECS patterns.
 */
public class ScavengeSystem {

    private final PooledEngine engine;
    private final UnitFactory factory;
    private final GameState gameState;
    private final MapGenerator.GameMap map;

    public ScavengeSystem(PooledEngine engine, UnitFactory factory, GameState gameState, MapGenerator.GameMap map) {
        this.engine = engine;
        this.factory = factory;
        this.gameState = gameState;
        this.map = map;
    }

    public static class ScavengeReward {
        public final String message;
        public final int fundingChange;
        public final int xpChange;

        public ScavengeReward(String message, int fundingChange, int xpChange) {
            this.message = message;
            this.fundingChange = fundingChange;
            this.xpChange = xpChange;
        }
    }

    public ScavengeReward performScavenge(Entity ruinsEntity, Entity unit) {
        GridPositionComponent unitPos = unit.getComponent(GridPositionComponent.class);
        StatsComponent unitStats = unit.getComponent(StatsComponent.class);
        if (unitPos == null || unitStats == null)
            return null;

        int owner = unitStats.owner;
        int fundingGain = 0;
        int xpGain = 0;

        // 1. Mark unit as acted
        unitStats.hasActed = true;

        // 2. Remove ruins from map and engine
        map.objects[unitPos.x][unitPos.y] = MapGenerator.ObjectType.NONE;
        engine.removeEntity(ruinsEntity);

        // 3. Roll for reward
        int roll = MathUtils.random(1, 100);
        String rewardMsg = "";

        if (roll <= 20) {
            fundingGain = 15;
            rewardMsg = "Found 15 Funding!";
        } else if (roll <= 40) {
            xpGain = 1000;
            rewardMsg = "Found 1000 XP!";
        } else if (roll <= 60) {
            factory.createUnit("RECON_DRONE", unitPos.x, unitPos.y, owner, false);
            rewardMsg = "Found a Recon Drone!";
        } else if (roll <= 80) {
            int[] spawn = factory.findValidSpawnPoint(unitPos.x, unitPos.y, StatsComponent.MoveType.LAND, map);
            if (spawn != null) {
                factory.createUnit("SNIPER", spawn[0], spawn[1], owner, false);
                rewardMsg = "Found a Sniper!";
            } else {
                fundingGain = 15;
                rewardMsg = "Found supplies (+15 Funding)!";
            }
        } else {
            int[] spawn = factory.findValidSpawnPoint(unitPos.x, unitPos.y, StatsComponent.MoveType.SEA, map);
            if (spawn != null) {
                factory.createUnit("DESTROYER", spawn[0], spawn[1], owner, false);
                rewardMsg = "Found a Destroyer!";
            } else {
                xpGain = 1000;
                rewardMsg = "Discovered ancient data (+1000 XP)!";
            }
        }

        if (owner == 1) {
            gameState.p1Funding += fundingGain;
            gameState.p1XP += xpGain;
        } else {
            gameState.p2Funding += fundingGain;
            gameState.p2XP += xpGain;
        }

        GameLogger.log(GameLogger.SCAVENGE, owner, rewardMsg + " at " + GameLogger.pos(unitPos.x, unitPos.y));
        return new ScavengeReward(rewardMsg, fundingGain, xpGain);
    }
}
