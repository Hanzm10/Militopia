package com.militopia.screen;

import com.militopia.screen.LoadGameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.MilitopiaGame;
import com.militopia.managers.AssetManager;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;
import com.militopia.utils.RenderUtils;

public class MenuScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;

    public MenuScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        TextButton newGameBtn = new TextButton("New Game", game.skin, "default");
        newGameBtn.addListener(new HoverListener());
        TextButton lanBtn = new TextButton("Multiplayer", game.skin, "default");
        lanBtn.addListener(new HoverListener());
        TextButton resumeBtn = new TextButton("Saved Games", game.skin, "default");
        resumeBtn.addListener(new HoverListener());
        TextButton exitBtn = new TextButton("Exit", game.skin, "default");
        exitBtn.addListener(new HoverListener());

        newGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameLogger.logScreen("Navigating → New Game Screen");
                game.setScreen(new NewGameScreen(game));
            }
        });

        lanBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameLogger.logScreen("Navigating → LAN Lobby");
                game.setScreen(new LobbyScreen(game));
            }
        });

        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameLogger.logScreen("Navigating → Load Game Screen");
                game.setScreen(new LoadGameScreen(game));
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameLogger.logScreen("Exit requested");
                Gdx.app.exit();
            }
        });

        table.add(newGameBtn).fillX().uniformX().pad(10).width(200);
        table.row();
        table.add(lanBtn).fillX().uniformX().pad(10);
        table.row();
        table.add(resumeBtn).fillX().uniformX().pad(10);
        table.row();
        table.add(exitBtn).fillX().uniformX().pad(10);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        RenderUtils.drawProportionalBackground(game.batch, game.assets.get(AssetManager.BACKGROUND));

        stage.act();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        GameLogger.logScreen("Main Menu opened");
    }

    @Override
    public void hide() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
