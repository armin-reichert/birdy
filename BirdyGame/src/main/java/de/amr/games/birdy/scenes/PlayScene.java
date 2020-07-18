package de.amr.games.birdy.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.games.birdy.BirdyGameApp.Scene.START_SCENE;
import static de.amr.games.birdy.entities.BirdEvent.CRASHED;
import static de.amr.games.birdy.entities.BirdEvent.LEFT_WORLD;
import static de.amr.games.birdy.entities.BirdEvent.PASSED_OBSTACLE;
import static de.amr.games.birdy.entities.BirdEvent.TOUCHED_GROUND;
import static de.amr.games.birdy.entities.BirdEvent.TOUCHED_PIPE;
import static de.amr.games.birdy.scenes.PlayScene.PlaySceneState.GAME_OVER;
import static de.amr.games.birdy.scenes.PlayScene.PlaySceneState.PLAYING;

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
import de.amr.games.birdy.entities.Bird;
import de.amr.games.birdy.entities.BirdEvent;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.entities.Obstacle;
import de.amr.games.birdy.entities.ObstacleController;
import de.amr.games.birdy.entities.Score;
import de.amr.games.birdy.scenes.PlayScene.PlaySceneState;
import de.amr.statemachine.api.TransitionMatchStrategy;
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
	private Score score;

	public PlayScene(EntityMap entities) {
		super(PlaySceneState.class, TransitionMatchStrategy.BY_VALUE);
		ent = entities;
		buildStateMachine();
		obstacleController = new ObstacleController(ent);
		obstacleController.getTracer().setLogger(Application.LOGGER);
	}

	private void buildStateMachine() {
		setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
		getTracer().setLogger(Application.LOGGER);
		//@formatter:off
		beginStateMachine()
			.description("[Play Scene]")
			.initialState(PLAYING)
		
		.states()

			.state(PLAYING)
				.onEntry(() -> {
					points = 0;
					start();
				})
				
			.state(GAME_OVER)
				.onEntry(() -> stop())

		.transitions()
		
			.stay(PLAYING)
				.on(TOUCHED_PIPE)
				.condition(() -> points > 3)
				.act(e -> {
					points -= 3;
					sound("sfx/hit.mp3").play();
					Bird bird = ent.named("bird");
					bird.tf.x += app().settings().getAsInt("obstacle-width") + bird.tf.width;
					bird.dispatch(TOUCHED_PIPE);
				})

			.stay(PLAYING)
				.on(PASSED_OBSTACLE)
				.act(e -> {
					points++;
					sound("sfx/point.mp3").play();
				})
			
			.when(PLAYING).then(GAME_OVER)
				.on(TOUCHED_PIPE)
				.condition(() -> points <= 3)
				.act(t -> {
					sound("sfx/hit.mp3").play();
					Bird bird = ent.named("bird");
					bird.dispatch(CRASHED);
				})

			.when(PLAYING).then(GAME_OVER)
				.on(TOUCHED_GROUND)
				.act(e -> {
					sound("music/bgmusic.mp3").stop();
					Bird bird = ent.named("bird");
					bird.dispatch(TOUCHED_GROUND);
				})

			.when(PLAYING).then(GAME_OVER)
				.on(LEFT_WORLD)
				.act(e -> {
					sound("music/bgmusic.mp3").stop();
					Bird bird = ent.named("bird");
					bird.dispatch(LEFT_WORLD);
				})

			.stay(GAME_OVER)
				.condition(() -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE))
				.act(() -> BirdyGameApp.setScene(START_SCENE))
				
			.stay(GAME_OVER)
				.on(TOUCHED_GROUND)
				.act(() -> sound("music/bgmusic.mp3").stop())
				
		.endStateMachine();
		//@formatter:on
	}

	@Override
	public void init() {
		int w = app().settings().width, h = app().settings().height;
		Ground ground = ent.named("ground");

		score = new Score(() -> points, 1.5f);
		score.tf.centerHorizontally(0, w);
		score.tf.y = (ground.tf.y / 4);
		ent.store(score);

		gameOverText = new ImageWidget(Assets.image("text_game_over"));
		gameOverText.tf.centerBoth(0, 0, w, h);
		ent.store(gameOverText);

		Bird bird = ent.named("bird");
		app().createCollisionHandler();
		app().collisionHandler().ifPresent(handler -> {
			handler.registerStart(bird, ground, TOUCHED_GROUND);
			handler.registerEnd(bird, ent.named("world"), LEFT_WORLD);
		});

		obstacleController.init();
		super.init();
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce("s")) {
			boolean showState = app().settings().getAsBoolean("show-state");
			app().settings().set("show-state", !showState);
		}
		app().collisionHandler().ifPresent(handler -> {
			for (Collision collision : handler.collisions()) {
				dispatch((BirdEvent) collision.getAppEvent());
			}
		});
		ent.implementing(Lifecycle.class).forEach(Lifecycle::update);
		obstacleController.update();
		super.update();
	}

	@Override
	public void start() {
		Ground ground = ent.named("ground");
		float speed = app().settings().get("world-speed");
		ground.tf.vx = speed;
		ent.ofClass(Obstacle.class).forEach(obstacle -> obstacle.tf.vx = speed);
		obstacleController.start();
	}

	@Override
	public void stop() {
		Ground ground = ent.named("ground");
		ground.tf.vx = 0;
		ent.ofClass(Obstacle.class).forEach(obstacle -> obstacle.tf.vx = 0);
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
		ent.ofClass(Obstacle.class).forEach(obstacle -> obstacle.draw(g));
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