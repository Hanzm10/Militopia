package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.militopia.components.FacingComponent; // Import this
import com.militopia.components.MovementComponent;
import com.militopia.components.TextureComponent;

public class MovementSystem extends IteratingSystem {

    public MovementSystem() {
        // We need TextureComponent and FacingComponent to change the sprite
        super(Family.all(MovementComponent.class, TextureComponent.class, FacingComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        MovementComponent move = entity.getComponent(MovementComponent.class);
        TextureComponent tex = entity.getComponent(TextureComponent.class);
        FacingComponent facing = entity.getComponent(FacingComponent.class);

        // --- FACING LOGIC (Run once at start of move) ---
        if (move.time == 0) {
            // Calculate Isometric Horizontal Delta
            // Formula: (TargetX - TargetY) - (StartX - StartY)
            // This tells us if we are moving visually Right or Left
            
            float startIsoX = (move.startX - move.startY);
            float targetIsoX = (move.targetX - move.targetY);
            float visualDelta = targetIsoX - startIsoX;

            if (visualDelta > 0) {
                // Moving Right (visually)
                tex.region = facing.rightRegion;
            } else if (visualDelta < 0) {
                // Moving Left (visually)
                tex.region = facing.leftRegion;
            }
            // If visualDelta == 0 (Straight Up/Down), keep previous facing
        }
        // ------------------------------------------------

        // Update Timer
        move.time += deltaTime;

        if (move.time >= move.duration) {
            entity.remove(MovementComponent.class);
        }
    }
}