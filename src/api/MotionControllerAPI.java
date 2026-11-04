package api;

public interface MotionControllerAPI {
    //Motor
    void startMotor(Direction direction);
    void stopMotor();
    void stepUpMotor();
    void stepDnMotor();

    //Cabin
    double getPosition();
    double getVelocity();
    Direction getDirection();
    boolean isMoving();
    MotionState getState();

    //Sensors
    boolean isTopAligned();
    boolean isBottomAligned();
    boolean isFloorAligned(int floor);


    //Levers
    /** True once the car has hit the safety lever above floor 10. */
    default boolean isTopLimitTripped() { return false; }

    /** True once the car has hit the safety lever below floor 1. */
    default boolean isBottomLimitTripped() { return false; }
}
