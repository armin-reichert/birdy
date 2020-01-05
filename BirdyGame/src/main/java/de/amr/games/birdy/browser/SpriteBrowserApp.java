package de.amr.games.birdy.browser;

import de.amr.easy.game.Application;
import de.amr.games.birdy.utils.SpritesheetReader;

public class SpriteBrowserApp extends Application {

	public static void main(String[] args) {
		launch(new SpriteBrowserApp(), args);
	}

	public SpriteBrowserApp() {
		settings().title = "Birdy Sprites";
		settings().width = 1024;
		settings().height = 1024;
		clock().setFrequency(10);
	}

	@Override
	public void init() {
		SpritesheetReader.extractSpriteSheet();
		setController(new SpriteBrowserScene(settings().width, settings().height));
	}
}
