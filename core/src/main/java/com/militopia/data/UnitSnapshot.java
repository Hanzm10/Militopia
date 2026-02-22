package com.militopia.data;

import com.militopia.components.StatsComponent;

/**
 * Immutable snapshot of a single unit's state at a point in time.
 * Used by TurnHistoryManager to restore units (including dead ones) on undo.
 */
public class UnitSnapshot {

    public final String unitTypeKey; // Factory key e.g. "RECRUIT", "TANK"
    public final int x, y;
    public final int owner;
    public final int currentHP;
    public final boolean hasActed;
    public final boolean hasMoved;
    public final StatsComponent.MoveType moveType;

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
