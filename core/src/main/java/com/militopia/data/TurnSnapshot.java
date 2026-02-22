package com.militopia.data;

import com.militopia.map.MapGenerator;

import java.util.List;

/**
 * Full state snapshot taken at the start of a turn.
 * Contains everything needed to rewind the game to that moment.
 */
public class TurnSnapshot {

    // --- GameState scalars ---
    public final int p1Funding, p2Funding;
    public final int p1XP, p2XP;
    public final int turn;
    public final int currentPlayer;
    public final int p1BaseCount, p2BaseCount;

    // --- ECS entity state ---
    public final List<UnitSnapshot> units;
    public final List<StructureSnapshot> structures;

    // --- Map objects layer (captures / uncaptures) ---
    public final MapGenerator.ObjectType[][] mapObjects; // cloned 2D array

    public TurnSnapshot(int p1Funding, int p2Funding,
            int p1XP, int p2XP,
            int turn, int currentPlayer,
            int p1BaseCount, int p2BaseCount,
            List<UnitSnapshot> units,
            List<StructureSnapshot> structures,
            MapGenerator.ObjectType[][] mapObjects) {
        this.p1Funding = p1Funding;
        this.p2Funding = p2Funding;
        this.p1XP = p1XP;
        this.p2XP = p2XP;
        this.turn = turn;
        this.currentPlayer = currentPlayer;
        this.p1BaseCount = p1BaseCount;
        this.p2BaseCount = p2BaseCount;
        this.units = units;
        this.structures = structures;
        this.mapObjects = mapObjects;
    }
}
