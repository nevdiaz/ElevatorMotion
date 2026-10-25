import gui.ElevatorViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sim.MotionEngine;

public class Main extends Application {

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
    }

    public static void main(String[] args) {
        launch(args);
    }
}
