package de.amr.games.birdy.entities;

import java.awt.Graphics2D;
import java.awt.Image;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.easy.game.ui.sprites.SpriteMap;

/**
 * The ground.
 * 
 * @author Armin Reichert
 */
public class Ground extends Entity implements Lifecycle {

	private final SpriteMap sprites = new SpriteMap();
	private float startX;

	public Ground() {
		Sprite land = Sprite.ofAssets("land");
		sprites.set("s_land", land);
		sprites.select("s_land");
		tf.setWidth(land.getWidth());
		tf.setHeight(land.getHeight());
	}
	
	@Override
	public void init() {
	}

	@Override
	public void update() {
		startX -= tf.getVelocityX();
		if (startX < 0) {
			startX = sprites.current().get().currentFrame().getWidth(null);
		}
	}

	public void setWidth(int width) {
		tf.setWidth(width);
		sprites.current().ifPresent(sprite -> sprite.scale(width, sprite.getHeight()));
	}

	public void stopMoving() {
		tf.setVelocityX(0);
	}

	@Override
	public void draw(Graphics2D g) {
		Image image = sprites.current().get().currentFrame();
		for (float x = -startX; x < tf.getWidth(); x += image.getWidth(null)) {
			g.drawImage(image, (int) x, (int) tf.getY(), null);
		}
	}
}