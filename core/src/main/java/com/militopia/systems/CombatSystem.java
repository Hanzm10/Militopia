package com.militopia.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.militopia.components.DeathAnimComponent;
import com.militopia.components.FloatingTextComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.factories.EntityFactory;
import com.militopia.map.MapGenerator;

/**
 * Handles all combat resolution: damage, counterattack, death flagging, and
 * spawning floating feedback text.
 *
 * This system is NOT an IteratingSystem — combat is event-driven (triggered by
 * player input), so the only public entry point is {@link #resolveAttack}.
 */
public class CombatSystem extends EntitySystem {

    private final MapGenerator.GameMap gameMap;
    private final EntityFactory entityFactory;
    private Engine engine;

    public CombatSystem(MapGenerator.GameMap gameMap, EntityFactory entityFactory) {
        this.gameMap = gameMap;
        this.entityFactory = entityFactory;
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.engine = engine;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Resolves a single attack action.
     *
     * Resolution order:
     * 1. Compute and apply attacker-to-defender damage.
     * 2. If defender dies → mark for death; no counter fires.
     * 3. If defender survives AND attacker is within defender's range → counter.
     * 4. Mark attacker as fully exhausted (hasActed = hasMoved = true).
     *
     * @param attacker The attacking unit entity.
     * @param defender The defending unit entity.
     */
    public void resolveAttack(Entity attacker, Entity defender) {
        GridPositionComponent aPos = attacker.getComponent(GridPositionComponent.class);
        GridPositionComponent dPos = defender.getComponent(GridPositionComponent.class);
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        StatsComponent dStats = defender.getComponent(StatsComponent.class);

        if (aPos == null || dPos == null || aStats == null || dStats == null)
            return;

        // --- 1. Attacker strikes ---
        int dist = chebyshev(aPos.x, aPos.y, dPos.x, dPos.y);
        boolean maxRange = (dist == aStats.attackRange);
        int defTerrainBonus = terrainDefBonus(dPos.x, dPos.y);

        int dmg = Math.max(0, aStats.attack - dStats.defense - defTerrainBonus
                - (maxRange && aStats.attackRange > 1 ? 1 : 0));
        dStats.currentHP -= dmg;

        // Spawn floating text above the defender
        spawnFloatingText(dmg, dPos.x, dPos.y, false);

        // --- 2. Defender death? ---
        if (dStats.currentHP <= 0) {
            dStats.currentHP = 0;
            flagDeath(defender);
            // Attacker-first resolution: no counter if defender is dead
            exhaustAttacker(aStats);
            return;
        }

        // --- 3. Counterattack (range-gated) ---
        int counterDist = chebyshev(dPos.x, dPos.y, aPos.x, aPos.y);
        if (counterDist <= dStats.attackRange) {
            boolean counterMaxRange = (counterDist == dStats.attackRange);
            int atkTerrainBonus = terrainDefBonus(aPos.x, aPos.y);

            int ctrDmg = Math.max(0, dStats.attack - aStats.defense - atkTerrainBonus
                    - (counterMaxRange && dStats.attackRange > 1 ? 1 : 0));
            aStats.currentHP -= ctrDmg;

            // Spawn floating text above the attacker (isCounter = true)
            spawnFloatingText(ctrDmg, aPos.x, aPos.y, true);

            if (aStats.currentHP <= 0) {
                aStats.currentHP = 0;
                flagDeath(attacker);
            }
        }

        // --- 4. Exhaust attacker ---
        exhaustAttacker(aStats);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Chebyshev (chessboard) distance — matches the diagonal-capable movement. */
    private int chebyshev(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    /**
     * Returns the DEF terrain bonus for the **defender's tile**.
     * MOUNTAIN terrain → +3
     * TREE object on GRASS → +1
     * everything else → 0
     */
    private int terrainDefBonus(int x, int y) {
        MapGenerator.TerrainType terrain = gameMap.terrain[x][y];
        MapGenerator.ObjectType obj = gameMap.objects[x][y];

        if (terrain == MapGenerator.TerrainType.MOUNTAIN) {
            return 3;
        }
        if (obj == MapGenerator.ObjectType.TREE) {
            return 1;
        }
        return 0;
    }

    /**
     * Adds a DeathAnimComponent to the entity so UnitRenderSystem can animate it.
     */
    private void flagDeath(Entity unit) {
        if (unit.getComponent(DeathAnimComponent.class) == null) {
            unit.add(new DeathAnimComponent());
        }
    }

    /** Marks the attacker so they cannot act or move again this turn. */
    private void exhaustAttacker(StatsComponent aStats) {
        aStats.hasActed = true;
        aStats.hasMoved = true;
    }

    /**
     * Spawns a floating feedback entity above the given grid tile.
     * Shows "BLOCKED" when damage is zero, otherwise shows the number.
     */
    private void spawnFloatingText(int dmg, int gx, int gy, boolean isCounter) {
        String label = (dmg == 0) ? "BLOCKED" : String.valueOf(dmg);
        float worldX = EntityFactory.gridToIsoX(gx, gy);
        float worldY = EntityFactory.gridToIsoY(gx, gy);
        entityFactory.createFloatingText(label, worldX, worldY, isCounter);
    }
}
