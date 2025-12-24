package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.militopia.components.MovementComponent;

public class MovementSystem extends IteratingSystem {

    public MovementSystem() {
        // This system only cares about entities that have a MovementComponent
        super(Family.all(MovementComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        MovementComponent move = entity.getComponent(MovementComponent.class);

        // Update Timer
        move.time += deltaTime;
        
        // Check if finished
        if (move.time >= move.duration) {
            entity.remove(MovementComponent.class); // Stop moving
        }
    }
}