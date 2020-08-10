package de.amr.games.birdy.entities;

import static de.amr.easy.game.ui.sprites.AnimationType.FORWARD_BACKWARDS;
import static de.amr.easy.game.ui.sprites.AnimationType.CYCLIC;

import java.awt.Graphics2D;
import java.util.Random;

import de.amr.easy.game.entity.GameObject;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.games.birdy.BirdyGameApp;

/**
 * A shining and blinking star...
 * 
 * @author Armin Reichert
 */
public class Star extends GameObject {

	private final Sprite sprite;

	public Star() {
		sprite = Sprite.ofAssets("blink_00", "blink_01", "blink_02")
				.animate(new Random().nextBoolean() ? FORWARD_BACKWARDS : CYCLIC, BirdyGameApp.random(300, 2000));
	}

	@Override
	public void draw(Graphics2D g) {
		sprite.draw(g, tf.x, tf.y);
	}
}
