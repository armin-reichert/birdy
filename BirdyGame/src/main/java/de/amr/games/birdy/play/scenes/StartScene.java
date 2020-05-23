package de.amr.games.birdy.play.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.easy.game.assets.Assets.sounds;
import static de.amr.games.birdy.BirdyGameApp.entities;
import static de.amr.games.birdy.BirdyGameApp.setScene;
import static de.amr.games.birdy.play.BirdEvent.BirdLeftWorld;
import static de.amr.games.birdy.play.BirdEvent.BirdTouchedGround;
import static de.amr.games.birdy.play.scenes.StartScene.StartSceneState.GAME_OVER;
import static de.amr.games.birdy.play.scenes.StartScene.StartSceneState.READY;
import static de.amr.games.birdy.play.scenes.StartScene.StartSceneState.STARTING;
import static de.amr.games.birdy.play.scenes.StartScene.StartSceneState.STARTING_TO_PLAY;
import static de.amr.games.birdy.utils.Util.randomInt;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.assets.Sound;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.collision.Collision;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.ui.widgets.ImageWidget;
import de.amr.easy.game.ui.widgets.PumpingImageWidget;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp.Scene;
import de.amr.games.birdy.entities.Area;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.entities.bird.Bird;
import de.amr.games.birdy.play.BirdEvent;
import de.amr.games.birdy.play.scenes.StartScene.StartSceneState;
import de.amr.statemachine.api.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Start scene of the game: bird flaps in the air until user presses the JUMP
 * key.
 * 
 * @author Armin Reichert
 */
public class StartScene extends StateMachine<StartSceneState, BirdEvent> implements Lifecycle, View {

	public enum StartSceneState {
		STARTING, READY, GAME_OVER, STARTING_TO_PLAY, SPRITE_BROWSER
	}

	private Bird bird;
	private City city;
	private Ground ground;
	private ImageWidget sceneText;

	public StartScene() {
		super(StartSceneState.class, EventMatchStrategy.BY_EQUALITY);
		buildStateMachine();
	}

	private void buildStateMachine() {
		setDescription("[Start Scene]");
		setInitialState(STARTING);

		// Starting ---

		state(STARTING).setOnEntry(() -> {
			reset();
			if (!sound("music/bgmusic.mp3").isRunning()) {
				sound("music/bgmusic.mp3").loop();
			}
		});

		state(STARTING).setOnTick(() -> keepBirdInAir());

		addTransition(STARTING, READY, () -> Keyboard.keyDown(app().settings().get("jump key")), null);

		addTransitionOnEventObject(STARTING, GAME_OVER, null, null, BirdTouchedGround);

		// Ready ---

		state(READY).setTimer(() -> {
			float readyTime = app().settings().getAsFloat("ready time sec");
			return app().clock().sec(readyTime);
		});

		state(READY).setOnEntry(() -> showSceneText("readyText"));

		state(READY).setOnExit(this::hideSceneText);

		addTransitionOnTimeout(READY, STARTING_TO_PLAY, null, e -> setScene(Scene.PLAY));

		addTransitionOnEventObject(READY, GAME_OVER, null, e -> showSceneText("title"), BirdTouchedGround);

		// GameOver ---

		state(GAME_OVER).setOnEntry(() -> {
			stop();
			sounds().forEach(Sound::stop);
			showSceneText("game_over");
		});

		addTransition(GAME_OVER, STARTING, () -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE), null);
	}

	@Override
	public void update() {
		for (Collision c : app().collisionHandler().collisions()) {
			BirdEvent event = (BirdEvent) c.getAppEvent();
			bird.receiveEvent(event);
			enqueue(event);
		}
		city.update();
		ground.update();
		bird.update();
		super.update();
	}

	@Override
	public void stop() {
		ground.tf.setVelocity(0, 0);
	}

	private void keepBirdInAir() {
		while (bird.tf.y > ground.tf.y / 2) {
			bird.flap(randomInt(1, 4));
		}
	}

	private void showSceneText(String imageName) {
		sceneText = entities().ofName(imageName);
	}

	private void hideSceneText() {
		sceneText = null;
	}

	private void reset() {
		int w = app().settings().width, h = app().settings().height;
		city = entities().ofClass(City.class).findAny().get();
		city.setWidth(w);
		city.init();

		ground = entities().ofClass(Ground.class).findAny().get();
		ground.setWidth(w);
		ground.tf.setPosition(0, h - ground.tf.height);
		ground.tf.setVelocity(app().settings().getAsFloat("world speed"), 0);

		bird = entities().ofClass(Bird.class).findAny().get();
		bird.init();
		bird.tf.setPosition(w / 8, ground.tf.y / 2);
		bird.tf.setVelocity(0, 0);

		if (!entities().contains("title")) {
			ImageWidget titleText = new ImageWidget(Assets.image("title"));
			entities().store("title", titleText);
		}

		if (!entities().contains("text_game_over")) {
			ImageWidget gameOverText = new ImageWidget(Assets.image("text_game_over"));
			entities().store("text_game_over", gameOverText);
		}

		if (!entities().contains("text_ready")) {
			PumpingImageWidget readyText = PumpingImageWidget.create().image(Assets.image("text_ready")).build();
			entities().store("text_ready", readyText);
		}

		if (!entities().contains("world")) {
			Area world = new Area(w, 2 * h);
			world.tf.setPosition(0, -h);
			entities().store("world", world);
		}

		app().collisionHandler().clear();
		app().collisionHandler().registerEnd(bird, entities().ofClass(Area.class).findAny().get(), BirdLeftWorld);
		app().collisionHandler().registerStart(bird, ground, BirdTouchedGround);

		showSceneText("title");
	}

	@Override
	public void draw(Graphics2D g) {
		int w = app().settings().width, h = app().settings().height;
		city.draw(g);
		ground.draw(g);
		bird.draw(g);
		if (sceneText != null) {
			sceneText.tf.center(w, h - ground.tf.height);
			sceneText.draw(g);
		}
		if (app().settings().getAsBoolean("show-state")) {
			String text = String.format("%s: (%s)  Bird: Flight: (%s) Sanity: (%s)", getDescription(), getState(),
					bird.getFlightState(), bird.getHealthState());
			g.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
			g.drawString(text, 20, app().settings().height - 20);
		}
	}
}