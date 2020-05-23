package de.amr.games.birdy.entities;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.BirdyGameApp.sec;
import static de.amr.games.birdy.entities.ObstacleControllerState.BREEDING;
import static de.amr.games.birdy.entities.ObstacleControllerState.GIVING_BIRTH;
import static de.amr.games.birdy.entities.ObstacleControllerState.STOPPED;
import static de.amr.games.birdy.entities.bird.BirdEvent.LEFT_PASSAGE;
import static de.amr.games.birdy.entities.bird.BirdEvent.TOUCHED_PIPE;
import static de.amr.games.birdy.utils.Util.randomInt;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.statemachine.api.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Manages the creation and deletion of obstacles.
 * 
 * @author Armin Reichert
 */
public class ObstacleController extends StateMachine<ObstacleControllerState, String> implements Lifecycle {

	private final BirdyGameEntities ent;
	public final List<Obstacle> obstacles = new LinkedList<>();

	public ObstacleController(BirdyGameEntities entities) {
		super(ObstacleControllerState.class, EventMatchStrategy.BY_EQUALITY);
		ent = entities;
		buildStateMachine();
	}

	private void buildStateMachine() {
		setDescription("[ObstacleController]");
		setInitialState(STOPPED);

		// On "Start" event, become breeding:
		addTransitionOnEventObject(STOPPED, BREEDING, null, null, "Start");

		// Stay breeding for some random time from interval [MIN_PIPE_TIME, MAX_PIPE_TIME]
		state(BREEDING).setTimer(() -> {
			int minCreationTime = sec(app().settings().getAsFloat("min-pipe-creation-sec"));
			int maxCreationTime = sec(app().settings().getAsFloat("max-pipe-creation-sec"));
			return randomInt(minCreationTime, maxCreationTime);
		});

		// Update (move) pipes during breeding:
		state(BREEDING).setOnTick(() -> obstacles.forEach(Obstacle::update));

		// On "Stop" event, enter "Stopped" state:
		addTransitionOnEventObject(BREEDING, STOPPED, null, null, "Stop");

		// When breeding time is over, it's birthday:
		addTransitionOnTimeout(BREEDING, GIVING_BIRTH, null, null);

		// On birth, update (add/remove) obstacles:
		state(GIVING_BIRTH).setOnEntry(this::addAndRemoveObstacles);

		// And immediately become breeding again
		addTransition(GIVING_BIRTH, BREEDING, null, null);

		addTransitionOnEventObject(GIVING_BIRTH, STOPPED, null, null, "Stop");
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

	private void addAndRemoveObstacles() {
		// Add new obstacle
		int minHeight = app().settings().get("min-pipe-height");
		int passageHeight = app().settings().get("passage-height");
		int width = app().settings().get("pipe-width");
		int height = app().settings().get("pipe-height");
		int passageCenterY = randomInt(minHeight + passageHeight / 2,
				(int) ent.theGround().tf.y - minHeight - passageHeight / 2);

		Obstacle obstacle = new Obstacle(width, height, passageHeight, passageCenterY);
		obstacle.tf.vx = app().settings().get("world-speed");
		obstacle.tf.x = app().settings().width;
		obstacle.setLighted(ent.theCity().isNight() && randomInt(0, 4) == 0);
		obstacles.add(obstacle);

		app().collisionHandler().registerStart(ent.theBird(), obstacle.getUpperPart(), TOUCHED_PIPE);
		app().collisionHandler().registerStart(ent.theBird(), obstacle.getLowerPart(), TOUCHED_PIPE);
		app().collisionHandler().registerEnd(ent.theBird(), obstacle.getPassage(), LEFT_PASSAGE);

		// Remove obstacles that ran out of screen
		Iterator<Obstacle> it = obstacles.iterator();
		while (it.hasNext()) {
			obstacle = it.next();
			if (obstacle.tf.x + obstacle.tf.width < 0) {
				app().collisionHandler().unregisterStart(ent.theBird(), obstacle.getUpperPart());
				app().collisionHandler().unregisterStart(ent.theBird(), obstacle.getLowerPart());
				app().collisionHandler().unregisterEnd(ent.theBird(), obstacle.getPassage());
				it.remove();
			}
		}
	}
}