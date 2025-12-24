package com.militopia.map;

import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapGenerator {

    public enum TerrainType {
        DEEP_WATER, WATER, SAND, GRASS, FOREST
    }

    public enum ObjectType {
        NONE,
        BASE_P1, BASE_P2, BASE_NEUTRAL,
        OIL, RUINS, CACTUS, TREE
    }

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

    // A simple helper class to store coordinates
    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    public GameMap generateMap(int width, int height, long seed) {
        GameMap map = new GameMap(width, height);
        SimpleNoise noise = new SimpleNoise(seed);
        MathUtils.random.setSeed(seed);

        float scale = 0.15f;

        // Lists to track where we can put Bases and Ruins
        List<Point> grassTiles = new ArrayList<>();
        List<Point> forestTiles = new ArrayList<>();

        // 1. GENERATE TERRAIN
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double value = noise.eval(x * scale, y * scale);
                TerrainType type;
                
                if (value < -0.4) type = TerrainType.DEEP_WATER;
                else if (value < -0.15) type = TerrainType.WATER;
                else if (value < 0.001) type = TerrainType.SAND;
                else if (value < 0.5) type = TerrainType.GRASS;
                else type = TerrainType.FOREST;

                map.terrain[x][y] = type;
                map.objects[x][y] = ObjectType.NONE;

                // Collect candidate spots for later
                if (type == TerrainType.GRASS) grassTiles.add(new Point(x, y));
                if (type == TerrainType.FOREST) forestTiles.add(new Point(x, y));
            }
        }

        // 2. PLACE PLAYER BASES (Priority #1)
        // We place these first to ensure they aren't overwritten
        setBase(map, 2, 2, ObjectType.BASE_P1);
        setBase(map, width - 3, height - 3, ObjectType.BASE_P2);

        // Remove P1/P2 spots from our candidate lists so we don't put Ruins on top of them
        removeFromList(grassTiles, 2, 2);
        removeFromList(grassTiles, width - 3, height - 3);

        // 3. GENERATE FLORA (Trees/Cacti/Oil)
        // We do this BEFORE Neutral Bases/Ruins, so Bases/Ruins can "overwrite" trees if needed.
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Skip if this tile is already occupied (by Player Bases)
                if (map.objects[x][y] != ObjectType.NONE) continue;

                TerrainType t = map.terrain[x][y];
                float chance = MathUtils.random();

                if (t == TerrainType.FOREST) {
                    // Rule 3: Forest is filled with trees (Let's say 50% so it's not a solid wall)
                    if (chance < 0.50f) map.objects[x][y] = ObjectType.TREE;
                } 
                else if (t == TerrainType.GRASS) {
                    // Rule 4: Trees in GRASS 1/3 of the time
                    if (chance < 0.33f) map.objects[x][y] = ObjectType.TREE;
                }
                else if (t == TerrainType.SAND) {
                    if (chance < 0.05f) map.objects[x][y] = ObjectType.CACTUS;
                    else if (chance > 0.98f) map.objects[x][y] = ObjectType.OIL;
                }
            }
        }

        // 4. PLACE EXACTLY 8 NEUTRAL BASES
        // Rule 1: There should always be 8 BASE_NEUTRAL
        Collections.shuffle(grassTiles, new java.util.Random(seed)); // Shuffle the list of grass spots
        int basesPlaced = 0;
        for (Point p : grassTiles) {
            if (basesPlaced >= 8) break;
            
            // Only place if not water (already checked by list) and not P1/P2 base
            if (map.objects[p.x][p.y] != ObjectType.BASE_P1 && map.objects[p.x][p.y] != ObjectType.BASE_P2) {
                map.objects[p.x][p.y] = ObjectType.BASE_NEUTRAL;
                basesPlaced++;
            }
        }

        // 5. PLACE EXACTLY 8 RUINS
        // Rule 2: There should always be 8 RUINS
        // Ruins usually look best in Forests, but can spill into Grass if needed.
        // We'll prioritize Forest tiles first.
        List<Point> ruinCandidates = new ArrayList<>(forestTiles);
        ruinCandidates.addAll(grassTiles); // Add grass as backup
        Collections.shuffle(ruinCandidates, new java.util.Random(seed));

        int ruinsPlaced = 0;
        for (Point p : ruinCandidates) {
            if (ruinsPlaced >= 8) break;

            // Ensure we don't overwrite a Base we just placed
            ObjectType currentObj = map.objects[p.x][p.y];
            if (currentObj != ObjectType.BASE_P1 && 
                currentObj != ObjectType.BASE_P2 && 
                currentObj != ObjectType.BASE_NEUTRAL) {
                
                map.objects[p.x][p.y] = ObjectType.RUINS;
                ruinsPlaced++;
            }
        }

        return map;
    }

    private void setBase(GameMap map, int x, int y, ObjectType type) {
        map.terrain[x][y] = TerrainType.GRASS; 
        map.objects[x][y] = type;
        // Clear obstacles around base so units can spawn
        clearObj(map, x+1, y);
        clearObj(map, x-1, y);
        clearObj(map, x, y+1);
        clearObj(map, x, y-1);
    }

    private void clearObj(GameMap map, int x, int y) {
        if(x >= 0 && x < map.width && y >= 0 && y < map.height) {
            map.objects[x][y] = ObjectType.NONE;
        }
    }

    private void removeFromList(List<Point> list, int x, int y) {
        list.removeIf(p -> p.x == x && p.y == y);
    }
}