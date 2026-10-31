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
