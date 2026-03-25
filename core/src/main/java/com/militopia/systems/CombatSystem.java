package com.militopia.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.militopia.components.DeathAnimComponent;
import com.militopia.components.FloatingTextComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.militopia.components.*;
import com.militopia.components.AbilitiesComponent;
import com.militopia.factories.EntityFactory;
import com.militopia.map.MapGenerator;
import com.militopia.utils.GameLogger;

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
    private final com.militopia.data.GameState gameState;
    private Engine engine;

    public CombatSystem(MapGenerator.GameMap gameMap, EntityFactory entityFactory,
            com.militopia.data.GameState gameState) {
        this.gameMap = gameMap;
        this.entityFactory = entityFactory;
        this.gameState = gameState;
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.engine = engine;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void launchNuke(Entity attacker, int tx, int ty) {
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        AbilitiesComponent aAbilities = attacker.getComponent(AbilitiesComponent.class);
        if (aStats == null || aAbilities == null)
            return;

        GameLogger.log(GameLogger.ABILITY, aStats.owner,
                "Nuke detonates at " + GameLogger.pos(tx, ty) + " | radius=1 | dmg=15");
        triggerExplosion(tx, ty, 1, 15, "NUKE");

        aStats.hasActed = true;
        aStats.hasMoved = true;
        aAbilities.nukeCooldown = 3;
    }

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

        // OIL DERRICK & NUCLEAR PLANT: Indestructible
        if (dStats.name.contains("Oil Derrick") || dStats.name.contains("Nuclear Plant")) {
            GameLogger.log(GameLogger.ATTACK, aStats.owner,
                    aStats.name + " attacks indestructible " + dStats.name + " at "
                            + GameLogger.pos(dPos.x, dPos.y) + " | BLOCKED");
            spawnFloatingText(0, dPos.x, dPos.y, false);
            exhaustAttacker(attacker, aStats, false);
            return;
        }

        // JUGGERNAUT: Suppressing Fire (AoE)
        if (aStats.unitTypeKey.equals("JUGGERNAUT")) {
            GameLogger.log(GameLogger.ABILITY, aStats.owner,
                    "Suppressing Fire by " + aStats.name + " at " + GameLogger.pos(aPos.x, aPos.y));
            resolveSuppressingFire(attacker, aPos);
            exhaustAttacker(attacker, aStats, false);
            return;
        }

        // RECON DRONE: High Altitude (Immune to Range-1 land attacks)
        if (dStats.unitTypeKey.equals("RECON_DRONE") && aStats.moveType == StatsComponent.MoveType.LAND
                && aStats.attackRange <= 1) {
            GameLogger.log(GameLogger.ATTACK, aStats.owner,
                    aStats.name + " attacks " + dStats.name + " | BLOCKED (High Altitude immunity)");
            spawnFloatingText(0, dPos.x, dPos.y, false);
            exhaustAttacker(attacker, aStats, false);
            return;
        }

        // --- 1. Attacker strikes ---
        int dist = chebyshev(aPos.x, aPos.y, dPos.x, dPos.y);
        boolean maxRange = (dist == aStats.attackRange);
        int defTerrainBonus = terrainDefBonus(dPos.x, dPos.y);

        // DESTROYER: Shore Bombardment (+5 vs Land)
        int shoreBonus = (aStats.unitTypeKey.equals("DESTROYER") && dStats.moveType == StatsComponent.MoveType.LAND) ? 5
                : 0;

        AbilitiesComponent dAbilities = defender.getComponent(AbilitiesComponent.class);
        int digInBonus = (dAbilities != null && dAbilities.isDiggingIn) ? 3 : 0;

        int dmg = Math.max(0, (aStats.attack + shoreBonus) - (dStats.defense + digInBonus) - defTerrainBonus
                - (maxRange && aStats.attackRange > 1 ? 1 : 0));
        dStats.currentHP -= dmg;

        // LOG: attack result
        GameLogger.log(GameLogger.ATTACK, aStats.owner,
                aStats.name + GameLogger.pos(aPos.x, aPos.y)
                        + " attacks " + dStats.name + GameLogger.pos(dPos.x, dPos.y)
                        + " | dmg=" + dmg
                        + " | defHP=" + dStats.currentHP + "/" + dStats.maxHP);

        // Spawn floating text above the defender
        spawnFloatingText(dmg, dPos.x, dPos.y, false);

        // --- 2. Defender death? ---
        if (dStats.currentHP <= 0) {
            dStats.currentHP = 0;
            GameLogger.log(GameLogger.ATTACK, aStats.owner,
                    dStats.name + " at " + GameLogger.pos(dPos.x, dPos.y) + " DESTROYED");
            flagDeath(defender);

            // Attacker-first resolution: no counter if defender is dead
            exhaustAttacker(attacker, aStats, true);
            return;
        }

        // --- 3. Counterattack (range-gated) ---
        // RECON DRONE: High Altitude (Immune to Range-1 Land attacks)
        boolean isHighAltitude = aStats.unitTypeKey.equals("RECON_DRONE");
        boolean isLandAttacker = dStats.moveType == StatsComponent.MoveType.LAND;

        int counterDist = chebyshev(dPos.x, dPos.y, aPos.x, aPos.y);
        if (counterDist <= dStats.attackRange) {
            if (isHighAltitude && isLandAttacker && dStats.attackRange == 1) {
                spawnFloatingText(0, aPos.x, aPos.y, true); // Visual feedback for immunity
            } else {
                boolean counterMaxRange = (counterDist == dStats.attackRange);
                int atkTerrainBonus = terrainDefBonus(aPos.x, aPos.y);

                int ctrDmg = Math.max(0, dStats.attack - aStats.defense - atkTerrainBonus
                        - (counterMaxRange && dStats.attackRange > 1 ? 1 : 0));
                aStats.currentHP -= ctrDmg;

                // LOG: counterattack result
                GameLogger.log(GameLogger.ATTACK, dStats.owner,
                        "Counter: " + dStats.name + GameLogger.pos(dPos.x, dPos.y)
                                + " hits " + aStats.name + GameLogger.pos(aPos.x, aPos.y)
                                + " | ctrDmg=" + ctrDmg
                                + " | atkHP=" + aStats.currentHP + "/" + aStats.maxHP);

                // Spawn floating text above the attacker (isCounter = true)
                spawnFloatingText(ctrDmg, aPos.x, aPos.y, true);

                if (aStats.currentHP <= 0) {
                    aStats.currentHP = 0;
                    GameLogger.log(GameLogger.ATTACK, dStats.owner,
                            aStats.name + " at " + GameLogger.pos(aPos.x, aPos.y) + " DESTROYED by counter");
                    flagDeath(attacker);
                }
            }
        }

        // --- 4. Exhaust attacker ---
        exhaustAttacker(attacker, aStats, false);
    }

    /**
     * Checks if any enemy "Ranger" with Overwatch active is in range of the moving
     * unit.
     */
    public void checkOverwatch(Entity movingUnit, int targetX, int targetY) {
        StatsComponent mStats = movingUnit.getComponent(StatsComponent.class);
        if (mStats == null)
            return;

        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class, AbilitiesComponent.class).get());

        for (Entity e : entities) {
            StatsComponent s = e.getComponent(StatsComponent.class);
            AbilitiesComponent a = e.getComponent(AbilitiesComponent.class);
            GridPositionComponent p = e.getComponent(GridPositionComponent.class);

            if (s.owner != mStats.owner && s.unitTypeKey.equals("RANGER") && a.isOverwatchActive) {
                int dist = chebyshev(p.x, p.y, targetX, targetY);
                if (dist <= s.attackRange) {
                    GameLogger.log(GameLogger.ABILITY, s.owner,
                            "Overwatch triggered: " + s.name + GameLogger.pos(p.x, p.y)
                                    + " fires at moving " + mStats.name + GameLogger.pos(targetX, targetY));
                    // Trigger overwatch attack
                    resolveAttack(e, movingUnit);
                    a.isOverwatchActive = false; // Limit 1 trigger per turn
                    break; // Only one overwatch can trigger per "move" for simplicity
                }
            }
        }
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
    public void flagDeath(Entity entity) {
        StatsComponent stats = entity.getComponent(StatsComponent.class);
        if (stats == null)
            return;

        // --- NEW: Track Base Destruction ---
        if (stats.name.contains("Base")) {
            if (stats.owner == 1) {
                gameState.p1BaseCount = Math.max(0, gameState.p1BaseCount - 1);
            } else if (stats.owner == 2) {
                gameState.p2BaseCount = Math.max(0, gameState.p2BaseCount - 1);
            }
            GridPositionComponent pos = entity.getComponent(GridPositionComponent.class);
            String posStr = (pos != null) ? GameLogger.pos(pos.x, pos.y) : "(?,?)";
            GameLogger.log(GameLogger.GAME_OVER, stats.owner,
                    "Base DESTROYED: " + stats.name + " at " + posStr
                            + " | P1 bases=" + gameState.p1BaseCount
                            + " P2 bases=" + gameState.p2BaseCount);
        }

        entity.add(new DeathAnimComponent());
    }

    private void triggerExplosion(int centerX, int centerY, int radius, int damage, String text) {
        spawnFloatingText(0, centerX, centerY, false); // Just for position
        ImmutableArray<Entity> victims = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class).get());

        for (Entity v : victims) {
            GridPositionComponent vPos = v.getComponent(GridPositionComponent.class);
            StatsComponent vStats = v.getComponent(StatsComponent.class);

            if (vPos == null || vStats == null)
                continue; // Skip if components are missing

            if (chebyshev(centerX, centerY, vPos.x, vPos.y) <= radius) {
                // OIL DERRICK & NUCLEAR PLANT: Indestructible
                if (vStats.name.contains("Oil Derrick") || vStats.name.contains("Nuclear Plant")) {
                    continue;
                }
                vStats.currentHP -= damage;
                spawnFloatingText(damage, vPos.x, vPos.y, false);
                if (vStats.currentHP <= 0) {
                    vStats.currentHP = 0;
                    // Note: Avoid recursive death flags if possible,
                    // but here it's okay because DeathAnimComponent prevents double processing.
                    if (v.getComponent(DeathAnimComponent.class) == null) {
                        flagDeath(v);
                    }
                }
            }
        }
    }

    /** Marks the attacker so they cannot act or move again this turn. */
    private void exhaustAttacker(Entity attacker, StatsComponent aStats, boolean targetDied) {
        aStats.hasActed = true;
        aStats.hasMoved = true;

        // TANK: Blitz (Move again if attack kills target)
        if (targetDied && aStats.unitTypeKey.equals("TANK")) {
            aStats.hasMoved = false;
        }

        // GUNBOAT: Skirmish (+1 move point after attacking)
        if (aStats.unitTypeKey.equals("GUNBOAT")) {
            aStats.hasMoved = false; // Allow moving 1 more space (effectively)
        }

        // SUICIDE DRONE: Kamikaze
        if (aStats.unitTypeKey.equals("SUICIDE_DRONE")) {
            flagDeath(attacker);
        }
    }

    private void resolveSuppressingFire(Entity attacker, GridPositionComponent aPos) {
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        ImmutableArray<Entity> entities = engine
                .getEntitiesFor(Family.all(GridPositionComponent.class, StatsComponent.class).get());

        for (Entity e : entities) {
            if (e == attacker)
                continue;
            GridPositionComponent p = e.getComponent(GridPositionComponent.class);
            StatsComponent s = e.getComponent(StatsComponent.class);

            if (s.owner != aStats.owner && chebyshev(aPos.x, aPos.y, p.x, p.y) <= 1) {
                // Skip if indestructible building
                if (s.name.contains("Oil Derrick") || s.name.contains("Nuclear Plant")) {
                    continue;
                }
                int defBonus = terrainDefBonus(p.x, p.y);
                AbilitiesComponent dAbilities = e.getComponent(AbilitiesComponent.class);
                int digInBonus = (dAbilities != null && dAbilities.isDiggingIn) ? 3 : 0;

                int dmg = Math.max(0, aStats.attack - (s.defense + digInBonus) - defBonus);
                s.currentHP -= dmg;
                spawnFloatingText(dmg, p.x, p.y, false);
                if (s.currentHP <= 0) {
                    s.currentHP = 0;
                    flagDeath(e);
                }
            }
        }
    }

    /**
     * Spawns a floating feedback entity above the given grid tile.
     * Shows "BLOCKED" when damage is zero, otherwise shows the number.
     */
    private void spawnFloatingText(int dmg, int gx, int gy, boolean isCounter) {
        if (entityFactory == null)
            return;
        String label = (dmg == 0) ? "BLOCKED" : String.valueOf(dmg);
        FloatingTextComponent.Type type = (dmg == 0) ? FloatingTextComponent.Type.BLOCKED
                : FloatingTextComponent.Type.DAMAGE;
        float worldX = EntityFactory.gridToIsoX(gx, gy);
        float worldY = EntityFactory.gridToIsoY(gx, gy);
        entityFactory.createFloatingText(label, worldX, worldY, type);
    }
}
