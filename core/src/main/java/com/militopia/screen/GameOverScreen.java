package com.militopia.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
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

public class GameOverScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;
    int winnerID;

    public GameOverScreen(final MilitopiaGame game, int winnerID) {
        this.game = game;
        this.winnerID = winnerID;
        GameLogger.log(GameLogger.GAME_OVER, winnerID, "=== PLAYER " + winnerID + " WINS ===");
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        String winnerName = (winnerID == 1) ? "PLAYER 1" : "PLAYER 2";
        Color winnerColor = (winnerID == 1) ? Color.CYAN : Color.RED;

        Label titleLabel = new Label("GAME OVER", game.skin, "default-font", Color.WHITE);
        titleLabel.setFontScale(2.5f);

        Label winnerLabel = new Label(winnerName + " VICTORIOUS!", game.skin, "default-font", winnerColor);
        winnerLabel.setFontScale(1.2f);

        TextButton mainMenuBtn = new TextButton("Return to Main Menu", game.skin);
        mainMenuBtn.addListener(new HoverListener());
        mainMenuBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameLogger.logScreen("Navigating → Main Menu from Game Over");
                game.setScreen(new MenuScreen(game));
            }
        });

        table.add(titleLabel).padBottom(20).row();
        table.add(winnerLabel).padBottom(40).row();
        table.add(mainMenuBtn).width(250).height(50).row();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

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
