package com.militopia;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class NewGameScreen implements Screen {
    final MilitopiaGame game;
    Stage stage;

    TextField nameField, seedField, p1Field, p2Field;

    public NewGameScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        nameField = new TextField("My Battle", game.skin);
        seedField = new TextField("12345", game.skin);
        p1Field = new TextField("Blue Commander", game.skin);
        p2Field = new TextField("Red General", game.skin);

        TextButton startBtn = new TextButton("Start Game", game.skin);
        TextButton backBtn = new TextButton("Back", game.skin);

        // Helper method to add row
        addInputRow(table, "Map Name:", nameField);
        addInputRow(table, "Seed:", seedField);
        addInputRow(table, "Player 1:", p1Field);
        addInputRow(table, "Player 2:", p2Field);

        table.row().padTop(20);
        table.add(backBtn).width(100).pad(10);
        table.add(startBtn).width(100).pad(10);

        startBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                long seed;
                try { seed = Long.parseLong(seedField.getText()); } 
                catch (Exception e) { seed = seedField.getText().hashCode(); }
                
                game.setScreen(new GameScreen(game, seed, p1Field.getText(), p2Field.getText()));
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
    }

    private void addInputRow(Table t, String labelText, TextField field) {
        t.add(new Label(labelText, game.skin)).right().pad(5);
        t.add(field).width(200).pad(5);
        t.row();
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