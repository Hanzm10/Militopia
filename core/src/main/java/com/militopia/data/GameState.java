package com.militopia.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class GameState {

    public long seed;
    public String p1Name;
    public String p2Name;
    public String saveName;
    public String timestamp;

    // --- NEW: Map Dimensions ---
    public int mapWidth;
    public int mapHeight;

    // --- SEPARATE LISTS ---
    public ArrayList<UnitData> units = new ArrayList<>();
    public ArrayList<StructureData> structures = new ArrayList<>();
    public ArrayList<AnimalData> animals = new ArrayList<>();

    public int currentPlayer = 1;
    public int turnCount = 1;
    public int p1XP = 500;
    public int p2XP = 500;
    public int p1Funding = 5;
    public int p2Funding = 5;

    // Base Counters
    public int p1BaseCount = 0;
    public int p2BaseCount = 0;

    public GameState() {
    }

    // --- UPDATED CONSTRUCTOR ---
    public GameState(long seed, String saveName, int width, int height) {
        this.seed = seed;
        this.saveName = saveName;
        this.mapWidth = width;
        this.mapHeight = height;

        // Default Names
        this.p1Name = "P1";
        this.p2Name = "P2";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        this.timestamp = LocalDateTime.now().format(formatter);
    }
}
