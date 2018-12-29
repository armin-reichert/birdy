package de.amr.games.birdy.entities;

import static de.amr.easy.game.ui.sprites.AnimationType.BACK_AND_FORTH;
import static de.amr.easy.game.ui.sprites.AnimationType.CYCLIC;
import static de.amr.games.birdy.utils.Util.randomInt;

import java.util.Random;

import de.amr.easy.game.entity.SpriteEntity;
import de.amr.easy.game.ui.sprites.Sprite;

/**
 * A shining and blinking star...
 * 
 * @author Armin Reichert
 */
public class Star extends SpriteEntity {

	public Star() {
		sprites.set("s_star", Sprite.ofAssets("blink_00", "blink_01", "blink_02")
				.animate(new Random().nextBoolean() ? BACK_AND_FORTH : CYCLIC, randomInt(300, 2000)));
		sprites.select("s_star");
	}
}
