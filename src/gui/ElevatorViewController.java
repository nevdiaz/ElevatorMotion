package gui;

import api.CabinSnapshot;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import java.io.IOException;
import java.util.List;
import java.util.function.DoubleConsumer;

public class ElevatorViewController {

    @FXML private Label acceleration;
    @FXML private Label cruisingSpeed;
    @FXML private Label currentFloor;
    @FXML private Label deceleration;
    @FXML private ComboBox<String> elevatorSelector;
    @FXML private Label motionState;
    @FXML private Label speed;
    @FXML private LineChart<Number, Number> speedChart;
    @FXML private ToggleButton viewMode;
    @FXML private StackPane viewport;
    @FXML private Label speedScaleLabel;
    @FXML private Slider speedSlider;

    private DoubleConsumer speedHandler;   //set from Main; null means no speed control
    private double speedScale = 1.0;

    private NumberAxis timeAxis;
    private NumberAxis speedAxis;
    private double graphTime = 0.0;        //running elapsed time
    private final double sampleInterval = 0.1;
    private double nextSampleTime = 0.0;
    private final double graphWindow = 30.0;   //Number of seconds the x axis shows
    private final XYChart.Series<Number, Number> speedSeries = new XYChart.Series<>();

    private ShaftView shaftView;
    private CabinView view;                //null until show() is called
    private List<CabinView> cabins = List.of();   //the cars the selector can show

    @FXML
    private void initialize() {
        currentFloor.setText("Current Floor");
        motionState.setText("Current Motion State");
        cruisingSpeed.setText("Cruising Speed: 4.0 ft/s");
        acceleration.setText("Acceleration: 2.0 ft/s^2");
        deceleration.setText("Deceleration: 2.0 ft/s^2");
        buildSpeedChart();
        buildSpeedControl();
        loadShaft();
        clipViewport();
        viewport.heightProperty().addListener((observable, oldValue, newValue) -> updateCamera());
        elevatorSelector.getSelectionModel().selectedIndexProperty().addListener((observable, oldIndex, newIndex) -> {
            int i = newIndex.intValue();
            if (i >= 0 && i < cabins.size()) {
                show(cabins.get(i));
            }
        });
        updateCamera();
    }

    /** Starts the render loop once. Call after the stage is shown. */
    public void start() {
        AnimationTimer timer = new AnimationTimer() {
            long previousTimeStamp = -1;
            @Override
            public void handle(long now) {
                if (view == null) { return; }
                CabinSnapshot s = view.cabin().snapshot();   //one consistent read per frame
                double elapsed = 0.0;
                if (previousTimeStamp != -1) { elapsed = (now - previousTimeStamp) / 1000000000.0; }
                shaftView.render(s, now);
                updateLabels(s);
                updateCamera();
                updateGraph(elapsed * speedScale, s);
                previousTimeStamp = now;
            }
        };
        timer.start();
    }

    /** Switches the display to another cabin. */
    public void show(CabinView view) {
        this.view = view;
        shaftView.show(view);
        speedSeries.getData().clear();   //restart the speed graph
        graphTime = 0.0;
        nextSampleTime = 0.0;
        timeAxis.setLowerBound(0);
        timeAxis.setUpperBound(graphWindow);
    }

    public void setSpeedHandler(DoubleConsumer handler) {
        this.speedHandler = handler;
        speedSlider.setDisable(handler == null);
    }

    private void loadShaft() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Shaft.fxml"));
            viewport.getChildren().add(loader.load());
            shaftView = loader.getController();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load Shaft.fxml", e);
        }
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

    private void buildSpeedControl() {
        speedSlider.setMin(0.5);
        speedSlider.setMax(8.0);
        speedSlider.setValue(1.0);
        speedSlider.setMajorTickUnit(0.5);
        speedSlider.setMinorTickCount(0);
        speedSlider.setSnapToTicks(true);
        speedSlider.setDisable(true);          // enabled once Main provides a handler
        speedScaleLabel.setText("Animation speed: 1.0x");
        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            speedScale = newValue.doubleValue();
            speedScaleLabel.setText(String.format("Animation speed: %.1fx", speedScale));
            if (speedHandler != null) {
                speedHandler.accept(speedScale);
            }
        });
    }

    private void updateLabels(CabinSnapshot s) {
        currentFloor.setText(s.alignedFloor() == 0 ? "-" : String.valueOf(s.alignedFloor()));
        speed.setText(String.format("%.2f ft/s", Math.abs(s.velocity())));
        if (s.topLimitTripped() || s.bottomLimitTripped()) {
            motionState.setText("EMERGENCY STOP");
            motionState.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            motionState.setText(s.state().name());
            motionState.setStyle("");
        }
    }

    private void updateGraph(double elapsedSeconds, CabinSnapshot s) {
        graphTime += elapsedSeconds;
        if (graphTime >= nextSampleTime) {
            speedSeries.getData().add(new XYChart.Data<>(graphTime, Math.abs(s.velocity())));
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
        if (shaftView != null) {
            shaftView.centerOn(viewport.getHeight());
        }
    }

    private void clipViewport() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
    }
}