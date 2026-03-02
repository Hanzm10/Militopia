package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.militopia.MilitopiaGame;
import com.militopia.config.GameConfig;
import com.militopia.managers.AssetManager;

/**
 * Top gradient strip showing XP, Funding, and Turn counter.
 * Owns all three stat labels and the gradient background.
 */
public class HudTopBar {

    private final MilitopiaGame game;
    private final AssetManager assets;

    private Label xpLabel;
    private Label fundsLabel;
    private Label fundingTitleLabel;
    private Label turnLabel;

    private final Table topContainer;

    public HudTopBar(MilitopiaGame game, AssetManager assets) {
        this.game = game;
        this.assets = assets;

        TextureRegionDrawable topBg = createGradientDrawable(80, true);

        Table topContent = new Table();
        topContent.add(createStatGroup("XP", "0")).expandX();
        topContent.add(createStatGroup("Funding", "1000")).expandX();
        topContent.add(createStatGroup("Turn", "1")).expandX();

        topContainer = new Table();
        topContainer.setBackground(topBg);
        topContainer.add(topContent).width(GameConfig.UI_WIDTH).padTop(10).padBottom(20);
    }

    /** Returns the top container Table for adding to rootTable. */
    public Table getActor() {
        return topContainer;
    }

    // -------------------------------------------------------------------------
    // Update API
    // -------------------------------------------------------------------------

    public void updateXP(int xp) {
        if (xpLabel != null)
            xpLabel.setText(String.valueOf(xp));
    }

    public void updateTurn(int turn) {
        if (turnLabel != null)
            turnLabel.setText(String.valueOf(turn));
    }

    public void updateFunding(int funding, int income) {
        if (fundsLabel != null)
            fundsLabel.setText(String.valueOf(funding));
        if (fundingTitleLabel != null)
            fundingTitleLabel.setText("Funding (+" + income + ")");
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private Table createStatGroup(String title, String placeholderValue) {
        Table t = new Table();

        if (title.equals("Funding")) {
            fundingTitleLabel = new Label(title + " (+0)", game.skin, "default-font", Color.WHITE);
            fundingTitleLabel.setFontScale(0.8f);
            t.add(fundingTitleLabel).left().row();

            Table valueRow = new Table();
            try {
                Texture iconTex = assets.get(AssetManager.FUNDING_ICON);
                iconTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                com.badlogic.gdx.scenes.scene2d.ui.Image iconImg = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                        new TextureRegion(iconTex));
                iconImg.setScaling(Scaling.fit);
                valueRow.add(iconImg).size(40, 40).padRight(0);
            } catch (Exception ignored) {
            }

            fundsLabel = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
            fundsLabel.setFontScale(1.2f);
            valueRow.add(fundsLabel);
            t.add(valueRow).left();

        } else {
            Label titleLbl = new Label(title, game.skin, "default-font", Color.WHITE);
            titleLbl.setFontScale(0.8f);
            Label valLbl = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
            valLbl.setFontScale(1.2f);

            if (title.equals("XP"))
                xpLabel = valLbl;
            if (title.equals("Turn"))
                turnLabel = valLbl;

            t.add(titleLbl).row();
            t.add(valLbl);
        }
        return t;
    }

    private TextureRegionDrawable createGradientDrawable(int height, boolean isTopDown) {
        Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            float alpha = isTopDown ? 1.0f - ((float) y / height) : ((float) y / height);
            pixmap.setColor(0f, 0f, 0f, alpha);
            pixmap.drawPixel(0, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}
