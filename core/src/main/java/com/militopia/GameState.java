package com.militopia;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class GameState {
    public long seed;
    public String p1Name;
    public String p2Name;
    public String saveName; // The filename
    public String timestamp; // e.g., "2023-10-27 10:00"
    
    // Later you will add unit positions here like:
    // public ArrayList<UnitData> units;
    
    // Empty constructor is required for JSON deserialization
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