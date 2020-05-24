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
import java.util.LinkedList;
import java.util.List;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.EntityMap;
import de.amr.games.birdy.entities.ObstacleController.Phase;
import de.amr.statemachine.api.EventMatchStrategy;
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
	public final List<Obstacle> obstacles = new LinkedList<>();

	public ObstacleController(EntityMap entities) {
		super(Phase.class, EventMatchStrategy.BY_EQUALITY);
		ent = entities;
		//@formatter:off
		beginStateMachine()
			.description("[ObstacleController]")
			.initialState(STOPPED)
			
			.states()

				.state(BREEDING)
					.timeoutAfter(this::breedingTime)
					.onTick(() -> obstacles.forEach(Obstacle::update))

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
		obstacles.clear();
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
		int minHeight = app().settings().get("min-pipe-height");
		int passageHeight = app().settings().get("passage-height");
		int width = app().settings().get("pipe-width");
		int height = app().settings().get("pipe-height");
		int passageCenterY = randomInt(minHeight + passageHeight / 2, (int) ground.tf.y - minHeight - passageHeight / 2);

		Obstacle obstacle = new Obstacle(width, height, passageHeight, passageCenterY);
		obstacle.tf.vx = app().settings().get("world-speed");
		obstacle.tf.x = app().settings().width;
		obstacle.setLighted(city.isNight() && randomInt(0, 4) == 0);
		obstacles.add(obstacle);

		app().collisionHandler().registerStart(bird, obstacle.getUpperPart(), TOUCHED_PIPE);
		app().collisionHandler().registerStart(bird, obstacle.getLowerPart(), TOUCHED_PIPE);
		app().collisionHandler().registerEnd(bird, obstacle.getPassage(), PASSED_OBSTACLE);

		// Remove obstacles that ran out of screen
		Iterator<Obstacle> it = obstacles.iterator();
		while (it.hasNext()) {
			obstacle = it.next();
			if (obstacle.tf.x + obstacle.tf.width < 0) {
				app().collisionHandler().unregisterStart(bird, obstacle.getUpperPart());
				app().collisionHandler().unregisterStart(bird, obstacle.getLowerPart());
				app().collisionHandler().unregisterEnd(bird, obstacle.getPassage());
				it.remove();
			}
		}
	}
}