package harness;


import bus.BusMessage;

import java.util.Arrays;

public class BusMessageChecks {

    private static int checksRun = 0;
    private static int checksFailed = 0;

    public static void main(String[] args) {

        byte[] body = {1, 2, 3, 4, 5, 6};

        //Round trip keeps every field
        BusMessage original = new BusMessage(7, 9, body);
        BusMessage copy = BusMessage.fromBytes(original.toBytes());
        check(copy.getTopic() == 7, "round trip keeps the topic");
        check(copy.getSubtopic() == 9, "round trip keeps the subtopic");
        check(Arrays.equals(copy.getBody(), body), "round trip keeps the body");

        //Bad input is rejected
        checkRejects(() -> new BusMessage(256, 0, body), "topic 256 rejected");
        checkRejects(() -> new BusMessage(-1, 0, body), "topic -1 rejected");
        checkRejects(() -> new BusMessage(0, 256, body), "subtopic 256 rejected");
        checkRejects(() -> new BusMessage(0, 0, new byte[5]), "5-byte body rejected");
        checkRejects(() -> new BusMessage(0, 0, null), "null body rejected");
        checkRejects(() -> BusMessage.fromBytes(new byte[7]), "7-byte word rejected");
        checkRejects(() -> BusMessage.fromBytes(null), "null word rejected");



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
