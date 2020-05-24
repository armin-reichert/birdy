package de.amr.games.birdy.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.games.birdy.entities.BirdEvent.CRASHED;
import static de.amr.games.birdy.entities.BirdEvent.LEFT_WORLD;
import static de.amr.games.birdy.entities.BirdEvent.PASSED_OBSTACLE;
import static de.amr.games.birdy.entities.BirdEvent.TOUCHED_GROUND;
import static de.amr.games.birdy.entities.BirdEvent.TOUCHED_PIPE;
import static de.amr.games.birdy.scenes.PlayScene.PlaySceneState.GAME_OVER;
import static de.amr.games.birdy.scenes.PlayScene.PlaySceneState.PLAYING;
import static de.amr.games.birdy.scenes.PlayScene.PlaySceneState.STARTING;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import de.amr.easy.game.Application;
import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.EntityMap;
import de.amr.easy.game.entity.collision.Collision;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.ui.widgets.ImageWidget;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp;
import de.amr.games.birdy.BirdyGameApp.Scene;
import de.amr.games.birdy.entities.Area;
import de.amr.games.birdy.entities.Bird;
import de.amr.games.birdy.entities.BirdEvent;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.entities.ObstacleController;
import de.amr.games.birdy.entities.ScoreDisplay;
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
	private EntityMap ent;
	private ImageWidget gameOverText;
	private ScoreDisplay score;

	public PlayScene(EntityMap entities) {
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
			Bird bird = ent.named("bird");
			points -= 3;
			bird.tf.x = bird.tf.x + app().settings().getAsInt("pipe-width") + bird.tf.width;
			bird.dispatch(TOUCHED_PIPE);
			sound("sfx/hit.mp3").play();
		}, TOUCHED_PIPE);

		addTransitionOnEventObject(PLAYING, GAME_OVER, () -> points <= 3, t -> {
			Bird bird = ent.named("bird");
			bird.dispatch(CRASHED);
			sound("sfx/hit.mp3").play();
		}, TOUCHED_PIPE);

		addTransitionOnEventObject(PLAYING, PLAYING, null, e -> {
			points++;
			sound("sfx/point.mp3").play();
		}, PASSED_OBSTACLE);

		addTransitionOnEventObject(PLAYING, GAME_OVER, null, e -> {
			Bird bird = ent.named("bird");
			bird.dispatch(TOUCHED_GROUND);
			sound("music/bgmusic.mp3").stop();
		}, TOUCHED_GROUND);

		addTransitionOnEventObject(PLAYING, GAME_OVER, null, e -> {
			Bird bird = ent.named("bird");
			bird.dispatch(LEFT_WORLD);
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
		Ground ground = ent.named("ground");

		score = new ScoreDisplay(() -> points, 1.5f);
		score.tf.centerX(w);
		score.tf.y = (ground.tf.y / 4);
		ent.store(score);

		gameOverText = new ImageWidget(Assets.image("text_game_over"));
		gameOverText.tf.center(w, h);
		ent.store(gameOverText);

		Area world = new Area(w, 2 * h);
		world.tf.setPosition(0, -h);

		Bird bird = ent.named("bird");
		app().collisionHandler().registerStart(bird, ground, TOUCHED_GROUND);
		app().collisionHandler().registerEnd(bird, world, LEFT_WORLD);

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
		ent.filter(entity -> entity instanceof Lifecycle).map(Lifecycle.class::cast).forEach(Lifecycle::update);
		obstacleController.update();
		super.update();
	}

	@Override
	public void start() {
		Ground ground = ent.named("ground");
		ground.tf.vx = app().settings().get("world-speed");
		obstacleController.start();
	}

	@Override
	public void stop() {
		Ground ground = ent.named("ground");
		ground.tf.vx = 0;
		obstacleController.stop();
	}

	public void dispatch(BirdEvent event) {
		enqueue(event);
		Bird bird = ent.named("bird");
		bird.dispatch(event);
	}

	@Override
	public void draw(Graphics2D g) {
		Bird bird = ent.named("bird");
		City city = ent.named("city");
		Ground ground = ent.named("ground");

		city.draw(g);
		obstacleController.obstacles.forEach(obstacle -> obstacle.draw(g));
		ground.draw(g);
		score.draw(g);
		bird.draw(g);
		if (getState() == GAME_OVER) {
			gameOverText.draw(g);
		}
		if (app().settings().getAsBoolean("show-state")) {
			String text = String.format("%s: %s,  Bird: %s and %s", getDescription(), getState(), bird.getFlightState(),
					bird.getHealthState());
			g.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
			g.drawString(text, 20, app().settings().height - 20);
		}
	}
}