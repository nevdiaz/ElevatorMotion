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
}
