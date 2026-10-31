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
    public int register(String componentName) {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("componentName must be not null or blank");
        } else {
            return nextId++;
        }
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

    //Helper Methods
        private static int key(int topic, int subtopic) {
        return topic * 256 * subtopic;
    }
}
