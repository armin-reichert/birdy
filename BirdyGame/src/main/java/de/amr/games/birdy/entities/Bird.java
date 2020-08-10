package de.amr.games.birdy.entities;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.BirdyGameApp.sec;
import static de.amr.games.birdy.entities.BirdEvent.CRASHED;
import static de.amr.games.birdy.entities.BirdEvent.LEFT_WORLD;
import static de.amr.games.birdy.entities.BirdEvent.TOUCHED_GROUND;
import static de.amr.games.birdy.entities.BirdEvent.TOUCHED_PIPE;
import static de.amr.games.birdy.entities.FlightState.CRASHING;
import static de.amr.games.birdy.entities.FlightState.DOWN;
import static de.amr.games.birdy.entities.FlightState.FLYING;
import static de.amr.games.birdy.entities.HealthState.DEAD;
import static de.amr.games.birdy.entities.HealthState.INJURED;
import static de.amr.games.birdy.entities.HealthState.SANE;
import static java.lang.Math.PI;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.entity.GameObject;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.math.Vector2f;
import de.amr.easy.game.ui.sprites.AnimationType;
import de.amr.easy.game.ui.sprites.Sprite;
import de.amr.easy.game.ui.sprites.SpriteMap;
import de.amr.statemachine.api.TransitionMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * The little bird.
 * <p>
 * The bird is controlled by two separate state machines, one controls the flight state and the
 * other the bird's health.
 * 
 * @author Armin Reichert
 */
public class Bird extends GameObject {

	private final FlightControl flightControl;
	private final HealthControl healthControl;
	private final SpriteMap sprites = new SpriteMap();
	private float gravity;

	/**
	 * State machine controlling the health state of the bird.
	 */
	private class HealthControl extends StateMachine<HealthState, BirdEvent> {

		/*
		 * Using the state machine builder results in prettier code, but I wanted to check that the API is
		 * working too.
		 */
		public HealthControl() {
			super(HealthState.class, TransitionMatchStrategy.BY_VALUE);

			setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
			setDescription("[Health]");
			setInitialState(SANE);

			state(SANE).entryAction = () -> sprites.select("s_yellow");

			addTransitionOnEventValue(SANE, INJURED, null, null, TOUCHED_PIPE, () -> "");
			addTransitionOnEventValue(SANE, DEAD, null, null, TOUCHED_GROUND, () -> "");
			addTransitionOnEventValue(SANE, DEAD, null, null, LEFT_WORLD, () -> "");

			state(INJURED).setTimer(() -> sec(app().settings().get("bird-injured-seconds")));
			state(INJURED).entryAction = () -> sprites.select("s_red");

			addTransitionOnEventValue(INJURED, INJURED, null, e -> resetTimer(INJURED), TOUCHED_PIPE, () -> "");
			addTransitionOnTimeout(INJURED, SANE, null, null, () -> "");
			addTransitionOnEventValue(INJURED, DEAD, null, null, TOUCHED_GROUND, () -> "");
			addTransitionOnEventValue(INJURED, DEAD, null, null, LEFT_WORLD, () -> "");

			state(DEAD).entryAction = () -> {
				sprites.select("s_blue");
				turnDown();
			};
		}
	}

	/**
	 * State machine controlling the flight state of the bird.
	 */
	private class FlightControl extends StateMachine<FlightState, BirdEvent> {

		public FlightControl() {
			super(FlightState.class, TransitionMatchStrategy.BY_VALUE);
			setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
			//@formatter:off
			beginStateMachine()
				.description("[Flight]")
				.initialState(FLYING)

			.states()
				
				.state(FLYING).onTick(() -> {
					if (Keyboard.keyDown(app().settings().get("jump-key"))) {
						flap();
					} else {
						fly();
					}
				})

				.state(CRASHING)
					.onEntry(() -> turnDown())
					.onTick(() -> fall(3))
					
				.state(DOWN)
					.onEntry(() -> {
						Assets.sound("sfx/die.mp3").play();
						turnDown();
					})

			.transitions()

					.when(FLYING).then(CRASHING).on(TOUCHED_PIPE)
					.when(FLYING).then(CRASHING).on(CRASHED)
					.when(FLYING).then(CRASHING).on(LEFT_WORLD)
					.when(FLYING).then(DOWN).on(TOUCHED_GROUND)

					.when(CRASHING).then(DOWN).on(TOUCHED_GROUND)

			.endStateMachine();
			//@formatter:off
		}
	}

	public Bird() {
		flightControl = new FlightControl();
		healthControl = new HealthControl();

		sprites.set("s_yellow", createFeatherSprite("bird0"));
		sprites.set("s_blue", createFeatherSprite("bird1"));
		sprites.set("s_red", createFeatherSprite("bird2"));
		sprites.select("s_yellow");

		tf.width = sprites.current().get().getWidth();
		tf.height = sprites.current().get().getHeight();

		gravity = app().settings().getAsFloat("world-gravity");
	}

	private Sprite createFeatherSprite(String birdName) {
		Sprite sprite = Sprite.ofAssets(birdName + "_0", birdName + "_1", birdName + "_2");
		sprite.animate(AnimationType.FORWARD_BACKWARDS, app().settings().get("bird-flap-millis"));
		return sprite;
	}

	@Override
	public void init() {
		healthControl.init();
		flightControl.init();
	}

	@Override
	public void update() {
		flightControl.update();
		healthControl.update();
		sprites.current().get().enableAnimation(tf.vy < 0);
	}

	public void dispatch(BirdEvent event) {
		flightControl.enqueue(event);
		healthControl.enqueue(event);
	}

	public FlightState getFlightState() {
		return flightControl.getState();
	}

	public HealthState getHealthState() {
		return healthControl.getState();
	}

	@Override
	public Rectangle2D getCollisionBox() {
		int margin = Math.min(tf.width / 4, tf.height / 4);
		return new Rectangle2D.Double(tf.x + margin, tf.y + margin, tf.width - 2 * margin, tf.height - 2 * margin);
	}

	public void flap() {
		flap(2.5f);
	}

	public void flap(float force) {
		Assets.sound("sfx/wing.mp3").play();
		tf.vy = tf.vy - force * gravity;
		fly();
	}

	public void fly() {
		if (tf.y < -tf.height) {
			tf.setVelocity(0, 0);
		}
		tf.vy += gravity;
		double damp = tf.vy < 0 ? 0.05 : 0.2;
		tf.rotation = -PI / 8 + damp * tf.vy;
		if (tf.rotation < -PI / 4)
			tf.rotation = -PI / 4;
		if (tf.rotation > PI / 2)
			tf.rotation = PI / 2;
		tf.move();
	}

	public void fall(float slowdown) {
		tf.vy = tf.vy + gravity / slowdown;
		tf.move();
	}

	private void turnDown() {
		tf.rotation = PI / 2;
		tf.setVelocity(0, 0);
	}

	@Override
	public void draw(Graphics2D g2) {
		Graphics2D g = (Graphics2D) g2.create();
		sprites.current().ifPresent(sprite -> {
			Vector2f center = tf.getCenter();
			float dx = center.x - sprite.getWidth() / 2;
			float dy = center.y - sprite.getHeight() / 2;
			g.translate(dx, dy);
			g.rotate(tf.rotation);
			sprite.draw(g);
		});
		g.dispose();
	}
}