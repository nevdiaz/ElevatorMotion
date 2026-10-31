package bus;


import java.util.Optional;

//TODO: Follows Team03's code and the Software Bus Codes sheet. Still need to confirm with them:
//  - where the subtopic goes in SUBSCRIBE / UNSUBSCRIBE / GET (their code uses byte 7),
//  - whether their server moves to the 0xF0+ command bytes from the sheet.

public interface BusMessageDispatcher {

    /** Announces this connection to the bus. The bus sends no reply. */
    void register();

    /** Creates a queue for topic and subtopic on this connection. There is no callback, poll with get. */
    void subscribe(int topic, int subtopic);

    /** Removes the queue for topic and subtopic and any messages in it. */
    void unsubscribe(int topic, int subtopic);

    /** Sends a message to every other connection subscribed to its topic. The sender does not receive its own message. */
    void publish(BusMessage message);

    /** Returns the oldest queued message for topic and subtopic, removes it, or empty if there is none. */
    Optional<BusMessage> get(int topic, int subtopic);
}
