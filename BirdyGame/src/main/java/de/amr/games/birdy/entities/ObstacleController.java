package de.amr.games.birdy.entities;

import static de.amr.easy.game.Application.app;
import static de.amr.games.birdy.entities.ObstacleControllerState.EMITTING;
import static de.amr.games.birdy.entities.ObstacleControllerState.BREEDING;
import static de.amr.games.birdy.entities.ObstacleControllerState.STOPPED;
import static de.amr.games.birdy.entities.bird.BirdEvent.LEFT_PASSAGE;
import static de.amr.games.birdy.entities.bird.BirdEvent.TOUCHED_PIPE;
import static de.amr.games.birdy.utils.Util.randomInt;

import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import de.amr.easy.game.controller.Lifecycle;
import de.amr.easy.game.entity.Entity;
import de.amr.easy.game.view.View;
import de.amr.statemachine.api.EventMatchStrategy;
import de.amr.statemachine.core.StateMachine;

/**
 * Manages the creation and deletion of obstacles.
 * 
 * @author Armin Reichert
 */
public class ObstacleController extends Entity implements Lifecycle, View {

	private final BirdyGameEntities ent;
	private final List<Obstacle> obstacles = new LinkedList<>();
	private final StateMachine<ObstacleControllerState, String> control;

	public ObstacleController(BirdyGameEntities entities) {
		ent = entities;

		control = new StateMachine<>(ObstacleControllerState.class, EventMatchStrategy.BY_EQUALITY);
		control.setDescription(getClass().getSimpleName());
		control.setInitialState(STOPPED);

		// Stay breeding for some random time from interval [MIN_PIPE_TIME,
		// MAX_PIPE_TIME]:
		control.state(BREEDING).setTimer(() -> {
			int minCreationTime = app().clock().sec(app().settings().getAsFloat("min-pipe-creation-sec"));
			int maxCreationTime = app().clock().sec(app().settings().getAsFloat("max-pipe-creation-sec"));
			return randomInt(minCreationTime, maxCreationTime);
		});

		// Update (move) pipes during breeding:
		control.state(BREEDING).setOnTick(() -> obstacles.forEach(Obstacle::update));

		// When breeding time is over, it's birthday:
		control.addTransitionOnTimeout(BREEDING, EMITTING, null, null);

		// On birthday, update (add/remove) obstacles:
		control.state(EMITTING).setOnEntry(() -> updateObstacles());

		// And immediately become breeding again, like the M-people
		control.addTransition(EMITTING, BREEDING, null, null);

		// On "Stop" event, enter "Stopped" state:
		control.addTransitionOnEventObject(BREEDING, STOPPED, null, null, "Stop");
		control.addTransitionOnEventObject(EMITTING, STOPPED, null, null, "Stop");

		// On "Start" event, become breeding again:
		control.addTransitionOnEventObject(STOPPED, BREEDING, null, null, "Start");
	}

	public void setLogger(Logger log) {
		control.getTracer().setLogger(log);
	}

	@Override
	public void init() {
		obstacles.clear();
		control.init();
	}

	@Override
	public void start() {
		control.enqueue("Start");
		control.update();
	}

	@Override
	public void stop() {
		control.enqueue("Stop");
		control.update();
	}

	@Override
	public void update() {
		control.update();
	}

	@Override
	public void draw(Graphics2D g) {
		obstacles.forEach(o -> o.draw(g));
	}

	private void updateObstacles() {
		// Add new obstacle
		int minHeight = app().settings().get("min-pipe-height");
		int passageHeight = app().settings().get("passage-height");
		int width = app().settings().get("pipe-width");
		int height = app().settings().get("pipe-height");
		int passageCenterY = randomInt(minHeight + passageHeight / 2,
				(int) ent.theGround().tf.y - minHeight - passageHeight / 2);
		float speed = app().settings().get("world-speed");

		Obstacle obstacle = new Obstacle(width, height, passageHeight, passageCenterY);
		obstacle.tf.vx = speed;
		obstacle.tf.x = app().settings().width;
		obstacle.setLighted(ent.theCity().isNight() && randomInt(0, 5) == 0);
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