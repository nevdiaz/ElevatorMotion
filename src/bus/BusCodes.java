package bus;

public final class BusCodes {

    //These numbers come from the Software Bus Code Sheet from Team03's repo
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
    public static final int CABIN_STATUS = 3;
    public static final int CAR_DIRECTION = 4;
    public static final int UP_CALL = 5;
    public static final int CABIN_CALL = 6;
    public static final int DOOR_STATE = 7;

    //Subtopics
    public static final int ALL = 0;
    public static final int ELEVATOR_1 = 1;
    public static final int ELEVATOR_2 = 2;
    public static final int ELEVATOR_3 = 3;
    public static final int ELEVATOR_4 = 4;

    //Limits
    public static final int FIRST_COMMAND = 0xF0;
    public static final int MAX_TOPIC = 0xEF;

    //TODO: not on the Software Bus Codes sheet yet. Confirm with the other teams.
    public static final int CAR_FAULT = 8;          //subtopic = elevator, body = fault code
    public static final int FAULT_TOP_LIMIT = 1;    //cabin hit the lever above floor 10
    public static final int FAULT_BOTTOM_LIMIT = 2; //cabin hit the lever below floor 1
}
