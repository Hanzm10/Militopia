package com.militopia.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class HoverListener extends ClickListener {
    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        super.enter(event, x, y, pointer, fromActor);
        // Switch to Hand Cursor
        if (pointer == -1) { // -1 means mouse movement (not drag)
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
        }
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
        super.exit(event, x, y, pointer, toActor);
        // Switch back to Arrow Cursor
        if (pointer == -1) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }
    }
}