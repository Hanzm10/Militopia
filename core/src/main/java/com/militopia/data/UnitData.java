package com.militopia.data;

public class UnitData {
    public int x;
    public int y;
    public String type;
    public int owner;

    // Default constructor for JSON
    public UnitData() {}

    public UnitData(int x, int y, String type, int owner) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.owner = owner;
    }
}