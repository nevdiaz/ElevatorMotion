package harness;

import manager.FloorSequencer;
import manager.TripController;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Test harness standing in for the elevator manager.
 * Owns the clock and the test route; trip logic lives in TripController
 * and the floor cycle in FloorSequencer, so removing this class loses nothing vital.
 */
public class TestHarness {

    private static final int[] ROUTE = {2, 10, 4, 1};
    private static final double PAUSE_SECONDS = 2.0;
    private static final long TIME_STEP = 50; // milliseconds (0.05 s)
    private final TripController controller;
    private final FloorSequencer sequencer;
    private final Runnable updateTimeStep;

    private final ScheduledExecutorService clock =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "test-harness-clock");
                thread.setDaemon(true);
                return thread;
            });


    public TestHarness(TripController controller, Runnable updateTimeStep) {
        this.controller = controller;
        this.sequencer = new FloorSequencer(controller, ROUTE, PAUSE_SECONDS);
        this.updateTimeStep = updateTimeStep;
    }

    public void start() {
        clock.scheduleAtFixedRate(this::onUpdate, 0, TIME_STEP, TimeUnit.MILLISECONDS);
    }

    void onUpdate() {
        try {
            sequencer.update(TIME_STEP / 1000.0);
            controller.controlStep();
            updateTimeStep.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
