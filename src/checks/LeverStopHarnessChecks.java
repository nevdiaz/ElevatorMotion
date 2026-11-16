package checks;

import api.Direction;
import bus.BusCabinGateway;
import bus.BusCodes;
import bus.BusStandIn;
import harness.LeverStopHarness;
import manager.TripController;
import sim.MotionEngine;

public class LeverStopHarnessChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) {
        play(Direction.Up, BusCodes.FAULT_TOP_LIMIT, "top lever");
        play(Direction.Down, BusCodes.FAULT_BOTTOM_LIMIT, "bottom lever");

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    private static void play(Direction runaway, int expectedFault, String name) {
        MotionEngine engine = new MotionEngine();
        TripController trip = new TripController(engine);
        BusStandIn bus = new BusStandIn();
        BusCabinGateway gateway = new BusCabinGateway(engine, trip, bus.connect(), BusCodes.ELEVATOR_1);
        LeverStopHarness scenario = new LeverStopHarness(engine, bus.connect(), BusCodes.ELEVATOR_1, runaway, 20);

        int ticks = 0;
        while (scenario.isFinished() == false && ticks < CheckTime.runaway()) {
            scenario.update();
            gateway.update();
            trip.controlStep();
            engine.tick();
            ticks += 1;
        }

        check(scenario.isFinished(), name + ": the fault reached the bus before the tick limit");
        check(scenario.getFault() == expectedFault, name + ": fault code " + scenario.getFault());
        boolean tripped = (expectedFault == BusCodes.FAULT_TOP_LIMIT)
                ? engine.isTopLimitTripped() : engine.isBottomLimitTripped();
        check(tripped, name + ": the matching lever is tripped");
        check(engine.isMoving() == false, name + ": the car is frozen");
    }

    private static void check(boolean condition, String message) {
        checksRun++;
        if (!condition) {
            checksFailed++;
            System.out.println("FAIL: " + message);
        }
    }
}