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
import com.militopia.components.JumpLandingComponent;
import com.militopia.factories.EntityFactory;
import com.militopia.managers.AudioManager;
import com.militopia.map.MapGenerator;
import com.militopia.config.CombatConstants;
import com.militopia.config.UnitType;
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

    @Override
    public void update(float deltaTime) {
        com.badlogic.gdx.utils.Array<Entity> toProcess = new com.badlogic.gdx.utils.Array<>();
        ImmutableArray<Entity> candidates = engine.getEntitiesFor(
                Family.all(JumpLandingComponent.class, GridPositionComponent.class, StatsComponent.class).get());
        for (Entity e : candidates) {
            if (e.getComponent(JumpLandingComponent.class).landed) toProcess.add(e);
        }
        for (Entity e : toProcess) {
            JumpLandingComponent jlc = e.getComponent(JumpLandingComponent.class);
            processJumpLanding(e, jlc);
            e.remove(JumpLandingComponent.class);
        }
    }

    /**
     * Sets up the Juggernaut jump attack animation and deferred landing.
     * Grid position is moved immediately; visual offset animates the leap.
     *
     * @param attacker     The Juggernaut entity.
     * @param primaryTarget The unit at the target tile, or null for an empty tile jump.
     * @param targetX      Landing tile X.
     * @param targetY      Landing tile Y.
     */
    public void resolveJumperAttack(Entity attacker, Entity primaryTarget, int targetX, int targetY) {
        GridPositionComponent aPos = attacker.getComponent(GridPositionComponent.class);
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        if (aPos == null || aStats == null) return;

        // Compute world-space delta BEFORE moving the grid position
        float dIsoX = (targetX - aPos.x - (targetY - aPos.y))
                * (com.militopia.config.GameConfig.TILE_WIDTH / 2.0f);
        float dIsoY = (targetX - aPos.x + targetY - aPos.y)
                * (com.militopia.config.GameConfig.TILE_HEIGHT / 2.0f);

        // Move grid position to landing tile immediately
        aPos.x = targetX;
        aPos.y = targetY;

        // Set up JUMP animation (visual starts at source, arrives at destination)
        AnimationComponent anim = attacker.getComponent(AnimationComponent.class);
        if (anim == null) {
            anim = new AnimationComponent();
            attacker.add(anim);
        }
        anim.type = AnimationComponent.Type.JUMP;
        anim.jumpStartOffX = -dIsoX;
        anim.jumpStartOffY = -dIsoY;
        anim.arcHeight = CombatConstants.JUMP_ARC_HEIGHT;
        anim.duration = CombatConstants.JUMP_DURATION;
        anim.stateTime = 0;

        // Attach landing payload
        JumpLandingComponent jlc = new JumpLandingComponent();
        jlc.primaryTarget = primaryTarget;
        attacker.add(jlc);

        String targetDesc = (primaryTarget != null)
                ? primaryTarget.getComponent(StatsComponent.class).name
                : "empty tile";
        GameLogger.log(GameLogger.ABILITY, aStats.owner,
                "Juggernaut JUMP: " + aStats.name + " → " + GameLogger.pos(targetX, targetY)
                        + " | target=" + targetDesc);

        exhaustAttacker(attacker, aStats, primaryTarget != null);
    }

    public void launchNuke(Entity attacker, int tx, int ty) {
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        AbilitiesComponent aAbilities = attacker.getComponent(AbilitiesComponent.class);
        if (aStats == null || aAbilities == null)
            return;

        GameLogger.log(GameLogger.ABILITY, aStats.owner,
                "Nuke detonates at " + GameLogger.pos(tx, ty)
                        + " | radius=" + CombatConstants.NUKE_RADIUS
                        + " | dmg=" + CombatConstants.NUKE_DAMAGE);
        triggerExplosion(tx, ty, CombatConstants.NUKE_RADIUS, CombatConstants.NUKE_DAMAGE, "NUKE");

        aStats.hasActed = true;
        aStats.hasMoved = true;
        aAbilities.nukeCooldown = CombatConstants.NUKE_COOLDOWN_TURNS;
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

        // JUGGERNAUT: Jump Attack (routed from resolveAttack as a fallback)
        if (aStats.unitType == UnitType.JUGGERNAUT) {
            resolveJumperAttack(attacker, defender, dPos.x, dPos.y);
            return;
        }

        // RECON DRONE: High Altitude (Immune to Range-1 land attacks)
        if (dStats.unitType == UnitType.RECON_DRONE && aStats.moveType == StatsComponent.MoveType.LAND
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

        // DESTROYER: Shore Bombardment
        int shoreBonus = (aStats.unitType == UnitType.DESTROYER && dStats.moveType == StatsComponent.MoveType.LAND)
                ? CombatConstants.SHORE_BOMBARDMENT_BONUS : 0;

        AbilitiesComponent dAbilities = defender.getComponent(AbilitiesComponent.class);
        int digInBonus = (dAbilities != null && dAbilities.isDiggingIn) ? CombatConstants.DIG_IN_DEFENSE_BONUS : 0;

        int dmg = Math.max(0, (aStats.attack + shoreBonus) - (dStats.defense + digInBonus) - defTerrainBonus
                - (maxRange && aStats.attackRange > 1 ? CombatConstants.MAX_RANGE_ATTACK_PENALTY : 0));

        boolean isKill = (dmg >= dStats.currentHP);
        boolean isRecruitMelee = aStats.unitType == UnitType.RECRUIT && aStats.attackRange <= 1;

        if (!(isKill && isRecruitMelee)) {
            triggerAttackAnimation(attacker, defender);
        }

        // Custom attack sprites (Enemy only)
        UnitType unitType = aStats.unitType;
        if (dStats.owner != aStats.owner && dStats.owner != 0) {
            if (unitType == UnitType.RECRUIT && aStats.attackRange <= 1) {
                entityFactory.createRecruitAttack(dPos.x, dPos.y);
            } else if (unitType == UnitType.TANK || unitType == UnitType.SUICIDE_DRONE) {
                entityFactory.createTankAttack(dPos.x, dPos.y);
            } else if (unitType == UnitType.JUGGERNAUT || unitType == UnitType.B2
                    || unitType == UnitType.SUBMARINE) {
                entityFactory.createNuclearAttack(dPos.x, dPos.y);
            }
        }

        // --- NEW: Muzzle Flash for ranged units ---
        if (aStats.attackRange > 1) {
            FacingComponent facing = attacker.getComponent(FacingComponent.class);
            TextureComponent tex = attacker.getComponent(TextureComponent.class);
            float muzzleOffsetX = CombatConstants.MUZZLE_OFFSET_X_RIGHT;
            float muzzleOffsetY = CombatConstants.MUZZLE_OFFSET_Y;
            if (facing != null && tex != null && tex.region == facing.leftRegion) {
                muzzleOffsetX = CombatConstants.MUZZLE_OFFSET_X_LEFT;
            }
            entityFactory.createMuzzleFlash(aPos.x, aPos.y, muzzleOffsetX, muzzleOffsetY);
        }

        // Play Attack SFX
        if (unitType == UnitType.JUGGERNAUT || unitType == UnitType.B2
                || unitType == UnitType.SUBMARINE) {
            AudioManager.getInstance().playSFX("explode.wav");
        } else if (unitType == UnitType.RANGER) {
            boolean isManTarget = dStats.moveType == StatsComponent.MoveType.LAND
                    && dStats.unitType != UnitType.JUGGERNAUT
                    && dStats.unitType != UnitType.TANK;
            if (!(isKill && isManTarget)) {
                AudioManager.getInstance().playSFX("ranger-ak47.WAV");
            }
        } else if (unitType == UnitType.RECRUIT) {
            AudioManager.getInstance().playSFX("recruit-knife.WAV");
        } else if (unitType == UnitType.SNIPER) {
            AudioManager.getInstance().playSFX("sniper-awp.WAV");
        } else if (unitType == UnitType.TANK) {
            AudioManager.getInstance().playSFX("tank-fire.WAV");
        } else {
            if (aStats.attackRange > 1) {
                AudioManager.getInstance().playSFX("attack_ranged.wav");
            } else {
                AudioManager.getInstance().playSFX("attack_melee.wav");
            }
        }

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

            entityFactory.createExplosion(dPos.x, dPos.y);

            // Melee Auto-Advance
            if (aStats.attackRange <= 1) {
                if (isRecruitMelee) {
                    attacker.add(new MovementComponent(aPos.x, aPos.y, dPos.x, dPos.y));
                }
                aPos.x = dPos.x;
                aPos.y = dPos.y;
            }

            flagDeath(defender);

            // Attacker-first resolution: no counter if defender is dead
            exhaustAttacker(attacker, aStats, true);
            return;
        } else {
            if (dmg == 0) {
                UnitType defType = dStats.unitType;
                if (defType == UnitType.RECRUIT || defType == UnitType.RANGER
                        || defType == UnitType.SNIPER) {
                    AudioManager.getInstance().playSFX("man-blocked.WAV");
                } else {
                    AudioManager.getInstance().playSFX("machine-blocked.WAV");
                }
            }
            entityFactory.createHit(dPos.x, dPos.y);
        }

        // --- 3. Counterattack (range-gated) ---
        // RECON DRONE: High Altitude (Immune to Range-1 Land attacks)
        boolean isHighAltitude = aStats.unitType == UnitType.RECON_DRONE;
        boolean isLandAttacker = dStats.moveType == StatsComponent.MoveType.LAND;

        int counterDist = chebyshev(dPos.x, dPos.y, aPos.x, aPos.y);
        if (counterDist <= dStats.attackRange) {
            if (isHighAltitude && isLandAttacker && dStats.attackRange == 1) {
                spawnFloatingText(0, aPos.x, aPos.y, true); // Visual feedback for immunity
            } else {
                boolean counterMaxRange = (counterDist == dStats.attackRange);
                int atkTerrainBonus = terrainDefBonus(aPos.x, aPos.y);

                int ctrDmg = Math.max(0, dStats.attack - aStats.defense - atkTerrainBonus
                        - (counterMaxRange && dStats.attackRange > 1 ? CombatConstants.MAX_RANGE_ATTACK_PENALTY : 0));
                aStats.currentHP -= ctrDmg;

                // LOG: counterattack result
                GameLogger.log(GameLogger.ATTACK, dStats.owner,
                        "Counter: " + dStats.name + GameLogger.pos(dPos.x, dPos.y)
                                + " hits " + aStats.name + GameLogger.pos(aPos.x, aPos.y)
                                + " | ctrDmg=" + ctrDmg
                                + " | atkHP=" + aStats.currentHP + "/" + aStats.maxHP);

                // Spawn floating text above the attacker (isCounter = true)
                spawnFloatingText(ctrDmg, aPos.x, aPos.y, true);

                // Defend SFX — only when counterattack is cancelled out
                if (ctrDmg == 0) {
                    UnitType defenderType = dStats.unitType;
                    if (defenderType == UnitType.RECRUIT || defenderType == UnitType.RANGER
                            || defenderType == UnitType.SNIPER) {
                        AudioManager.getInstance().playSFX("man-blocked.WAV");
                    } else {
                        AudioManager.getInstance().playSFX("machine-blocked.WAV");
                    }
                }

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

            if (s.owner != mStats.owner && s.unitType == UnitType.RANGER && a.isOverwatchActive) {
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
            return CombatConstants.TERRAIN_BONUS_MOUNTAIN;
        }
        if (obj == MapGenerator.ObjectType.TREE) {
            return CombatConstants.TERRAIN_BONUS_TREE;
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

        // Play Death SFX
        if (stats.moveType == StatsComponent.MoveType.LAND &&
                stats.unitType != UnitType.JUGGERNAUT &&
                stats.unitType != UnitType.TANK) {
            AudioManager.getInstance().playSFX("man-finished.WAV");
        } else {
            AudioManager.getInstance().playSFX("machine-finished.WAV");
            AudioManager.getInstance().playSFX("explode.wav");
        }

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
        if (targetDied && aStats.unitType == UnitType.TANK) {
            aStats.hasMoved = false;
        }

        // GUNBOAT: Skirmish (Move 1 tile after attacking)
        if (aStats.unitType == UnitType.GUNBOAT) {
            AbilitiesComponent aAbilities = attacker.getComponent(AbilitiesComponent.class);
            if (aAbilities != null) {
                aStats.hasMoved = false;
                aAbilities.pendingSkirmishMove = true;
            }
        }

        // SUICIDE DRONE: Kamikaze
        if (aStats.unitType == UnitType.SUICIDE_DRONE) {
            flagDeath(attacker);
        }
    }

    private void processJumpLanding(Entity jumper, JumpLandingComponent jlc) {
        GridPositionComponent landPos = jumper.getComponent(GridPositionComponent.class);
        StatsComponent aStats = jumper.getComponent(StatsComponent.class);
        if (landPos == null || aStats == null) return;

        // 1. Instantly kill primary target
        if (jlc.primaryTarget != null) {
            StatsComponent tStats = jlc.primaryTarget.getComponent(StatsComponent.class);
            if (tStats != null && tStats.currentHP > 0) {
                tStats.currentHP = 0;
                entityFactory.createExplosion(landPos.x, landPos.y);
                flagDeath(jlc.primaryTarget);
            }
        }

        // 2. AoE damage to adjacent enemies on landing
        resolveJumpLandingAoE(jumper, landPos.x, landPos.y, jlc.primaryTarget);

        // 3. Nuclear sprite exactly on landing
        entityFactory.createNuclearAttack(landPos.x, landPos.y);

        // 4. SFX on landing
        AudioManager.getInstance().playSFX("explode.wav");

        GameLogger.log(GameLogger.ABILITY, aStats.owner,
                "Juggernaut LANDED at " + GameLogger.pos(landPos.x, landPos.y));
    }

    private void resolveJumpLandingAoE(Entity jumper, int cx, int cy, Entity skipTarget) {
        StatsComponent aStats = jumper.getComponent(StatsComponent.class);
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, StatsComponent.class).get());

        for (Entity e : entities) {
            if (e == jumper || e == skipTarget) continue;
            GridPositionComponent p = e.getComponent(GridPositionComponent.class);
            StatsComponent s = e.getComponent(StatsComponent.class);
            if (p == null || s == null) continue;
            if (s.owner == aStats.owner) continue;
            if (chebyshev(cx, cy, p.x, p.y) > 1) continue;
            if (s.name.contains("Oil Derrick") || s.name.contains("Nuclear Plant")) continue;

            int defBonus = terrainDefBonus(p.x, p.y);
            AbilitiesComponent dAbilities = e.getComponent(AbilitiesComponent.class);
            int digInBonus = (dAbilities != null && dAbilities.isDiggingIn) ? CombatConstants.DIG_IN_DEFENSE_BONUS : 0;
            int dmg = Math.max(0, aStats.attack - (s.defense + digInBonus) - defBonus);
            s.currentHP -= dmg;
            spawnFloatingText(dmg, p.x, p.y, false);
            if (s.currentHP <= 0) {
                s.currentHP = 0;
                if (e.getComponent(DeathAnimComponent.class) == null) {
                    flagDeath(e);
                }
            }
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
                int digInBonus = (dAbilities != null && dAbilities.isDiggingIn) ? CombatConstants.DIG_IN_DEFENSE_BONUS : 0;

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

    private void triggerAttackAnimation(Entity attacker, Entity defender) {
        AnimationComponent anim = attacker.getComponent(AnimationComponent.class);
        GridPositionComponent aPos = attacker.getComponent(GridPositionComponent.class);
        GridPositionComponent dPos = defender.getComponent(GridPositionComponent.class);
        StatsComponent s = attacker.getComponent(StatsComponent.class);

        if (anim == null || aPos == null || dPos == null)
            return;

        anim.startX = 0;
        anim.startY = 0;

        // Calculate relative vector in pixels (isometric)
        float dx = (dPos.x - aPos.x - (dPos.y - aPos.y)) * (com.militopia.config.GameConfig.TILE_WIDTH / 4.0f);
        float dy = (dPos.x - aPos.x + (dPos.y - aPos.y)) * (com.militopia.config.GameConfig.TILE_HEIGHT / 4.0f);

        if (s.attackRange <= 1) {
            anim.type = AnimationComponent.Type.LUNGE;
            anim.targetX = dx * CombatConstants.LUNGE_MELEE_DISTANCE;
            anim.targetY = dy * CombatConstants.LUNGE_MELEE_DISTANCE;
            anim.duration = CombatConstants.LUNGE_MELEE_DURATION;
        } else {
            anim.type = AnimationComponent.Type.LUNGE; // Simple pop for ranged
            anim.targetX = dx * CombatConstants.LUNGE_RANGED_RECOIL;
            anim.targetY = dy * CombatConstants.LUNGE_RANGED_RECOIL;
            anim.duration = CombatConstants.LUNGE_RANGED_DURATION;
        }
        anim.stateTime = 0;
    }
}
