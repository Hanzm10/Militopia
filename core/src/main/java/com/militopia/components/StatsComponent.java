package com.militopia.components;

import com.badlogic.ashley.core.Component;

public class StatsComponent implements Component {

    public enum MoveType {
        LAND, SEA, AIR
    }

    // --- Refactored Unit Stats ---
    public int maxHP;
    public int currentHP;
    public int attack;      // Atk
    public int defense;     // Def
    public int move;        // Move
    public int attackRange; // Atk_Rng
    public int vision;      // Vis
    public int cost;        // Cost
    
    public String name;
    public MoveType moveType;
    public int owner;
    public boolean hasActed = false;

    // --- Structure Stats (Hidden for Units) ---
    public int income = 0; // Default to 0
    public float currentBaseXP = 0;
    public float maxBaseXP = 2000;
    public String baseOrdinal = ""; 
    public int level = 1;

    // Structure Logic
    public int xpGain = 0; // Amount of XP this structure adds to its base
    public int parentBaseX = -1; // X coordinate of the base this structure belongs to
    public int parentBaseY = -1; // Y coordinate of the base this structure belongs to
    /**
     * UNIT CONSTRUCTOR: Matches your specific list (Income is auto-set to 0).
     */
    public StatsComponent(String name, int hp, int atk, int def, int move, int atkRng, int vis, int cost, 
                          MoveType type, int owner) {
        this.name = name;
        this.maxHP = hp;
        this.currentHP = hp;
        this.attack = atk;
        this.defense = def;
        this.move = move;
        this.attackRange = atkRng;
        this.vision = vis;
        this.cost = cost;
        this.moveType = type;
        this.owner = owner;
        this.income = 0; // Units generate no income
    }

    /**
     * STRUCTURE CONSTRUCTOR: Allows setting Income (for Bases/Towns).
     */
    public StatsComponent(String name, int hp, int atk, int def, int move, int atkRng, int vis, int cost, 
                          MoveType type, int owner, int income) {
        this(name, hp, atk, def, move, atkRng, vis, cost, type, owner);
        this.income = income;
    }
}