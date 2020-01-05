package de.amr.games.birdy.play.scenes;

import static de.amr.games.birdy.BirdyGameApp.entities;
import static de.amr.games.birdy.play.BirdEvent.BirdCrashed;
import static de.amr.games.birdy.play.BirdEvent.BirdLeftPassage;
import static de.amr.games.birdy.play.BirdEvent.BirdLeftWorld;
import static de.amr.games.birdy.play.BirdEvent.BirdTouchedGround;
import static de.amr.games.birdy.play.BirdEvent.BirdTouchedPipe;
import static de.amr.games.birdy.play.scenes.PlayScene.State.GameOver;
import static de.amr.games.birdy.play.scenes.PlayScene.State.Playing;
import static de.amr.games.birdy.play.scenes.PlayScene.State.StartingNewGame;
import static java.lang.String.format;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.entity.collision.Collision;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.ui.widgets.ImageWidget;
import de.amr.games.birdy.BirdyGameApp;
import de.amr.games.birdy.entities.Area;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.entities.ObstacleManager;
import de.amr.games.birdy.entities.ScoreDisplay;
import de.amr.games.birdy.entities.bird.Bird;
import de.amr.games.birdy.play.BirdEvent;
import de.amr.games.birdy.utils.Score;
import de.amr.statemachine.core.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Play scene of the game.
 * 
 * @author Armin Reichert
 */
public class PlayScene extends Entity implements Lifecycle {

	private final BirdyGameApp app;
	private final PlaySceneControl control;
	private final Score score = new Score();
	private final ObstacleManager obstacleManager;
	private Bird bird;
	private City city;
	private Ground ground;
	private ImageWidget gameOverText;
	private ScoreDisplay scoreDisplay;

	public enum State {
		Playing, GameOver, StartingNewGame;
	}

	private class PlaySceneControl extends StateMachine<State, BirdEvent> {

		public PlaySceneControl() {

			super(State.class, EventMatchStrategy.BY_EQUALITY);
			setDescription("Play Scene Control");
			setInitialState(Playing);

			state(Playing).setOnEntry(() -> {
				score.reset();
				obstacleManager.init();
				start();
			});

			addTransitionOnEventObject(Playing, Playing, () -> score.points > 3, e -> {
				score.points -= 3;
				bird.tf.setX(bird.tf.getX() + app.settings().getAsInt("pipe width") + bird.tf.getWidth());
				bird.receiveEvent(BirdTouchedPipe);
				Assets.sound("sfx/hit.mp3").play();
			}, BirdTouchedPipe);

			addTransitionOnEventObject(Playing, GameOver, () -> score.points <= 3, t -> {
				bird.receiveEvent(BirdCrashed);
				Assets.sound("sfx/hit.mp3").play();
			}, BirdTouchedPipe);

			addTransitionOnEventObject(Playing, Playing, null, e -> {
				score.points++;
				Assets.sound("sfx/point.mp3").play();
			}, BirdLeftPassage);

			addTransitionOnEventObject(Playing, GameOver, null, e -> {
				bird.receiveEvent(BirdTouchedGround);
				Assets.sound("music/bgmusic.mp3").stop();
			}, BirdTouchedGround);

			addTransitionOnEventObject(Playing, GameOver, null, e -> {
				bird.receiveEvent(BirdLeftWorld);
				Assets.sound("music/bgmusic.mp3").stop();
			}, BirdLeftWorld);

			state(GameOver).setOnEntry(() -> stop());

			addTransition(GameOver, StartingNewGame, () -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE), null);

			addTransitionOnEventObject(GameOver, GameOver, null, null, BirdTouchedPipe);
			addTransitionOnEventObject(GameOver, GameOver, null, null, BirdLeftPassage);
			addTransitionOnEventObject(GameOver, GameOver, null, e -> Assets.sound("music/bgmusic.mp3").stop(),
					BirdTouchedGround);

			state(StartingNewGame).setOnEntry(() -> app.setController(app.getStartScene()));
		}
	}

	public PlayScene(BirdyGameApp game) {
		this.app = game;
		control = new PlaySceneControl();
		obstacleManager = new ObstacleManager(app);
	}

	public void receive(BirdEvent event) {
		control.enqueue(event);
		bird.receiveEvent(event);
	}

	public int getWidth() {
		return app.settings().width;
	}

	public int getHeight() {
		return app.settings().height;
	}

	@Override
	public void init() {
		ground = entities.ofClass(Ground.class).findAny().get();
		city = entities.ofClass(City.class).findAny().get();
		bird = entities.ofClass(Bird.class).findAny().get();
		scoreDisplay = new ScoreDisplay(score, 1.5f);
		scoreDisplay.tf.centerX(getWidth());
		scoreDisplay.tf.setY(ground.tf.getY() / 4);
		gameOverText = entities.store(new ImageWidget(Assets.image("text_game_over")));
		gameOverText.tf.center(getWidth(), getHeight());
		Area world = new Area(getWidth(), 2 * getHeight());
		world.tf.setPosition(0, -getHeight());

		app.collisionHandler.registerStart(bird, ground, BirdTouchedGround);
		app.collisionHandler.registerEnd(bird, world, BirdLeftWorld);

		obstacleManager.init();
		// obstacleManager.setLogger(Application.LOG);
		control.init();
	}

	@Override
	public void update() {
		for (Collision collision : app.collisionHandler.collisions()) {
			receive((BirdEvent) collision.getAppEvent());
		}
		control.update();
		obstacleManager.update();
		bird.update();
		city.update();
		ground.update();
		gameOverText.update();
		scoreDisplay.update();
	}

	@Override
	public void draw(Graphics2D g) {
		city.draw(g);
		obstacleManager.draw(g);
		ground.draw(g);
		scoreDisplay.draw(g);
		bird.draw(g);
		if (control.getState() == GameOver) {
			gameOverText.draw(g);
		}
		showState(g);
	}

	@Override
	public void start() {
		ground.tf.setVelocity(app.settings().get("world speed"), 0);
		obstacleManager.start();
	}

	@Override
	public void stop() {
		ground.stopMoving();
		obstacleManager.stop();
	}

	private Font stateTextFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

	private void showState(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.setFont(stateTextFont);
		g.drawString(format("%s: %s  Bird: %s & %s", control.getDescription(), control.getState(), bird.getFlightState(),
				bird.getHealthState()), 20, getHeight() - 50);
	}
}