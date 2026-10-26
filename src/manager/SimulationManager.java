package manager;

import sim.MotionEngine;



public class SimulationManager {

    private final MotionEngine engine;
    private long lastFramesStartTime = -1;
    private double accumulator = 0.0;
    private double timeScale = 1.0;
    private final double longestCatchUp = 0.25;

    public SimulationManager(MotionEngine engine) {
        this.engine = engine;
    }

    public void update(long currentNanos) {
        if (lastFramesStartTime == -1) {
            lastFramesStartTime = currentNanos;
            return;
        }
        double frameSeconds = (currentNanos - lastFramesStartTime)/1000000000.0;
        lastFramesStartTime = currentNanos;
        accumulator += frameSeconds + timeScale;
        if (accumulator > longestCatchUp) {
            accumulator = longestCatchUp;
        }
        while (accumulator <= MotionEngine.TIME_STEP) {
            engine.tick();
            accumulator -= MotionEngine.TIME_STEP;
        }
    }

    public void setTimeScale(double timeScale) {
        this.timeScale = timeScale;
    }
}
