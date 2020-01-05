package de.amr.games.birdy;

import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.event.KeyEvent;

import de.amr.easy.game.GenericApplication;
import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.entity.EntityMap;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.entities.bird.Bird;
import de.amr.games.birdy.play.scenes.IntroScene;
import de.amr.games.birdy.play.scenes.PlayScene;
import de.amr.games.birdy.play.scenes.StartScene;
import de.amr.games.birdy.utils.SpritesheetReader;

/**
 * "Flappy Bird"-like game.
 * 
 * @author Armin Reichert
 */
public class BirdyGameApp extends GenericApplication {

	public static void main(String[] args) {
		launch(new BirdyGameApp(), args);
	}

	public BirdyGameApp() {
		// general settings
		settings().title = "Zwick, das listige Vögelchen";
		settings().width = 640;
		settings().height = 480;
		settings().fullScreenMode = new DisplayMode(640, 480, 32, DisplayMode.REFRESH_RATE_UNKNOWN);
		settings().fullScreenOnStart = false;

		// specific settings
		settings().set("jump key", KeyEvent.VK_UP);
		settings().set("world gravity", 0.4f);
		settings().set("world speed", -2.5f);
		settings().set("ready time sec", 2f);
		settings().set("max stars", 5);
		settings().set("bird flap millis", 50);
		settings().set("bird injured seconds", 1f);
		settings().set("min pipe creation sec", 1f);
		settings().set("max pipe creation sec", 5f);
		settings().set("pipe height", 480 - 112);
		settings().set("pipe width", 52);
		settings().set("min pipe height", 100);
		settings().set("passage height", 100);
	}

	public static final EntityMap entities = new EntityMap();

	private IntroScene introScene;

	public IntroScene getIntroScene() {
		if (introScene == null) {
			introScene = new IntroScene(this);
		}
		return introScene;
	}

	private StartScene startScene;

	public StartScene getStartScene() {
		if (startScene == null) {
			startScene = new StartScene(this);
		}
		return startScene;
	}

	private PlayScene playScene;

	public PlayScene getPlayScene() {
		if (playScene == null) {
			playScene = new PlayScene(this);
		}
		return playScene;
	}

	@Override
	public void init() {
		SpritesheetReader.extractSpriteSheet();
		Assets.sound("music/bgmusic.mp3").volume(0.5f);
		Assets.storeTrueTypeFont("Pacifico-Regular", "fonts/Pacifico-Regular.ttf", Font.BOLD, 40);

		// create entities shared by different scenes:
		entities.store(new Bird());
		entities.store(new Ground());
		entities.store(new City());

		setController(getIntroScene());
	}
}