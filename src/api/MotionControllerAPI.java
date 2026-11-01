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

    /** True once the car has hit the safety lever above floor 10. */
    default boolean isTopLimitTripped() { return false; }

    /** True once the car has hit the safety lever below floor 1. */
    default boolean isBottomLimitTripped() { return false; }
}
