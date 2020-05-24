package de.amr.games.birdy.entities;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.EntityMap;
import de.amr.games.birdy.entities.bird.Bird;

public class BirdyGameEntities extends EntityMap {

	public void update() {
		filter(entity -> entity instanceof Lifecycle).forEach(entity -> {
			((Lifecycle) entity).update();
		});
	}

	public Bird theBird() {
		return ofName("bird");
	}

	public City theCity() {
		return ofName("city");
	}

	public Ground theGround() {
		return ofName("ground");
	}
}