package checks;


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

        //Values above 127 survive (the signed-byte case)
        BusMessage high = new BusMessage(200, 255, new byte[]{-1, -128, 127, 0, 1, 2});
        BusMessage highCopy = BusMessage.fromBytes(high.toBytes());
        check(highCopy.getTopic() == 200, "topic 200 survives the round trip");
        check(highCopy.getSubtopic() == 255, "subtopic 255 survives the round trip");
        check(Arrays.equals(highCopy.getBody(), high.getBody()), "high body bytes survive");

        //Layout on the wire
        byte[] word = original.toBytes();
        check(word.length == 8, "a word is 8 bytes");
        check(word[0] == 7, "byte 0 is the topic");
        check(word[1] == 9, "byte 1 is the subtopic");
        check(Arrays.equals(Arrays.copyOfRange(word, 2, 8), body), "bytes 2 to 7 are the body");


        //Bad input is rejected
        checkRejects(() -> new BusMessage(256, 0, body), "topic 256 rejected");
        checkRejects(() -> new BusMessage(-1, 0, body), "topic -1 rejected");
        checkRejects(() -> new BusMessage(0, 256, body), "subtopic 256 rejected");
        checkRejects(() -> new BusMessage(0, 0, new byte[5]), "5-byte body rejected");
        checkRejects(() -> new BusMessage(0, 0, null), "null body rejected");
        checkRejects(() -> BusMessage.fromBytes(new byte[7]), "7-byte word rejected");
        checkRejects(() -> BusMessage.fromBytes(null), "null word rejected");

        //Immutability
        byte[] mutable = {1, 2, 3, 4, 5, 6};
        BusMessage message = new BusMessage(1, 1, mutable);
        mutable[0] = 99;
        check(message.getBody()[0] == 1, "changing the input array does not change the message");
        message.getBody()[0] = 77;
        check(message.getBody()[0] == 1, "changing the returned body does not change the message");

        numberChecks();
        payloadChecks();

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }

    }

    //Needed these checks after incorporating Team03's repo
    private static void numberChecks() {
        //A small numbers lands in the last body byte
        BusMessage five = BusMessage.of(1, 1, 5);
        check(Arrays.equals(five.getBody(), new byte[]{0, 0, 0, 0, 0, 5}), "of(5) fills the last body byte");
        check(five.value() == 5, "value() returns 5");

        //Big-endian: 256 is {1, 0} at the end, not {0, 1}
        BusMessage big = BusMessage.of(1, 1, 256);
        check(Arrays.equals(big.getBody(), new byte[]{0, 0, 0, 0, 1, 0}), "256 is stored big-endian");

        //Largest 48-bit value
        BusMessage max = BusMessage.of(1, 1, BusMessage.MAX_VALUE);
        check(Arrays.equals(max.getBody(), new byte[]{-1, -1, -1, -1, -1, -1}), "MAX_VALUE fills all six bytes");
        check(max.value() == BusMessage.MAX_VALUE, "MAX_VALUE round trips");

        //Zero
        check(BusMessage.of(1, 1, 0).value() == 0, "zero round trips");

        //Out of range
        checkRejects(() -> BusMessage.of(1, 1, -1), "negative value rejected");
        checkRejects(() -> BusMessage.of(1, 1, BusMessage.MAX_VALUE + 1), "value above 48 bits rejected");

        //equals and hashCode
        BusMessage a = BusMessage.of(3, 1, 7);
        BusMessage b = BusMessage.of(3, 1, 7);
        check(a.equals(b), "messages with the same fields are equal");
        check(a.hashCode() == b.hashCode(), "equal messages have the same hash code");
        check(!a.equals(BusMessage.of(3, 1, 8)), "different body is not equal");
        check(!a.equals(BusMessage.of(4, 1, 7)), "different topic is not equal");
    }

    //Catalog messages put each field in its own byte, starting at payload byte 1 (Team03's Message layout)
    private static void payloadChecks() {
        //Same layout as Team03's cabinStatus: stopped, floor, direction in payload bytes 1 to 3
        BusMessage status = BusMessage.ofPayload(3, 2, 1, 7, 0);
        check(Arrays.equals(status.getBody(), new byte[]{1, 7, 0, 0, 0, 0}), "payload bytes fill the body from the front");
        check(status.payload(1) == 1 && status.payload(2) == 7 && status.payload(3) == 0, "payload(n) reads back each byte");
        check(status.flag(1) && !status.flag(3), "flag(n) is true for non-zero");
        check(status.payload(6) == 0, "unused payload bytes are 0");

        //A floor sits in payload byte 1, not the last byte like of()
        BusMessage call = BusMessage.ofPayload(6, 1, 5);
        check(call.payload(1) == 5, "a floor is in payload byte 1");
        check(call.toBytes()[2] == 5 && call.toBytes()[7] == 0, "floor 5 is wire index 2, not wire index 7");
        check(BusMessage.of(6, 1, 5).payload(6) == 5, "of(5) is payload byte 6 (the old style)");

        //Bytes above 127 survive
        check(BusMessage.ofPayload(1, 0, 200).payload(1) == 200, "payload 200 reads back unsigned");

        //Survives the wire
        BusMessage copy = BusMessage.fromBytes(status.toBytes());
        check(copy.equals(status) && copy.payload(2) == 7, "a payload message survives the round trip");

        //Bad input
        checkRejects(() -> BusMessage.ofPayload(1, 0, 256), "payload byte 256 rejected");
        checkRejects(() -> BusMessage.ofPayload(1, 0, -1), "negative payload byte rejected");
        checkRejects(() -> BusMessage.ofPayload(1, 0, 1, 2, 3, 4, 5, 6, 7), "seven payload bytes rejected");
        checkRejects(() -> status.payload(0), "payload(0) rejected");
        checkRejects(() -> status.payload(7), "payload(7) rejected");
        check(BusMessage.ofPayload(1, 0).equals(BusMessage.of(1, 0, 0)), "no payload bytes is an all-zero body");
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
