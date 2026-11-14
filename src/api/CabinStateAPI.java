package api;

public interface CabinStateAPI {

    double getPosition();
    double getVelocity();
    Direction getDirection();
    boolean isMoving();
    MotionState getState();

}
