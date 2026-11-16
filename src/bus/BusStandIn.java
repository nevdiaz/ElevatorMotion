package bus;

import java.util.*;

/**
 * In-memory stand-in for the software bus. Safe to use from several threads.
 * One lock guards every connection so a publish from one elevator's thread can never
 * corrupt a queue that another elevator's thread is reading.
 */
public class BusStandIn {

    private final List<Connection> connections = new ArrayList<>();
    private static final int MAX_QUEUE_SIZE = 1024;

    //One lock for the whole bus. Never one lock per connection: publish touches two connections
    //at once, and two locks taken in opposite orders by two threads would deadlock.
    private final Object lock = new Object();

    public BusMessageDispatcher connect() {
        synchronized (lock) {
            Connection connection = new Connection();
            connections.add(connection);
            return connection;
        }
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
            if (topic < 1 || topic > BusCodes.MAX_TOPIC || subtopic < 0 || subtopic > 255) {
                throw new IllegalArgumentException("Invalid topic or subtopic");
            }
            //Subscribing twice must not wipe the queue, computeIfAbsent handles this
            synchronized (lock) {
                //Subscribing twice must not wipe the queue, computeIfAbsent handles this
                queues.computeIfAbsent(key(topic, subtopic), k -> new ArrayDeque<>());
            }
        }

        @Override
        public void unsubscribe(int topic, int subtopic) {
            if (topic < 1 || topic > BusCodes.MAX_TOPIC || subtopic < 0 || subtopic > 255) {
                throw new IllegalArgumentException("Invalid topic or subtopic");
            }
            synchronized (lock) {
                queues.remove(key(topic, subtopic));
            }
        }

        @Override
        public void publish(BusMessage message) {
            if (message == null || message.getTopic() < 1 || message.getTopic() > BusCodes.MAX_TOPIC) {
                throw new IllegalArgumentException("message must not be null and its topic must be 1 to " + BusCodes.MAX_TOPIC);
            }
            synchronized (lock) {
                for (Connection other : connections) {
                    if (other != this) {
                        other.receive(message);
                    }
                }
            }
        }

        private void receive(BusMessage message) {
            int topic = message.getTopic();
            int subtopic = message.getSubtopic();

            if (subtopic == BusCodes.ALL) {
                for (Map.Entry<Integer, Deque<BusMessage>> entry : queues.entrySet()) {
                    if (entry.getKey() / 256 == topic) {
                        addIfRoom(entry.getValue(), message);
                    }
                }
            } else {
                Deque<BusMessage> queue = queues.get(key(topic, subtopic));
                if (queue != null) {
                    addIfRoom(queue, message);
                }
            }
        }

        private void addIfRoom(Deque<BusMessage> queue, BusMessage message) {
            if (queue.size() < MAX_QUEUE_SIZE) {
                queue.addLast(message);
            }
        }

        @Override
        public Optional<BusMessage> get(int topic, int subtopic) {
            if (topic < 1 || topic > BusCodes.MAX_TOPIC || subtopic < 0 || subtopic > 255) {
                throw new IllegalArgumentException("topic must be 1 to " + BusCodes.MAX_TOPIC + " and subtopic 0 to 255");
            }
            synchronized (lock) {
                Deque<BusMessage> queue = queues.get(key(topic, subtopic));
                if (queue == null || queue.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(queue.pollFirst());   //takes the oldest and removes it
            }
        }


    }
}
