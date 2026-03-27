package com.militopia.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.militopia.components.FloatingTextComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FloatingTextSystemTest {

    private PooledEngine engine;

    @BeforeEach
    public void setup() {
        engine = new PooledEngine();
        engine.addSystem(new FloatingTextSystem());
    }

    private Entity addFloatingText(float worldX, float worldY) {
        Entity entity = engine.createEntity();
        entity.add(new FloatingTextComponent("+5", worldX, worldY, FloatingTextComponent.Type.DAMAGE));
        engine.addEntity(entity);
        return entity;
    }

    @Test
    public void alphaFadesOverTime() {
        // Arrange
        Entity entity = addFloatingText(0f, 0f);
        FloatingTextComponent ft = entity.getComponent(FloatingTextComponent.class);

        // Act: advance half of MAX_TIME (0.5s)
        engine.update(0.5f);

        // Assert: alpha = 1 - (0.5 / 1.0) = 0.5
        assertEquals(0.5f, ft.alpha, 0.001f);
    }

    @Test
    public void worldYDriftsUpward() {
        // Arrange: start at worldY = 100
        Entity entity = addFloatingText(0f, 100f);
        FloatingTextComponent ft = entity.getComponent(FloatingTextComponent.class);

        // Act: advance 0.5s
        engine.update(0.5f);

        // Assert: worldY = 100 + (18 * 0.5) = 109
        assertEquals(109f, ft.worldY, 0.001f);
    }

    @Test
    public void entityRemovedAfterMaxTime() {
        // Arrange
        addFloatingText(0f, 0f);
        assertEquals(1, engine.getEntities().size());

        // Act: advance exactly MAX_TIME
        engine.update(FloatingTextComponent.MAX_TIME);

        // Assert: entity removed from engine
        assertEquals(0, engine.getEntities().size());
    }
}
