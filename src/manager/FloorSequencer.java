package manager;

//This is a demo script
public class FloorSequencer {

    private final SimulationManager manager;

    private final int[] floors = {2,10,4,1};

    private int nextIndex = 0;

    private final double pauseSeconds = 2.0;

    private double pauseLeft = pauseSeconds;

    private boolean wasActive = false;

    public FloorSequencer(SimulationManager manager) {
        this.manager = manager;
    }

    public void update(double elapsedSeconds) {
        if (manager.isTripActive() == true) {
            wasActive = true;
            return;
        } else  {
            if (wasActive) {
                pauseLeft = pauseSeconds;
                wasActive = false;
            }
            pauseLeft -= elapsedSeconds;
            if (pauseLeft <= 0) {
                manager.goToFloor(floors[nextIndex]);
                nextIndex = (nextIndex + 1) % floors.length;
                pauseLeft = pauseSeconds;
            }
        }
    }
}
