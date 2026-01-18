package com.militopia.data;

public class StructureData {
    public int x;
    public int y;
    public int owner;
    public float currentBaseXP;
    public String baseName;
    public String baseOrdinal;

    // Default constructor for JSON
    public StructureData() {}

    public StructureData(int x, int y, int owner, float currentBaseXP, String baseName, String baseOrdinal) {
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.currentBaseXP = currentBaseXP;
        this.baseName = baseName;
        this.baseOrdinal = baseOrdinal;
    }
}