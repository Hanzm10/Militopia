package com.militopia.map;

import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapGenerator {

    public enum TerrainType {
        DEEP_WATER, WATER, SAND, GRASS, MOUNTAIN
    }

    public enum ObjectType {
        NONE,
        BASE_P1, BASE_P2, TOWN,
        OIL, RUINS, CACTUS, TREE, MOUNTAIN_OBJ
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

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Main Method
    public GameMap generateMap(int width, int height, long seed) {
        GameMap map = new GameMap(width, height);
        SimpleNoise noise = new SimpleNoise(seed);
        MathUtils.random.setSeed(seed);

        // Track valid spots for structures
        List<Point> grassTiles = new ArrayList<>();

        // 1. TERRAIN PASS (Includes Mountain Objects)
        generateTerrain(map, width, height, noise, grassTiles);

        // 2. PLAYER BASES PASS (Priority #1)
        placePlayerBases(map, width, height, grassTiles);

        // 3. FLORA PASS (Trees, Cacti, Oil)
        // Now strictly checks terrain rules
        placeFlora(map, width, height);

        // 4. STRUCTURES PASS (Towns, Ruins)
        placeStructures(map, width, height, seed, grassTiles);

        return map;
    }

    // --- HELPER METHODS ---
    private void generateTerrain(GameMap map, int width, int height, SimpleNoise noise, List<Point> grassTiles) {
        float scale = 0.15f;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double value = noise.eval(x * scale, y * scale);
                TerrainType type;

                if (value < -0.4) {
                    type = TerrainType.DEEP_WATER;
                } else if (value < -0.15) {
                    type = TerrainType.WATER;
                } else if (value < 0.001) {
                    type = TerrainType.SAND;
                } else if (value < 0.5) {
                    type = TerrainType.GRASS;
                } else {
                    type = TerrainType.MOUNTAIN;
                }

                map.terrain[x][y] = type;
                map.objects[x][y] = ObjectType.NONE; // Reset default

                // --- APPLY NEW RULES ---
                if (type == TerrainType.GRASS) {
                    grassTiles.add(new Point(x, y));
                }
                // RULE: If terrain is Mountain, place Mountain Object automatically
                if (type == TerrainType.MOUNTAIN) {
                    map.objects[x][y] = ObjectType.MOUNTAIN_OBJ;
                }
            }
        }
    }

    private void placePlayerBases(GameMap map, int width, int height, List<Point> grassTiles) {
        // We overwrite whatever was there (even Mountains) to ensure bases exist
        setBase(map, 2, 2, ObjectType.BASE_P1);
        setBase(map, width - 3, height - 3, ObjectType.BASE_P2);

        // Remove these spots from candidates so Towns/Ruins don't spawn on top
        removeFromList(grassTiles, 2, 2);
        removeFromList(grassTiles, width - 3, height - 3);
    }

    private void placeFlora(GameMap map, int width, int height) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Skip if occupied (e.g. by Bases or Mountain Objects)
                if (map.objects[x][y] != ObjectType.NONE) {
                    continue;
                }

                TerrainType t = map.terrain[x][y];
                float chance = MathUtils.random();

                // RULE: Trees only on GRASS (1/3 chance)
                // (Removed the check for MOUNTAINS because map.objects is already occupied by MOUNTAIN_OBJ)
                if (t == TerrainType.GRASS) {
                    if (chance < 0.33f) {
                        map.objects[x][y] = ObjectType.TREE;
                    }
                } // RULE: Cacti/Oil on SAND
                else if (t == TerrainType.SAND) {
                    if (chance < 0.05f) {
                        map.objects[x][y] = ObjectType.CACTUS;
                    } else if (chance > 0.98f) {
                        map.objects[x][y] = ObjectType.OIL;
                    }
                }
            }
        }
    }

    private void placeStructures(GameMap map, int width, int height, long seed, List<Point> grassTiles) {
        // Shuffle grass tiles for random placement
        Collections.shuffle(grassTiles, new java.util.Random(seed));

        int townsPlaced = 0;
        int ruinsPlaced = 0;

        // Iterate through valid grass spots
        for (Point p : grassTiles) {
            // Safety check: Don't overwrite Player Bases
            if (map.objects[p.x][p.y] == ObjectType.BASE_P1 || map.objects[p.x][p.y] == ObjectType.BASE_P2) {
                continue;
            }

            // 1. Place Towns (Target: 8)
            if (townsPlaced < 8) {
                // Ensure we don't overwrite anything important
                if (map.objects[p.x][p.y] == ObjectType.NONE || map.objects[p.x][p.y] == ObjectType.TREE) {
                    map.objects[p.x][p.y] = ObjectType.TOWN;
                    townsPlaced++;
                    continue; // Move to next tile
                }
            }

            // 2. Place Ruins (Target: 8)
            if (ruinsPlaced < 8) {
                // Reuse logic: Ruins can overwrite Trees or Empty Grass
                if (map.objects[p.x][p.y] == ObjectType.NONE || map.objects[p.x][p.y] == ObjectType.TREE) {
                    map.objects[p.x][p.y] = ObjectType.RUINS;
                    ruinsPlaced++;
                }
            }

            // Stop if we finished both
            if (townsPlaced >= 8 && ruinsPlaced >= 8) {
                break;
            }
        }
    }

    // Helper to clean up the list removal
    private void removeFromList(List<Point> list, int x, int y) {
        list.removeIf(p -> p.x == x && p.y == y);
    }

    private void setBase(GameMap map, int x, int y, ObjectType type) {
        map.terrain[x][y] = TerrainType.GRASS;
        map.objects[x][y] = type;
        // Clear obstacles around base so units can spawn
        clearObj(map, x + 1, y);
        clearObj(map, x - 1, y);
        clearObj(map, x, y + 1);
        clearObj(map, x, y - 1);
    }

    private void clearObj(GameMap map, int x, int y) {
        if (x >= 0 && x < map.width && y >= 0 && y < map.height) {
            map.objects[x][y] = ObjectType.NONE;
        }
    }
}
