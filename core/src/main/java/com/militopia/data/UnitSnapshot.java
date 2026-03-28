package com.militopia.data;

import com.militopia.components.StatsComponent;

/**
 * Immutable snapshot of a single unit's state at a point in time.
 * Used by TurnHistoryManager to restore units (including dead ones) on undo.
 */
public class UnitSnapshot {

    public String unitTypeKey; // Factory key e.g. "RECRUIT", "TANK"
    public int x, y;
    public int owner;
    public int currentHP;
    public boolean hasActed;
    public boolean hasMoved;
    public StatsComponent.MoveType moveType;

    /** No-arg constructor for LibGDX Json serialization */
    public UnitSnapshot() {
        this.unitTypeKey = "";
        this.x = 0;
        this.y = 0;
        this.owner = 0;
        this.currentHP = 0;
        this.hasActed = false;
        this.hasMoved = false;
        this.moveType = null;
    }

    public UnitSnapshot(String unitTypeKey, int x, int y, int owner,
            int currentHP, boolean hasActed, boolean hasMoved,
            StatsComponent.MoveType moveType) {
        this.unitTypeKey = unitTypeKey;
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.currentHP = currentHP;
        this.hasActed = hasActed;
        this.hasMoved = hasMoved;
        this.moveType = moveType;
    }
}
