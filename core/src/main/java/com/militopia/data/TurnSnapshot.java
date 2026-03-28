package com.militopia.data;

import com.militopia.map.MapGenerator;

import java.util.List;

/**
 * Full state snapshot taken at the start of a turn.
 * Contains everything needed to rewind the game to that moment.
 */
public class TurnSnapshot {

    // --- GameState scalars ---
    public int p1Funding, p2Funding;
    public int p1XP, p2XP;
    public int turn;
    public int currentPlayer;
    public int p1BaseCount, p2BaseCount;

    // --- ECS entity state ---
    public List<UnitSnapshot> units;
    public List<StructureSnapshot> structures;

    // --- Map objects layer (captures / uncaptures) ---
    public MapGenerator.ObjectType[][] mapObjects; // cloned 2D array

    /** No-arg constructor for LibGDX Json serialization */
    public TurnSnapshot() {
        this.p1Funding = 0;
        this.p2Funding = 0;
        this.p1XP = 0;
        this.p2XP = 0;
        this.turn = 1;
        this.currentPlayer = 1;
        this.p1BaseCount = 0;
        this.p2BaseCount = 0;
        this.units = null;
        this.structures = null;
        this.mapObjects = null;
    }

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
