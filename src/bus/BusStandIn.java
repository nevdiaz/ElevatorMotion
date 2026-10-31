package bus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class BusStandIn implements BusMessageDispatcher {

    private int nextId = 1;
    Map<Integer, List<Consumer<BusMessage>>> subscribers = new HashMap<>();
    Map<Integer, BusMessage> mostRecentMessage = new HashMap<>();

    @Override
    public void register(String componentName) {

    }

    @Override
    public void subscribe(int topic, Consumer<BusMessage> listener) {

    }

    @Override
    public void publish(BusMessage message) {

    }

    @Override
    public Optional<BusMessage> get(int topic, int subtopic) {
        return Optional.empty();
    }
}
