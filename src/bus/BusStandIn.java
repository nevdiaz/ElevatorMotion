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
        }

        @Override
        public void unsubscribe(int topic, int subtopic) {

        }

        @Override
        public void publish(BusMessage message) {

        }

        @Override
        public Optional<BusMessage> get(int topic, int subtopic) {
            return Optional.empty();
        }
    }
}
