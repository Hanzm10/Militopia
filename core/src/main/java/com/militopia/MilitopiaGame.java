package com.militopia;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class MilitopiaGame extends Game {
    public SpriteBatch batch;
    public Skin skin;

    @Override
    public void create() {
        batch = new SpriteBatch();
        skin = createBasicSkin(); // Generate skin programmatically
        this.setScreen(new MenuScreen(this));
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();

        // 1. Generate a 1x1 white texture to use for backgrounds
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        // 2. Add the default font
        skin.add("default", new BitmapFont());

        // 3. Configure Label Style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        skin.add("default", labelStyle);

        // 4. Configure TextButton Style
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = skin.getFont("default");
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);    // Normal state
        textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);  // Clicked state
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY); // Hover state
        skin.add("default", textButtonStyle);

        // 5. Configure TextField Style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = skin.newDrawable("white", Color.GRAY);
        textFieldStyle.selection = skin.newDrawable("white", Color.BLUE);
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        skin.add("default", textFieldStyle);
        
        // 6. Configure ScrollPane Style
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        // (Optional: add knobs if you want visible scrollbars)
        skin.add("default", scrollStyle);

        return skin;
    }

    @Override
    public void render() { super.render(); }

    @Override
    public void dispose() {
        batch.dispose();
        skin.dispose();
    }
}   