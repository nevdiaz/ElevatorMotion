import bus.BusCabinGateway;
import bus.BusCodes;
import bus.BusStandIn;
import gui.ElevatorViewController;
import harness.TopAndBottomHarness;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import manager.FloorSequencer;
import manager.SimulationManager;
import manager.TripController;
import sim.MotionEngine;

public class Main extends Application {

    private static final int[] DEMO_ROUTE = {2, 10, 4, 1};
    private enum Mode { DEMO, ROUTE_HARNESS, TOP_AND_BOTTOM }
    private static final Mode MODE = Mode.DEMO; //TODO: Change mode to one of the enums to run it

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/NakatomiTower.fxml"));
        Parent root = loader.load();
        ElevatorViewController controller = loader.getController();
        MotionEngine motionEngine = new MotionEngine();
        controller.attach(motionEngine);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NakatomiTower");
        primaryStage.show();
        TripController tripController = new TripController(motionEngine);
        BusStandIn bus = new BusStandIn();
        BusCabinGateway gateway = new BusCabinGateway(motionEngine, tripController, bus.connect(), BusCodes.ELEVATOR_1);

        if (MODE == Mode.ROUTE_HARNESS) {
            harness.TestHarness testHarness = new harness.TestHarness(tripController, motionEngine::tick);
            testHarness.setBeforeStep(gateway::update);
            testHarness.start();
        } else {
            SimulationManager simulationManager =
                    new SimulationManager(tripController, motionEngine::tick);
            simulationManager.setBeforeStep(gateway::update);

            //The demo route only runs in DEMO mode, scenarios drive the car over the bus.
            FloorSequencer floorSequencer = (MODE == Mode.DEMO)
                    ? new FloorSequencer(tripController, DEMO_ROUTE, 2.0)
                    : null;

            if (MODE == Mode.TOP_AND_BOTTOM) {
                TopAndBottomHarness scenario = new TopAndBottomHarness(bus.connect());
                simulationManager.setBeforeStep(() -> { scenario.update(); gateway.update(); });
            } else {
                simulationManager.setBeforeStep(gateway::update);
            }


            AnimationTimer timer = new AnimationTimer() {
                long previousFramesTimeStamp = -1;
                @Override
                public void handle(long now) {
                    double elapsedSeconds = 0;
                    if (previousFramesTimeStamp != -1) {
                        elapsedSeconds = (now - previousFramesTimeStamp) / 1000000000.0;
                    }
                    simulationManager.update(now);
                    if (floorSequencer != null) {
                        floorSequencer.update(elapsedSeconds);
                    }
                    previousFramesTimeStamp = now;
                }
            };
            timer.start();
        }
    }

    public static void main(String[] args) {launch(args);}
}
