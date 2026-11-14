package api;

public interface MotorAPI {

    void startMotor(Direction direction);
    void stopMotor();
    void stepUpMotor();
    void stepDnMotor();

}
