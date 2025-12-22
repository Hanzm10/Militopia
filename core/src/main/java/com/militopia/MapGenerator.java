package com.militopia;

import com.badlogic.gdx.math.MathUtils;

public class MapGenerator {
    
    public enum TerrainType {
        DEEP_WATER, WATER, SAND, GRASS, FOREST
    }

    public enum ObjectType {
        NONE, 
        BASE_P1, BASE_P2, BASE_NEUTRAL, // Cities
        OIL, RUINS, CACTUS, TREE        // Resources/Decor
    }

    // A container class to hold both terrain and objects
    public static class GameMap {
        public TerrainType[][] terrain;
        public ObjectType[][] objects;
        public int width, height;

        public GameMap(int w, int h) {
            this.width = w;
            this.height = h;
            terrain = new TerrainType[w][h];
            objects = new ObjectType[w][h];
        }
    }

    public GameMap generateMap(int width, int height, long seed) {
        GameMap map = new GameMap(width, height);
        SimpleNoise noise = new SimpleNoise(seed);
        
        // Use a separate random for objects so terrain stays smooth
        MathUtils.random.setSeed(seed); 

        float scale = 0.15f; 

        // 1. GENERATE TERRAIN (With Mirroring for Balance)
        // We only generate the TOP LEFT half, then copy it to the BOTTOM RIGHT.
        // This ensures Player 2 has the exact same terrain as Player 1.
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Get noise value
                double value = noise.eval(x * scale, y * scale);

                // Assign Biome
                TerrainType type;
                if (value < -0.4) type = TerrainType.DEEP_WATER;
                else if (value < -0.15) type = TerrainType.WATER;
                else if (value < 0.1) type = TerrainType.SAND;
                else if (value < 0.5) type = TerrainType.GRASS;
                else type = TerrainType.FOREST;

                map.terrain[x][y] = type;
                map.objects[x][y] = ObjectType.NONE; // Default empty
            }
        }

        // 2. POPULATE OBJECTS
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TerrainType terrain = map.terrain[x][y];
                float chance = MathUtils.random(); // 0.0 to 1.0

                if (terrain == TerrainType.SAND) {
                    if (chance < 0.05f) map.objects[x][y] = ObjectType.CACTUS;
                    else if (chance > 0.98f) map.objects[x][y] = ObjectType.OIL; // Rare!
                } 
                else if (terrain == TerrainType.GRASS) {
                    if (chance < 0.02f) map.objects[x][y] = ObjectType.BASE_NEUTRAL;
                }
                else if (terrain == TerrainType.FOREST) {
                    if (chance < 0.05f) map.objects[x][y] = ObjectType.RUINS;
                }
            }
        }

        // 3. FORCE STARTING BASES (P1 Top-Left, P2 Bottom-Right)
        // We force the ground under the base to be GRASS so they don't spawn in water.
        setBase(map, 2, 2, ObjectType.BASE_P1);
        setBase(map, width - 3, height - 3, ObjectType.BASE_P2);

        return map;
    }

    private void setBase(GameMap map, int x, int y, ObjectType type) {
        map.terrain[x][y] = TerrainType.GRASS; // Force safe ground
        map.objects[x][y] = type;
        
        // Clear obstacles around base
        if(x+1 < map.width) map.objects[x+1][y] = ObjectType.NONE;
        if(y+1 < map.height) map.objects[x][y+1] = ObjectType.NONE;
    }
}