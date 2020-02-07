package de.amr.games.birdy.play.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.BirdyGameApp.entities;
import static de.amr.games.birdy.play.BirdEvent.BirdLeftWorld;
import static de.amr.games.birdy.play.BirdEvent.BirdTouchedGround;
import static de.amr.games.birdy.play.scenes.StartScene.State.GameOver;
import static de.amr.games.birdy.play.scenes.StartScene.State.Ready;
import static de.amr.games.birdy.play.scenes.StartScene.State.StartPlaying;
import static de.amr.games.birdy.play.scenes.StartScene.State.Starting;
import static de.amr.games.birdy.utils.Util.randomInt;
import static java.lang.String.format;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.assets.Sound;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.entity.collision.Collision;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.ui.widgets.ImageWidget;
import de.amr.easy.game.ui.widgets.PumpingImageWidget;
import de.amr.games.birdy.BirdyGameApp;
import de.amr.games.birdy.entities.Area;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.entities.bird.Bird;
import de.amr.games.birdy.play.BirdEvent;
import de.amr.statemachine.core.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Start scene of the game: bird flaps in the air until user presses the JUMP key.
 * 
 * @author Armin Reichert
 */
public class StartScene extends Entity implements Lifecycle {

	public enum State {
		Starting, Ready, GameOver, StartPlaying, StartSpriteBrowser
	}

	private class StartSceneControl extends StateMachine<State, BirdEvent> {

		public StartSceneControl() {

			super(State.class, EventMatchStrategy.BY_EQUALITY);

			setDescription("Start Scene Control");
			setInitialState(Starting);

			// Starting ---

			state(Starting).setOnEntry(() -> {
				reset();
				if (!Assets.sound("music/bgmusic.mp3").isRunning()) {
					Assets.sound("music/bgmusic.mp3").loop();
				}
			});

			state(Starting).setOnTick(() -> keepBirdInAir());

			addTransition(Starting, Ready, () -> Keyboard.keyDown(app.settings().get("jump key")), null);
			addTransitionOnEventObject(Starting, GameOver, null, null, BirdTouchedGround);

			// Ready ---

			state(Ready).setTimer(() -> app().clock().sec(app.settings().getAsFloat("ready time sec")));
			state(Ready).setOnEntry(() -> displayText("readyText"));
			addTransitionOnTimeout(Ready, StartPlaying, null, e -> app.setController(app.getPlayScene()));
			addTransitionOnEventObject(Ready, GameOver, null, e -> displayText("title"), BirdTouchedGround);
			state(Ready).setOnExit(() -> displayedText = null);

			// GameOver ---

			state(GameOver).setOnEntry(() -> {
				stop();
				displayedText = entities.ofName("game_over");
				Assets.sounds().forEach(Sound::stop);
			});

			addTransition(GameOver, Starting, () -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE), null);
		}
	}

	private final BirdyGameApp app;
	private final StartSceneControl control;
	private Bird bird;
	private City city;
	private Ground ground;
	private ImageWidget displayedText;

	public StartScene(BirdyGameApp game) {
		this.app = game;
		control = new StartSceneControl();
	}

	public int getWidth() {
		return app.settings().width;
	}

	public int getHeight() {
		return app.settings().height;
	}

	@Override
	public void init() {
		// control.setLogger(Application.LOG);
		control.init();
	}

	@Override
	public void stop() {
		ground.tf.setVelocity(0, 0);
	}

	private void keepBirdInAir() {
		while (bird.tf.getY() > ground.tf.getY() / 2) {
			bird.flap(randomInt(1, 4));
		}
	}

	private void displayText(String name) {
		displayedText = entities.ofName(name);
	}

	private void reset() {
		city = entities.ofClass(City.class).findAny().get();
		city.setWidth(getWidth());
		city.init();

		ground = entities.ofClass(Ground.class).findAny().get();
		ground.setWidth(getWidth());
		ground.tf.setPosition(0, getHeight() - ground.tf.getHeight());
		ground.tf.setVelocity(app.settings().getAsFloat("world speed"), 0);

		bird = entities.ofClass(Bird.class).findAny().get();
		bird.init();
		bird.tf.setPosition(getWidth() / 8, ground.tf.getY() / 2);
		bird.tf.setVelocity(0, 0);

		if (!entities.contains("title")) {
			ImageWidget titleText = new ImageWidget(Assets.image("title"));
			entities.store("title", titleText);
		}

		if (!entities.contains("text_game_over")) {
			ImageWidget gameOverText = new ImageWidget(Assets.image("text_game_over"));
			entities.store("text_game_over", gameOverText);
		}

		if (!entities.contains("text_ready")) {
			PumpingImageWidget readyText = PumpingImageWidget.create().image(Assets.image("text_ready")).build();
			entities.store("text_ready", readyText);
		}

		if (!entities.contains("world")) {
			Area world = new Area(getWidth(), 2 * getHeight());
			world.tf.setPosition(0, -getHeight());
			entities.store("world", world);
		}

		app.collisionHandler().clear();
		app.collisionHandler().registerEnd(bird, entities.ofClass(Area.class).findAny().get(), BirdLeftWorld);
		app.collisionHandler().registerStart(bird, ground, BirdTouchedGround);

		displayText("title");
	}

	@Override
	public void update() {
		for (Collision c : app.collisionHandler().collisions()) {
			BirdEvent event = (BirdEvent) c.getAppEvent();
			bird.receiveEvent(event);
			control.enqueue(event);
		}
		city.update();
		ground.update();
		bird.update();
		control.update();
	}

	@Override
	public void draw(Graphics2D g) {
		city.draw(g);
		ground.draw(g);
		bird.draw(g);
		if (displayedText != null) {
			displayedText.tf.center(getWidth(), getHeight() - ground.tf.getHeight());
			displayedText.draw(g);
		}
		g.setColor(Color.BLACK);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		g.drawString(format("%s: (%s)  Bird: Flight: (%s) Sanity: (%s)", control.getDescription(), control.getState(),
				bird.getFlightState(), bird.getHealthState()), 20, getHeight() - 50);
	}
}