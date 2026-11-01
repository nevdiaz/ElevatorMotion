package manager;

import api.Direction;
import sim.MotionEngine;



public class SimulationManager {

    private final TripController tripController;
    private final Runnable tick;
    private long lastFramesStartTime = -1;
    private double accumulator = 0.0;
    private double timeScale = 1.0;
    private static final double longestCatchUp = 0.25;
    private Runnable beforeStep = () -> { };

    public SimulationManager(TripController tripController, Runnable tick) {
        this.tripController = tripController;
        this.tick = tick;
    }

    public void setBeforeStep(Runnable beforeStep) {
        if (beforeStep == null) {
            throw new IllegalArgumentException("beforeStep must not be null");
        }
        this.beforeStep = beforeStep;
    }

    public void update(long currentNanos) {
        if (lastFramesStartTime == -1) {
            lastFramesStartTime = currentNanos;
            return;
        }
        double frameSeconds = (currentNanos - lastFramesStartTime)/1000000000.0;
        lastFramesStartTime = currentNanos;
        accumulator += frameSeconds * timeScale;
        if (accumulator > longestCatchUp) {
            accumulator = longestCatchUp;
        }
        while (accumulator >= MotionEngine.TIME_STEP) {
            tripController.controlStep();
            tick.run();
            accumulator -= MotionEngine.TIME_STEP;
        }
    }

    public void setTimeScale(double timeScale) {
        this.timeScale = timeScale;
    }

    public void goToFloor(int floor) {
        tripController.goToFloor(floor);
    }

    public boolean isTripActive() {
        return tripController.isTripActive();
    }
}
