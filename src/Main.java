import gui.ElevatorViewController;
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

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/NakatomiTower.fxml"));
        Parent root = loader.load();
        ElevatorViewController controller = loader.getController();
        MotionEngine motionEngine = new MotionEngine();
        controller.attach(motionEngine);

        //Kenny commented this out for testing purposes, can't use two clocks. Uncomment when appropriate.
        //new harness.TestHarness(motionEngine, motionEngine::tick).start();


        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NakatomiTower");
        primaryStage.show();

        TripController tripController = new TripController(motionEngine);
        SimulationManager simulationManager =
                new SimulationManager(tripController, motionEngine::tick);
        FloorSequencer floorSequencer = new FloorSequencer(simulationManager);
        AnimationTimer timer = new AnimationTimer() {

            long previousFramesTimeStamp = -1;
            @Override
            public void handle(long now) {
                double elapsedSeconds = 0;
                if (previousFramesTimeStamp != -1) {
                    elapsedSeconds = (now - previousFramesTimeStamp) / 1000000000.0;
                }
                simulationManager.update(now);
                floorSequencer.update(elapsedSeconds);
                previousFramesTimeStamp = now;
            }
        };
        timer.start();
        //simulationManager.goToFloor(5);
    }

    public static void main(String[] args) {launch(args);}
}
