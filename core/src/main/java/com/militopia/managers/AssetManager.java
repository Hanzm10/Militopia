package com.militopia.managers;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class AssetManager {

    public final com.badlogic.gdx.assets.AssetManager manager;

    // --- ASSET PATHS (Constants to avoid typos) ---
    // Terrain
    public static final String TILE_GRASS = "tile_grass.png";
    public static final String TILE_WATER = "tile_water.png";
    public static final String TILE_DEEPWATER = "tile_deepwater.png";
    public static final String TILE_SAND = "tile_sand.png";
    public static final String TILE_MOUNTAIN = "tile_mountain.png";

    // Objects
    public static final String OBJ_TREE = "obj_tree.png";
    public static final String OBJ_RUINS = "obj_ruins.png";
    public static final String OBJ_OIL = "obj_oil.png";
    public static final String OBJ_CACTUS = "obj_cactus.png";
    public static final String OBJ_MOUNTAIN = "obj_mountain.png";
    public static final String STRUCT_BASE_BLUE = "struct_base_blue.png";
    public static final String STRUCT_BASE_RED = "struct_base_red.png";
    public static final String STRUCT_TOWN = "struct_town.png";

    // Units
    public static final String RECRUIT_RIGHT = "recruit_right.png";
    public static final String RECRUIT_LEFT = "recruit_left.png";
    public static final String RECRUIT_DISPLAY = "display_recruit.png";

    // UI & Misc
    public static final String MARKER_DOT = "marker_dot.png";
    public static final String ICON_SETTINGS = "icon_settings.png";
    public static final String ICON_STATS = "icon_stats.png";
    public static final String ICON_END = "icon_end.png";
    public static final String BTN_SLIDEDOWN = "slidedown_btn.png";
    public static final String UISKIN = "uiskin.json";
    
    // Fonts
    public static final String GAME_FONT = "game_font.ttf";
    public static final String FONT_SMALL = "font_small.ttf"; // Alias for use

    public AssetManager() {
        manager = new com.badlogic.gdx.assets.AssetManager();
        loadAssets();
    }

    private void loadAssets() {
        // 1. Load Textures
        manager.load(TILE_GRASS, Texture.class);
        manager.load(TILE_WATER, Texture.class);
        manager.load(TILE_DEEPWATER, Texture.class);
        manager.load(TILE_SAND, Texture.class);
        manager.load(TILE_MOUNTAIN, Texture.class);

        manager.load(OBJ_TREE, Texture.class);
        manager.load(OBJ_RUINS, Texture.class);
        manager.load(OBJ_OIL, Texture.class);
        manager.load(OBJ_CACTUS, Texture.class);
        manager.load(OBJ_MOUNTAIN, Texture.class);
        manager.load(STRUCT_BASE_BLUE, Texture.class);
        manager.load(STRUCT_BASE_RED, Texture.class);
        manager.load(STRUCT_TOWN, Texture.class);

        manager.load(RECRUIT_RIGHT, Texture.class);
        manager.load(RECRUIT_LEFT, Texture.class);
        manager.load(RECRUIT_DISPLAY, Texture.class);

        manager.load(MARKER_DOT, Texture.class);
        manager.load(ICON_SETTINGS, Texture.class);
        manager.load(ICON_STATS, Texture.class);
        manager.load(ICON_END, Texture.class);
        manager.load(BTN_SLIDEDOWN, Texture.class);

        // 2. Load Skin
        manager.load(UISKIN, Skin.class);

        // 3. Load Fonts (Special Setup for FreeType)
        FileHandleResolver resolver = new InternalFileHandleResolver();
        manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));

        // Define Font Parameter (Size 24)
        FreetypeFontLoader.FreeTypeFontLoaderParameter fontParam = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        fontParam.fontFileName = GAME_FONT;
        fontParam.fontParameters.size = 24;
        fontParam.fontParameters.minFilter = Texture.TextureFilter.Linear;
        fontParam.fontParameters.magFilter = Texture.TextureFilter.Linear;
        
        // We load the TTF but map it to a logical name if we want, or just use the filename
        manager.load(GAME_FONT, BitmapFont.class, fontParam);
    }

    public void finishLoading() {
        manager.finishLoading();
    }

    // Helper to get Texture easily
    public Texture get(String fileName) {
        if (!manager.isLoaded(fileName)) return null;
        Texture t = manager.get(fileName, Texture.class);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    public Skin getSkin() {
        return manager.get(UISKIN, Skin.class);
    }
    
    public BitmapFont getFont() {
        return manager.get(GAME_FONT, BitmapFont.class);
    }

    public void dispose() {
        manager.dispose();
    }
}