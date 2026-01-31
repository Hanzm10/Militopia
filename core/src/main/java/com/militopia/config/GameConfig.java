package com.militopia.config;

public class GameConfig {
    // Grid Settings
    //width & height of map(by tiles)
    public static final int MAP_WIDTH = 16;  
    public static final int MAP_HEIGHT = 16; 
    //size of tiles
    public static final int TILE_WIDTH = 16;
    public static final int TILE_HEIGHT = 10;

    // Drawing Settings
    public static final float DRAW_WIDTH = 18f;
    public static final float DRAW_HEIGHT = 20f;

    public static final float WORLD_WIDTH = 640f;
    public static final float WORLD_HEIGHT = 360f;

    // Animation Settings
    public static final float BOUNCE_DURATION = 0.2f;
    public static final float BOUNCE_HEIGHT = 1.5f;

    // mouse/touch x & y adjustment Settings
    public static final float INPUT_OFFSET_Y = -8f;
    public static final float INPUT_OFFSET_X = -8f;

    // Territory Settings
    public static final int BORDER_RADIUS = 1; // "1 tiles wide"
    
    //Wild Animal Count
    public static final int WILD_ANIMAL_COUNT_MIN = 1;
    public static final int WILD_ANIMAL_COUNT_MAX = 3;

    // Camera Settings
    public static final float ZOOM_MIN = 0.2f; // Closest (Zoomed In)
    public static final float ZOOM_MAX = 2.0f; // Furthest (Zoomed Out)
    public static final float ZOOM_SPEED = 0.1f; // How fast scrolling zooms
    public static final float STARTUP_ZOOM = 0.8f;
    
    // Drag Sensitivity (Lower = Slower/Smoother)
    public static final float DRAG_SPEED = 0.35f;
    
    //UI LAYOUT SETTINGS
    public static final float UI_WIDTH = 300f;
    
    public static final boolean TESTING_MODE = false;
}
