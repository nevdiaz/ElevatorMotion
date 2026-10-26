package manager;

import api.Direction;
import sim.MotionEngine;



public class SimulationManager {

    private final MotionEngine engine;
    private long lastFramesStartTime = -1;
    private double accumulator = 0.0;
    private double timeScale = 1.0;
    private static final double longestCatchUp = 0.25;
    boolean tripActive = false;
    private double targetPosition;

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
        accumulator += frameSeconds * timeScale;
        if (accumulator > longestCatchUp) {
            accumulator = longestCatchUp;
        }
        while (accumulator >= MotionEngine.TIME_STEP) {
            controlStep();
            engine.tick();
            accumulator -= MotionEngine.TIME_STEP;
        }
    }

    public void setTimeScale(double timeScale) {
        this.timeScale = timeScale;
    }

    public void goToFloor(int floor) {
        if (floor < 1 || floor > MotionEngine.FLOORS) {
            System.out.println("Floor must be 1 through 10.");
            return;
        }
        if (tripActive == true || engine.isMoving() == true) {
            System.out.println("Elevator is busy.");
            return;
        }
        if (engine.isFloorAligned(floor) == true) {
            System.out.println("Elevator has already arrived.");
            return;
        }
        targetPosition = MotionEngine.floorPosition(floor);
        tripActive = true;
        if (targetPosition > engine.getPosition()) {
            engine.startMotor(Direction.Up);
        } else {
            engine.startMotor(Direction.Down);
        }
    }

    private void controlStep() {
        if (tripActive == false) {
            return;
        }
        if (engine.isMoving() == true) {
            double remaining = Math.abs(targetPosition - engine.getPosition());
            if (remaining <= MotionEngine.brakingDistance(engine.getVelocity())) {
                engine.stopMotor();
            }
        } else {
            tripActive = false;
            System.out.println("Arrived at " + engine.getPosition() + "ft, floor " + engine.getCurrentFloor());
        }
    }
}
