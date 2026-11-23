package checks;

import Utilities.Message;
import api.Direction;
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
        gotoAndServiceChecks();
        invalidCallChecks();
        sameFloorChecks();
        otherElevatorChecks();
        constructorChecks();
        faultChecks();

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    /** One CABIN_STATUS message: P1 stopped, P2 floor, P3 direction. */
    private record Status(int stopped, int floor, int direction) {}

    /** The status of a car that has stopped at a floor. */
    private static Status stoppedAt(int floor) {
        return new Status(1, floor, BusCodes.DIR_IDLE);
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
            monitor.subscribe(BusCodes.CABIN_STATUS, BusCodes.ELEVATOR_1);
            monitor.subscribe(BusCodes.ALIGN, BusCodes.ELEVATOR_1);
            monitor.subscribe(BusCodes.CAR_FAULT, BusCodes.ELEVATOR_1);
            gateway = new BusCabinGateway(engine, trip, bus.connect(), BusCodes.ELEVATOR_1);
        }

        void run(int ticks) {
            for (int i = 0; i < ticks; i++) {
                gateway.update();
                trip.controlStep();
                engine.tick();
            }
        }

        void call(int subtopic, int floor) {
            send(BusCodes.CABIN_CALL, subtopic, floor);
        }

        void send(int topic, int subtopic, int floor) {
            center.publish(Message.ofPayload(topic, subtopic, floor));
        }

        // Takes every CABIN_STATUS the monitor has queued, oldest first.
        List<Status> statuses() {
            List<Status> found = new ArrayList<>();
            for (Message m : drain(BusCodes.CABIN_STATUS)) {
                found.add(new Status(m.payload(1), m.payload(2), m.payload(3)));
            }
            return found;
        }

        // Takes every ALIGN or CAR_FAULT message queued, and returns payload byte 1 of each.
        List<Integer> firstBytes(int topic) {
            List<Integer> found = new ArrayList<>();
            for (Message m : drain(topic)) {
                found.add(m.payload(1));
            }
            return found;
        }

        private List<Message> drain(int topic) {
            List<Message> messages = new ArrayList<>();
            Optional<Message> next = monitor.get(topic, BusCodes.ELEVATOR_1);
            while (next.isPresent()) {
                messages.add(next.get());
                next = monitor.get(topic, BusCodes.ELEVATOR_1);
            }
            return messages;
        }

        // Forgets everything announced so far
        void clear() {
            statuses();
            firstBytes(BusCodes.ALIGN);
            firstBytes(BusCodes.CAR_FAULT);
        }
    }

    private static void initialAnnouncementChecks() {
        Rig rig = new Rig();
        rig.run(1);
        check(rig.statuses().equals(List.of(stoppedAt(1))), "the first update announces stopped at floor 1, direction idle");
        check(rig.firstBytes(BusCodes.ALIGN).equals(List.of(BusCodes.ALIGN_BOTTOM)),
                "the first update announces the car is aligned with the bottom of the shaft");

        rig.run(50);
        check(rig.statuses().isEmpty(), "the status is not repeated while the car sits still");
        check(rig.firstBytes(BusCodes.ALIGN).isEmpty(), "the alignment is not repeated while the car sits still");
    }

    private static void callServedChecks() {
        Rig rig = new Rig();
        rig.run(1);
        rig.clear();

        rig.call(BusCodes.ELEVATOR_1, 5);
        rig.run(100);
        check(rig.engine.isMoving(), "the car is travelling after the call");
        check(rig.statuses().isEmpty(), "no status is published while the car travels");

        rig.run(2000);
        check(rig.engine.isFloorAligned(5), "a cabin call for floor 5 takes the car to floor 5");
        check(rig.statuses().equals(List.of(stoppedAt(5))),
                "one status is published, stopped at floor 5, and none for the floors it passed");
        check(rig.firstBytes(BusCodes.ALIGN).equals(List.of(BusCodes.ALIGN_NONE)),
                "the alignment goes to none when the car leaves floor 1 and stays none until it stops at floor 5");
    }

    private static void queuedCallsChecks() {
        Rig rig = new Rig();
        rig.run(1);
        rig.clear();

        rig.call(BusCodes.ELEVATOR_1, 3);
        rig.call(BusCodes.ELEVATOR_1, 2);
        rig.call(BusCodes.ELEVATOR_1, 4);
        rig.run(4000);
        check(rig.engine.isFloorAligned(4), "three calls are served and the car ends at floor 4");
        check(rig.statuses().equals(List.of(stoppedAt(3), stoppedAt(2), stoppedAt(4))),
                "calls are served in the order they arrived (3, then 2, then 4), one status each");
    }

    private static void gotoAndServiceChecks() {
        Rig rig = new Rig();
        rig.run(1);
        rig.clear();

        rig.send(BusCodes.GOTO, BusCodes.ELEVATOR_1, 3);
        rig.run(2000);
        check(rig.engine.isFloorAligned(3), "GOTO takes the car to the floor");
        check(rig.statuses().equals(List.of(stoppedAt(3))), "GOTO ends with a stopped status at floor 3");

        rig.send(BusCodes.SERVICE, BusCodes.ELEVATOR_1, 2);
        rig.run(2000);
        check(rig.engine.isFloorAligned(2), "SERVICE takes the car to the floor");
        check(rig.statuses().equals(List.of(stoppedAt(2))), "SERVICE ends with a stopped status at floor 2");
    }

    private static void invalidCallChecks() {
        Rig rig = new Rig();
        rig.run(1);

        rig.call(BusCodes.ELEVATOR_1, 0);
        rig.call(BusCodes.ELEVATOR_1, 11);
        rig.call(BusCodes.ELEVATOR_1, 255);
        //The old number style puts the floor in payload byte 6, so payload byte 1 is 0 and the call is not a floor
        rig.center.publish(Message.of(BusCodes.CABIN_CALL, BusCodes.ELEVATOR_1, 5));
        rig.run(200);
        check(rig.engine.isFloorAligned(1), "invalid floors and old-style messages do not move the car");

        rig.call(BusCodes.ELEVATOR_1, 2);
        rig.run(2000);
        check(rig.engine.isFloorAligned(2), "invalid floors are skipped and the valid call is still served");
    }

    private static void sameFloorChecks() {
        Rig rig = new Rig();
        rig.run(1);
        rig.clear();

        rig.call(BusCodes.ELEVATOR_1, 1);
        rig.run(200);
        check(rig.engine.isFloorAligned(1), "a call for the current floor does not move the car");
        check(rig.statuses().isEmpty(), "a call for the current floor publishes no new status");
        check(rig.firstBytes(BusCodes.ALIGN).isEmpty(), "a call for the current floor publishes no alignment change");
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

    private static void faultChecks() {
        Rig rig = new Rig();
        rig.run(1);
        rig.clear();

        rig.engine.startMotor(Direction.Up);      //runaway: no trip, so nothing brakes
        rig.run(1000);
        check(rig.engine.isTopLimitTripped(), "the runaway car trips the top lever");
        check(rig.firstBytes(BusCodes.CAR_FAULT).equals(List.of(BusCodes.FAULT_TOP_LIMIT)),
                "the top lever fault is announced once, with the fault code in payload byte 1");
        check(rig.statuses().isEmpty(), "a car stopped at a lever has no floor, so no status is published");

        rig.call(BusCodes.ELEVATOR_1, 5);
        rig.run(40);
        check(rig.firstBytes(BusCodes.CAR_FAULT).isEmpty(), "the fault is not announced twice");
        check(rig.statuses().isEmpty(), "a call is ignored while locked out");
        check(rig.engine.getPosition() == MotionEngine.MAX_POSITION, "the car stays at the lever");

        Rig low = new Rig();
        low.run(1);
        low.clear();
        low.engine.startMotor(Direction.Down);
        low.run(200);
        check(low.engine.isBottomLimitTripped(), "the runaway car trips the bottom lever");
        check(low.firstBytes(BusCodes.CAR_FAULT).equals(List.of(BusCodes.FAULT_BOTTOM_LIMIT)),
                "the bottom lever fault is announced once");
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