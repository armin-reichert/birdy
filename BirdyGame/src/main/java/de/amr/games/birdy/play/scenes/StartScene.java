package de.amr.games.birdy.play.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.easy.game.assets.Assets.sounds;
import static de.amr.games.birdy.BirdyGameApp.sec;
import static de.amr.games.birdy.BirdyGameApp.setScene;
import static de.amr.games.birdy.play.BirdEvent.LEFT_WORLD;
import static de.amr.games.birdy.play.BirdEvent.TOUCHED_GROUND;
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
import de.amr.games.birdy.entities.BirdyGameEntities;
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

	private BirdyGameEntities ent;
	private ImageWidget sceneText;

	public StartScene(BirdyGameEntities entities) {
		super(StartSceneState.class, EventMatchStrategy.BY_EQUALITY);
		this.ent = entities;
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

		addTransition(STARTING, READY, () -> Keyboard.keyDown(app().settings().get("jump-key")), null);

		addTransitionOnEventObject(STARTING, GAME_OVER, null, null, TOUCHED_GROUND);

		// Ready ---

		state(READY).setTimer(() -> sec(app().settings().getAsFloat("ready-time-sec")));

		state(READY).setOnEntry(() -> showSceneText("readyText"));

		state(READY).setOnExit(this::hideSceneText);

		addTransitionOnTimeout(READY, STARTING_TO_PLAY, null, e -> setScene(Scene.PLAY_SCENE));

		addTransitionOnEventObject(READY, GAME_OVER, null, e -> showSceneText("title"), TOUCHED_GROUND);

		// GameOver ---

		state(GAME_OVER).setOnEntry(() -> {
			stop();
			sounds().forEach(Sound::stop);
			showSceneText("game_over");
		});

		addTransition(GAME_OVER, STARTING, () -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE), null);
	}

	private void reset() {
		int w = app().settings().width, h = app().settings().height;

		ent.theCity().setWidth(w);
		ent.theCity().init();

		ent.theGround().setWidth(w);
		ent.theGround().tf.setPosition(0, h - ent.theGround().tf.height);
		ent.theGround().tf.setVelocity(app().settings().getAsFloat("world-speed"), 0);

		ent.theBird().init();
		ent.theBird().tf.setPosition(w / 8, ent.theGround().tf.y / 2);
		ent.theBird().tf.setVelocity(0, 0);

		if (!ent.contains("title")) {
			ImageWidget titleText = new ImageWidget(Assets.image("title"));
			ent.store("title", titleText);
		}

		if (!ent.contains("text_game_over")) {
			ImageWidget gameOverText = new ImageWidget(Assets.image("text_game_over"));
			ent.store("text_game_over", gameOverText);
		}

		if (!ent.contains("text_ready")) {
			PumpingImageWidget readyText = PumpingImageWidget.create().image(Assets.image("text_ready")).build();
			ent.store("text_ready", readyText);
		}

		if (!ent.contains("world")) {
			Area world = new Area(w, 2 * h);
			world.tf.setPosition(0, -h);
			ent.store("world", world);
		}

		app().collisionHandler().clear();
		app().collisionHandler().registerEnd(ent.theBird(), ent.ofClass(Area.class).findAny().get(), LEFT_WORLD);
		app().collisionHandler().registerStart(ent.theBird(), ent.theGround(), TOUCHED_GROUND);

		showSceneText("title");
	}

	@Override
	public void update() {
		checkCollisions();
		ent.update();
		super.update();
	}

	private void checkCollisions() {
		for (Collision c : app().collisionHandler().collisions()) {
			BirdEvent event = (BirdEvent) c.getAppEvent();
			ent.theBird().receiveEvent(event);
			enqueue(event);
		}
	}

	@Override
	public void stop() {
		ent.theGround().tf.setVelocity(0, 0);
	}

	@Override
	public void draw(Graphics2D g) {
		int w = app().settings().width, h = app().settings().height;
		ent.theCity().draw(g);
		ent.theGround().draw(g);
		ent.theBird().draw(g);
		if (sceneText != null) {
			sceneText.tf.center(w, h - ent.theGround().tf.height);
			sceneText.draw(g);
		}
		if (app().settings().getAsBoolean("show-state")) {
			String text = String.format("%s: (%s)  Bird: Flight: (%s) Sanity: (%s)", getDescription(), getState(),
					ent.theBird().getFlightState(), ent.theBird().getHealthState());
			g.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
			g.drawString(text, 20, app().settings().height - 20);
		}
	}

	private void keepBirdInAir() {
		while (ent.theBird().tf.y > ent.theGround().tf.y / 2) {
			ent.theBird().flap(randomInt(1, 4));
		}
	}

	private void showSceneText(String imageName) {
		sceneText = ent.ofName(imageName);
	}

	private void hideSceneText() {
		sceneText = null;
	}
}