package harness;

import bus.BusCodes;
import bus.BusMessageDispatcher;
import manager.TripController;

import java.util.List;

/**
 * Scenario: the car brakes late, so it passes the sensors of the target floor and stops past
 * them. The trip controller then nudges it back until it is level. The GUI shows the in-between
 * sensor color while the nudging happens, then lime green once the car is level.
 */
public class NudgeHarness {

    /** How much later than normal the car brakes. It ends up about 0.8 ft past the floor. */
    public static final double BRAKE_DELAY_FEET = 0.6;

    private static final int PAUSE_TICKS = 40;   // 2 seconds at 0.05 s per step

    /** Skips floors 1 and 10 so the overshoot stays inside the 1 ft lever margin. */
    public static final int[] FLOORS = {4, 8, 3, 6};

    private final CmdCtrStandIn runner;

    public NudgeHarness(BusMessageDispatcher commandCenter, TripController trip) {
        trip.setBrakeDelay(BRAKE_DELAY_FEET);
        runner = new CmdCtrStandIn(commandCenter, BusCodes.ELEVATOR_1, FLOORS, PAUSE_TICKS);
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