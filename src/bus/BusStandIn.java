package bus;

import java.util.*;

public class BusStandIn {

    private final List<Connection> connections = new ArrayList<>();
    private static final int MAX_QUEUE_SIZE = 1024;

    public BusMessageDispatcher connect() {
        Connection connection = new Connection();
        connections.add(connection);
        return connection;
    }

    //Helper Methods
    private static int key(int topic, int subtopic) {
        return topic * 256 + subtopic;
    }

    private class Connection implements BusMessageDispatcher {

        private final Map<Integer, Deque<BusMessage>> queues = new HashMap<>();


        @Override
        public void register() {
            //The real bus will print a line
        }

        @Override
        public void subscribe(int topic, int subtopic) {
            if (topic < 1 || topic > BusCodes.MAX_TOPIC || subtopic <= 0 || subtopic > 255) {
                throw new IllegalArgumentException("Invalid topic or subtopic");
            }
            //Subscribing twice must not wipe the queue, computeIfAbsent handles this
            queues.computeIfAbsent(key(topic, subtopic), k -> new ArrayDeque<>());
        }

        @Override
        public void unsubscribe(int topic, int subtopic) {
            if (topic < 1 || topic > BusCodes.MAX_TOPIC || subtopic <= 0 || subtopic > 255) {
                throw new IllegalArgumentException("Invalid topic or subtopic");
            }
            queues.remove(key(topic, subtopic));
        }

        @Override
        public void publish(BusMessage message) {
            
        }

        @Override
        public Optional<BusMessage> get(int topic, int subtopic) {
            if (topic < 1 || topic > BusCodes.MAX_TOPIC || subtopic < 0 || subtopic > 255) {
                throw new IllegalArgumentException("topic must be 1 to " + BusCodes.MAX_TOPIC + " and subtopic 0 to 255");
            }
            Deque<BusMessage> queue = queues.get(key(topic, subtopic));
            if (queue == null || queue.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(queue.pollFirst());   //takes the oldest and removes it
        }


    }
}
