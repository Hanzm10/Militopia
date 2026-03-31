package com.militopia.data;

/**
 * Snapshot of a structure (base or town) for undo purposes.
 * Identified by tile position — restored in-place (entity is never removed).
 */
public class StructureSnapshot {

    public int x, y;
    public int owner;
    public int level;
    public float currentBaseXP;
    public int income;
    public String name;
    public String baseOrdinal;
    public String chosenSuperUnit;
    public String unitTypeKey;
    public int xpGain;
    public int parentBaseX;
    public int parentBaseY;

    /** No-arg constructor for LibGDX Json serialization */
    public StructureSnapshot() {
        this.x = 0;
        this.y = 0;
        this.owner = 0;
        this.level = 1;
        this.currentBaseXP = 0f;
        this.income = 0;
        this.name = "";
        this.baseOrdinal = "";
        this.chosenSuperUnit = "";
        this.unitTypeKey = "";
        this.xpGain = 0;
        this.parentBaseX = -1;
        this.parentBaseY = -1;
    }

    public StructureSnapshot(int x, int y, int owner, int level,
            float currentBaseXP, int income,
            String name, String baseOrdinal, String chosenSuperUnit,
            String unitTypeKey, int xpGain, int parentBaseX, int parentBaseY) {
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.level = level;
        this.currentBaseXP = currentBaseXP;
        this.income = income;
        this.name = name;
        this.baseOrdinal = baseOrdinal;
        this.chosenSuperUnit = chosenSuperUnit;
        this.unitTypeKey = unitTypeKey;
        this.xpGain = xpGain;
        this.parentBaseX = parentBaseX;
        this.parentBaseY = parentBaseY;
    }
}
