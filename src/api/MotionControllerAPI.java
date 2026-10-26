package api;

public interface MotionControllerAPI {
    void startMotor(Direction direction);
    void stopMotor();
    void stepUpMotor();
    void stepDnMotor();

    double getPosition();
    double getVelocity();
    Direction getDirection();
    boolean isMoving();

    boolean isTopAligned();
    boolean isBottomAligned();
    boolean isFloorAligned(int floor);
    MotionState getState();
}
