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
    public ArrayList<UnitData> units = new ArrayList<>();
    
    // Turn Tracking
    public int currentPlayer = 1;
    public int turnCount = 1;
    
    // XP System (Global)
    public int p1XP = 500;
    public int p2XP = 500;

    // Funding System
    public int p1Funding = 5;
    public int p2Funding = 5;

    // --- Base Counters ---
    public int p1BaseCount = 0;
    public int p2BaseCount = 0;
    
    public GameState() {} 
    
    public GameState(long seed, String p1, String p2, String saveName) {
        this.seed = seed;
        this.p1Name = p1;
        this.p2Name = p2;
        this.saveName = saveName;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        this.timestamp = LocalDateTime.now().format(formatter);
    }
}