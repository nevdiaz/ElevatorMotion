package harness;

import bus.BusCodes;
import bus.BusMessage;
import bus.BusMessageDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Plays the command center: publishes one car call at a time and waits for the car to arrive. */
public class CmdCtrStandIn {

    private final BusMessageDispatcher center;
    private final int elevator;
    private final int[] floors;
    private final int pauseTicks;
    private final List<Integer> arrivals = new ArrayList<>();

    private int next = 0;
    private int waitingFor = 0;      //0 means no call is outstanding
    private int pauseLeft = 0;
    private long lastFloor = -1;     //-1 means nothing heard yet
    private long lastDirection = -1;

    public CmdCtrStandIn(BusMessageDispatcher center, int elevator, int[] floors, int pauseTicks) {
        this.center = center;
        this.elevator = elevator;
        this.floors = floors;
        this.pauseTicks = pauseTicks;
        center.subscribe(BusCodes.CABIN_STATUS, elevator);
        center.subscribe(BusCodes.CAR_DIRECTION, elevator);
    }

    /** Call once per simulation step from the beforeStep hook. */
    public void update() {
        lastFloor = latest(BusCodes.CABIN_STATUS, lastFloor);
        lastDirection = latest(BusCodes.CAR_DIRECTION, lastDirection);

        if (waitingFor != 0) {
            //Arrived means: at the target floor and idle (direction 0).
            if (lastFloor == waitingFor && lastDirection == 0) {
                arrivals.add(waitingFor);
                waitingFor = 0;
                pauseLeft = pauseTicks;
            }
            return;
        }
        if (pauseLeft > 0) {
            pauseLeft -= 1;
            return;
        }
        if (next < floors.length) {
            waitingFor = floors[next];
            next += 1;
            center.publish(BusMessage.of(BusCodes.CABIN_CALL, elevator, waitingFor));
        }
    }

    public boolean isFinished() {
        return next >= floors.length && waitingFor == 0;
    }

    public List<Integer> getArrivals() {
        return arrivals;
    }

    /** Reads everything queued for the topic and returns the newest value or the old one if nothing new. */
    private long latest(int topic, long old) {
        long result = old;
        Optional<BusMessage> m = center.get(topic, elevator);
        while (m.isPresent()) {
            result = m.get().value();
            m = center.get(topic, elevator);
        }
        return result;
    }
}