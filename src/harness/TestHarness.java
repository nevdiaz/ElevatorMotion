package harness;

import api.Direction;
import api.MotionControllerAPI;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Test harness class to stand in for the elevator manager.
 * Runs the clocks and moves the elevator through the floors.
 * I chose the floors randomly for now.
 */
public class TestHarness {

    private static final long TIME_STEP = 50; // 0.05s
    private static final double FLOOR_HEIGHT = 14.0;
    private static final double DECELERATION = 2.0;

    private static final int[] FLOORS = {2, 10, 4, 1}; // random floor numbers for now

    private static final double PAUSE_TIME = 2.0; // seconds

    private int nextFloorIndex = 0;
    private double targetPosition;
    private boolean onTrip = false;
    private int pauseStepsLeft = (int) Math.round(PAUSE_TIME * 1000 / TIME_STEP);

    private final MotionControllerAPI motion;
    private final Runnable updateTimeStep;

    private final ScheduledExecutorService clock =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "test-harness-clock");
                thread.setDaemon(true);
                return thread;
            });


    public TestHarness(MotionControllerAPI motion, Runnable updateTimeStep) {
        this.motion = motion;
        this.updateTimeStep = updateTimeStep;
    }

    public void start() {
        clock.scheduleAtFixedRate(this::onUpdate, 0, TIME_STEP, TimeUnit.MILLISECONDS);
    }

    void onUpdate() {

        try {
            if (onTrip) {
                continueTrip();
            } else if (pauseStepsLeft > 0) {
                pauseStepsLeft--;
            } else {
                startNextTrip();
            }
            updateTimeStep.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
    private void startNextTrip() {

        int floor = FLOORS[nextFloorIndex];
        nextFloorIndex = (nextFloorIndex + 1) % FLOORS.length;
        targetPosition = (floor - 1) * FLOOR_HEIGHT;

        if (targetPosition > motion.getPosition()) {
            motion.startMotor(Direction.Up);
        } else {
            motion.startMotor(Direction.Down);
        }

        onTrip = true;
    }

    private void continueTrip() {

        if (!motion.isMoving()) {
            onTrip = false;
            pauseStepsLeft = (int) Math.round(PAUSE_TIME * 1000 / TIME_STEP);
            return;
        }

        double remaining = Math.abs(targetPosition - motion.getPosition());
        double speed = motion.getVelocity();
        double brakingDistance = speed * speed / (2 * DECELERATION);

        if (remaining <= brakingDistance) {
            motion.stopMotor();
        }
    }
}
