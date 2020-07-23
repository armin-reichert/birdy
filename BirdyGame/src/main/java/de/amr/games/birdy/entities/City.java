package de.amr.games.birdy.entities;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.BirdyGameApp.randomInt;
import static de.amr.games.birdy.entities.City.DayEvent.SUNRISE;
import static de.amr.games.birdy.entities.City.DayEvent.SUNSET;
import static de.amr.games.birdy.entities.City.DayTime.DAY;
import static de.amr.games.birdy.entities.City.DayTime.NIGHT;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.stream.IntStream;

import de.amr.easy.game.Application;
import de.amr.easy.game.entity.EntityMap;
import de.amr.easy.game.entity.GameObject;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.easy.game.ui.sprites.SpriteMap;
import de.amr.statemachine.api.TransitionMatchStrategy;
import de.amr.statemachine.core.StateMachine;
import de.amr.statemachine.core.StateMachine.MissingTransitionBehavior;

/**
 * The city shown in the background.
 * 
 * @author Armin Reichert
 */
public class City extends GameObject {

	public enum DayTime {
		DAY, NIGHT
	}

	public enum DayEvent {
		SUNSET, SUNRISE
	}

	private final EntityMap ent;
	private final SpriteMap sprites = new SpriteMap();
	private final StateMachine<DayTime, DayEvent> fsm;

	public City(EntityMap entities, DayTime dayTime) {

		ent = entities;

		sprites.set("s_night", Sprite.ofAssets("bg_night"));
		sprites.set("s_day", Sprite.ofAssets("bg_day"));
		sprites.select(dayTime == DAY ? "s_day" : "s_night");

		sprites.current().ifPresent(sprite -> {
			tf.width = sprite.getWidth();
			tf.height = sprite.getHeight();
		});

		fsm = new StateMachine<>(DayTime.class, TransitionMatchStrategy.BY_VALUE);
		fsm.setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
		fsm.setDescription("City");
		fsm.setInitialState(DAY);

		fsm.state(DAY).entryAction = () -> {
			sprites.select("s_day");
		};

		fsm.addTransitionOnEventValue(DAY, NIGHT, null, null, SUNSET, () -> "");

		fsm.state(NIGHT).setTimer(() -> app().clock().sec(10));

		fsm.state(NIGHT).entryAction = () -> {
			sprites.select("s_night");
			replaceStars();
		};

		fsm.state(NIGHT).exitAction = () -> {
			ent.removeAll(Star.class);
		};

		fsm.addTransitionOnTimeout(NIGHT, NIGHT, null, e -> {
			replaceStars();
			fsm.restartTimer(NIGHT);
		}, () -> "");

		fsm.addTransitionOnEventValue(NIGHT, DAY, null, null, SUNRISE, () -> "");
	}

	@Override
	public void init() {
		fsm.init();
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce(KeyEvent.VK_N)) {
			fsm.enqueue(SUNSET);
		} else if (Keyboard.keyPressedOnce(KeyEvent.VK_D)) {
			fsm.enqueue(SUNRISE);
		}
		fsm.update();
	}

	private void replaceStars() {
		ent.removeAll(Star.class);
		int numStars = randomInt(1, app().settings().get("max-stars"));
		IntStream.range(1, numStars).forEach(i -> {
			Star star = ent.store(new Star());
			star.tf.setPosition(randomInt(50, tf.width - 50), randomInt(100, 180));
		});
		Application.LOGGER.info("Created " + numStars + " new stars");
	}

	public boolean isNight() {
		return fsm.getState() == NIGHT;
	}

	public void setWidth(int width) {
		if (tf.width != width) {
			tf.width = width;
			sprites.forEach(sprite -> {
				sprite.scale(width, sprite.getHeight());
			});
		}
	}

	@Override
	public void draw(Graphics2D g) {
		sprites.current().ifPresent(sprite -> {
			sprite.currentAnimationFrame().ifPresent(image -> {
				g.translate(tf.x, tf.y);
				for (int x = 0; x < tf.width; x += image.getWidth(null)) {
					g.drawImage(image, x, 0, null);
				}
				ent.ofClass(Star.class).forEach(star -> star.draw(g));
				g.translate(-tf.x, -tf.y);
			});
		});
	}
}