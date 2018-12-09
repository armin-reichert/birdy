package de.amr.games.birdy.utils;

import java.awt.image.BufferedImage;

import de.amr.easy.game.assets.Assets;

public class SpritesheetReader {

	public static void extractSpriteSheet() {
		BufferedImage atlas = Assets.image("spritesheet/atlas.png");
		String[] map = Assets.readTextFile("spritesheet/atlas.txt").split("\\s+");
		int scale = 1024;
		for (int i = 0; i < map.length;) {
			String spriteName = map[i++];
			i += 2; // skip next 2 tokens
			int x = Math.round(Float.parseFloat(map[i++]) * scale);
			int y = Math.round(Float.parseFloat(map[i++]) * scale);
			int width = Math.round(Float.parseFloat(map[i++]) * scale);
			int height = Math.round(Float.parseFloat(map[i++]) * scale);
			Assets.storeImage(spriteName, atlas.getSubimage(x, y, width, height));
		}
	}
}
