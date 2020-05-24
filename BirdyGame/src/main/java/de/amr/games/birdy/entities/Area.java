package de.amr.games.birdy.entities;

import de.amr.easy.game.entity.Entity;

/**
 * A transparent area used for collision handling.
 * 
 * @author Armin Reichert
 */
public class Area extends Entity {

	public Area(int x, int y, int width, int height) {
		tf.x = x;
		tf.vy = y;
		tf.width = width;
		tf.height = height;
	}
}
