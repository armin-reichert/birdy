package de.amr.games.birdy.entities;

import java.awt.Graphics2D;
import java.awt.Image;

import de.amr.easy.game.entity.SpriteEntity;
import de.amr.easy.game.ui.sprites.Sprite;

/**
 * The ground.
 * 
 * @author Armin Reichert
 */
public class Ground extends SpriteEntity {

	private float startX;

	public Ground() {
		sprites.set("s_land", Sprite.ofAssets("land"));
		sprites.select("s_land");
		tf.setWidth(sprites.current().getWidth());
		tf.setHeight(sprites.current().getHeight());
	}

	@Override
	public void update() {
		startX -= tf.getVelocityX();
		if (startX < 0) {
			startX = sprites.current().currentFrame().getWidth(null);
		}
	}

	public void setWidth(int width) {
		tf.setWidth(width);
		sprites.current().scale(width, sprites.current().getHeight());
	}

	public void stopMoving() {
		tf.setVelocityX(0);
	}

	@Override
	public void draw(Graphics2D g) {
		Image image = sprites.current().currentFrame();
		for (float x = -startX; x < tf.getWidth(); x += image.getWidth(null)) {
			g.drawImage(image, (int) x, (int) tf.getY(), null);
		}
	}
}