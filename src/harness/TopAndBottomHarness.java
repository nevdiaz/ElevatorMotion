package harness;

import bus.BusCodes;
import bus.BusMessageDispatcher;

import java.util.List;

/** Scenario: the car goes straight to the top floor, then straight to floor 1. */
public class TopAndBottomHarness {

    private static final int PAUSE_TICKS = 40;   // 2 seconds at 0.05 s per step

    public static final int[] FLOORS = {10, 1};

    private final ScriptRunner runner;

    public TopAndBottomHarness(BusMessageDispatcher commandCenter) {
        runner = new ScriptRunner(commandCenter, BusCodes.ELEVATOR_1, FLOORS, PAUSE_TICKS);
    }

    /** Call once per simulation step, from the beforeStep hook. */
    public void update() {
        runner.update();
    }

    public boolean isFinished() {
        return runner.isFinished();
    }

    public List<Integer> getArrivals() {
        return runner.getArrivals();
    }
}