package harness;

import bus.BusMessage;
import bus.BusStandIn;

import java.util.Arrays;
import java.util.Optional;

public class BusStandInChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    private static final byte[] BODY_A = {1, 2, 3, 4, 5, 6};
    private static final byte[] BODY_B = {9, 8, 7, 6, 5, 4};

    private static boolean holds(Optional<BusMessage> message, byte[] expectedBody) {
        return message.isPresent() && Arrays.equals(message.get().getBody(), expectedBody);
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
