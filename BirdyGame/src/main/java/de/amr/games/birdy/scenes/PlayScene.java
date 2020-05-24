package de.amr.games.birdy.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.games.birdy.entities.bird.BirdEvent.CRASHED;
import static de.amr.games.birdy.entities.bird.BirdEvent.LEFT_WORLD;
import static de.amr.games.birdy.entities.bird.BirdEvent.PASSED_OBSTACLE;
import static de.amr.games.birdy.entities.bird.BirdEvent.TOUCHED_GROUND;
import static de.amr.games.birdy.entities.bird.BirdEvent.TOUCHED_PIPE;
import static de.amr.games.birdy.scenes.PlayScene.PlaySceneState.GAME_OVER;
import static de.amr.games.birdy.scenes.PlayScene.PlaySceneState.PLAYING;
import static de.amr.games.birdy.scenes.PlayScene.PlaySceneState.STARTING;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import de.amr.easy.game.Application;
import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.collision.Collision;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.ui.widgets.ImageWidget;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp;
import de.amr.games.birdy.BirdyGameApp.Scene;
import de.amr.games.birdy.entities.Area;
import de.amr.games.birdy.entities.BirdyGameEntities;
import de.amr.games.birdy.entities.ObstacleController;
import de.amr.games.birdy.entities.ScoreDisplay;
import de.amr.games.birdy.entities.bird.BirdEvent;
import de.amr.games.birdy.scenes.PlayScene.PlaySceneState;
import de.amr.statemachine.api.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Play scene of the game.
 * 
 * @author Armin Reichert
 */
public class PlayScene extends StateMachine<PlaySceneState, BirdEvent> implements Lifecycle, View {

	public enum PlaySceneState {
		STARTING, PLAYING, GAME_OVER;
	}

	private int points;
	private ObstacleController obstacleController;
	private BirdyGameEntities ent;
	private ImageWidget gameOverText;
	private ScoreDisplay scoreDisplay;

	public PlayScene(BirdyGameEntities entities) {
		super(PlaySceneState.class, EventMatchStrategy.BY_EQUALITY);
		this.ent = entities;
		buildStateMachine();
		getTracer().setLogger(Application.LOGGER);
		obstacleController = new ObstacleController(ent);
		obstacleController.getTracer().setLogger(Application.LOGGER);
	}

	private void buildStateMachine() {
		setDescription("[Play Scene]");
		setInitialState(PLAYING);

		state(PLAYING).setOnEntry(() -> {
			points = 0;
			start();
		});

		addTransitionOnEventObject(PLAYING, PLAYING, () -> points > 3, e -> {
			points -= 3;
			ent.theBird().tf.x = (ent.theBird().tf.x + app().settings().getAsInt("pipe-width") + ent.theBird().tf.width);
			ent.theBird().dispatch(TOUCHED_PIPE);
			sound("sfx/hit.mp3").play();
		}, TOUCHED_PIPE);

		addTransitionOnEventObject(PLAYING, GAME_OVER, () -> points <= 3, t -> {
			ent.theBird().dispatch(CRASHED);
			sound("sfx/hit.mp3").play();
		}, TOUCHED_PIPE);

		addTransitionOnEventObject(PLAYING, PLAYING, null, e -> {
			points++;
			sound("sfx/point.mp3").play();
		}, PASSED_OBSTACLE);

		addTransitionOnEventObject(PLAYING, GAME_OVER, null, e -> {
			ent.theBird().dispatch(TOUCHED_GROUND);
			sound("music/bgmusic.mp3").stop();
		}, TOUCHED_GROUND);

		addTransitionOnEventObject(PLAYING, GAME_OVER, null, e -> {
			ent.theBird().dispatch(LEFT_WORLD);
			sound("music/bgmusic.mp3").stop();
		}, LEFT_WORLD);

		state(GAME_OVER).setOnEntry(() -> stop());

		addTransition(GAME_OVER, STARTING, () -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE), null);

		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, null, TOUCHED_PIPE);
		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, null, PASSED_OBSTACLE);
		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, e -> sound("music/bgmusic.mp3").stop(), TOUCHED_GROUND);

		state(STARTING).setOnEntry(() -> BirdyGameApp.setScene(Scene.START_SCENE));
	}

	@Override
	public void init() {
		int w = app().settings().width, h = app().settings().height;
		scoreDisplay = new ScoreDisplay(() -> points, 1.5f);
		scoreDisplay.tf.centerX(w);
		scoreDisplay.tf.y = (ent.theGround().tf.y / 4);
		ent.store(scoreDisplay);

		gameOverText = new ImageWidget(Assets.image("text_game_over"));
		gameOverText.tf.center(w, h);
		ent.store(gameOverText);

		Area world = new Area(w, 2 * h);
		world.tf.setPosition(0, -h);

		app().collisionHandler().registerStart(ent.theBird(), ent.theGround(), TOUCHED_GROUND);
		app().collisionHandler().registerEnd(ent.theBird(), world, LEFT_WORLD);

		obstacleController.init();
		super.init();
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce("s")) {
			boolean showState = app().settings().getAsBoolean("show-state");
			app().settings().set("show-state", !showState);
		}
		for (Collision collision : app().collisionHandler().collisions()) {
			dispatch((BirdEvent) collision.getAppEvent());
		}
		ent.update();
		obstacleController.update();
		super.update();
	}

	@Override
	public void start() {
		ent.theGround().tf.vx = app().settings().get("world-speed");
		obstacleController.start();
	}

	@Override
	public void stop() {
		ent.theGround().tf.vx = 0;
		obstacleController.stop();
	}

	public void dispatch(BirdEvent event) {
		enqueue(event);
		ent.theBird().dispatch(event);
	}

	@Override
	public void draw(Graphics2D g) {
		ent.theCity().draw(g);
		obstacleController.obstacles.forEach(obstacle -> obstacle.draw(g));
		ent.theGround().draw(g);
		scoreDisplay.draw(g);
		ent.theBird().draw(g);
		if (getState() == GAME_OVER) {
			gameOverText.draw(g);
		}
		if (app().settings().getAsBoolean("show-state")) {
			String text = String.format("%s: %s,  Bird: %s and %s", getDescription(), getState(),
					ent.theBird().getFlightState(), ent.theBird().getHealthState());
			g.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
			g.drawString(text, 20, app().settings().height - 20);
		}
	}
}