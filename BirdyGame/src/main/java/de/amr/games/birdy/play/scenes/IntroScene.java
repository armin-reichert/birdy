package de.amr.games.birdy.play.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.BirdyGameApp.sec;
import static de.amr.games.birdy.play.scenes.IntroScene.IntroSceneState.COMPLETE;
import static de.amr.games.birdy.play.scenes.IntroScene.IntroSceneState.CREDITS;
import static de.amr.games.birdy.play.scenes.IntroScene.IntroSceneState.LOGO;
import static de.amr.games.birdy.play.scenes.IntroScene.IntroSceneState.WAITING;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

import de.amr.easy.game.Application;
import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.assets.Sound;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.ui.widgets.PumpingImageWidget;
import de.amr.easy.game.ui.widgets.TextWidget;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp;
import de.amr.games.birdy.BirdyGameApp.Scene;
import de.amr.games.birdy.entities.BirdyGameEntities;
import de.amr.games.birdy.play.scenes.IntroScene.IntroSceneState;
import de.amr.statemachine.api.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Intro scene.
 * 
 * @author Armin Reichert
 */
public class IntroScene extends StateMachine<IntroSceneState, Void> implements View, Lifecycle {

	public enum IntroSceneState {
		CREDITS, WAITING, LOGO, COMPLETE
	}

	static final String CREDITS_TEXT = String.join("\n",
	/*@formatter:off*/
		"Anna proudly presents", 
		"in cooperation with",
		"Prof. Zwickmann", 
		"Geräteschuppen Software, 2017"
	/*@formatter:on*/
	);

	private BirdyGameEntities ent;
	private PumpingImageWidget logoImage;
	private TextWidget creditsText;

	public IntroScene(BirdyGameEntities entities) {
		super(IntroSceneState.class, EventMatchStrategy.BY_EQUALITY);
		ent = entities;
		/*@formatter:off*/
		beginStateMachine()
				.description("Intro Scene")
				.initialState(CREDITS)
				.states()

					.state(CREDITS)
						.onEntry(() -> creditsText.start())
						.onTick(() -> creditsText.update())
						.onExit(() -> creditsText.stop())

					.state(WAITING)
						.timeoutAfter(sec(2))
						.onExit(() -> creditsText.visible = false)
						
					.state(LOGO)
						.timeoutAfter(sec(4)) 
						.onEntry(() -> logoImage.visible = true)
						.onExit(() -> BirdyGameApp.setScene(Scene.START_SCENE))
						
				.transitions()
					.when(CREDITS).then(WAITING).condition(() -> creditsText.isComplete())
					.when(WAITING).then(LOGO).onTimeout()
					.when(LOGO).then(COMPLETE).onTimeout()
				
		.endStateMachine();
		/*@formatter:on*/
		getTracer().setLogger(Application.LOGGER);
	}

	@Override
	public void init() {
		int width = app().settings().width, height = app().settings().height;
		ent.theCity().setWidth(width);
		if (new Random().nextBoolean()) {
			ent.theCity().sunset();
		} else {
			ent.theCity().sunrise();
		}

		creditsText = TextWidget.create().text(CREDITS_TEXT).font(Assets.font("Pacifico-Regular"))
				.color(ent.theCity().isNight() ? Color.WHITE : Color.DARK_GRAY).build();
		creditsText.tf.centerX(width);
		creditsText.tf.y = (height);
		creditsText.tf.vy = -1.5f;
		creditsText.setCompletion(() -> creditsText.tf.y < height / 4);

		logoImage = PumpingImageWidget.create().image(Assets.image("title")).scale(3).build();
		logoImage.tf.center(width, height);
		logoImage.visible = false;

		super.init();
		Sound music = Assets.sound("music/bgmusic.mp3");
		music.volume(0.9f);
		music.loop();
	}

	@Override
	public void draw(Graphics2D g) {
		ent.theCity().draw(g);
		logoImage.draw(g);
		creditsText.draw(g);
	}
}