package bus;

import Utilities.Message;
import Utilities.Protocol;
import Utilities.Topics;

public final class BusCodes {

    //These numbers come from the approved Software Bus Message Catalog in Team03's repo (Utilities/Topics.java).
    //Empty constructor so no one can instantiate this like ViewScale
    public BusCodes() {}

    //Commands
    public static final int REGISTER = 0xF0;
    public static final int SUBSCRIBE = 0xF1;
    public static final int UNSUBSCRIBE = 0xF2;
    public static final int GET = 0xF3;
    public static final int NO_MESSAGE = 0xF4;

    //Topics. Payload layout in the comment, payload byte 1 is the first byte after the subtopic.
    public static final int FIRE = 1;               //subtopic 0. P1: 1 = fire alert / test fire, 0 = reset
    public static final int ELEVATOR_COMMAND = 2;   //P1: 0 = out of service, 1 = in service (was MODE)
    public static final int CABIN_STATUS = 3;       //P1: stopped (1 = stopped), P2: floor 1-10, P3: direction (was CAR_POSITION)
    //4 is retired (was CAR_DIRECTION, now part of CABIN_STATUS)
    public static final int UP_CALL = 5;            //subtopic 0. P1: floor 1-10 (was HALL_CALL)
    public static final int CABIN_CALL = 6;         //P1: floor 1-10 (was CAR_CALL)
    public static final int DOOR_STATUS = 7;        //P1: fully open, P2: fully closed, P3: obstructed, P4: overweight (was DOOR_STATE)
    public static final int DOWN_CALL = 8;          //subtopic 0. P1: floor 1-10
    public static final int ALIGN = 9;              //P1: 0 none, 1 bottom, 2 top, 3 both
    public static final int MOTOR_COMMAND = 10;     //P1: 0 stop / 1 start, P2: 1 up / 2 down
    public static final int DOOR_COMMAND = 11;      //P1: 0 close / 1 open
    public static final int BUZZER = 12;            //P1: 0 off / 1 on
    public static final int ARRIVAL_CHIME = 13;     //P1: floor 1-10, P2: leaving direction 0 none / 1 up / 2 down
    public static final int KEY = 14;               //P1: 0 off / 1 on
    public static final int GOTO = 15;              //P1: floor 1-10, keep the doors closed on arrival
    public static final int SERVICE = 16;           //P1: floor 1-10, open the doors on arrival
    public static final int CLEAR_REQUEST = 17;     //subtopic 0. P1: hallway floor 1-10, P2: 1 up / 2 down / 3 both
    public static final int CLEAR_BUTTONS = 18;     //P1: elevator panel button 1-10

    //Subtopics
    public static final int ALL = 0;
    public static final int ELEVATOR_1 = 1;
    public static final int ELEVATOR_2 = 2;
    public static final int ELEVATOR_3 = 3;
    public static final int ELEVATOR_4 = Topics.ELEVATOR_4;

    //Payload values
    public static final int DIR_IDLE = 0;           //"idle" in CABIN_STATUS, "none" in ARRIVAL_CHIME
    public static final int DIR_UP = 1;
    public static final int DIR_DOWN = 2;
    public static final int ALIGN_NONE = 0;
    public static final int ALIGN_BOTTOM = 1;
    public static final int ALIGN_TOP = 2;
    public static final int ALIGN_BOTH = 3;

    //Limits
    public static final int FIRST_COMMAND = 0xF0;
    public static final int MAX_TOPIC = 0xEF;

    //Our own message, NOT in the catalog. Team03's catalog uses topics 1 to 18, so ours starts after them.
    //TODO: tell the other teams about topic 20 when we integrate.
    public static final int CAR_FAULT = 20;         //subtopic = elevator, P1 = fault code
    public static final int FAULT_TOP_LIMIT = 1;    //cabin hit the lever above floor 10
    public static final int FAULT_BOTTOM_LIMIT = 2; //cabin hit the lever below floor 1
}