package assembly;

import bus.BusCabinGateway;
import bus.BusMessageDispatcher;
import manager.TripController;
import sim.MotionEngine;

/**
 * One complete elevator car: motion engine, trip controller and bus gateway.
 * The car number (1 to 4) is also its bus subtopic. Nothing here depends on the GUI.
 */
public class CabinAssembly {

    private final int number;
    private final MotionEngine engine = new MotionEngine();
    private final TripController trip = new TripController(engine);
    private final BusCabinGateway gateway;

    public CabinAssembly(int number, BusMessageDispatcher bus) {
        this.number = number;
        this.gateway = new BusCabinGateway(engine, trip, bus, number);   //rejects numbers outside 1 to 4
    }

    /** One simulation step for this car: bus first, then the controller, then the physics. */
    public void step() {
        gateway.update();
        trip.controlStep();
        engine.tick();
    }

    public int number() { return number; }
    public MotionEngine engine() { return engine; }
    public TripController trip() { return trip; }
    public BusCabinGateway gateway() { return gateway; }
}