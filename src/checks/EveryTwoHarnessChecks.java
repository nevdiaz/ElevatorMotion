package checks;

import bus.BusCabinGateway;
import bus.BusCodes;
import bus.BusMessage;
import bus.BusMessageDispatcher;
import bus.BusStandIn;
import harness.EveryTwoHarness;
import manager.TripController;
import sim.MotionEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EveryTwoHarnessChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) {
        everyTwoFloorsChecks();

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    private static void everyTwoFloorsChecks() {
        MotionEngine engine = new MotionEngine();
        TripController trip = new TripController(engine);
        BusStandIn bus = new BusStandIn();
        BusMessageDispatcher monitor = bus.connect();
        monitor.subscribe(BusCodes.CAR_POSITION, BusCodes.ELEVATOR_1);
        monitor.subscribe(BusCodes.CAR_DIRECTION, BusCodes.ELEVATOR_1);
        BusCabinGateway gateway = new BusCabinGateway(engine, trip, bus.connect(), BusCodes.ELEVATOR_1);
        EveryTwoHarness scenario = new EveryTwoHarness(bus.connect());

        List<Long> floorsPassed = new ArrayList<>();
        List<Long> stops = new ArrayList<>();       //floor the car was at each time it paused
        boolean everWentDown = false;
        long lastFloor = 1;
        int ticks = 0;
        while (scenario.isFinished() == false && ticks < 40000) {
            scenario.update();
            gateway.update();
            trip.controlStep();
            engine.tick();
            ticks += 1;

            for (long floor : drain(monitor, BusCodes.CAR_POSITION)) {
                floorsPassed.add(floor);
                lastFloor = floor;
            }
            for (long direction : drain(monitor, BusCodes.CAR_DIRECTION)) {
                if (direction == 2) {
                    everWentDown = true;
                } else if (direction == 0 && ticks > 1) {
                    stops.add(lastFloor);
                }
            }
        }

        check(scenario.isFinished(), "Every two floors: finished before the tick limit");
        check(scenario.getArrivals().equals(List.of(3, 5, 7, 9)),
                "Every two floors: arrivals " + scenario.getArrivals());
        check(stops.equals(List.of(3L, 5L, 7L, 9L)),
                "Every two floors: the car only stopped at 3, 5, 7 and 9 " + stops);
        check(floorsPassed.equals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)),
                "Every two floors: floors 1 to 9 called in order " + floorsPassed);
        check(everWentDown == false, "Every two floors: the car never went down");
        check(Math.abs(engine.getPosition() - MotionEngine.floorPosition(9)) < 0.11,
                "Every two floors: car ends at floor 9");
        check(engine.isTopLimitTripped() == false && engine.isBottomLimitTripped() == false,
                "Every two floors: no safety lever was engaged");
    }

    private static List<Long> drain(BusMessageDispatcher monitor, int topic) {
        List<Long> values = new ArrayList<>();
        Optional<BusMessage> next = monitor.get(topic, BusCodes.ELEVATOR_1);
        while (next.isPresent()) {
            values.add(next.get().value());
            next = monitor.get(topic, BusCodes.ELEVATOR_1);
        }
        return values;
    }

    private static void check(boolean condition, String message) {
        checksRun++;
        if (!condition) {
            checksFailed++;
            System.out.println("FAIL: " + message);
        }
    }
}