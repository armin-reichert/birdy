package de.amr.games.birdy.entities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.entity.collision.Collider;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp;

/**
 * An obstacle consisting of a hanging and a standing pipe with a passage in the middle.
 * 
 * @author Armin Reichert
 */
public class Obstacle extends Entity implements Lifecycle, View {

	private Rectangle2D upperPart;
	private Rectangle2D lowerPart;
	private Rectangle2D passage;
	private Image pipeDown;
	private Image pipeUp;
	private boolean lighted;
	private final Random rand = new Random();

	public Obstacle(BirdyGameApp app, int width, int height, int passageHeight, int passageCenterY) {
		tf.width = width;
		tf.height = height;
		int passageRadius = passageHeight / 2;
		upperPart = new Rectangle2D.Double(0, 0, width, passageCenterY - passageRadius);
		passage = new Rectangle2D.Double(0, passageCenterY - passageRadius, width, passageHeight);
		lowerPart = new Rectangle2D.Double(0, passageCenterY + passageRadius, width,
				height - passageRadius - passageCenterY);
		pipeDown = Assets.image("pipe_down").getScaledInstance(width, (int) upperPart.getHeight(),
				BufferedImage.SCALE_SMOOTH);
		pipeUp = Assets.image("pipe_up").getScaledInstance(width, (int) lowerPart.getHeight(),
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
		g.drawImage(pipeDown, 0, 0, null);
		if (lighted) {
			int inset = (int) passage.getWidth() / 10;
			g.setColor(new Color(255, 255, 0, rand.nextInt(170)));
			g.fillRect((int) passage.getX() + inset, (int) passage.getY(),
					(int) (passage.getWidth() - 2 * inset), (int) passage.getHeight());
		}
		g.drawImage(pipeUp, 0, (int) (upperPart.getHeight() + passage.getHeight()), null);
		g.translate(-tf.x, -tf.y);
	}

	public void setLighted(boolean lighted) {
		this.lighted = lighted;
	}

	public Collider getUpperPart() {
		return () -> new Rectangle2D.Double(tf.x, tf.y, upperPart.getWidth(),
				upperPart.getHeight());
	}

	public Collider getLowerPart() {
		return () -> new Rectangle2D.Double(tf.x,
				tf.y + upperPart.getHeight() + passage.getHeight(), lowerPart.getWidth(),
				lowerPart.getHeight());
	}

	public Collider getPassage() {
		return () -> new Rectangle2D.Double(tf.x, tf.y + upperPart.getHeight(),
				passage.getWidth(), passage.getHeight());
	}
}