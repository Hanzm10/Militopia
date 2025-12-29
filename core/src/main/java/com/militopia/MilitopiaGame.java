package com.militopia;

import com.militopia.managers.AssetManager;
import com.militopia.screen.MenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class MilitopiaGame extends Game {

    public SpriteBatch batch;
    public AssetManager assets; 
    public Skin skin; // <--- RESTORED PUBLIC VARIABLE

    @Override
    public void create() {
        batch = new SpriteBatch();
        
        // 1. Initialize AssetManager
        assets = new AssetManager();
        assets.finishLoading(); 

        // 2. Initialize Skin from Manager
        this.skin = assets.getSkin(); 
        
        // 3. Inject Font & White Pixel
        skin.add("default-font", assets.getFont()); 
        
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        assets.dispose(); 
    }
}