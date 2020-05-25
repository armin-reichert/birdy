package de.amr.games.birdy.sprites;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.amr.easy.game.assets.Assets;
import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.input.Keyboard;
import de.amr.easy.game.view.View;

public class SpriteBrowserScene implements Lifecycle, View {

	private final int height;
	private final Font font = new Font("Courier New", Font.PLAIN, 16);
	private final List<String> spriteNames;
	private int index;

	public SpriteBrowserScene(int width, int height) {
		this.height = height;
		spriteNames = new ArrayList<>();
		for (String name : Assets.imageNames()) {
			spriteNames.add(name);
		}
		Collections.sort(spriteNames);
	}

	@Override
	public void init() {
	}

	@Override
	public void update() {
		if (Keyboard.keyPressedOnce(KeyEvent.VK_RIGHT)) {
			index = index + 1 == spriteNames.size() ? 0 : index + 1;
		} else if (Keyboard.keyPressedOnce(KeyEvent.VK_LEFT)) {
			index = index == 0 ? spriteNames.size() - 1 : index - 1;
		}
	}

	@Override
	public void draw(Graphics2D g) {
		if (spriteNames.isEmpty()) {
			return;
		}
		String name = spriteNames.get(index);
		BufferedImage image = Assets.image(name);
		String text = name + " (" + image.getWidth() + "x" + image.getHeight() + ")";
		text += " (Keys: LEFT=Previous, RIGHT=Next, X=Exit)";
		g.setColor(Color.WHITE);
		g.setFont(font);
		g.drawImage(image, 0, 0, null);
		g.drawString(text, 0, height - 2 * font.getSize());
	}
}