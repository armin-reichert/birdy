package de.amr.games.birdy.play.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.Application.loginfo;
import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.games.birdy.BirdyGameApp.entities;
import static de.amr.games.birdy.BirdyGameApp.setScene;
import static de.amr.games.birdy.play.BirdEvent.BirdCrashed;
import static de.amr.games.birdy.play.BirdEvent.BirdLeftPassage;
import static de.amr.games.birdy.play.BirdEvent.BirdLeftWorld;
import static de.amr.games.birdy.play.BirdEvent.BirdTouchedGround;
import static de.amr.games.birdy.play.BirdEvent.BirdTouchedPipe;
import static de.amr.games.birdy.play.scenes.PlayScene.State.GAME_OVER;
import static de.amr.games.birdy.play.scenes.PlayScene.State.PLAYING;
import static de.amr.games.birdy.play.scenes.PlayScene.State.STARTING;

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
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.entities.ObstacleManager;
import de.amr.games.birdy.entities.ScoreDisplay;
import de.amr.games.birdy.entities.bird.Bird;
import de.amr.games.birdy.play.BirdEvent;
import de.amr.games.birdy.play.scenes.PlayScene.State;
import de.amr.games.birdy.utils.Score;
import de.amr.statemachine.api.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Play scene of the game.
 * 
 * @author Armin Reichert
 */
public class PlayScene extends StateMachine<State, BirdEvent> implements Lifecycle, View {

	public enum State {
		STARTING, PLAYING, GAME_OVER;
	}

	private final Score score = new Score();
	private final ObstacleManager obstacleManager = new ObstacleManager();;
	private Bird bird;
	private City city;
	private Ground ground;
	private ImageWidget gameOverText;
	private ScoreDisplay scoreDisplay;

	public PlayScene() {
		super(State.class, EventMatchStrategy.BY_EQUALITY);
		buildStateMachine();
		obstacleManager.setLogger(Application.LOGGER);
	}

	private void buildStateMachine() {
		setDescription("[Play Scene]");
		setInitialState(PLAYING);

		state(PLAYING).setOnEntry(() -> {
			score.reset();
			obstacleManager.init();
			start();
		});

		addTransitionOnEventObject(PLAYING, PLAYING, () -> score.points > 3, e -> {
			score.points -= 3;
			bird.tf.x = (bird.tf.x + app().settings().getAsInt("pipe width") + bird.tf.width);
			bird.receiveEvent(BirdTouchedPipe);
			sound("sfx/hit.mp3").play();
		}, BirdTouchedPipe);

		addTransitionOnEventObject(PLAYING, GAME_OVER, () -> score.points <= 3, t -> {
			bird.receiveEvent(BirdCrashed);
			sound("sfx/hit.mp3").play();
		}, BirdTouchedPipe);

		addTransitionOnEventObject(PLAYING, PLAYING, null, e -> {
			score.points++;
			sound("sfx/point.mp3").play();
		}, BirdLeftPassage);

		addTransitionOnEventObject(PLAYING, GAME_OVER, null, e -> {
			bird.receiveEvent(BirdTouchedGround);
			sound("music/bgmusic.mp3").stop();
		}, BirdTouchedGround);

		addTransitionOnEventObject(PLAYING, GAME_OVER, null, e -> {
			bird.receiveEvent(BirdLeftWorld);
			sound("music/bgmusic.mp3").stop();
		}, BirdLeftWorld);

		state(GAME_OVER).setOnEntry(() -> stop());

		addTransition(GAME_OVER, STARTING, () -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE), null);

		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, null, BirdTouchedPipe);
		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, null, BirdLeftPassage);
		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, e -> sound("music/bgmusic.mp3").stop(), BirdTouchedGround);

		state(STARTING).setOnEntry(() -> setScene(Scene.START));
	}

	public void receive(BirdEvent event) {
		enqueue(event);
		bird.receiveEvent(event);
	}

	@Override
	public void init() {
		int w = app().settings().width, h = BirdyGameApp.app().settings().height;
		ground = entities().ofClass(Ground.class).findAny().get();
		city = entities().ofClass(City.class).findAny().get();
		bird = entities().ofClass(Bird.class).findAny().get();
		scoreDisplay = new ScoreDisplay(score, 1.5f);
		scoreDisplay.tf.centerX(w);
		scoreDisplay.tf.y = (ground.tf.y / 4);
		gameOverText = entities().store(new ImageWidget(Assets.image("text_game_over")));
		gameOverText.tf.center(w, h);
		Area world = new Area(w, 2 * h);
		world.tf.setPosition(0, -h);

		app().collisionHandler().registerStart(bird, ground, BirdTouchedGround);
		app().collisionHandler().registerEnd(bird, world, BirdLeftWorld);

		super.init();
	}

	@Override
	public void start() {
		ground.tf.setVelocity(BirdyGameApp.app().settings().get("world speed"), 0);
		obstacleManager.start();
	}

	@Override
	public void stop() {
		ground.stopMoving();
		obstacleManager.stop();
	}

	@Override
	public void update() {
		for (Collision collision : app().collisionHandler().collisions()) {
			receive((BirdEvent) collision.getAppEvent());
		}
		obstacleManager.update();
		bird.update();
		city.update();
		ground.update();
		gameOverText.update();
		scoreDisplay.update();
		super.update();
//		loginfo("%s: %s,  Bird: %s and %s", getDescription(), getState(), bird.getFlightState(), bird.getHealthState());
	}

	@Override
	public void draw(Graphics2D g) {
		city.draw(g);
		obstacleManager.draw(g);
		ground.draw(g);
		scoreDisplay.draw(g);
		bird.draw(g);
		if (getState() == GAME_OVER) {
			gameOverText.draw(g);
		}
	}
}