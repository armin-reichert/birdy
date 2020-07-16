package de.amr.games.birdy.entities;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.BirdyGameApp.randomInt;
import static de.amr.games.birdy.BirdyGameApp.sec;
import static de.amr.games.birdy.entities.BirdEvent.PASSED_OBSTACLE;
import static de.amr.games.birdy.entities.BirdEvent.TOUCHED_PIPE;
import static de.amr.games.birdy.entities.ObstacleController.Phase.BREEDING;
import static de.amr.games.birdy.entities.ObstacleController.Phase.GIVING_BIRTH;
import static de.amr.games.birdy.entities.ObstacleController.Phase.STOPPED;

import java.util.Iterator;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.EntityMap;
import de.amr.games.birdy.entities.ObstacleController.Phase;
import de.amr.statemachine.api.TransitionMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Manages the creation and deletion of obstacles.
 * 
 * @author Armin Reichert
 */
public class ObstacleController extends StateMachine<Phase, String> implements Lifecycle {

	public enum Phase {
		STOPPED, BREEDING, GIVING_BIRTH
	}

	private final EntityMap ent;

	public ObstacleController(EntityMap entities) {
		super(Phase.class, TransitionMatchStrategy.BY_VALUE);
		ent = entities;
		//@formatter:off
		beginStateMachine()
			.description("[ObstacleController]")
			.initialState(STOPPED)
			
			.states()

				.state(BREEDING)
					.timeoutAfter(this::breedingTime)

				.state(GIVING_BIRTH)
					.onEntry(this::updateObstacleList)
			
			.transitions()
			
				.when(STOPPED).then(BREEDING).on("Start")
				.when(BREEDING).then(STOPPED).on("Stop")
				.when(BREEDING).then(GIVING_BIRTH).onTimeout()
				.when(GIVING_BIRTH).then(BREEDING)
				.when(GIVING_BIRTH).then(STOPPED).on("Stop")
				
		.endStateMachine();
		//@formatter:on
	}

	@Override
	public void init() {
		ent.removeAll(Obstacle.class);
		super.init();
	}

	@Override
	public void start() {
		process("Start");
	}

	@Override
	public void stop() {
		process("Stop");
	}

	private int breedingTime() {
		int min = sec(app().settings().getAsFloat("min-pipe-creation-sec"));
		int max = sec(app().settings().getAsFloat("max-pipe-creation-sec"));
		return randomInt(min, max);
	}

	private void updateObstacleList() {
		Bird bird = ent.named("bird");
		City city = ent.named("city");
		Ground ground = ent.named("ground");

		// Add new obstacle
		int minHeight = app().settings().get("min-obstacle-height");
		int passageHeight = app().settings().get("passage-height");
		int passageCenterY = randomInt(minHeight + passageHeight / 2, (int) ground.tf.y - minHeight - passageHeight / 2);

		Obstacle newObstacle = new Obstacle(passageHeight / 2, passageCenterY);
		newObstacle.tf.x = app().settings().width;
		newObstacle.tf.vx = app().settings().getAsFloat("world-speed");
		newObstacle.illuminated = city.isNight() && randomInt(0, 100) == 20;
		ent.store(newObstacle);

		app().collisionHandler().ifPresent(handler -> {
			handler.registerStart(bird, newObstacle.getUpperPart(), TOUCHED_PIPE);
			handler.registerStart(bird, newObstacle.getLowerPart(), TOUCHED_PIPE);
			handler.registerEnd(bird, newObstacle.getPassage(), PASSED_OBSTACLE);
		});

		// Remove obstacles that ran out of screen
		Iterator<Obstacle> obstacles = ent.ofClass(Obstacle.class).iterator();
		while (obstacles.hasNext()) {
			Obstacle nextObstacle = obstacles.next();
			if (nextObstacle.tf.x + nextObstacle.tf.width < 0) {
				app().collisionHandler().ifPresent(handler -> {
					handler.unregisterStart(bird, nextObstacle.getUpperPart());
					handler.unregisterStart(bird, nextObstacle.getLowerPart());
					handler.unregisterEnd(bird, nextObstacle.getPassage());
				});
				ent.removeEntity(nextObstacle);
			}
		}
	}
}