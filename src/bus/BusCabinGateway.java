package bus;

import api.MotionControllerAPI;
import manager.TripController;

import java.util.ArrayDeque;
import java.util.Optional;

public class BusCabinGateway {

    private final MotionControllerAPI motion;
    private final TripController trip;
    private final BusMessageDispatcher bus;
    private final int elevator;
    private final ArrayDeque<Integer> pendingFloors = new ArrayDeque<>();
    private int lastFloor = 0;
    private int lastDirection = -1;


    public BusCabinGateway(MotionControllerAPI motion, TripController trip,
                           BusMessageDispatcher bus, int elevator) {

        if (elevator < BusCodes.ELEVATOR_1 || elevator > BusCodes.ELEVATOR_4) {
            throw new IllegalArgumentException("elevator must be 1 to 4: " + elevator);
        }
        this.motion = motion;
        this.trip = trip;
        this.bus = bus;
        this.elevator = elevator;

        bus.register();
        bus.subscribe(BusCodes.CAR_CALL, elevator);
    }

    //Empty the bus into the waiting queue
    private void readCalls() {}

    private void startNextCall() {}

    private void publishChanges() {}

    public void update() {
        readCalls();
        startNextCall();
        publishChanges();
    }


}
