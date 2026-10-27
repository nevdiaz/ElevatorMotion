package gui;

import api.MotionControllerAPI;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
    private Label cruisingSpeed;

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
    private ComboBox<String> elevatorSelector;

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
    private LineChart<Number, Number> speedChart;

    @FXML
    private ToggleButton viewMode;

    @FXML
    private StackPane viewport;

    @FXML
    private Pane world;

    private NumberAxis timeAxis;

    private NumberAxis speedAxis;

    private double graphTime = 0.0; //running elapsed time

    private final double sampleInterval = 0.1;

    private double nextSampleTime = 0.0;

    private final double graphWindow = 30.0; //Number of seconds the x axis shows

    private final XYChart.Series<Number, Number> speedSeries = new XYChart.Series<>();

    private MotionControllerAPI motion;

    private double shownFeet = 0;

    private static final Color sensorsAligned = Color.LIGHTGREEN;

    private static final Color shaftSensorColor = Color.GRAY;

    private static final Color cabinSensorColor = Color.ORANGE;

    private record FloorMarkers(Rectangle floorLeft, Rectangle floorRight, Rectangle ceilingLeft, Rectangle ceilingRight) {}

    private final List<FloorMarkers> floorMarkers = new ArrayList<>();

    @FXML
    private void initialize() {
        currentFloor.setText("Current Floor");
        motionState.setText("Current Motion State");
        cruisingSpeed.setText("Cruising Speed: 4.0 ft/s");
        acceleration.setText("Acceleration: 2.0 ft/s^2");
        deceleration.setText("Deceleration: 2.0 ft/s^2");
        buildSpeedChart();
        buildShaft();
        buildFloors();
        clipViewport();
        placeCabin(0);
        viewport.heightProperty().addListener((observable, oldValue, newValue)
                -> {updateCamera();});
        updateCamera();
    }

    private void startRenderLoop() {
        AnimationTimer timer = new AnimationTimer() {
            long previousTimeStamp = -1;
            @Override
            public void handle(long now) {
                double elapsed = 0.0;
                double feet = motion.getPosition();
                shownFeet += (feet - shownFeet) * 0.3; //this should smooth out some of the jitteriness
                if (previousTimeStamp != -1) {elapsed = ((now - previousTimeStamp) / 1000000000.0);}
                if (Math.abs(feet - shownFeet) < 0.001) { shownFeet = feet; } //checks if the gap is tiny
                placeCabin(shownFeet);
                updateSensors();
                updateLabels();
                updateCamera();
                updateGraph(elapsed);
                previousTimeStamp = now;
            }
        };
        timer.start();
    }

    //Has to be public so that it works in main
    public void attach(MotionControllerAPI motion) {
        this.motion = motion;
        startRenderLoop();
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

    private void buildSpeedChart() {
        timeAxis = (NumberAxis) speedChart.getXAxis();

        speedAxis = (NumberAxis) speedChart.getYAxis();

        timeAxis.setAutoRanging(false);
        timeAxis.setLowerBound(0);
        timeAxis.setUpperBound(30);
        timeAxis.setTickUnit(5);
        timeAxis.setLabel("Time (s)");

        speedAxis.setAutoRanging(false);
        speedAxis.setLowerBound(0);
        speedAxis.setUpperBound(5);
        speedAxis.setTickUnit(1);
        speedAxis.setLabel("Speed (ft/s)");

        speedSeries.setName("Speed");
        speedChart.getData().add(speedSeries);
        speedChart.setLegendVisible(false);
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

    private void setSensorColor(Rectangle sensor, boolean lit, Color offColor) {
        if (lit == true) {
            sensor.setFill(sensorsAligned);
        } else {
            sensor.setFill(offColor);
        }
    }

    private void updateLabels() {
        int floor = 0;
        for (int i = 1; i <= ViewScale.floorCount; i++) {
            if (motion.isFloorAligned(i) == true) {
                floor = i;
            }
        }

        if (floor == 0) {
            currentFloor.setText("-");
        } else {
            currentFloor.setText(floor + "");
        }

        speed.setText(String.format("%.2f ft/s", Math.abs(motion.getVelocity())));
        motionState.setText(motion.getState().name());
    }

    private void updateGraph(double elapsedSeconds) {
        graphTime += elapsedSeconds;
        if (graphTime >= nextSampleTime) {
            speedSeries.getData().add(new XYChart.Data<>(graphTime, Math.abs(motion.getVelocity())));
            nextSampleTime += sampleInterval;
        }

        if (graphTime > graphWindow) {
            timeAxis.setUpperBound(graphTime);
            timeAxis.setLowerBound(graphTime - graphWindow);

            while (!speedSeries.getData().isEmpty()
                    && speedSeries.getData().get(0).getXValue().doubleValue() < timeAxis.getLowerBound()) {
                speedSeries.getData().remove(0);
            }
        }
    }

    private void updateCamera() {
        double liveHeight = viewport.getHeight()/2;
        double gondolaView = cabinAssembly.getLayoutY() + gondola.getLayoutY() + gondola.getHeight()/2;
        world.setTranslateY(liveHeight - gondolaView);
    }

    private void updateSensors() {
        boolean anyAligned = false;
        for (int i = 1; i <= ViewScale.floorCount; i++) {
            boolean aligned = motion.isFloorAligned(i);
            FloorMarkers markers = floorMarkers.get(i-1);
            setSensorColor(markers.floorLeft(), aligned, shaftSensorColor);
            setSensorColor(markers.floorRight(), aligned, shaftSensorColor);
            setSensorColor(markers.ceilingLeft(), aligned, shaftSensorColor);
            setSensorColor(markers.ceilingRight(), aligned, shaftSensorColor);
            if (aligned == true) {
                anyAligned = true;
            }
        }
        setSensorColor(floorSensorLeft, anyAligned, cabinSensorColor);
        setSensorColor(floorSensorRight, anyAligned, cabinSensorColor);
        setSensorColor(ceilingSensorLeft, anyAligned, cabinSensorColor);
        setSensorColor(ceilingSensorRight, anyAligned, cabinSensorColor);
    }

    private void clipViewport() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
    }

}
