package de.amr.games.birdy.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.BirdyGameApp.sec;
import static de.amr.games.birdy.scenes.IntroScene.IntroSceneState.COMPLETE;
import static de.amr.games.birdy.scenes.IntroScene.IntroSceneState.CREDITS;
import static de.amr.games.birdy.scenes.IntroScene.IntroSceneState.LOGO;
import static de.amr.games.birdy.scenes.IntroScene.IntroSceneState.WAITING;

import java.awt.Color;
import java.awt.Graphics2D;

import de.amr.easy.game.Application;
import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.assets.Sound;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.EntityMap;
import de.amr.easy.game.ui.widgets.PumpingImageWidget;
import de.amr.easy.game.ui.widgets.TextWidget;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp;
import de.amr.games.birdy.BirdyGameApp.Scene;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.scenes.IntroScene.IntroSceneState;
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
		"Anna Schillo", 
		"in cooperation with",
		"Geräteschuppen Software",
		"proudly presents"
	/*@formatter:on*/
	);

	private EntityMap ent;
	private PumpingImageWidget logoImage;
	private TextWidget creditsText;

	public IntroScene(EntityMap entities) {
		super(IntroSceneState.class, EventMatchStrategy.BY_EQUALITY);
		ent = entities;
		/*@formatter:off*/
		beginStateMachine()
				.description("[Intro Scene]")
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

		City city = ent.named("city");
		city.setWidth(width);

		creditsText = TextWidget.create().text(CREDITS_TEXT).font(Assets.font("Pacifico-Regular"))
				.color(city.isNight() ? Color.WHITE : new Color(50, 50, 255)).build();
		creditsText.tf.centerX(width);
		creditsText.tf.y = (height);
		creditsText.tf.vy = -1.5f;
		creditsText.setCompletion(() -> creditsText.tf.y < height / 4);

		logoImage = PumpingImageWidget.create().image(Assets.image("title")).scale(3).build();
		logoImage.tf.center(width, height);
		logoImage.visible = false;

		Sound music = Assets.sound("music/bgmusic.mp3");
		music.volume(0.9f);
		music.loop();

		super.init();
	}

	@Override
	public void draw(Graphics2D g) {
		City city = ent.named("city");
		city.draw(g);
		logoImage.draw(g);
		creditsText.draw(g);
	}
}