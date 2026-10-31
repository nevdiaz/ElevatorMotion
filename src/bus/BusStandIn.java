package bus;

import java.util.Optional;
import java.util.function.Consumer;

public class BusStandIn implements BusMessageDispatcher{
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
