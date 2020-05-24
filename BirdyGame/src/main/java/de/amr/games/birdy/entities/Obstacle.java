package de.amr.games.birdy.entities;

import static de.amr.easy.game.Application.app;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Random;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.entity.collision.Collider;
import de.amr.easy.game.view.View;

/**
 * An obstacle consisting of a hanging and a standing pipe with a passage in the middle.
 * 
 * @author Armin Reichert
 */
public class Obstacle extends Entity implements Lifecycle, View {

	public int width, height;
	public Rectangle hanging, passage, standing;
	public Image hangingImage, standingImage;
	public boolean illuminated;

	public Obstacle(int passageRadius, int passageCenterY) {
		width = app().settings().get("obstacle-width");
		height = app().settings().get("obstacle-height");
		tf.width = width;
		tf.height = height;
		hanging = new Rectangle(0, 0, width, passageCenterY - passageRadius);
		passage = new Rectangle(0, passageCenterY - passageRadius, width, 2 * passageRadius);
		standing = new Rectangle(0, passageCenterY + passageRadius, width, height - passageRadius - passageCenterY);
		hangingImage = Assets.image("pipe_down").getScaledInstance(width, (int) hanging.getHeight(),
				BufferedImage.SCALE_SMOOTH);
		standingImage = Assets.image("pipe_up").getScaledInstance(width, (int) standing.getHeight(),
				BufferedImage.SCALE_SMOOTH);
	}

	@Override
	public void init() {
	}

	@Override
	public void update() {
		tf.move();
	}

	@Override
	public void draw(Graphics2D g) {
		g.translate(tf.x, tf.y);
		g.drawImage(hangingImage, 0, 0, width, hanging.height, null);
		if (illuminated) {
			int inset = passage.width / 10;
			g.setColor(new Color(255, 255, 0, new Random().nextInt(170)));
			g.fillRect(passage.x + inset, passage.y, passage.width - 2 * inset, passage.height);
		}
		g.drawImage(standingImage, 0, hanging.height + passage.height, null);
		g.translate(-tf.x, -tf.y);
	}

	public Collider getUpperPart() {
		return () -> new Rectangle((int) tf.x, (int) tf.y, hanging.width, hanging.height);
	}

	public Collider getLowerPart() {
		return () -> new Rectangle((int) tf.x, (int) tf.y + hanging.height + passage.height, standing.width,
				standing.height);
	}

	public Collider getPassage() {
		return () -> new Rectangle((int) tf.x, (int) tf.y + hanging.height, passage.width, passage.height);
	}
}