package checks;

import manager.SimulationManager;
import manager.TripController;
import sim.MotionEngine;

public class SimulationManagerChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) {
        MotionEngine engine = new MotionEngine();
        StringBuilder log = new StringBuilder();
        SimulationManager manager = new SimulationManager(new TripController(engine), () -> log.append("T"));
        manager.setBeforeStep(() -> log.append("H"));

        long frame = 50_000_000L;            //0.05 s, exactly one step
        manager.update(0);                   //first call only records the time
        check(log.length() == 0, "no step on the first update");
        manager.update(frame);
        manager.update(2 * frame);
        manager.update(3 * frame);
        check(log.toString().equals("HTHTHT"), "the hook runs before the tick, once per step");

        checkRejects(() -> manager.setBeforeStep(null), "null hook rejected");

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    private static void check(boolean condition, String message) {
        checksRun++;
        if (!condition) {
            checksFailed++;
            System.out.println("FAIL: " + message);
        }
    }

    private static void checkRejects(Runnable action, String message) {
        boolean threw = false;
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, message);
    }
}