package com.militopia;

import com.militopia.managers.AssetManager;
import com.militopia.screen.MenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

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
        Texture whiteTexture = new Texture(pixmap);
        skin.add("white", whiteTexture);

        // 4. Setup Default Styles
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = assets.getFont();
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = assets.getFont();
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("default", textButtonStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        skin.add("default", scrollPaneStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = assets.getFont();
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", Color.LIGHT_GRAY);
        textFieldStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default", textFieldStyle);

        TextButton.TextButtonStyle toggleStyle = new TextButton.TextButtonStyle();
        toggleStyle.font = assets.getFont();
        toggleStyle.fontColor = Color.WHITE;
        toggleStyle.checkedFontColor = Color.YELLOW;
        toggleStyle.up = skin.newDrawable("white", Color.GRAY);
        toggleStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        toggleStyle.checked = skin.newDrawable("white", Color.NAVY);
        toggleStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("toggle", toggleStyle);

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