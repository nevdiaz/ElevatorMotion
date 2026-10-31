package bus;


import java.util.Optional;
import java.util.function.Consumer;

//TODO: These are assumptions as we don't have Team03's software bus details, ERASE when/if necessary
public interface BusMessageDispatcher {

    //ASSUMPTION: The bus hands back an id for the component
    int register(String componentName);

    //ASSUMPTION: We assume the delivery is by callback
    void subscribe(int topic, Consumer<BusMessage> listener);

    //ASSUMPTION: publish sens one message to every subscriber of its topic
    void publish(BusMessage message);

    //ASSUMPTION: returns the latest message with for the topic and subtopic
    Optional<BusMessage> get(int topic, int subtopic);
}
