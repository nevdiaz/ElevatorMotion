package bus;

public final class BusCodes {

    //Empty constructor so no one can instantiate this like ViewScale
    public BusCodes() {}

    //Commands
    public static final int REGISTER = 0xF0;
    public static final int SUBSCRIBE = 0xF1;
    public static final int UNSUBSCRIBE = 0xF2;
    public static final int GET = 0xF3;
    public static final int NO_MESSAGE = 0xF4;

    //Topics
    public static final int FIRE = 1;
    public static final int MODE = 2;
    public static final int CAR_POSITION = 3;
    public static final int CAR_DIRECTION = 4;
    public static final int HALL_CALL = 5;
    public static final int CAR_CALL = 6;
    public static final int DOOR_STATE = 7;

    
}
