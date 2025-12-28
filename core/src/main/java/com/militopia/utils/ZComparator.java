package com.militopia.utils;

import com.badlogic.ashley.core.Entity;
import com.militopia.components.GridPositionComponent;
import java.util.Comparator;

public class ZComparator implements Comparator<Entity> {
@Override
    public int compare(Entity e1, Entity e2) {
        GridPositionComponent p1 = e1.getComponent(GridPositionComponent.class);
        GridPositionComponent p2 = e2.getComponent(GridPositionComponent.class);
        
        // 1. PRIMARY SORT: DEPTH (Back to Front)
        // Objects with a higher sum are "further back" (higher on screen).
        // We draw Back first, so we sort DESCENDING.
        int depth1 = p1.x + p1.y;
        int depth2 = p2.x + p2.y;
        
        int result = Integer.compare(depth2, depth1); 
        
        // 2. SECONDARY SORT: LAYER (Floor -> Object -> Unit -> Marker)
        // If two objects are on the exact same tile, use zIndex.
        // Lower zIndex draws first (bottom).
        if (result == 0) {
            result = Integer.compare(p1.zIndex, p2.zIndex);
        }
        
        return result;
    }
}