package api;

/** One consistent reading of the cabin, taken under a single lock. */
public record CabinSnapshot(double position, double velocity, Direction direction,
                            MotionState state, int alignedFloor,
                            boolean topLimitTripped, boolean bottomLimitTripped) {}
