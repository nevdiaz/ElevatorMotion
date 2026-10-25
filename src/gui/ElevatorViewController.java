package gui;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.List;

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
    private Color sensorsAligned = Color.LIGHTGREEN;

    @FXML
    private Color shaftSensorColor = Color.GREEN;

    @FXML
    private Color cabinSensorColor = Color.ORANGE;

    private static final double fakeSpeed = 4.0;

    long startTime = -1;

    private record FloorMarkers(Rectangle floorLeft, Rectangle floorRight, Rectangle ceilingLeft, Rectangle ceilingRight) {}

    private final List<FloorMarkers> floorMarkers = new ArrayList<>();

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
        viewport.heightProperty().addListener((observable, oldValue, newValue)
                -> {updateCamera();});
        updateCamera();
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (startTime < 0) {
                    startTime = now;
                }

                double elapsedSeconds = (now - startTime)/1000000000.0;
                double feet = fakePosition(elapsedSeconds);
                placeCabin(feet);
                updateCamera();

            }
        };
        timer.start();
    }

    private double fakePosition(double elapsedSeconds) {
        double feet = elapsedSeconds * fakeSpeed;
        double cycle = 2 * ViewScale.totalTravelBetweenFloorOneAndTen;
        feet = feet % cycle;

        if (feet > ViewScale.totalTravelBetweenFloorOneAndTen) {
            feet = cycle - feet;
        }

        return feet;
    }

    private void buildShaft() {
        shaft.setLayoutY(0);
        shaft.setHeight(ViewScale.shaftHeight); //Height is 2840
    }

    private Rectangle buildShaftSensor(Rectangle cabinSensor, double floorLineY) {
        double x = cabinAssembly.getLayoutX() + cabinSensor.getLayoutX();
        double y = floorLineY + cabinSensor.getLayoutY() - gondolaBottomOffset();
        Rectangle shaftSensor = new Rectangle(x, y, cabinSensor.getWidth(), cabinSensor.getHeight());
        shaftSensor.setFill(Color.GRAY);
        shaftSensor.setStroke(Color.BLACK);
        world.getChildren().add(0, shaftSensor);
        return shaftSensor;
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
            floorMarkers.add(
                    new FloorMarkers(
                            buildShaftSensor(floorSensorLeft, floorYPosition),
                            buildShaftSensor(floorSensorRight, floorYPosition),
                            buildShaftSensor(ceilingSensorLeft, floorYPosition),
                            buildShaftSensor(ceilingSensorRight, floorYPosition)));
        }
    }

    private double gondolaBottomOffset() {
        return gondola.getLayoutY() + gondola.getHeight();
    }

    private void placeCabin(double feet) {
        double floorLineY = ViewScale.feetToWorldYValue(feet);
        cabinAssembly.setLayoutY(floorLineY - gondolaBottomOffset());
    }

    private void updateCamera() {
        double liveHeight = viewport.getHeight()/2;
        double gondolaView = cabinAssembly.getLayoutY() + gondola.getLayoutY() + gondola.getHeight()/2;
        world.setTranslateY(liveHeight - gondolaView);
    }

    private void clipViewport() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
    }

}
