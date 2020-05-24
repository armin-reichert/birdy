package de.amr.games.birdy.entities;

import static java.lang.Math.round;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.view.View;

/**
 * Displays the game score.
 * 
 * @author Armin Reichert
 */
public class Score extends Entity implements View {

	private final Supplier<Integer> score;
	private final float scale;
	private final Image[] digits;
	private String scoreText;

	public Score(Supplier<Integer> score, float scale) {
		this.score = score;
		this.scale = scale;
		this.digits = new Image[10];
		for (int d = 0; d <= 9; d++) {
			BufferedImage digitImage = Assets.image("number_score_0" + d);
			digits[d] = digitImage.getScaledInstance(-1, round(scale) * digitImage.getHeight(), Image.SCALE_SMOOTH);
		}
		updateText();
	}

	private String pointsText() {
		return String.format("%d", score.get());
	}

	private void updateText() {
		scoreText = pointsText();
		tf.width = (scoreText.length() * digits[0].getWidth(null));
		tf.height = (digits[0].getHeight(null));
	}

	@Override
	public void draw(Graphics2D g) {
		updateText();
		for (int i = 0; i < scoreText.length(); i++) {
			int digit = "0123456789".indexOf(scoreText.charAt(i));
			g.drawImage(digits[digit], (int) tf.x + i * (digits[0].getWidth(null) - round(3 * scale)), (int) tf.y, null);
		}
	}
}