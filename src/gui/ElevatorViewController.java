package gui;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class ElevatorViewController {

    @FXML
    private Label acceleration;

    @FXML
    private Group cabinAssembly;

    @FXML
    private Rectangle ceilingSensorLeft;

    @FXML
    private Rectangle ceilingSensorRight;

    @FXML
    private Label currentFloor;

    @FXML
    private Label deceleration;

    @FXML
    private Rectangle doorLeft;

    @FXML
    private Rectangle doorOpening;

    @FXML
    private Rectangle doorRight;

    @FXML
    private ComboBox<?> elevatorSelector;

    @FXML
    private Rectangle floorSensorLeft;

    @FXML
    private Rectangle floorSensorRight;

    @FXML
    private Rectangle gondola;

    @FXML
    private Label motionState;

    @FXML
    private Rectangle motor;

    @FXML
    private Rectangle shaft;

    @FXML
    private Label speed;

    @FXML
    private LineChart<?, ?> speedChart;

    @FXML
    private ToggleButton viewMode;

    @FXML
    private StackPane viewport;

    @FXML
    private Pane world;

    @FXML
    private void initialize() {
        currentFloor.setText("Current Floor");
        motionState.setText("Current Motion State");
        speed.setText("Current Speed");
        acceleration.setText("Current Acceleration");
        deceleration.setText("Current Deceleration");
        buildShaft();
        clipViewport();
    }

    void buildShaft() {
        shaft.setLayoutY(0);
        shaft.setHeight(ViewScale.shaftHeight); //Height is 2840
    }

    private void clipViewport() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
    }

}
