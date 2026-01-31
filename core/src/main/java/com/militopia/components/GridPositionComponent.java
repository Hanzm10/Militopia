package com.militopia.components;

import com.badlogic.ashley.core.Component;

public class GridPositionComponent implements Component {
    public int x;
    public int y;
    
    // Z-INDEX LAYERS:
    // 0 = Terrain (Handled by MapRenderSystem, not Entities usually)
    // 1 = Static Objects (Bases, Trees, Oil, Ruins)
    // 2 = Dynamic Objects / Animals (Horses, Deer, Items)
    // 3 = Units (Tanks, Soldiers)
    // 4 = UI Markers / Effects
    public int zIndex = 0;

    public GridPositionComponent(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.zIndex = z;
    }
}