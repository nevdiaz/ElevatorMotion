package harness;

import manager.TripController;


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
    private static final int[] FLOORS = {2, 10, 4, 1}; // random floor numbers for now
    private static final double PAUSE_TIME = 2.0; // seconds
    private int nextFloorIndex = 0;
    private boolean onTrip = false;
    private static final int PAUSE_STEPS = (int) Math.round(PAUSE_TIME * 1000 / TIME_STEP);
    private int pauseStepsLeft = PAUSE_STEPS;
    private final TripController controller;
    private final Runnable updateTimeStep;

    private final ScheduledExecutorService clock =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "test-harness-clock");
                thread.setDaemon(true);
                return thread;
            });


    public TestHarness(TripController controller, Runnable updateTimeStep) {
        this.controller = controller;
        this.updateTimeStep = updateTimeStep;
    }

    public void start() {
        clock.scheduleAtFixedRate(this::onUpdate, 0, TIME_STEP, TimeUnit.MILLISECONDS);
    }

    void onUpdate() {
        try {
            if (controller.isTripActive()) {
                controller.controlStep();
                onTrip = true;
            } else if (onTrip) {
                onTrip = false;
                pauseStepsLeft = PAUSE_STEPS;
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
        controller.goToFloor(floor);
        pauseStepsLeft = PAUSE_STEPS;
    }
}
