package de.amr.games.birdy.entities;

import de.amr.easy.game.entity.Entity;

/**
 * A transparent area used for collision handling.
 * 
 * @author Armin Reichert
 */
public class Area extends Entity {

	public Area(int width, int height) {
		tf.setWidth(width);
		tf.setHeight(height);
	}
}
