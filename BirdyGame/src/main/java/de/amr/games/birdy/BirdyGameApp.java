package de.amr.games.birdy;

import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.easy.game.assets.Assets.storeTrueTypeFont;

import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.EnumMap;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.easy.game.controller.Lifecycle;
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
public class BirdyGameApp extends Application {

	public static void main(String[] args) {
		launch(BirdyGameApp.class, args);
	}

	public enum Scene {
		INTRO, START, PLAY
	};

	public static void setScene(Scene scene) {
		BirdyGameApp app = (BirdyGameApp) app();
		app.setController(app.scenes.get(scene));
	}

	public static EntityMap entities() {
		BirdyGameApp app = (BirdyGameApp) app();
		return app.entities;
	}

	private EnumMap<Scene, Lifecycle> scenes = new EnumMap<>(Scene.class);
	private EntityMap entities = new EntityMap();

	@Override
	protected void configure(AppSettings settings) {
		// general settings
		settings.title = "Zwick, das listige Vögelchen";
		settings.width = 640;
		settings.height = 480;
		settings.fullScreenMode = new DisplayMode(640, 480, 32, DisplayMode.REFRESH_RATE_UNKNOWN);
		settings.fullScreenOnStart = false;

		// specific settings
		settings.set("jump key", KeyEvent.VK_UP);
		settings.set("world gravity", 0.4f);
		settings.set("world speed", -2.5f);
		settings.set("ready time sec", 2f);
		settings.set("max stars", 5);
		settings.set("bird flap millis", 50);
		settings.set("bird injured seconds", 1f);
		settings.set("min pipe creation sec", 1f);
		settings.set("max pipe creation sec", 5f);
		settings.set("pipe height", 480 - 112);
		settings.set("pipe width", 52);
		settings.set("min pipe height", 100);
		settings.set("passage height", 100);
		settings.set("show-state", true);
	}

	@Override
	public void init() {
		SpritesheetReader.extractSpriteSheet();
		sound("music/bgmusic.mp3").volume(0.5f);
		storeTrueTypeFont("Pacifico-Regular", "fonts/Pacifico-Regular.ttf", Font.BOLD, 40);
		entities.store(new City());
		entities.store(new Ground());
		entities.store(new Bird());
		scenes.put(Scene.INTRO, new IntroScene());
		scenes.put(Scene.START, new StartScene());
		scenes.put(Scene.PLAY, new PlayScene());
		setScene(Scene.INTRO);
	}
}