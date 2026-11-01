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
    private void readCalls() {
        Optional<BusMessage> call = bus.get(BusCodes.CAR_CALL, elevator);
        //Removes the oldest message each time until empty
        while (call.isPresent()) {
            long floor = call.get().value();
            //skips a floor that is already waiting so a repeated button press doesn't queue twice
            if (floor >= 1 && floor <= Integer.MAX_VALUE && !pendingFloors.contains((int) floor)) {
                pendingFloors.addLast((int) floor);
            }
            call = bus.get(BusCodes.CAR_CALL, elevator);
        }
    }

    //Take the next waiting floor when the cabin is free
    private void startNextCall() {
        //Only starts a trip if the cabin is idle
        if (pendingFloors.isEmpty() || trip.isTripActive() || motion.isMoving()) {
            return;
        }
        trip.goToFloor(pendingFloors.pollFirst());
    }

    private void publishChanges() {}

    public void update() {
        readCalls();
        startNextCall();
        publishChanges();
    }


}
