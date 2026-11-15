package assembly;

import manager.FloorSequencer;
import manager.SimulationManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs one car on its own thread. This thread is the only writer of the car: the gateway,
 * trip controller, scenario hook, demo route and physics all run here, one after another.
 * The GUI never calls into the car except through read-only snapshots.
 */
public class CabinRunner {

    private static final long POLL_MILLIS = 5;   //the manager's accumulator makes the exact rate unimportant

    private final String name;
    private final SimulationManager manager;
    private final FloorSequencer sequencer;      //demo route, may be null
    private ScheduledExecutorService executor;
    private long lastNanos = -1;                 //only touched by the car's own thread
    private volatile Throwable failure;

    public CabinRunner(String name, SimulationManager manager, FloorSequencer sequencer) {
        this.name = name;
        this.manager = manager;
        this.sequencer = sequencer;
    }

    /** Starts the car's thread. Calling it twice does nothing. */
    public synchronized void start() {
        if (executor != null) {
            return;
        }
        lastNanos = -1;
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, name);
            thread.setDaemon(true);              //never keeps the program alive after the window closes
            return thread;
        });
        executor.scheduleWithFixedDelay(this::runOnce, 0, POLL_MILLIS, TimeUnit.MILLISECONDS);
    }
    
}