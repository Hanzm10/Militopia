package com.militopia;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class LoadGameScreen implements Screen {
    final MilitopiaGame game;
    Stage stage;

    public LoadGameScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        mainTable.add(new Label("Saved Games", game.skin)).pad(20).row();

        Table listTable = new Table();
        // Dummy Data
        for (int i = 1; i <= 5; i++) {
            final String name = "Save " + i;
            TextButton btn = new TextButton("Game " + i + " - P1 vs P2 - 10:00", game.skin);
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    game.setScreen(new GameScreen(game, 12345, "P1", "P2"));
                }
            });
            listTable.add(btn).fillX().pad(5).width(300).row();
        }

        ScrollPane scroll = new ScrollPane(listTable, game.skin);
        mainTable.add(scroll).size(400, 300).row();

        TextButton backBtn = new TextButton("Back", game.skin);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
        mainTable.add(backBtn).pad(20);
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        stage.act();
        stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() { stage.dispose(); }
}