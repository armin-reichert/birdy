package de.amr.games.birdy.entities;

import static java.lang.Math.round;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.entity.GameObject;

/**
 * Displays the game score.
 * 
 * @author Armin Reichert
 */
public class Score extends GameObject {

	private final Supplier<Integer> fnPoints;
	private final float scale;
	private final Image[] digits;

	public Score(Supplier<Integer> fnPoints, float scale) {
		this.fnPoints = fnPoints;
		this.scale = scale;
		this.digits = new Image[10];
		for (int d = 0; d <= 9; d++) {
			BufferedImage img = Assets.image("number_score_0" + d);
			digits[d] = img.getScaledInstance(-1, round(scale) * img.getHeight(), Image.SCALE_SMOOTH);
		}
	}

	@Override
	public void draw(Graphics2D g) {
		String pointsText = String.format("%d", fnPoints.get());
		tf.width = (pointsText.length() * digits[0].getWidth(null));
		tf.height = (digits[0].getHeight(null));
		for (int i = 0; i < pointsText.length(); i++) {
			int digit = "0123456789".indexOf(pointsText.charAt(i));
			g.drawImage(digits[digit], (int) tf.x + i * (digits[0].getWidth(null) - round(3 * scale)), (int) tf.y, null);
		}
	}
}