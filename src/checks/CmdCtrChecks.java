package checks;

import bus.BusCabinGateway;
import bus.BusCodes;
import bus.BusStandIn;
import harness.TopAndBottomHarness;
import manager.TripController;
import sim.MotionEngine;

import java.util.List;

public class CmdCtrChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) {
        topAndBottomChecks();

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    private static void topAndBottomChecks() {
        MotionEngine engine = new MotionEngine();
        TripController trip = new TripController(engine);
        BusStandIn bus = new BusStandIn();
        BusCabinGateway gateway = new BusCabinGateway(engine, trip, bus.connect(), BusCodes.ELEVATOR_1);
        TopAndBottomHarness scenario = new TopAndBottomHarness(bus.connect());

        int ticks = 0;
        while (scenario.isFinished() == false && ticks < 40000) {
            scenario.update();
            gateway.update();
            trip.controlStep();
            engine.tick();
            ticks += 1;
        }

        check(scenario.isFinished(), "top and bottom: finished before the tick limit");
        check(scenario.getArrivals().equals(List.of(10, 1)), "top and bottom: arrivals " + scenario.getArrivals());
        check(Math.abs(engine.getPosition() - MotionEngine.floorPosition(1)) < 0.11,
                "top and bottom: car ends level at floor 1");
    }

    private static void check(boolean condition, String message) {
        checksRun++;
        if (!condition) {
            checksFailed++;
            System.out.println("FAIL: " + message);
        }
    }
}