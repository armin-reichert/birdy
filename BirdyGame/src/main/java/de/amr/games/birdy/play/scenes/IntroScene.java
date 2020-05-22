package de.amr.games.birdy.play.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.play.scenes.IntroScene.State.COMPLETE;
import static de.amr.games.birdy.play.scenes.IntroScene.State.CREDITS;
import static de.amr.games.birdy.play.scenes.IntroScene.State.LOGO;
import static de.amr.games.birdy.play.scenes.IntroScene.State.WAITING;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

import de.amr.easy.game.Application;
import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.assets.Sound;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.ui.widgets.PumpingImageWidget;
import de.amr.easy.game.ui.widgets.TextWidget;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp;
import de.amr.games.birdy.BirdyGameApp.Scene;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.play.scenes.IntroScene.State;
import de.amr.statemachine.api.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Intro scene.
 * 
 * @author Armin Reichert
 */
public class IntroScene extends StateMachine<State, Void> implements View, Lifecycle {

	public enum State {
		CREDITS, WAITING, LOGO, COMPLETE
	}

	private static final String CREDITS_TEXT = String.join("\n",
	/*@formatter:off*/
			"Anna proudly presents", 
			"in cooperation with",
			"Prof. Zwickmann", 
			"Geräteschuppen Software 2017"
	/*@formatter:on*/
	);

	private City city;
	private PumpingImageWidget logoImage;
	private TextWidget creditsText;

	public IntroScene(BirdyGameApp app) {
		super(State.class, EventMatchStrategy.BY_EQUALITY);
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
						.onExit(() -> BirdyGameApp.setScene(Scene.START))
						
				.transitions()
					.when(CREDITS).then(WAITING).condition(() -> creditsText.isComplete())
					.when(WAITING).then(LOGO).onTimeout()
					.when(LOGO).then(COMPLETE).onTimeout()
				
		.endStateMachine();
		/*@formatter:on*/
		getTracer().setLogger(Application.LOGGER);
	}

	private IntSupplier sec(float amount) {
		return () -> app().clock().sec(amount);
	}

	@Override
	public void init() {
		int width = app().settings().width, height = app().settings().height;

		city = new City();
		city.setWidth(width);
		if (new Random().nextBoolean()) {
			city.sunset();
		} else {
			city.sunrise();
		}

		creditsText = TextWidget.create().text(CREDITS_TEXT).font(Assets.font("Pacifico-Regular"))
				.color(city.isNight() ? Color.WHITE : Color.DARK_GRAY).build();
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
		Stream.of(city, logoImage, creditsText).forEach(e -> e.draw(g));
	}
}