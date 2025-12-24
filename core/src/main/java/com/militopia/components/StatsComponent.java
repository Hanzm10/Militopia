package com.militopia.components;

import com.badlogic.ashley.core.Component;

public class StatsComponent implements Component {

    public enum MoveType {
        LAND, SEA, AIR
    }

    public int moveRange;
    public int maxHealth;
    public int currentHealth;
    public int attackDamage;
    public String name;
    public MoveType moveType; // <-- NEW: Determines where it can walk

    public StatsComponent(String name, int range, int hp, int atk, MoveType type) {
        this.name = name;
        this.moveRange = range;
        this.maxHealth = hp;
        this.currentHealth = hp;
        this.attackDamage = atk;
        this.moveType = type;
    }
}
