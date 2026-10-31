package harness;

import manager.TripController;
import sim.MotionEngine;

public class BusMessageChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) {
        MotionEngine engine = new MotionEngine();
        TripController controller = new TripController(engine);
        Runnable tick = engine::tick;

        int[] route = {2, 10, 4, 1, 10, 1, 5, 6, 3};


        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }
}
