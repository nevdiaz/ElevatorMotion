package gui;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.shape.Line;
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
        buildFloors();
        clipViewport();
        placeCabin(0);
        System.out.println(cabinAssembly.getLayoutY()); //TODO: Delete after testing
    }

    private void buildShaft() {
        shaft.setLayoutY(0);
        shaft.setHeight(ViewScale.shaftHeight); //Height is 2840
    }

    private void buildFloors() {
        double feet;
        double floorYPosition;
        for (int i = 1; i <= ViewScale.floorCount; i++) {
            feet = ViewScale.floorToFeet(i);
            floorYPosition = ViewScale.feetToWorldYValue(feet);
            Line floorLine = new Line(shaft.getLayoutX(), floorYPosition, shaft.getLayoutX() + shaft.getWidth(), floorYPosition);
            floorLine.setStroke(Color.GREEN);
            Label floorNumber = new Label(String.valueOf(i));
            floorNumber.setLayoutX(shaft.getLayoutX() - 35);
            floorNumber.setLayoutY(floorYPosition - 10);
            world.getChildren().add(0, floorLine);
            world.getChildren().add(0, floorNumber);
        }
    }

    private void placeCabin(double feet) {
        double floorLineY = ViewScale.feetToWorldYValue(feet);
        double cabinPosition = gondola.getLayoutY() + gondola.getHeight();
        cabinAssembly.setLayoutY(floorLineY - cabinPosition);
    }

    private void clipViewport() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
    }

}
