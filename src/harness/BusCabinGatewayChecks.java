package harness;

import bus.*;
import manager.TripController;
import sim.MotionEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BusCabinGatewayChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) {

        initialAnnouncementChecks();
        callServedChecks();
        queuedCallsChecks();
        invalidCallChecks();
        sameFloorChecks();
        otherElevatorChecks();
        constructorChecks();

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    /** One car, one bus, a command-center connection, and a monitor that listens to what the cabin publishes. */
    private static class Rig {
        final MotionEngine engine = new MotionEngine();
        final TripController trip = new TripController(engine);
        final BusStandIn bus = new BusStandIn();
        final BusMessageDispatcher center = bus.connect();
        final BusMessageDispatcher monitor = bus.connect();
        final BusCabinGateway gateway;

        Rig() {
            monitor.subscribe(BusCodes.CAR_POSITION, BusCodes.ELEVATOR_1);
            monitor.subscribe(BusCodes.CAR_DIRECTION, BusCodes.ELEVATOR_1);
            gateway = new BusCabinGateway(engine, trip, bus.connect(), BusCodes.ELEVATOR_1);
        }

        void run(int ticks) {
            for (int i = 0; i < ticks; i++) {
                gateway.update();
                trip.controlStep();
                engine.tick();
            }
        }

        void call(int subtopic, long floor) {
            center.publish(BusMessage.of(BusCodes.CAR_CALL, subtopic, floor));
        }

        // Takes every queued message the monitor has for this topic, oldest first.
        List<Long> drain(int topic) {
            List<Long> values = new ArrayList<>();
            Optional<BusMessage> next = monitor.get(topic, BusCodes.ELEVATOR_1);
            while (next.isPresent()) {
                values.add(next.get().value());
                next = monitor.get(topic, BusCodes.ELEVATOR_1);
            }
            return values;
        }
    }

    private static void initialAnnouncementChecks() {
        Rig rig = new Rig();
        rig.run(1);
        check(rig.drain(BusCodes.CAR_POSITION).equals(List.of(1L)), "the first update announces floor 1");
        check(rig.drain(BusCodes.CAR_DIRECTION).equals(List.of(0L)), "the first update announces idle");
    }

    private static void callServedChecks() {
        Rig rig = new Rig();
        rig.run(1);
        rig.drain(BusCodes.CAR_POSITION);
        rig.drain(BusCodes.CAR_DIRECTION);

        rig.call(BusCodes.ELEVATOR_1, 5);
        rig.run(2000);
        check(rig.engine.isFloorAligned(5), "a car call for floor 5 takes the car to floor 5");
        check(rig.drain(BusCodes.CAR_POSITION).equals(List.of(2L, 3L, 4L, 5L)),
                "floors 2, 3, 4 and 5 are published in order");
        check(rig.drain(BusCodes.CAR_DIRECTION).equals(List.of(1L, 0L)),
                "direction goes up, then idle");
    }

    private static void queuedCallsChecks() {
        Rig rig = new Rig();
        rig.run(1);
        rig.drain(BusCodes.CAR_POSITION);
        rig.drain(BusCodes.CAR_DIRECTION);

        rig.call(BusCodes.ELEVATOR_1, 3);
        rig.call(BusCodes.ELEVATOR_1, 2);
        rig.call(BusCodes.ELEVATOR_1, 4);
        rig.run(4000);
        check(rig.engine.isFloorAligned(4), "three calls are served and the car ends at floor 4");
        check(rig.drain(BusCodes.CAR_POSITION).equals(List.of(2L, 3L, 2L, 3L, 4L)),
                "calls are served in the order they arrived (3, then 2, then 4)");
        check(rig.drain(BusCodes.CAR_DIRECTION).equals(List.of(1L, 0L, 2L, 0L, 1L, 0L)),
                "direction is up, idle, down, idle, up, idle");
    }

    private static void invalidCallChecks() {
        Rig rig = new Rig();
        rig.run(1);

        rig.call(BusCodes.ELEVATOR_1, 0);
        rig.call(BusCodes.ELEVATOR_1, 11);
        rig.call(BusCodes.ELEVATOR_1, 99999999999L);
        rig.call(BusCodes.ELEVATOR_1, 2);
        rig.run(2000);
        check(rig.engine.isFloorAligned(2), "invalid floors are skipped and the valid call is still served");
    }

    private static void sameFloorChecks() {
        Rig rig = new Rig();
        rig.run(1);
        rig.drain(BusCodes.CAR_POSITION);
        rig.drain(BusCodes.CAR_DIRECTION);

        rig.call(BusCodes.ELEVATOR_1, 1);
        rig.run(200);
        check(rig.engine.isFloorAligned(1), "a call for the current floor does not move the car");
        check(rig.drain(BusCodes.CAR_DIRECTION).isEmpty(), "a call for the current floor publishes no direction change");
    }

    private static void otherElevatorChecks() {
        Rig rig = new Rig();
        rig.run(1);

        rig.call(BusCodes.ELEVATOR_2, 5);
        rig.run(200);
        check(rig.engine.isFloorAligned(1), "a call for elevator 2 is ignored by elevator 1");
    }

    private static void constructorChecks() {
        MotionEngine engine = new MotionEngine();
        TripController trip = new TripController(engine);
        BusStandIn bus = new BusStandIn();
        checkRejects(() -> new BusCabinGateway(engine, trip, bus.connect(), 0), "elevator 0 rejected");
        checkRejects(() -> new BusCabinGateway(engine, trip, bus.connect(), 5), "elevator 5 rejected");
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
