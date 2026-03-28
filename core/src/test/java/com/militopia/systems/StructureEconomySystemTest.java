package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.StructureType;
import com.militopia.data.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StructureEconomySystemTest {

    private PooledEngine engine;
    private StructureEconomySystem economySystem;
    private GameState gameState;

    @BeforeEach
    public void setup() {
        engine = new PooledEngine();
        gameState = new GameState(1L, "test", 10, 10);
        // null factories are safe for income/XP query methods (they only call getEngine())
        economySystem = new StructureEconomySystem(gameState, null, null, null);
        engine.addSystem(economySystem);
    }

    /** Creates a structure entity with income and adds it to the engine. */
    private Entity createStructure(String name, int x, int y, int owner, int income) {
        Entity e = engine.createEntity();
        e.add(new GridPositionComponent(x, y, 2));
        e.add(new TypeComponent(TypeComponent.Type.OBJECT));
        StatsComponent stats = new StatsComponent(name, 30, 0, 5, 0, 0, 0, 0,
                StatsComponent.MoveType.LAND, owner, income);
        e.add(stats);
        engine.addEntity(e);
        return e;
    }

    // -------------------------------------------------------------------------
    // calculateIncome
    // -------------------------------------------------------------------------

    @Test
    public void testCalculateIncomeSumsAllOwnedEntities() {
        createStructure("Base", 0, 0, 1, 5);
        createStructure("Town", 1, 0, 1, 3);
        createStructure("Base", 2, 0, 2, 7);

        assertEquals(8, economySystem.calculateIncome(1));
        assertEquals(7, economySystem.calculateIncome(2));
    }

    @Test
    public void testCalculateIncomeIgnoresEnemyEntities() {
        createStructure("Base", 0, 0, 2, 5);
        assertEquals(0, economySystem.calculateIncome(1));
    }

    @Test
    public void testCalculateIncomeZeroWhenNoEntities() {
        assertEquals(0, economySystem.calculateIncome(1));
    }

    // -------------------------------------------------------------------------
    // calculateBaseIncome — Solar Array adjacency bonus
    // -------------------------------------------------------------------------

    @Test
    public void testNonSolarIncomeIsJustBaseValue() {
        Entity base = createStructure(StructureType.BASE.getDisplayName(), 0, 0, 1, 5);
        assertEquals(5, economySystem.calculateBaseIncome(base));
    }

    @Test
    public void testSolarArrayNoAdjacentBonus() {
        Entity solar = createStructure(StructureType.SOLAR.getDisplayName(), 0, 0, 1, 2);
        assertEquals(2, economySystem.calculateBaseIncome(solar));
    }

    @Test
    public void testSolarArrayGetsAdjacencyBonusFromFriendlyBase() {
        // Solar at (1,0), Base at (0,0) — chebyshev dist = 1
        Entity solar = createStructure(StructureType.SOLAR.getDisplayName(), 1, 0, 1, 2);
        createStructure(StructureType.BASE.getDisplayName(), 0, 0, 1, 5);

        // base income 2 + 1 adjacency bonus = 3
        assertEquals(3, economySystem.calculateBaseIncome(solar));
    }

    @Test
    public void testSolarArrayDoesNotCountEnemyAdjacent() {
        Entity solar = createStructure(StructureType.SOLAR.getDisplayName(), 1, 0, 1, 2);
        createStructure(StructureType.BASE.getDisplayName(), 0, 0, 2, 5); // enemy owner
        assertEquals(2, economySystem.calculateBaseIncome(solar));
    }

    @Test
    public void testSolarArrayDoesNotCountFarStructures() {
        // distance = 3 — outside chebyshev-1 range
        Entity solar = createStructure(StructureType.SOLAR.getDisplayName(), 0, 0, 1, 2);
        createStructure(StructureType.BASE.getDisplayName(), 3, 0, 1, 5);
        assertEquals(2, economySystem.calculateBaseIncome(solar));
    }

    // -------------------------------------------------------------------------
    // calculateGroupedBaseIncome
    // -------------------------------------------------------------------------

    @Test
    public void testGroupedIncomeIncludesLinkedChildren() {
        Entity base = createStructure(StructureType.BASE.getDisplayName(), 0, 0, 1, 5);
        Entity child = createStructure("Oil Derrick", 4, 4, 1, 3);
        child.getComponent(StatsComponent.class).parentBaseX = 0;
        child.getComponent(StatsComponent.class).parentBaseY = 0;

        assertEquals(8, economySystem.calculateGroupedBaseIncome(base));
    }

    @Test
    public void testGroupedIncomeIgnoresUnlinkedStructures() {
        Entity base = createStructure(StructureType.BASE.getDisplayName(), 0, 0, 1, 5);
        createStructure("Oil Derrick", 4, 4, 1, 3); // parentBaseX stays -1

        assertEquals(5, economySystem.calculateGroupedBaseIncome(base));
    }

    @Test
    public void testGroupedIncomeIgnoresEnemyLinkedStructures() {
        Entity base = createStructure(StructureType.BASE.getDisplayName(), 0, 0, 1, 5);
        Entity enemyChild = createStructure("Oil Derrick", 4, 4, 2, 3);
        // enemy child points at (0,0) but is not owned by player 1
        enemyChild.getComponent(StatsComponent.class).parentBaseX = 0;
        enemyChild.getComponent(StatsComponent.class).parentBaseY = 0;

        // grouped only counts if oStats.owner == stats.owner
        assertEquals(5, economySystem.calculateGroupedBaseIncome(base));
    }

    // -------------------------------------------------------------------------
    // calculateBaseXPGain
    // -------------------------------------------------------------------------

    @Test
    public void testBaseXPGainLevel1Is250() {
        Entity base = createStructure(StructureType.BASE.getDisplayName(), 0, 0, 1, 5);
        // level=1 by default: 250 + (1-1)*10 = 250
        assertEquals(250, economySystem.calculateBaseXPGain(base));
    }

    @Test
    public void testBaseXPGainScalesWithLevel() {
        Entity base = createStructure(StructureType.BASE.getDisplayName(), 0, 0, 1, 5);
        base.getComponent(StatsComponent.class).level = 3;
        // 250 + (3-1)*10 = 270
        assertEquals(270, economySystem.calculateBaseXPGain(base));
    }

    @Test
    public void testBaseXPGainIncludesLinkedStructureXP() {
        Entity base = createStructure(StructureType.BASE.getDisplayName(), 0, 0, 1, 5);
        Entity child = createStructure("Radar Station", 4, 4, 1, 0);
        StatsComponent childStats = child.getComponent(StatsComponent.class);
        childStats.parentBaseX = 0;
        childStats.parentBaseY = 0;
        childStats.xpGain = 100;

        // 250 (natural) + 100 (linked structure) = 350
        assertEquals(350, economySystem.calculateBaseXPGain(base));
    }

    // -------------------------------------------------------------------------
    // processTurn — Turn 1 guard
    // -------------------------------------------------------------------------

    @Test
    public void testProcessTurnSkipsOnTurnOne() {
        gameState.turnCount = 1;
        // Short-circuits before touching factories — safe to call with nulls
        assertEquals(0, economySystem.processTurn(1));
    }
}
