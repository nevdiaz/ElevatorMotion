package bus;

import java.util.Arrays;

public class BusMessage {

    private final int topic;
    private final int subtopic;
    private final byte[] body;

    public static final int WORD_SIZE = 8;
    public static final int BODY_SIZE = 6;
    public static final int HEADER_SIZE = 2;

    public BusMessage(int topic, int subtopic, byte[] body) {


        if (topic < 0 || topic > 255 || subtopic < 0 || subtopic > 255) {
            throw new IllegalArgumentException("topic and subtopic must be a single byte between 0 and 255: "
                    + topic + " " + subtopic);
        }

        if (body == null || body.length != BODY_SIZE) {
            throw new IllegalArgumentException("body must be exactly " + BODY_SIZE + " bytes");
        }

        this.topic = topic;
        this.subtopic = subtopic;
        this.body = body.clone();
    }

    public int getTopic() {
        return topic;
    }

    public int getSubtopic() {
        return subtopic;
    }

    public byte[] getBody() {
        return body.clone();
    }

    public byte[] toBytes() {
        byte[] word = new byte[WORD_SIZE];
        word[0] = (byte) topic;
        word[1] = (byte) subtopic;
        for (int i = 0; i < BODY_SIZE; i++) {
            word[i + HEADER_SIZE] = body[i];
        }
        return word;
    }

    public static BusMessage fromBytes(byte[] word) {
        if (word == null || word.length != WORD_SIZE) {
            throw new IllegalArgumentException("word must be exactly " + WORD_SIZE + " bytes");
        }

        //0xFF is used to prevent bugs with signed integers
        int topic = word[0] & 0xFF;
        int subtopic = word[1] & 0xFF;
        byte[] body = Arrays.copyOfRange(word, HEADER_SIZE, WORD_SIZE);

        return new BusMessage(topic, subtopic, body);
    }


}
