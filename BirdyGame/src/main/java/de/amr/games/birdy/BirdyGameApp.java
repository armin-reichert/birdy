package de.amr.games.birdy;

import static de.amr.easy.game.assets.Assets.sound;
import static de.amr.easy.game.assets.Assets.storeTrueTypeFont;
import static de.amr.games.birdy.browser.SpritesheetReader.extractSpriteSheet;

import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Random;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.EntityMap;
import de.amr.games.birdy.entities.City;
import de.amr.games.birdy.entities.City.DayTime;
import de.amr.games.birdy.entities.Ground;
import de.amr.games.birdy.entities.bird.Bird;
import de.amr.games.birdy.scenes.IntroScene;
import de.amr.games.birdy.scenes.PlayScene;
import de.amr.games.birdy.scenes.StartScene;

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
		INTRO_SCENE, START_SCENE, PLAY_SCENE
	};

	private static EnumMap<Scene, Lifecycle> scenes = new EnumMap<>(Scene.class);

	public static void setScene(Scene scene) {
		app().setController(scenes.get(scene));
	}

	public static int sec(float amount) {
		return app().clock().sec(amount);
	}

	/**
	 * @param min lower bound (inclusive)
	 * @param max upper bound (inclusive)
	 * @return random integer from given closed interval
	 */
	public static int randomInt(int min, int max) {
		return min + new Random().nextInt(max - min + 1);
	}

	@Override
	protected void configure(AppSettings settings) {
		// general settings
		settings.title = "Zwick, das listige Vögelchen";
		settings.width = 640;
		settings.height = 480;
		settings.fullScreenMode = new DisplayMode(640, 480, 32, DisplayMode.REFRESH_RATE_UNKNOWN);
		settings.fullScreenOnStart = false;

		// specific settings
		settings.set("jump-key", KeyEvent.VK_UP);
		settings.set("world-gravity", 0.4f);
		settings.set("world-speed", -2.5f);
		settings.set("ready-time-sec", 2f);
		settings.set("max-stars", 5);
		settings.set("bird-flap-millis", 50);
		settings.set("bird-injured-seconds", 1f);
		settings.set("min-pipe-creation-sec", 1f);
		settings.set("max-pipe-creation-sec", 5f);
		settings.set("pipe-height", 480 - 112);
		settings.set("pipe-width", 52);
		settings.set("min-pipe-height", 100);
		settings.set("passage-height", 100);
		settings.set("show-state", false);
	}

	@Override
	public void init() {
		extractSpriteSheet();
		sound("music/bgmusic.mp3").volume(0.5f);
		storeTrueTypeFont("Pacifico-Regular", "fonts/Pacifico-Regular.ttf", Font.BOLD, 40);
		EntityMap entities = new EntityMap();
		int hour = ZonedDateTime.now().getHour(); // 0-23
		DayTime dayTime = hour > 5 && hour < 21 ? DayTime.DAY : DayTime.NIGHT;
		loginfo("Its %s now", dayTime);
		entities.store("city", new City(entities, dayTime));
		entities.store("ground", new Ground());
		entities.store("bird", new Bird());
		scenes.put(Scene.INTRO_SCENE, new IntroScene(entities));
		scenes.put(Scene.START_SCENE, new StartScene(entities));
		scenes.put(Scene.PLAY_SCENE, new PlayScene(entities));
		setScene(Scene.INTRO_SCENE);
	}
}