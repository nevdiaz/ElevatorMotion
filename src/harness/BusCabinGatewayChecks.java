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
