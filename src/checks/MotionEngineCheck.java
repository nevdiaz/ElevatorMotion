package checks;

import api.Direction;
import api.MotionControllerAPI;
import api.MotionState;
import sim.MotionEngine;

/** checks for MotionEngine */
public class MotionEngineCheck {

    static int failures = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }

    static class Trip {
        double time;
        double stopError;      // final position minus target position (ft)
        int minReads;          // fewest sensor reads at any floor passed
        boolean endedAligned;  // isFloorAligned(target) is true at the stop
        boolean leverTripped;  //a safety lever tripped during the trip
    }

    /**
     * Example controller: drives only through the MotionControllerAPI and calls stopMotor
     * when the distance remaining is within the braking distance.
     */
    static Trip run(int from, int to) {
        MotionEngine eng = new MotionEngine(MotionEngine.floorPosition(from));
        MotionControllerAPI api = eng;
        double target = MotionEngine.floorPosition(to);
        Direction dir = to > from ? Direction.Up : Direction.Down;
        int lo = Math.min(from, to), hi = Math.max(from, to);
        int[] reads = new int[MotionEngine.FLOORS + 1];

        api.startMotor(dir);
        int ticks = 0;
        do {
            double remaining = Math.abs(target - api.getPosition());
            if (remaining <= MotionEngine.brakingDistance(api.getVelocity())) {
                api.stopMotor();
            }
            eng.tick();
            ticks++;
            for (int f = lo; f <= hi; f++) {
                if (api.isFloorAligned(f)) {
                    reads[f]++;
                }
            }
        } while (api.isMoving() && ticks < 100000);

        Trip t = new Trip();
        t.time = ticks * MotionEngine.TIME_STEP;
        t.stopError = api.getPosition() - target;
        t.minReads = Integer.MAX_VALUE;
        for (int f = lo; f <= hi; f++) {
            t.minReads = Math.min(t.minReads, reads[f]);
        }
        t.endedAligned = api.isFloorAligned(to);
        t.leverTripped = api.isTopLimitTripped() || api.isBottomLimitTripped();
        return t;
    }

    public static void main(String[] args) {
        System.out.println("floors  dir   time(s)  expected  stopError(ft)  minSensorReads  endsAligned");
        for (int n = 1; n <= 9; n++) {
            for (int up = 0; up < 2; up++) {
                int from = up == 0 ? 1 : 10;
                int to = up == 0 ? 1 + n : 10 - n;
                Trip t = run(from, to);
                double expected = n * MotionEngine.FLOOR_HEIGHT / MotionEngine.CRUISE_SPEED + 2.0;
                String label = n + (up == 0 ? " up" : " down");
                System.out.printf("%-7d %-5s %-8.2f %-9.1f %-14.2f %-15d %s%n",
                        n, up == 0 ? "up" : "down", t.time, expected, t.stopError, t.minReads, t.endedAligned);
                check(Math.abs(t.time - expected) <= 0.15, "time " + label);
                check(Math.abs(t.stopError) <= 0.2 + 1e-9, "stop error " + label);
                check(t.minReads >= 2, "sensor reads " + label);
                check(t.endedAligned, "ends aligned " + label);
                check(!t.leverTripped, "no lever tripped on a normal trip " + label);
            }
        }

        // Method rules
        MotionEngine e = new MotionEngine();
        e.stopMotor();
        check(e.getState() == MotionState.STOPPED && !e.isMoving(), "stopMotor while stopped does nothing");
        e.stepUpMotor();
        check(Math.abs(e.getPosition() - 0.2) < 1e-9, "stepUpMotor moves 0.2 ft");
        for (int i = 0; i < 10; i++) e.stepDnMotor();
        check(e.getPosition() == MotionEngine.MIN_POSITION, "step stops at the bottom of the shaft");
        e = new MotionEngine();
        e.startMotor(Direction.Up);
        check(e.getState() == MotionState.ACCELERATING && e.isMoving(), "startMotor accelerates");
        e.startMotor(Direction.Down);
        check(e.getDirection() == Direction.Up, "startMotor ignored while moving");
        double before = e.getPosition();
        e.stepUpMotor();
        check(e.getPosition() == before, "step ignored while moving");
        for (int i = 0; i < 20; i++) e.tick();           // 1 s: speed 2 ft/s
        check(e.getState() == MotionState.ACCELERATING, "still accelerating at 2 ft/s");
        check(Math.abs(e.getVelocity() - 2.0) < 1e-6, "velocity is 2 ft/s after 1 s");
        e.stopMotor();
        int guard = 0;
        while (e.isMoving() && guard++ < 1000) e.tick();
        check(e.getState() == MotionState.STOPPED && e.getVelocity() == 0.0, "stopMotor during acceleration reaches stopped");

        // Top and bottom limits
        MotionEngine top = new MotionEngine(MotionEngine.SHAFT_TOP - 1.0);
        top.startMotor(Direction.Up);
        guard = 0;
        while (top.isMoving() && guard++ < 5000) top.tick();
        check(top.getPosition() == MotionEngine.MAX_POSITION, "car cannot pass the top of the shaft");
        check(top.isTopLimitTripped() && !top.isBottomLimitTripped(), "top lever trips at the top of the shaft");
        check(top.getVelocity() == 0.0 && !top.isMoving(), "car stops dead at the lever");
        top.startMotor(Direction.Down);
        check(!top.isMoving(), "motor stays locked after the lever trips");
        double lockedAt = top.getPosition();
        top.stepDnMotor();
        check(top.getPosition() == lockedAt, "steps are refused after the lever trips");
        MotionEngine low = new MotionEngine();
        low.startMotor(Direction.Down);
        guard = 0;
        while (low.isMoving() && guard++ < 5000) low.tick();
        check(low.isBottomLimitTripped() && !low.isTopLimitTripped(), "bottom lever trips below floor 1");
        check(low.getPosition() == MotionEngine.MIN_POSITION, "car stops at the bottom lever");

        MotionEngine bottom = new MotionEngine();
        check(bottom.isBottomAligned() && bottom.getCurrentFloor() == 1, "bottom aligned at floor 1");
        check(new MotionEngine(20.0).getCurrentFloor() == 0, "between zones reports floor 0");
        check(new MotionEngine(MotionEngine.floorPosition(4)).isFloorAligned(4), "aligned at floor 4");

        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}