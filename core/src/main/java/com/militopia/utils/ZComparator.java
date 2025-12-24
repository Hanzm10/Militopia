package com.militopia.utils;

import com.badlogic.ashley.core.Entity;
import com.militopia.components.GridPositionComponent;
import java.util.Comparator;

public class ZComparator implements Comparator<Entity> {
    @Override
    public int compare(Entity e1, Entity e2) {
        GridPositionComponent pos1 = e1.getComponent(GridPositionComponent.class);
        GridPositionComponent pos2 = e2.getComponent(GridPositionComponent.class);
        
        // In Isometric, higher Y (back) draws first. Lower Y (front) draws last.
        // We sort descending by Y. 
        // If Y is equal, use X or Z-index.
        int result = Integer.compare(pos2.y, pos1.y);
        if (result == 0) {
            result = Integer.compare(pos1.zIndex, pos2.zIndex);
        }
        return result;
    }
}