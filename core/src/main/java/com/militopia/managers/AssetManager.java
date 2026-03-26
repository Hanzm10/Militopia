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

    // Terrain
    public static final String TILE_GRASS = "tiles/tile_grass.png";
    public static final String TILE_WATER = "tiles/tile_water.png";
    public static final String TILE_DEEPWATER = "tiles/tile_deepwater.png";
    public static final String TILE_SAND = "tiles/tile_sand.png";
    public static final String TILE_MOUNTAIN = "tiles/tile_mountain.png";
    public static final String FOG_OF_WAR = "tiles/fog_of_war.png";

    // Objects
    public static final String OBJ_TREE = "objects/obj_tree.png";
    public static final String OBJ_RUINS = "objects/obj_ruins.png";
    public static final String OBJ_OIL = "objects/obj_oil.png";
    public static final String OBJ_CACTUS = "objects/obj_cactus.png";
    public static final String OBJ_MOUNTAIN = "objects/obj_mountain.png";
    public static final String STRUCT_TOWN = "bases/struct_town.png";

    // Base 1-10
    public static final String BASE_LVL1_BLUE = "bases/struct_base_lvl1_blue.png";
    public static final String BASE_LVL1_RED = "bases/struct_base_lvl1_red.png";
    public static final String BASE_LVL2_BLUE = "bases/struct_base_lvl2_blue.png";
    public static final String BASE_LVL2_RED = "bases/struct_base_lvl2_red.png";
    public static final String BASE_LVL3_BLUE = "bases/struct_base_lvl3_blue.png";
    public static final String BASE_LVL3_RED = "bases/struct_base_lvl3_red.png";
    public static final String BASE_LVL4_BLUE = "bases/struct_base_lvl4_blue.png";
    public static final String BASE_LVL4_RED = "bases/struct_base_lvl4_red.png";
    public static final String BASE_LVL5_BLUE = "bases/struct_base_lvl5_blue.png";
    public static final String BASE_LVL5_RED = "bases/struct_base_lvl5_red.png";
    public static final String BASE_LVL6_BLUE = "bases/struct_base_lvl6_blue.png";
    public static final String BASE_LVL6_RED = "bases/struct_base_lvl6_red.png";
    public static final String BASE_LVL7_BLUE = "bases/struct_base_lvl7_blue.png";
    public static final String BASE_LVL7_RED = "bases/struct_base_lvl7_red.png";
    public static final String BASE_LVL8_BLUE = "bases/struct_base_lvl8_blue.png";
    public static final String BASE_LVL8_RED = "bases/struct_base_lvl8_red.png";
    public static final String BASE_LVL9_BLUE = "bases/struct_base_lvl9_blue.png";
    public static final String BASE_LVL9_RED = "bases/struct_base_lvl9_red.png";
    public static final String BASE_LVL10_BLUE = "bases/struct_base_lvl10_blue.png";
    public static final String BASE_LVL10_RED = "bases/struct_base_lvl10_red.png";

    // Land Units
    public static final String RECRUIT_RIGHT = "units/recruit_right.png";
    public static final String RECRUIT_LEFT = "units/recruit_left.png";
    public static final String RECRUIT_DISPLAY = "displays/display_recruit.png";

    public static final String RANGER_RIGHT = "units/ranger_right.png";
    public static final String RANGER_LEFT = "units/ranger_left.png";
    public static final String RANGER_DISPLAY = "displays/display_ranger.png";

    public static final String SNIPER_RIGHT = "units/sniper_right.png";
    public static final String SNIPER_LEFT = "units/sniper_left.png";
    public static final String SNIPER_DISPLAY = "displays/display_sniper.png";

    public static final String TANK_RIGHT = "units/tank_right.png";
    public static final String TANK_LEFT = "units/tank_left.png";
    public static final String TANK_DISPLAY = "displays/display_tank.png";

    // Land Super Unit
    public static final String JUGGERNAUT_RIGHT = "units/juggernaut_right.png";
    public static final String JUGGERNAUT_LEFT = "units/juggernaut_left.png";
    public static final String JUGGERNAUT_DISPLAY = "displays/display_juggernaut.png";

    // Air Units
    public static final String RECON_DRONE_RIGHT = "units/recon_drone_right.png";
    public static final String RECON_DRONE_LEFT = "units/recon_drone_left.png";
    public static final String RECON_DRONE_DISPLAY = "displays/display_recon_drone.png";

    public static final String SUICIDE_DRONE_RIGHT = "units/suicide_drone_right.png";
    public static final String SUICIDE_DRONE_LEFT = "units/suicide_drone_left.png";
    public static final String SUICIDE_DRONE_DISPLAY = "displays/display_suicide_drone.png";

    public static final String APACHE_RIGHT = "units/apache_right.png";
    public static final String APACHE_LEFT = "units/apache_left.png";
    public static final String APACHE_DISPLAY = "displays/display_apache.png";

    // Air Super Unit
    public static final String B2_RIGHT = "units/b2_right.png";
    public static final String B2_LEFT = "units/b2_left.png";
    public static final String B2_DISPLAY = "displays/display_b2.png";

    // Water Units
    public static final String GUNBOAT_RIGHT = "units/gunboat_right.png";
    public static final String GUNBOAT_LEFT = "units/gunboat_left.png";
    public static final String GUNBOAT_DISPLAY = "displays/display_gunboat.png";

    public static final String DESTROYER_RIGHT = "units/destroyer_right.png";
    public static final String DESTROYER_LEFT = "units/destroyer_left.png";
    public static final String DESTROYER_DISPLAY = "displays/display_destroyer.png";

    public static final String CARRIER_RIGHT = "units/carrier_right.png";
    public static final String CARRIER_LEFT = "units/carrier_left.png";
    public static final String CARRIER_DISPLAY = "displays/display_carrier.png";

    // Water Super Unit
    public static final String SUBMARINE_RIGHT = "units/submarine_right.png";
    public static final String SUBMARINE_LEFT = "units/submarine_left.png";
    public static final String SUBMARINE_DISPLAY = "displays/display_submarine.png";

    // Structures
    public static final String MUNITION_FACTORY = "structures/munition_factory.png";
    public static final String PORT = "structures/port.png";
    public static final String SOLAR_ARRAY = "structures/solar_array.png";
    public static final String OIL_DERRICK = "structures/oil_derrick.png";
    public static final String NUCLEAR_PLANT = "structures/nuclear_plant.png";
    public static final String FIELD_HOSPITAL = "structures/field_hospital.png";
    public static final String RADAR_STATION = "structures/radar_station.png";
    public static final String SIGNAL_JAMMER = "structures/signal_jammer.png";

    // Animals
    public static final String DEER_DISPLAY = "displays/display_deer.png";
    public static final String FISH_DISPLAY = "displays/display_fish.png";
    public static final String ZEBRA_DISPLAY = "displays/display_zebra.png";
    public static final String HORSE_DISPLAY = "displays/display_horse.png";

    public static final String DEER = "objects/deer.png";
    public static final String FISH = "objects/fish.png";
    public static final String ZEBRA = "objects/zebra.png";
    public static final String HORSE = "objects/horse.png";

    // UI
    public static final String ENEMY_MARKER = "ui/enemy_marker.png";
    public static final String MARKER_DOT = "ui/marker_dot.png";
    public static final String ICON_SETTINGS = "ui/icon_settings.png";
    public static final String ICON_STATS = "ui/icon_stats.png";
    public static final String ICON_END = "ui/icon_end.png";
    public static final String BTN_SLIDEDOWN = "ui/slidedown_btn.png";
    public static final String CIRCLE_UI = "ui/circle_ui.png";
    public static final String CIRCLE_UI2 = "ui/circle_ui2.png";
    public static final String BACKGROUND = "game-system/militopia_background.png";

    public static final String FUNDING_ICON = "ui/funding_icon.png";
    public static final String FUNDING_ICON2 = "ui/funding_icon2.png";

    // Fonts
    public static final String GAME_FONT = "game-system/game_font.ttf";

    public AssetManager() {
        manager = new com.badlogic.gdx.assets.AssetManager();
        loadAssets();
    }

    private void loadAssets() {
        manager.load(TILE_GRASS, Texture.class);
        manager.load(TILE_WATER, Texture.class);
        manager.load(TILE_DEEPWATER, Texture.class);
        manager.load(TILE_SAND, Texture.class);
        manager.load(TILE_MOUNTAIN, Texture.class);
        manager.load(FOG_OF_WAR, Texture.class);

        manager.load(OBJ_TREE, Texture.class);
        manager.load(OBJ_RUINS, Texture.class);
        manager.load(OBJ_OIL, Texture.class);
        manager.load(OBJ_CACTUS, Texture.class);
        manager.load(OBJ_MOUNTAIN, Texture.class);
        manager.load(STRUCT_TOWN, Texture.class);

        // Load Base Levels
        manager.load(BASE_LVL1_BLUE, Texture.class);
        manager.load(BASE_LVL1_RED, Texture.class);
        manager.load(BASE_LVL2_BLUE, Texture.class);
        manager.load(BASE_LVL2_RED, Texture.class);
        manager.load(BASE_LVL3_BLUE, Texture.class);
        manager.load(BASE_LVL3_RED, Texture.class);
        manager.load(BASE_LVL4_BLUE, Texture.class);
        manager.load(BASE_LVL4_RED, Texture.class);
        manager.load(BASE_LVL5_BLUE, Texture.class);
        manager.load(BASE_LVL5_RED, Texture.class);
        manager.load(BASE_LVL6_BLUE, Texture.class);
        manager.load(BASE_LVL6_RED, Texture.class);
        manager.load(BASE_LVL7_BLUE, Texture.class);
        manager.load(BASE_LVL7_RED, Texture.class);
        manager.load(BASE_LVL8_BLUE, Texture.class);
        manager.load(BASE_LVL8_RED, Texture.class);
        manager.load(BASE_LVL9_BLUE, Texture.class);
        manager.load(BASE_LVL9_RED, Texture.class);
        manager.load(BASE_LVL10_BLUE, Texture.class);
        manager.load(BASE_LVL10_RED, Texture.class);

        // Load Land Units
        manager.load(RECRUIT_RIGHT, Texture.class);
        manager.load(RECRUIT_LEFT, Texture.class);
        manager.load(RECRUIT_DISPLAY, Texture.class);

        manager.load(RANGER_RIGHT, Texture.class);
        manager.load(RANGER_LEFT, Texture.class);
        manager.load(RANGER_DISPLAY, Texture.class);

        manager.load(SNIPER_RIGHT, Texture.class);
        manager.load(SNIPER_LEFT, Texture.class);
        manager.load(SNIPER_DISPLAY, Texture.class);

        manager.load(TANK_RIGHT, Texture.class);
        manager.load(TANK_LEFT, Texture.class);
        manager.load(TANK_DISPLAY, Texture.class);

        // Load Land Super Unit
        manager.load(JUGGERNAUT_RIGHT, Texture.class);
        manager.load(JUGGERNAUT_LEFT, Texture.class);
        manager.load(JUGGERNAUT_DISPLAY, Texture.class);

        // Load Air Units
        manager.load(RECON_DRONE_RIGHT, Texture.class);
        manager.load(RECON_DRONE_LEFT, Texture.class);
        manager.load(RECON_DRONE_DISPLAY, Texture.class);

        manager.load(SUICIDE_DRONE_RIGHT, Texture.class);
        manager.load(SUICIDE_DRONE_LEFT, Texture.class);
        manager.load(SUICIDE_DRONE_DISPLAY, Texture.class);

        manager.load(APACHE_RIGHT, Texture.class);
        manager.load(APACHE_LEFT, Texture.class);
        manager.load(APACHE_DISPLAY, Texture.class);

        // Load Air Super Unit
        manager.load(B2_RIGHT, Texture.class);
        manager.load(B2_LEFT, Texture.class);
        manager.load(B2_DISPLAY, Texture.class);

        // Load Water Units
        manager.load(GUNBOAT_RIGHT, Texture.class);
        manager.load(GUNBOAT_LEFT, Texture.class);
        manager.load(GUNBOAT_DISPLAY, Texture.class);

        manager.load(DESTROYER_RIGHT, Texture.class);
        manager.load(DESTROYER_LEFT, Texture.class);
        manager.load(DESTROYER_DISPLAY, Texture.class);

        manager.load(CARRIER_RIGHT, Texture.class);
        manager.load(CARRIER_LEFT, Texture.class);
        manager.load(CARRIER_DISPLAY, Texture.class);

        // Load Water Super Unit
        manager.load(SUBMARINE_RIGHT, Texture.class);
        manager.load(SUBMARINE_LEFT, Texture.class);
        manager.load(SUBMARINE_DISPLAY, Texture.class);

        // Load Structures & UI
        manager.load(MUNITION_FACTORY, Texture.class);
        manager.load(PORT, Texture.class);
        manager.load(SOLAR_ARRAY, Texture.class);
        manager.load(OIL_DERRICK, Texture.class);
        manager.load(NUCLEAR_PLANT, Texture.class);
        manager.load(FIELD_HOSPITAL, Texture.class);
        manager.load(RADAR_STATION, Texture.class);
        manager.load(SIGNAL_JAMMER, Texture.class);

        manager.load(DEER_DISPLAY, Texture.class);
        manager.load(FISH_DISPLAY, Texture.class);
        manager.load(ZEBRA_DISPLAY, Texture.class);
        manager.load(HORSE_DISPLAY, Texture.class);

        manager.load(DEER, Texture.class);
        manager.load(FISH, Texture.class);
        manager.load(ZEBRA, Texture.class);
        manager.load(HORSE, Texture.class);

        manager.load(ENEMY_MARKER, Texture.class);
        manager.load(MARKER_DOT, Texture.class);
        manager.load(ICON_SETTINGS, Texture.class);
        manager.load(ICON_STATS, Texture.class);
        manager.load(ICON_END, Texture.class);
        manager.load(BTN_SLIDEDOWN, Texture.class);
        manager.load(CIRCLE_UI, Texture.class);
        manager.load(CIRCLE_UI2, Texture.class);
        manager.load(BACKGROUND, Texture.class);

        manager.load(FUNDING_ICON, Texture.class);
        manager.load(FUNDING_ICON2, Texture.class);

        FileHandleResolver resolver = new InternalFileHandleResolver();
        manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
        manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));

        FreetypeFontLoader.FreeTypeFontLoaderParameter fontParam = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
        fontParam.fontFileName = GAME_FONT;
        fontParam.fontParameters.size = 24;
        fontParam.fontParameters.minFilter = Texture.TextureFilter.Linear;
        fontParam.fontParameters.magFilter = Texture.TextureFilter.Linear;

        manager.load(GAME_FONT, BitmapFont.class, fontParam);
    }

    public void finishLoading() {
        manager.finishLoading();
    }

    public Texture get(String fileName) {
        if (!manager.isLoaded(fileName)) {
            return null;
        }
        Texture t = manager.get(fileName, Texture.class);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    public Skin getSkin() {
        return new Skin();
    }

    public BitmapFont getFont() {
        return manager.get(GAME_FONT, BitmapFont.class);
    }

    public void dispose() {
        manager.dispose();
    }
}
