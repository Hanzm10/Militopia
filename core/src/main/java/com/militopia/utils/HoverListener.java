package com.militopia.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import static javax.xml.datatype.DatatypeConstants.DURATION;

public class HoverListener extends ClickListener {
    
    private static final float SCALE_FACTOR = 1.2f; // Grow to 120%
    private static final float DURATION = 0.1f;     // Animation speed (seconds)
    
    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        super.enter(event, x, y, pointer, fromActor);
        // Switch to Hand Cursor
        if (pointer == -1) { // -1 means mouse movement (not drag)
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            
            Actor actor = event.getListenerActor();
            
            // CRITICAL: Set origin to center so it grows from the middle
            actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
            
            actor.clearActions(); // Stop any previous animation
            actor.addAction(Actions.scaleTo(SCALE_FACTOR, SCALE_FACTOR, DURATION));
        }
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
        super.exit(event, x, y, pointer, toActor);
        // Switch back to Arrow Cursor
        if (pointer == -1) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            
            Actor actor = event.getListenerActor();
            
            // CRITICAL: Set origin to center
            actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
            
            actor.clearActions();
            actor.addAction(Actions.scaleTo(1.0f, 1.0f, DURATION)); // Return to normal
        }
    }
}