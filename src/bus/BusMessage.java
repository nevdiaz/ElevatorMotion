package bus;

public class BusMessage {

    private final int topic;
    private final int subtopic;
    private final byte[] body;

    public final int WORD_SIZE = 8;
    public final int BODY_SIZE = 6;

    public BusMessage(int topic, int subtopic, byte[] body) {
        this.topic = topic;
        this.subtopic = subtopic;
        this.body = body;
    }


}
