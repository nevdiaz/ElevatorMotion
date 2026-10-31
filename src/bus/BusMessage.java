package bus;

public class BusMessage {

    private final int topic;
    private final int subtopic;
    private final byte[] body;

    public static final int WORD_SIZE = 8;
    public static final int BODY_SIZE = 6;

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


}
