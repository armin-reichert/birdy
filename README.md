# birdy
Flappy-Bird like game using finite state machines.

This is just another test application for finite state machines in games. 

## The game control
```java
private class PlaySceneControl extends StateMachine<State, BirdEvent> {

  public PlaySceneControl() {

    super(State.class, Match.BY_EQUALITY);
    setDescription("Play Scene Control");
    setInitialState(Playing);

    state(Playing).setOnEntry(() -> {
      score.reset();
      obstacleManager.init();
      start();
    });

    addTransitionOnEventObject(Playing, Playing, () -> score.points > 3, e -> {
      score.points -= 3;
      bird.tf.setX(bird.tf.getX() + app.settings.getAsInt("pipe width") + bird.tf.getWidth());
      bird.receiveEvent(BirdTouchedPipe);
      Assets.sound("sfx/hit.mp3").play();
    }, BirdTouchedPipe);

    addTransitionOnEventObject(Playing, GameOver, () -> score.points <= 3, t -> {
      bird.receiveEvent(BirdCrashed);
      Assets.sound("sfx/hit.mp3").play();
    }, BirdTouchedPipe);

    addTransitionOnEventObject(Playing, Playing, null, e -> {
      score.points++;
      Assets.sound("sfx/point.mp3").play();
    }, BirdLeftPassage);

    addTransitionOnEventObject(Playing, GameOver, null, e -> {
      bird.receiveEvent(BirdTouchedGround);
      Assets.sound("music/bgmusic.mp3").stop();
    }, BirdTouchedGround);

    addTransitionOnEventObject(Playing, GameOver, null, e -> {
      bird.receiveEvent(BirdLeftWorld);
      Assets.sound("music/bgmusic.mp3").stop();
    }, BirdLeftWorld);

    state(GameOver).setOnEntry(() -> stop());

    addTransition(GameOver, StartingNewGame, () -> Keyboard.keyPressedOnce(KeyEvent.VK_SPACE), null);

    addTransitionOnEventObject(GameOver, GameOver, null, null, BirdTouchedPipe);
    addTransitionOnEventObject(GameOver, GameOver, null, null, BirdLeftPassage);
    addTransitionOnEventObject(GameOver, GameOver, null, e -> Assets.sound("music/bgmusic.mp3").stop(),
        BirdTouchedGround);

    state(StartingNewGame).setOnEntry(() -> app.setController(app.getStartScene()));
  }
}

```

## The bird's flight control

```java
private class FlightControl extends StateMachine<FlightState, BirdEvent> {

  public FlightControl() {

    super(FlightState.class, Match.BY_EQUALITY);
    setDescription("Bird Flight Control");
    setInitialState(Flying);

    state(Flying).setOnTick(() -> {
      if (Keyboard.keyDown(app().settings.get("jump key"))) {
        flap();
      } else {
        fly();
      }
    });

    addTransitionOnEventObject(Flying, Flying, null, null, BirdLeftPassage);
    addTransitionOnEventObject(Flying, Crashing, null, null, BirdTouchedPipe);
    addTransitionOnEventObject(Flying, Crashing, null, null, BirdCrashed);
    addTransitionOnEventObject(Flying, Crashing, null, null, BirdLeftWorld);
    addTransitionOnEventObject(Flying, OnGround, null, null, BirdTouchedGround);

    state(Crashing).setOnEntry(() -> turnDown());
    state(Crashing).setOnTick(() -> fall(3));

    addTransitionOnEventObject(Crashing, Crashing, null, null, BirdCrashed);
    addTransitionOnEventObject(Crashing, Crashing, null, null, BirdLeftPassage);
    addTransitionOnEventObject(Crashing, Crashing, null, null, BirdTouchedPipe);
    addTransitionOnEventObject(Crashing, OnGround, null, null, BirdTouchedGround);

    state(OnGround).setOnEntry(() -> {
      Assets.sound("sfx/die.mp3").play();
      turnDown();
    });

    addTransitionOnEventObject(OnGround, OnGround, null, null, BirdTouchedGround);
  }
}

```


