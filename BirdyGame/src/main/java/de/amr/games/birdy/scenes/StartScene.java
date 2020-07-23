package de.amr.games.birdy.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.easy.game.assets.Assets.sounds;
import static de.amr.games.birdy.BirdyGameApp.randomInt;
import static de.amr.games.birdy.BirdyGameApp.sec;
import static de.amr.games.birdy.BirdyGameApp.setScene;
import static de.amr.games.birdy.entities.BirdEvent.LEFT_WORLD;
import static de.amr.games.birdy.entities.BirdEvent.TOUCHED_GROUND;
import static de.amr.games.birdy.scenes.StartScene.StartSceneState.COMPLETE;
import static de.amr.games.birdy.scenes.StartScene.StartSceneState.GAME_OVER;
import static de.amr.games.birdy.scenes.StartScene.StartSceneState.READY;
import static de.amr.games.birdy.scenes.StartScene.StartSceneState.STARTING;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.assets.SoundClip;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.EntityMap;
import de.amr.easy.game.entity.collision.Collision;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.ui.widgets.ImageWidget;
import de.amr.easy.game.ui.widgets.PumpingImageWidget;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp.Scene;
import de.amr.games.birdy.entities.Bird;
import de.amr.games.birdy.entities.BirdEvent;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.scenes.StartScene.StartSceneState;
import de.amr.statemachine.api.TransitionMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Start scene of the game: bird flaps in the air until user presses the JUMP key.
 * 
 * @author Armin Reichert
 */
public class StartScene extends StateMachine<StartSceneState, BirdEvent> implements Lifecycle, View {

	public enum StartSceneState {
		STARTING, READY, GAME_OVER, COMPLETE
	}

	private EntityMap ent;
	private ImageWidget displayedText;

	public StartScene(EntityMap entities) {
		super(StartSceneState.class, TransitionMatchStrategy.BY_VALUE);
		ent = entities;
		ent.store("title", new ImageWidget(Assets.image("title")));
		ent.store("text_game_over", new ImageWidget(Assets.image("text_game_over")));
		ent.store("text_ready", PumpingImageWidget.create().image(Assets.image("text_ready")).build());
		buildStateMachine();
	}

	private void buildStateMachine() {
		setMissingTransitionBehavior(MissingTransitionBehavior.LOG);
		//@formatter:off
		beginStateMachine()
			.description("[Start Scene]")
			.initialState(STARTING)
			
			.states()
			
				.state(STARTING)
					.onEntry(() -> {
						reset();
						if (!sound("music/bgmusic.mp3").isRunning()) {
							sound("music/bgmusic.mp3").loop();
						}
					})
				.onTick(() -> keepBirdInAir())
				
				.state(READY)
					.timeoutAfter(() -> sec(app().settings().getAsFloat("ready-time-sec")))
					.onEntry(() -> {
						displayedText = ent.named("readyText");
					})
					.onExit(() -> {
						displayedText = null;
					})
	
				.state(GAME_OVER)
					.onEntry(() -> {
						stop();
						sounds().forEach(SoundClip::stop);
						displayedText = ent.named("game_over");
					})
	
			.transitions()
	
				.when(STARTING).then(READY)
					.condition(() -> Keyboard.keyDown(app().settings().get("jump-key")))
					
				.when(STARTING).then(GAME_OVER).on(TOUCHED_GROUND)
				
				.when(READY).then(COMPLETE).onTimeout()
					.act(() -> setScene(Scene.PLAY_SCENE))
				
				.when(READY).then(GAME_OVER).on(TOUCHED_GROUND)
					.act(e -> {
						displayedText = ent.named("title");
					})
				
				.when(GAME_OVER).then(STARTING)
					.condition(() -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE))
			
		.endStateMachine();
		//@formatter:on
	}

	private void reset() {
		int w = app().settings().width, h = app().settings().height;
		displayedText = ent.named("title");
		City city = ent.named("city");
		city.setWidth(w);
		city.init();
		Ground ground = ent.named("ground");
		ground.setWidth(w);
		ground.tf.setPosition(0, h - ground.tf.height);
		ground.tf.setVelocity(app().settings().getAsFloat("world-speed"), 0);
		Bird bird = ent.named("bird");
		bird.init();
		bird.tf.setPosition(w / 8, ground.tf.y / 2);
		bird.tf.setVelocity(0, 0);
		app().collisionHandler().ifPresent(collisions -> {
			collisions.clear();
			collisions.registerEnd(bird, ent.named("world"), LEFT_WORLD);
			collisions.registerStart(bird, ground, TOUCHED_GROUND);
		});
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce("s")) {
			boolean showState = app().settings().getAsBoolean("show-state");
			app().settings().set("show-state", !showState);
		}
		checkCollisions();
		ent.implementing(Lifecycle.class).forEach(Lifecycle::update);
		super.update();
	}

	private void checkCollisions() {
		Bird bird = ent.named("bird");
		app().collisionHandler().ifPresent(handler -> {
			for (Collision c : handler.collisions()) {
				BirdEvent event = (BirdEvent) c.getAppEvent();
				bird.dispatch(event);
				enqueue(event);
			}
		});
	}

	@Override
	public void stop() {
		Ground ground = ent.named("ground");
		ground.tf.setVelocity(0, 0);
	}

	@Override
	public void draw(Graphics2D g) {
		int w = app().settings().width, h = app().settings().height;
		Bird bird = ent.named("bird");
		City city = ent.named("city");
		Ground ground = ent.named("ground");

		city.draw(g);
		ground.draw(g);
		bird.draw(g);
		if (displayedText != null) {
			displayedText.tf.centerBoth(0, 0, w, h - ground.tf.height);
			displayedText.draw(g);
		}
		if (app().settings().getAsBoolean("show-state")) {
			String text = String.format("%s: %s,  Bird: %s and %s", getDescription(), getState(), bird.getFlightState(),
					bird.getHealthState());
			g.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
			g.drawString(text, 20, app().settings().height - 20);
		}
	}

	private void keepBirdInAir() {
		Bird bird = ent.named("bird");
		Ground ground = ent.named("ground");
		while (bird.tf.y > ground.tf.y / 2) {
			bird.flap(randomInt(1, 4));
		}
	}
}