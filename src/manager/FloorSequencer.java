package manager;

//This is a demo script
public class FloorSequencer {

    private final TripController controller;
    private final int[] floors;
    private final double pauseSeconds;
    private double pauseLeft;
    private int nextIndex = 0;
    private boolean wasActive = false;

    public FloorSequencer(TripController controller, int[] floors, double pauseSeconds) {
        this.controller = controller;
        this.floors = floors;
        this.pauseSeconds = pauseSeconds;
        this.pauseLeft = pauseSeconds;
    }

    public void update(double elapsedSeconds) {
        if (controller.isTripActive() == true) {
            wasActive = true;
            return;
        } else  {
            if (wasActive) {
                pauseLeft = pauseSeconds;
                wasActive = false;
            }
            pauseLeft -= elapsedSeconds;
            if (pauseLeft <= 0) {
                controller.goToFloor(floors[nextIndex]);
                nextIndex = (nextIndex + 1) % floors.length;
                pauseLeft = pauseSeconds;
            }
        }
    }
}
