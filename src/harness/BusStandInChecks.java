package harness;

import bus.BusCodes;
import bus.BusMessage;
import bus.BusMessageDispatcher;
import bus.BusStandIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BusStandInChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    private static final byte[] BODY_A = {1, 2, 3, 4, 5, 6};
    private static final byte[] BODY_B = {9, 8, 7, 6, 5, 4};

    public static void main(String[] args) {
        deliveryChecks();
        getChecks();
        collisionChecks();
        registerChecks();
        badInputChecks();

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    private static boolean holds(Optional<BusMessage> message, byte[] expectedBody) {
        return message.isPresent() && Arrays.equals(message.get().getBody(), expectedBody);
    }

    private static void deliveryChecks() {
        BusStandIn bus = new BusStandIn();
        BusMessageDispatcher sender = bus.connect();
        BusMessageDispatcher listener = bus.connect();
        int topic = BusCodes.CAR_DIRECTION;
        int elevator = BusCodes.ELEVATOR_1;

        listener.subscribe(topic, elevator);
        sender.publish(BusMessage.of(topic, elevator, 1));
        check(holdsValue(listener.get(topic, elevator), 1), "subscriber receives a published message");
        check(listener.get(topic, elevator).isEmpty(), "get removes the message");

        sender.subscribe(topic, elevator);
        sender.publish(BusMessage.of(topic, elevator, 2));
        check(sender.get(topic, elevator).isEmpty(), "publisher does not receive its own message");
        check(holdsValue(listener.get(topic, elevator), 2), "the other connection still receives it");

        sender.publish(BusMessage.of(BusCodes.CAR_POSITION, elevator, 5));
        check(listener.get(BusCodes.CAR_POSITION, elevator).isEmpty(), "no message without a subscription");

        BusMessageDispatcher second = bus.connect();
        second.subscribe(topic, elevator);
        sender.publish(BusMessage.of(topic, elevator, 3));
        check(holdsValue(listener.get(topic, elevator), 3), "first listener gets the message");
        check(holdsValue(second.get(topic, elevator), 3), "second listener gets the message");
    }

    private static void collisionChecks() {
        BusStandIn bus = new BusStandIn();
        bus.publish(new BusMessage(1, 2, BODY_A));
        bus.publish(new BusMessage(2, 1, BODY_B));
        check(holds(bus.get(1, 2), BODY_A), "(1, 2) is not overwritten by (2, 1)");
        check(holds(bus.get(2, 1), BODY_B), "(2, 1) is kept separate from (1, 2)");

        bus.publish(new BusMessage(1, 0, BODY_A));
        bus.publish(new BusMessage(2, 0, BODY_B));
        check(holds(bus.get(1, 0), BODY_A), "(1, 0) is not overwritten by (2, 0)");
        check(holds(bus.get(2, 0), BODY_B), "(2, 0) is kept separate from (1, 0)");
    }

    private static void getChecks() {
        BusStandIn bus = new BusStandIn();
        check(bus.get(3, 4).isEmpty(), "get returns empty when nothing was published");

        bus.publish(new BusMessage(3, 4, BODY_A));
        bus.publish(new BusMessage(3, 4, BODY_B));
        check(holds(bus.get(3, 4), BODY_B), "get returns the latest message");
    }

    private static void registerChecks() {
        BusStandIn bus = new BusStandIn();
        int a = bus.register("cabin");
        int b = bus.register("panel");
        check(a != b, "register gives different ids");
        checkRejects(() -> bus.register(null), "null component name rejected");
        checkRejects(() -> bus.register("  "), "blank component name rejected");
    }

    private static void badInputChecks() {
        BusStandIn bus = new BusStandIn();
        checkRejects(() -> bus.subscribe(256, m -> { }), "subscribe topic 256 rejected");
        checkRejects(() -> bus.subscribe(5, null), "subscribe null listener rejected");
        checkRejects(() -> bus.publish(null), "publish null rejected");
        checkRejects(() -> bus.get(300, 0), "get topic 300 rejected");
        checkRejects(() -> bus.get(0, -1), "get subtopic -1 rejected");
    }

    private static void check(boolean condition, String message) {
        checksRun++;
        if (condition == false) {
            checksFailed++;
            System.out.println("FAIL: " + message);
        }
    }

    //Passes only if the action throws an IllegalArgumentException.
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
