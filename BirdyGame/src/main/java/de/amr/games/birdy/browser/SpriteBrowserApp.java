package de.amr.games.birdy.browser;

import de.amr.easy.game.Application;
import de.amr.easy.game.config.AppSettings;
import de.amr.games.birdy.utils.SpritesheetReader;

public class SpriteBrowserApp extends Application {

	public static void main(String[] args) {
		launch(SpriteBrowserApp.class, args);
	}

	@Override
	protected void configure(AppSettings settings) {
		settings.title = "Birdy Sprites";
		settings.width = 1024;
		settings.height = 1024;
	}

	@Override
	public void init() {
		SpritesheetReader.extractSpriteSheet();
		setController(new SpriteBrowserScene(settings().width, settings().height));
		clock().setFrequency(10);
	}
}
