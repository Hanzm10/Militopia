package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.ui.GameHUD;
import com.militopia.utils.GameLogger;
import java.util.ArrayList;
import java.util.List;

/**
 * StructureEconomySystem — Ashley ECS system responsible for all per-turn
 * structure economic processing. This decouples the economy loop from
 * GameScreen so it can be unit-tested and extended independently.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Distribute XP from specialized structures to their linked parent
 * base</li>
 * <li>Apply Hospital healing (+3 HP) to adjacent friendly units</li>
 * <li>Apply natural base XP growth per turn</li>
 * <li>Trigger base level-up checks via UnitFactory</li>
 * </ul>
 *
 * <p>
 * Income calculation remains in
 * {@link com.militopia.screen.GameScreen#calculateIncome}
 * because it is also queried mid-turn (e.g. for HUD display after capturing a
 * structure).
 * XP-side processing is fully owned here.
 *
 * <p>
 * Called explicitly by GameScreen at the start of each player's turn (after
 * the fade-out transition) via {@link #processTurn(int)}.
 */
public class StructureEconomySystem extends EntitySystem {

    private final GameState gameState;
    private final UnitFactory unitFactory;
    private GameHUD gameHUD;

    public StructureEconomySystem(GameState gameState, UnitFactory unitFactory, GameHUD gameHUD) {
        // Priority 0 — runs last in the engine update chain (we call it manually)
        super(0);
        this.gameState = gameState;
        this.unitFactory = unitFactory;
        this.gameHUD = gameHUD;
    }

    /**
     * Called after GameHUD is constructed, since HUD is created after the engine
     * systems.
     */
    public void setGameHUD(GameHUD gameHUD) {
        this.gameHUD = gameHUD;
    }

    /**
     * Main entry point. Called once per turn start for the player whose turn began.
     *
     * @param playerID the player whose structures should generate economy (1 or 2)
     * @return total XP gained this turn processing (for logging / HUD)
     */
    public int processTurn(int playerID) {
        if (gameState.turnCount <= 1) {
            // No XP or structure bonuses on Turn 1 (first round setup)
            return 0;
        }

        List<Entity> myBases = new ArrayList<>();
        List<Entity> xpStructures = new ArrayList<>();

        ImmutableArray<Entity> entities = getEngine().getEntitiesFor(
                Family.all(StatsComponent.class).get());

        for (Entity entity : entities) {
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            if (stats.owner != playerID)
                continue;

            if (stats.income >= 2 && stats.name.contains("Base")) {
                myBases.add(entity);
            } else if (stats.xpGain > 0 && stats.parentBaseX != -1
                    && entity.getComponent(GridPositionComponent.class) != null) {
                xpStructures.add(entity);
            }
        }

        int totalXPGain = 0;

        // 1. Structure XP → parent base
        for (Entity struct : xpStructures) {
            StatsComponent structStats = struct.getComponent(StatsComponent.class);
            Entity parentBase = findEntityAt(structStats.parentBaseX, structStats.parentBaseY);
            if (parentBase != null) {
                StatsComponent baseStats = parentBase.getComponent(StatsComponent.class);
                if (baseStats.owner == playerID) {
                    baseStats.currentBaseXP += structStats.xpGain;
                    totalXPGain += structStats.xpGain;
                    GameLogger.log(GameLogger.ECONOMY, playerID,
                            "Structure XP: " + structStats.name
                                    + " → " + baseStats.name
                                    + " +" + structStats.xpGain + " XP");
                }
            }
        }

        // 2. Hospital: heal adjacent friendly units by +3 HP
        ImmutableArray<Entity> allUnits = getEngine().getEntitiesFor(
                Family.all(StatsComponent.class, GridPositionComponent.class, TypeComponent.class).get());
        for (Entity struct : xpStructures) {
            StatsComponent sStats = struct.getComponent(StatsComponent.class);
            if (!sStats.name.equals("Field Hospital"))
                continue;
            GridPositionComponent sPos = struct.getComponent(GridPositionComponent.class);
            for (Entity unit : allUnits) {
                TypeComponent tc = unit.getComponent(TypeComponent.class);
                if (tc.type != TypeComponent.Type.UNIT)
                    continue;
                StatsComponent uStats = unit.getComponent(StatsComponent.class);
                GridPositionComponent uPos = unit.getComponent(GridPositionComponent.class);
                if (uStats.owner == playerID
                        && Math.max(Math.abs(sPos.x - uPos.x), Math.abs(sPos.y - uPos.y)) <= 1) {
                    int healed = Math.min(uStats.currentHP + 3, uStats.maxHP) - uStats.currentHP;
                    uStats.currentHP += healed;
                    if (healed > 0) {
                        GameLogger.log(GameLogger.ECONOMY, playerID,
                                "Hospital heal: " + uStats.name
                                        + " at " + GameLogger.pos(uPos.x, uPos.y)
                                        + " +" + healed + " HP");
                    }
                }
            }
        }

        // 3. Base natural XP growth + level-up check
        for (Entity base : myBases) {
            StatsComponent stats = base.getComponent(StatsComponent.class);
            // Dynamic gain: 250 + (level-1)*10 per turn
            int naturalGain = 250 + ((stats.level - 1) * 10);
            stats.currentBaseXP += naturalGain;
            totalXPGain += naturalGain;
            unitFactory.checkAndApplyLevelUp(base, gameState, gameHUD);
        }

        // 4. Update global player XP
        if (playerID == 1) {
            gameState.p1XP += totalXPGain;
        } else {
            gameState.p2XP += totalXPGain;
        }

        return totalXPGain;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Entity findEntityAt(int x, int y) {
        ImmutableArray<Entity> entities = getEngine().getEntitiesFor(
                Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);
            if (pos.x == x && pos.y == y && type.type == TypeComponent.Type.OBJECT) {
                return e;
            }
        }
        return null;
    }
}
