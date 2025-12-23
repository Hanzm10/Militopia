package com.militopia.components;

import com.badlogic.ashley.core.Component;

public class GridPositionComponent implements Component {
    public int x;
    public int y;
    public int zIndex = 0; // 0=Terrain, 1=Base, 2=Unit

    public GridPositionComponent(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.zIndex = z;
    }
}