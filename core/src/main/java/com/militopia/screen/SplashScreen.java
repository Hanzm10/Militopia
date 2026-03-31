package com.militopia.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.MilitopiaGame;
import com.militopia.managers.AssetManager;

/**
 * Splash screen displayed at application startup.
 */
public class SplashScreen implements Screen {

    private final Stage stage;

    public SplashScreen(final MilitopiaGame game) {
        this.stage = new Stage(new ScreenViewport());

        // Get the splash screen texture
        Texture splashTex = game.assets.get(AssetManager.SPLASH_SCREEN);
        if (splashTex == null) {
            // Fallback: if somehow it's not loaded, just go to menu
            Gdx.app.postRunnable(() -> game.setScreen(new MenuScreen(game)));
            return;
        }

        Image splashImage = new Image(new TextureRegionDrawable(new TextureRegion(splashTex)));
        splashImage.setScaling(Scaling.fit);

        // Center on screen
        Table table = new Table();
        table.setFillParent(true);
        table.add(splashImage).expand().fill();
        stage.addActor(table);

        // Animation sequence: Fade in, Wait, Fade out, Then go to Menu
        splashImage.getColor().a = 0;
        splashImage.addAction(Actions.sequence(
            Actions.fadeIn(1.2f),
            Actions.delay(1.0f),
            Actions.fadeOut(0.8f),
            Actions.run(() -> game.setScreen(new MenuScreen(game)))
        ));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null); // No input during splash
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
