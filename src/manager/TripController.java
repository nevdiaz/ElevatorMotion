package manager;

import api.Direction;
import api.MotionControllerAPI;
import sim.MotionEngine;

public class TripController {

    private double targetPosition;
    private boolean tripActive = false;
    private static final double levelTolerance = 0.11; //Little over half a step (0.1 ft)
    private static final int ticksBetweenSteps = 10; //Helps slow down the elevator stepping up and down
    private int levelWait;
    private final MotionControllerAPI motionController;
    private int targetFloor;

    public TripController(MotionControllerAPI  motionController) {
        this.motionController = motionController;
    }

    public boolean isTripActive() {
        return tripActive;
    }

    public void goToFloor(int floor) {
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

    public void controlStep() {
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
            if (remaining <= MotionEngine.brakingDistance(motionController.getVelocity())) {
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
