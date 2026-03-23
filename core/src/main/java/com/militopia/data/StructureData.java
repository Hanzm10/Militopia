package com.militopia.data;

public class StructureData {
    public int x;
    public int y;
    public int owner;
    public int level;
    public float currentBaseXP;
    public String baseName;
    public String baseOrdinal;

    // Default constructor for JSON
    public StructureData() {
    }

    public StructureData(int x, int y, int owner, int level, float currentBaseXP, String baseName, String baseOrdinal) {
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.level = level;
        this.currentBaseXP = currentBaseXP;
        this.baseName = baseName;
        this.baseOrdinal = baseOrdinal;
    }
}