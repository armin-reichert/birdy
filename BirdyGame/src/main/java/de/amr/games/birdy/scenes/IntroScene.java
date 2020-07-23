package de.amr.games.birdy.scenes;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.BirdyGameApp.sec;
import static de.amr.games.birdy.scenes.IntroScene.IntroSceneState.COMPLETE;
import static de.amr.games.birdy.scenes.IntroScene.IntroSceneState.CREDITS;
import static de.amr.games.birdy.scenes.IntroScene.IntroSceneState.LOGO;
import static de.amr.games.birdy.scenes.IntroScene.IntroSceneState.WAITING;

import java.awt.Color;
import java.awt.Graphics2D;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.assets.SoundClip;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.EntityMap;
import de.amr.easy.game.ui.widgets.PumpingImageWidget;
import de.amr.easy.game.ui.widgets.TextWidget;
import de.amr.easy.game.view.View;
import de.amr.games.birdy.BirdyGameApp;
import de.amr.games.birdy.BirdyGameApp.Scene;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.City.DayTime;
import de.amr.games.birdy.scenes.IntroScene.IntroSceneState;
import de.amr.statemachine.api.TransitionMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Intro scene. Show a scrolling text and a flashing logo before switching to the start scene.
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
	private PumpingImageWidget flashingLogo;
	private TextWidget scrollingText;

	public IntroScene(EntityMap entities) {
		super(IntroSceneState.class, TransitionMatchStrategy.BY_VALUE);
		ent = entities;
		scrollingText = TextWidget.create().text(CREDITS_TEXT).font(Assets.font("Pacifico-Regular"))
				.color(BirdyGameApp.getDayTime() == DayTime.NIGHT ? Color.WHITE : new Color(50, 50, 255)).build();
		flashingLogo = PumpingImageWidget.create().image(Assets.image("title")).scale(3).build();
		/*@formatter:off*/
		beginStateMachine()
				.description("[Intro Scene]")
				.initialState(CREDITS)
				.states()

					.state(CREDITS)
						.onEntry(() -> scrollingText.start())
						.onTick(() -> scrollingText.update())

					.state(WAITING)
						.timeoutAfter(sec(2))
						.onExit(() -> scrollingText.visible = false)
						
					.state(LOGO)
						.timeoutAfter(sec(4)) 
						.onEntry(() -> flashingLogo.visible = true)
						.onExit(() -> BirdyGameApp.setScene(Scene.START_SCENE))
						
				.transitions()
					.when(CREDITS).then(WAITING).condition(scrollingText::isComplete)
					.when(WAITING).then(LOGO).onTimeout()
					.when(LOGO).then(COMPLETE).onTimeout()
				
		.endStateMachine();
		/*@formatter:on*/
	}

	@Override
	public void init() {
		int width = app().settings().width, height = app().settings().height;

		City city = ent.named("city");
		city.setWidth(width);

		scrollingText.tf.centerHorizontally(0, width);
		scrollingText.tf.y = height;
		scrollingText.tf.vy = -1.5f;
		scrollingText.setCompletion(() -> scrollingText.tf.y < height / 4);

		flashingLogo.tf.centerBoth(0, 0, width, height);
		flashingLogo.visible = false;

		SoundClip music = Assets.sound("music/bgmusic.mp3");
		music.volume(0.9f);
		music.loop();

		super.init();
	}

	@Override
	public void draw(Graphics2D g) {
		City city = ent.named("city");
		city.draw(g);
		scrollingText.draw(g);
		flashingLogo.draw(g);
	}
}