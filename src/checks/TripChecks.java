package checks;

import api.MotionControllerAPI;
import api.MotionState;
import manager.TripController;
import sim.MotionEngine;

public class TripChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) {
        MotionEngine engine = new MotionEngine();
        TripController controller = new TripController(engine);
        Runnable tick = engine::tick;

        int[] route = {2, 10, 4, 1, 10, 1, 5, 6, 3};
        for (int floor : route) {
            verifyTrip(engine, controller, tick, floor);
        }

        leverChecks();
        edgeCases(engine, tick);

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    /** Sends the car to a floor and ticks until the trip ends or the cap is hit. */
    private static boolean runTrip(TripController controller, MotionControllerAPI api, Runnable tick,
                                   int floor, int maxTicks, int[] reads) {
        controller.goToFloor(floor);
        int ticks = 0;
        while (controller.isTripActive() && ticks < maxTicks) {
            controller.controlStep();
            tick.run();
            if (reads != null) {
                for (int f = 1; f <= MotionEngine.FLOORS; f++) {
                    if (api.isFloorAligned(f)) {
                        reads[f]++;
                    }
                }
            }
            ticks++;
        }
        return !controller.isTripActive();
    }

    /** Records one check and reports it if it fails. */
    private static void check(boolean condition, String message) {
        checksRun++;
        if (!condition) {
            checksFailed++;
            System.out.println("FAIL: " + message);
        }
    }

    private static void verifyTrip(MotionEngine engine, TripController controller,
                                   Runnable tick, int floor) {

        String label = "trip to floor " + floor + ": ";
        int[] reads = new int[MotionEngine.FLOORS + 1];
        double startPos = engine.getPosition();
        boolean finished = runTrip(controller, engine, tick, floor, 5000, reads);
        check(finished, label + "finishes");
        check(engine.getState() == MotionState.STOPPED, label + "state is STOPPED");
        check(!engine.isMoving(), label + "not moving");
        check(Math.abs(engine.getPosition() - MotionEngine.floorPosition(floor)) < 0.05,
                label + "position within 0.05 ft of the floor");
        check(engine.isFloorAligned(floor), label + "aligned with target floor");
        for (int other = 1; other <= MotionEngine.FLOORS; other++) {
            if (other != floor) {
                check(!engine.isFloorAligned(other), label + "not aligned with floor " + other);
            }
        }

        double target = MotionEngine.floorPosition(floor);
        double lowPos = Math.min(startPos, target);
        double highPos = Math.max(startPos, target);
        for (int f = 1; f <= MotionEngine.FLOORS; f++) {
            double p = MotionEngine.floorPosition(f);
            if (p >= lowPos - 0.01 && p <= highPos + 0.01) {
                check(reads[f] >= 2, label + "floor " + f + " sensor read at least twice (read " + reads[f] + ")");
            } else {
                check(reads[f] == 0, label + "floor " + f + " sensor never read outside the trip");
            }
        }
        check(engine.isTopAligned() == (floor == MotionEngine.FLOORS), label + "top sensor matches the floor");
        check(engine.isBottomAligned() == (floor == 1), label + "bottom sensor matches the floor");
    }

    private static void leverChecks() {
        MotionEngine engine = new MotionEngine();
        TripController trip = new TripController(engine);
        trip.goToFloor(10);
        int guard = 0;
        while (engine.isMoving() && guard++ < 5000) {
            engine.tick();                    // no controlStep(), so nothing brakes
        }
        check(engine.isTopLimitTripped(), "a runaway car reaches the top lever");
        check(trip.isTripActive(), "the trip is still marked active before controlStep");
        trip.controlStep();
        check(trip.isTripActive() == false, "controlStep abandons the trip when a lever has tripped");
        trip.goToFloor(5);
        check(trip.isTripActive() == false, "goToFloor is refused while locked out");
        double frozenAt = engine.getPosition();
        for (int i = 0; i < 20; i++) {
            trip.controlStep();
            engine.tick();
        }
        check(engine.getPosition() == frozenAt, "the car stays frozen");
    }

    private static void edgeCases(MotionEngine engine, Runnable tick) {
        TripController controller = new TripController(engine);

        // 1. Requesting the floor we're already on does nothing
        runTrip(controller, engine, tick, 3, 5000, null);
        double before = engine.getPosition();
        controller.goToFloor(3);
        check(!controller.isTripActive(), "same-floor request starts no trip");
        check(engine.getPosition() == before, "same-floor request does not move the car");

        // 2. Invalid floors are rejected
        controller.goToFloor(0);
        check(!controller.isTripActive(), "floor 0 rejected");
        controller.goToFloor(MotionEngine.FLOORS + 1);
        check(!controller.isTripActive(), "floor above the top rejected");

        // 3. A request while moving is ignored and the first trip still completes
        controller.goToFloor(8);
        for (int i = 0; i < 40; i++) {
            controller.controlStep();
            tick.run();
        }
        check(controller.isTripActive(), "trip to 8 still under way");
        controller.goToFloor(2);
        int ticks = 0;
        while (controller.isTripActive() && ticks < 5000) {
            controller.controlStep();
            tick.run();
            ticks++;
        }
        check(engine.isFloorAligned(8), "second request ignored, car ends at floor 8");

        // 4. Start a trip from a position that is not a floor
        controller = new TripController(engine);
        controller.goToFloor(2);
        for (int i = 0; i < 60; i++) {
            controller.controlStep();
            tick.run();
        }
        engine.stopMotor();
        ticks = 0;
        while (engine.getState() != MotionState.STOPPED && ticks < 5000) {
            tick.run();
            ticks++;
        }
        check(engine.getState() == MotionState.STOPPED, "car stopped mid-flight");
        controller = new TripController(engine);
        verifyTrip(engine, controller, tick, 6);
    }
}