package com.militopia.data;

/**
 * Snapshot of a structure (base or town) for undo purposes.
 * Identified by tile position — restored in-place (entity is never removed).
 */
public class StructureSnapshot {

    public final int x, y;
    public final int owner;
    public final int level;
    public final float currentBaseXP;
    public final int income;
    public final String name;
    public final String baseOrdinal;

    public StructureSnapshot(int x, int y, int owner, int level,
            float currentBaseXP, int income,
            String name, String baseOrdinal) {
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.level = level;
        this.currentBaseXP = currentBaseXP;
        this.income = income;
        this.name = name;
        this.baseOrdinal = baseOrdinal;
    }
}
