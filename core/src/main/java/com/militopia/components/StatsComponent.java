package com.militopia.components;

import com.badlogic.ashley.core.Component;

public class StatsComponent implements Component {
    public int moveRange;
    public String name;

    public StatsComponent(int range, String name) {
        this.moveRange = range;
        this.name = name;
    }
}