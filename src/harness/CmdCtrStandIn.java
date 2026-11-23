package harness;

import Utilities.Message;
import bus.BusCodes;
import bus.BusMessage;
import bus.BusMessageDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Plays the command center: publishes one cabin call at a time and waits for the car to arrive. */
public class CmdCtrStandIn {

    private final BusMessageDispatcher center;
    private final int elevator;
    private final int[] floors;
    private final int pauseTicks;
    private final List<Integer> arrivals = new ArrayList<>();

    private int next = 0;
    private int waitingFor = 0;      //0 means no call is outstanding
    private int pauseLeft = 0;
    private boolean lastStopped = false;   //newest CABIN_STATUS: was the car stopped
    private int lastFloor = -1;            //newest CABIN_STATUS: floor, -1 means nothing heard yet

    public CmdCtrStandIn(BusMessageDispatcher center, int elevator, int[] floors, int pauseTicks) {
        this.center = center;
        this.elevator = elevator;
        this.floors = floors;
        this.pauseTicks = pauseTicks;
        center.subscribe(BusCodes.CABIN_STATUS, elevator);
    }

    /** Call once per simulation step from the beforeStep hook. */
    public void update() {
        readStatus();

        if (waitingFor != 0) {
            //Arrived means: the newest CABIN_STATUS says stopped at the target floor.
            if (lastStopped && lastFloor == waitingFor) {
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
            center.publish(Message.ofPayload(BusCodes.CABIN_CALL, elevator, waitingFor));
        }
    }

    public boolean isFinished() {
        return next >= floors.length && waitingFor == 0;
    }

    public List<Integer> getArrivals() {
        return arrivals;
    }

    /** Reads everything queued for CABIN_STATUS and keeps the newest one. */
    private void readStatus() {
        Optional<Message> m = center.get(BusCodes.CABIN_STATUS, elevator);
        while (m.isPresent()) {
            lastStopped = m.get().flag(1);
            lastFloor = m.get().payload(2);
            m = center.get(BusCodes.CABIN_STATUS, elevator);
        }
    }
}