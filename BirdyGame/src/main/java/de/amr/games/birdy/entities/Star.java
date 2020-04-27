package de.amr.games.birdy.entities;

import static de.amr.easy.game.ui.sprites.AnimationType.BACK_AND_FORTH;
import static de.amr.easy.game.ui.sprites.AnimationType.CYCLIC;
import static de.amr.games.birdy.utils.Util.randomInt;

import java.awt.Graphics2D;
import java.util.Random;

import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.easy.game.view.View;

/**
 * A shining and blinking star...
 * 
 * @author Armin Reichert
 */
public class Star extends Entity implements View {

	private final Sprite sprite;

	public Star() {
		sprite = Sprite.ofAssets("blink_00", "blink_01", "blink_02")
				.animate(new Random().nextBoolean() ? BACK_AND_FORTH : CYCLIC, randomInt(300, 2000));
	}

	@Override
	public void draw(Graphics2D g) {
		sprite.draw(g, tf.getX(), tf.getY());
	}
}
