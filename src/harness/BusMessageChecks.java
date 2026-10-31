package harness;


import bus.BusMessage;

import java.util.Arrays;

import static bus.BusMessage.of;

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

        System.out.println(checksRun + " checks run, " + checksFailed + " failed");
        if (checksFailed > 0) {
            System.exit(1);
        }

    }

    //Needed these checks after incorporating Team03's repo
    private static void numberChecks() {
        //A small number lands in the last body byte
        BusMessage five = BusMessage.of(1, 1, 5);
        check(Arrays.equals(five.getBody(), new byte[]{0, 0, 0, 0, 0, 5}), "of(5) fills the last body byte");
        check(five.value() == 5, "value() returns 5");
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
