package de.amr.games.birdy.play.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.games.birdy.play.BirdEvent.BirdCrashed;
import static de.amr.games.birdy.play.BirdEvent.BirdLeftPassage;
import static de.amr.games.birdy.play.BirdEvent.BirdLeftWorld;
import static de.amr.games.birdy.play.BirdEvent.BirdTouchedGround;
import static de.amr.games.birdy.play.BirdEvent.BirdTouchedPipe;
import static de.amr.games.birdy.play.scenes.PlayScene.PlaySceneState.GAME_OVER;
import static de.amr.games.birdy.play.scenes.PlayScene.PlaySceneState.PLAYING;
import static de.amr.games.birdy.play.scenes.PlayScene.PlaySceneState.STARTING;

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
import de.amr.games.birdy.play.BirdEvent;
import de.amr.games.birdy.play.scenes.PlayScene.PlaySceneState;
import de.amr.games.birdy.utils.Score;
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

	private Score score = new Score();

	private BirdyGameEntities ent;
	private ImageWidget gameOverText;
	private ScoreDisplay scoreDisplay;

	public PlayScene(BirdyGameEntities entities) {
		super(PlaySceneState.class, EventMatchStrategy.BY_EQUALITY);
		this.ent = entities;
		buildStateMachine();
	}

	private void buildStateMachine() {
		setDescription("[Play Scene]");
		setInitialState(PLAYING);

		state(PLAYING).setOnEntry(() -> {
			score.reset();
			ent.theObstacles().init();
			start();
		});

		addTransitionOnEventObject(PLAYING, PLAYING, () -> score.points > 3, e -> {
			score.points -= 3;
			ent.theBird().tf.x = (ent.theBird().tf.x + app().settings().getAsInt("pipe-width") + ent.theBird().tf.width);
			ent.theBird().receiveEvent(BirdTouchedPipe);
			sound("sfx/hit.mp3").play();
		}, BirdTouchedPipe);

		addTransitionOnEventObject(PLAYING, GAME_OVER, () -> score.points <= 3, t -> {
			ent.theBird().receiveEvent(BirdCrashed);
			sound("sfx/hit.mp3").play();
		}, BirdTouchedPipe);

		addTransitionOnEventObject(PLAYING, PLAYING, null, e -> {
			score.points++;
			sound("sfx/point.mp3").play();
		}, BirdLeftPassage);

		addTransitionOnEventObject(PLAYING, GAME_OVER, null, e -> {
			ent.theBird().receiveEvent(BirdTouchedGround);
			sound("music/bgmusic.mp3").stop();
		}, BirdTouchedGround);

		addTransitionOnEventObject(PLAYING, GAME_OVER, null, e -> {
			ent.theBird().receiveEvent(BirdLeftWorld);
			sound("music/bgmusic.mp3").stop();
		}, BirdLeftWorld);

		state(GAME_OVER).setOnEntry(() -> stop());

		addTransition(GAME_OVER, STARTING, () -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE), null);

		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, null, BirdTouchedPipe);
		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, null, BirdLeftPassage);
		addTransitionOnEventObject(GAME_OVER, GAME_OVER, null, e -> sound("music/bgmusic.mp3").stop(), BirdTouchedGround);

		state(STARTING).setOnEntry(() -> BirdyGameApp.setScene(Scene.START_SCENE));
	}

	public void receive(BirdEvent event) {
		enqueue(event);
		ent.theBird().receiveEvent(event);
	}

	@Override
	public void init() {
		int w = app().settings().width, h = BirdyGameApp.app().settings().height;
		scoreDisplay = new ScoreDisplay(score, 1.5f);
		scoreDisplay.tf.centerX(w);
		scoreDisplay.tf.y = (ent.theGround().tf.y / 4);
		ent.store(scoreDisplay);

		gameOverText = ent.store(new ImageWidget(Assets.image("text_game_over")));
		gameOverText.tf.center(w, h);
		ent.store(gameOverText);

		Area world = new Area(w, 2 * h);
		world.tf.setPosition(0, -h);

		ObstacleController obstacles = new ObstacleController(ent);
		obstacles.setLogger(Application.LOGGER);
		ent.store("obstacles", obstacles);

		app().collisionHandler().registerStart(ent.theBird(), ent.theGround(), BirdTouchedGround);
		app().collisionHandler().registerEnd(ent.theBird(), world, BirdLeftWorld);

		super.init();
	}

	@Override
	public void update() {
		for (Collision collision : app().collisionHandler().collisions()) {
			receive((BirdEvent) collision.getAppEvent());
		}
		ent.all().forEach(entity -> {
			if (entity instanceof Lifecycle) {
				((Lifecycle) entity).update();
			}
		});
		super.update();
	}

	@Override
	public void start() {
		ent.theGround().tf.setVelocity(app().settings().get("world-speed"), 0);
		ent.theObstacles().start();
	}

	@Override
	public void stop() {
		ent.theGround().stopMoving();
		ent.theObstacles().stop();
	}

	@Override
	public void draw(Graphics2D g) {
		ent.theCity().draw(g);
		ent.theObstacles().draw(g);
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