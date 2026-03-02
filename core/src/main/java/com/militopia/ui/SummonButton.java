package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.militopia.MilitopiaGame;
import com.militopia.managers.AssetManager;
import com.militopia.utils.HoverListener;

/**
 * Static factory for the reusable circle-icon-label action button used in the
 * sliding bottom menus (summon, hunt, capture, build, ability).
 *
 * Call {@link #addTo} to build the widget and add it directly to a container.
 */
public final class SummonButton {

    private SummonButton() {
    } // utility class

    /**
     * Builds a circle-icon button with a label below and adds it to
     * {@code container}.
     *
     * @param container  the Table that should receive the new button group
     * @param icon       the icon drawn inside the circle (may be null for fallback)
     * @param label      text shown below the circle
     * @param game       used for skin fallback
     * @param assets     used to load the CIRCLE_UI texture
     * @param listener   click handler wired to the circle stack
     * @param iconSize   size of the inner icon (px)
     * @param btnSize    size of the outer circle (px)
     * @param labelScale font scale for the label
     * @param wrapWidth  label wrap width (0 = no wrap)
     */
    public static void addTo(Table container, TextureRegion icon, String label,
            MilitopiaGame game, AssetManager assets,
            ClickListener listener,
            float iconSize, float btnSize,
            float labelScale, float wrapWidth) {

        com.badlogic.gdx.scenes.scene2d.utils.Drawable circleDrawable;
        try {
            Texture circleTex = assets.get(AssetManager.CIRCLE_UI);
            circleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            circleDrawable = new TextureRegionDrawable(new TextureRegion(circleTex));
        } catch (Exception e) {
            circleDrawable = game.skin.newDrawable("white", Color.DARK_GRAY);
        }

        Stack buttonStack = new Stack();
        buttonStack.setTransform(true);

        com.badlogic.gdx.scenes.scene2d.ui.Image circleBg = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                circleDrawable);
        circleBg.setScaling(Scaling.fit);
        buttonStack.add(circleBg);

        if (icon != null) {
            com.badlogic.gdx.scenes.scene2d.ui.Image iconImg = new com.badlogic.gdx.scenes.scene2d.ui.Image(icon);
            iconImg.setScaling(Scaling.fit);
            Container<com.badlogic.gdx.scenes.scene2d.ui.Image> iconContainer = new Container<>(iconImg);
            iconContainer.size(iconSize, iconSize).center();
            buttonStack.add(iconContainer);
        }

        buttonStack.setOrigin(btnSize / 2f, btnSize / 2f);
        buttonStack.addListener(new HoverListener());
        buttonStack.addListener(listener);

        Table group = new Table();
        group.add(buttonStack).size(btnSize, btnSize).row();

        Label nameLbl = new Label(label, game.skin, "default-font", Color.WHITE);
        nameLbl.setFontScale(labelScale);
        nameLbl.setAlignment(Align.center);
        if (wrapWidth > 0) {
            nameLbl.setWrap(true);
            group.add(nameLbl).width(wrapWidth).padTop(5);
        } else {
            group.add(nameLbl).padTop(5);
        }

        container.add(group).pad(10);
    }

    /**
     * Convenience overload with sane defaults (icon=50, btn=80, scale=0.7, no
     * wrap).
     */
    public static void addTo(Table container, TextureRegion icon, String label,
            MilitopiaGame game, AssetManager assets, ClickListener listener) {
        addTo(container, icon, label, game, assets, listener, 50f, 80f, 0.7f, 0f);
    }

    /** Convenience overload with wrap (for build-menu labels). */
    public static void addToWrapped(Table container, TextureRegion icon, String label,
            MilitopiaGame game, AssetManager assets, ClickListener listener) {
        addTo(container, icon, label, game, assets, listener, 50f, 80f, 0.6f, 90f);
    }
}
