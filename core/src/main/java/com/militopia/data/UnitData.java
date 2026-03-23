package com.militopia.data;

public class UnitData {
    public int x;
    public int y;
    public String type;
    public int owner;
    public String unitTypeKey;
    public int hp;
    public int maxHp;
    public boolean hasMoved;
    public boolean hasActed;

    // Default constructor for JSON
    public UnitData() {
    }

    public UnitData(int x, int y, String type, int owner) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.owner = owner;
        this.unitTypeKey = type;
        this.hp = 10;
        this.maxHp = 10;
        this.hasMoved = false;
        this.hasActed = false;
    }

    public UnitData(int x, int y, String type, int owner, String unitTypeKey, int hp, int maxHp, boolean hasMoved,
            boolean hasActed) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.owner = owner;
        this.unitTypeKey = unitTypeKey;
        this.hp = hp;
        this.maxHp = maxHp;
        this.hasMoved = hasMoved;
        this.hasActed = hasActed;
    }
}