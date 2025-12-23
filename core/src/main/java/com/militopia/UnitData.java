package com.militopia;

public class UnitData {
    public int x, y;
    public String type; // e.g., "RECRUIT"

    public UnitData() {} // Required for JSON

    public UnitData(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}