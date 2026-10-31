package bus;

import java.util.*;
import java.util.function.Consumer;

public class BusStandIn implements BusMessageDispatcher {

    private int nextId = 1;
    Map<Integer, List<Consumer<BusMessage>>> subscribers = new HashMap<>();
    Map<Integer, BusMessage> mostRecentMessage = new HashMap<>();

    @Override
    public int register(String componentName) {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("componentName must not be null or blank");
        } else {
            return nextId++;
        }
    }

    @Override
    public void subscribe(int topic, Consumer<BusMessage> listener) {
        if (topic < 0 || topic > 255 || listener == null) {
            throw new IllegalArgumentException("topic must be between 0 and 255");
        }
        subscribers.computeIfAbsent(topic, t -> new ArrayList<>()).add(listener);
    }

    @Override
    public void publish(BusMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must be not null");
        }
        mostRecentMessage.put(key(message.getTopic(), message.getSubtopic()), message);
        List<Consumer<BusMessage>> listeners = subscribers.get(message.getTopic());
        if (listeners == null) {
            return;
        }
        for (Consumer<BusMessage> listener : new ArrayList<>(listeners)) {
            listener.accept(message);
        }

    }

    @Override
    public Optional<BusMessage> get(int topic, int subtopic) {
        return Optional.ofNullable(mostRecentMessage.get(key(topic, subtopic)));
    }

    //Helper Methods
        private static int key(int topic, int subtopic) {
        return topic * 256 * subtopic;
    }
}
