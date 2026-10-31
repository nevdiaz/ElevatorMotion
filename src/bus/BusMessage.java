package bus;

public class BusMessage {

    private final int topic;
    private final int subtopic;
    private final byte[] body;

    public static final WORD_SIZE = 8;
    public static final BODY_SIZE = 6;

    public BusMessage(int topic, int subtopic, byte[] body) {}


}
