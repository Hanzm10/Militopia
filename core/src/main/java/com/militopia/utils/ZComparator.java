package com.militopia.utils;

import com.badlogic.ashley.core.Entity;
import com.militopia.components.GridPositionComponent;
import java.util.Comparator;

public class ZComparator implements Comparator<Entity> {
    @Override
    public int compare(Entity e1, Entity e2) {
        GridPositionComponent p1 = e1.getComponent(GridPositionComponent.class);
        GridPositionComponent p2 = e2.getComponent(GridPositionComponent.class);
        
        int depth1 = (p1.x + p1.y);
        int depth2 = (p2.x + p2.y);

        if (depth1 != depth2) {
            return Integer.compare(depth2, depth1);
        }

        // 2. LAYER SORTING (Stacking on same tile)
        // Lower Z (0=Ground) draws first. Higher Z (3=Unit) draws last (on top).
        return Integer.compare(p1.zIndex, p2.zIndex);
    }
}