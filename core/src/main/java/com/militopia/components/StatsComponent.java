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
    public int vision; 
    public int income;
    public String name;
    public MoveType moveType;
    public int owner;
    public boolean hasActed = false;

    // Updated Constructor
    public StatsComponent(String name, int range, int hp, int atk, int vision, int income, MoveType type, int owner) {
        this.name = name;
        this.moveRange = range;
        this.maxHealth = hp;
        this.currentHealth = hp;
        this.attackDamage = atk;
        this.vision = vision; 
        this.income = income;
        this.moveType = type;
        this.owner = owner;
        this.hasActed = false;
    }
}