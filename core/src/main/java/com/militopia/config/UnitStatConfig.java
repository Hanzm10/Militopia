package com.militopia.config;

import com.militopia.components.StatsComponent;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Single source of truth for base unit stats.
 * Eliminates the duplicate switches in UnitFactory (createUnit, getUnitCost, getUnitMoveType).
 */
public class UnitStatConfig {

    public static class UnitStatData {
        public final int hp;
        public final int atk;
        public final int def;
        public final int move;
        public final int rng;
        public final int vis;
        public final int cost;
        public final StatsComponent.MoveType moveType;

        public UnitStatData(int hp, int atk, int def, int move, int rng, int vis, int cost,
                StatsComponent.MoveType moveType) {
            this.hp = hp;
            this.atk = atk;
            this.def = def;
            this.move = move;
            this.rng = rng;
            this.vis = vis;
            this.cost = cost;
            this.moveType = moveType;
        }
    }

    private static final Map<UnitType, UnitStatData> STATS;

    static {
        Map<UnitType, UnitStatData> m = new EnumMap<>(UnitType.class);
        //                                    hp   atk  def  mov  rng  vis  cost  moveType
        m.put(UnitType.RECRUIT,       new UnitStatData(10,   3,   1,   1,   1,   1,   2,  StatsComponent.MoveType.LAND));
        m.put(UnitType.RANGER,        new UnitStatData(12,   5,   1,   1,   2,   2,   5,  StatsComponent.MoveType.LAND));
        m.put(UnitType.SNIPER,        new UnitStatData( 8,  15,   0,   1,   3,   3,   8,  StatsComponent.MoveType.LAND));
        m.put(UnitType.TANK,          new UnitStatData(30,  12,   5,   2,   3,   3,  15,  StatsComponent.MoveType.LAND));
        m.put(UnitType.JUGGERNAUT,    new UnitStatData(50,  12,   6,   4,   4,   3,   0,  StatsComponent.MoveType.LAND));

        m.put(UnitType.RECON_DRONE,   new UnitStatData( 5,   0,   0,   3,   0,   3,   4,  StatsComponent.MoveType.AIR));
        m.put(UnitType.SUICIDE_DRONE, new UnitStatData( 5,  20,   0,   2,   1,   2,   7,  StatsComponent.MoveType.AIR));
        m.put(UnitType.APACHE,        new UnitStatData(20,  15,   2,   3,   2,   3,  18,  StatsComponent.MoveType.AIR));
        m.put(UnitType.B2,            new UnitStatData(45,  18,   3,   3,   3,   3,   0,  StatsComponent.MoveType.AIR));

        m.put(UnitType.GUNBOAT,       new UnitStatData(10,   5,   2,   2,   2,   2,   6,  StatsComponent.MoveType.SEA));
        m.put(UnitType.DESTROYER,     new UnitStatData(30,  15,   3,   3,   3,   3,  13,  StatsComponent.MoveType.SEA));
        m.put(UnitType.CARRIER,       new UnitStatData(45,   5,   4,   3,   3,   3,  25,  StatsComponent.MoveType.SEA));
        m.put(UnitType.SUBMARINE,     new UnitStatData(40,  25,   3,   4,   4,   3,   0,  StatsComponent.MoveType.SEA));

        STATS = Collections.unmodifiableMap(m);
    }

    /**
     * Returns the base stats for the given unit type.
     * Falls back to RECRUIT stats if the type has no entry (should not happen).
     */
    public static UnitStatData get(UnitType unitType) {
        UnitStatData data = STATS.get(unitType);
        return data != null ? data : STATS.get(UnitType.RECRUIT);
    }
}
