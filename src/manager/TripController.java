package manager;

import api.Direction;
import api.MotionControllerAPI;
import sim.MotionEngine;

public class TripController {

    private double targetPosition;
    private boolean tripActive = false;
    private static final double levelTolerance = MotionEngine.NUDGE_STEP / 2.0 + 0.01; //half a step, so a nudge can always land inside it
    private static final int ticksBetweenSteps = 3; //Helps slow down the elevator stepping up and down
    private int levelWait;
    private final MotionControllerAPI motionController;
    private int targetFloor;
    private double brakeDelay = 0.0; //Feet to brake later than normal. Only scenarios change this.

    public TripController(MotionControllerAPI  motionController) {
        this.motionController = motionController;
    }

    public synchronized boolean isTripActive() {
        return tripActive;
    }

    /**
     * Scenario hook: makes the car brake this many feet later than normal, so it passes the
     * target floor's sensors and then has to be nudged back. 0 (the default) is normal braking.
     * Keep it under the 1 ft lever margin or the car will trip a safety lever.
     */
    public synchronized void setBrakeDelay(double feet) {
        if (Double.isNaN(feet) || feet < 0) {
            throw new IllegalArgumentException("brake delay must be 0 or more: " + feet);
        }
        brakeDelay = feet;
    }

    /**
     * The floor whose sensors are being corrected by nudging, or 0 when no nudging is happening.
     * Nudging means a trip is active, the car has stopped, and it is not level with the target yet.
     */
    public synchronized int getNudgingFloor() {
        if (tripActive && motionController.isMoving() == false
                && Math.abs(motionController.getPosition() - targetPosition) > levelTolerance) {
            return targetFloor;
        }
        return 0;
    }

    public synchronized void goToFloor(int floor) {
        if (floor < 1 || floor > MotionEngine.FLOORS) {
            System.out.println("Floor must be 1 through 10.");
            return;
        }
        if (tripActive == true || motionController.isMoving() == true) {
            System.out.println("Elevator is busy.");
            return;
        }
        if (motionController.isFloorAligned(floor) == true) {
            System.out.println("Elevator has already arrived.");
            return;
        }
        if (motionController.isTopLimitTripped() || motionController.isBottomLimitTripped()) {
            System.out.println("A safety lever has tripped. The elevator is locked out.");
            return;
        }
        targetPosition = MotionEngine.floorPosition(floor);
        targetFloor = floor;
        tripActive = true;
        levelWait = ticksBetweenSteps;
        if (targetPosition > motionController.getPosition()) {
            motionController.startMotor(Direction.Up);
        } else {
            motionController.startMotor(Direction.Down);
        }
    }

    public synchronized void controlStep() {
        if (motionController.isTopLimitTripped() || motionController.isBottomLimitTripped()) {
            if (tripActive) {
                tripActive = false;
                System.out.println("Trip abandoned: a safety lever tripped at "
                        + String.format("%.2f", motionController.getPosition()) + " ft");
            }
            return;
        }
        if (tripActive == false) {
            return;
        }
        if (motionController.isMoving() == true) {
            double remaining = Math.abs(targetPosition - motionController.getPosition());
            if (remaining <= MotionEngine.brakingDistance(motionController.getVelocity()) - brakeDelay) {
                motionController.stopMotor();
            }
        } else {
            double error = motionController.getPosition() - targetPosition;
            if (Math.abs(error) > levelTolerance) {

                if (levelWait > 0) {
                    levelWait -= 1;
                    return;
                }

                if (error > 0) {
                    motionController.stepDnMotor();
                } else {
                    motionController.stepUpMotor();
                }
                levelWait = ticksBetweenSteps;
                return;
            }
            tripActive = false;
            System.out.println("Arrived at " + String.format("%.2f", motionController.getPosition())
                    + " ft, floor " + targetFloor);
        }
    }
}