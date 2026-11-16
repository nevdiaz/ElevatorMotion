package gui;

import api.CabinSnapshot;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
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

    private static final double shaftColumnWidth = 300;   //width of one shaft's pane in Shaft.fxml
    private DoubleConsumer speedHandler;   //set from Main; null means no speed control
    private double speedScale = 1.0;
    private NumberAxis timeAxis;
    private NumberAxis speedAxis;
    private double graphTime = 0.0;        //running elapsed time
    private final double sampleInterval = 0.1;
    private double nextSampleTime = 0.0;
    private final double graphWindow = 30.0;   //Number of seconds the x axis shows
    private final XYChart.Series<Number, Number> speedSeries = new XYChart.Series<>();
    private List<CabinView> cabins = List.of();                  //the cars the GUI can show
    private final List<ShaftView> shaftViews = new ArrayList<>(); //one shaft drawing per car, same order
    private int selectedIndex = -1;                               //-1 until a car is selected
    private boolean worldMode = false;                            //false = single view, true = world view
    private final HBox worldBox = new HBox();                     //the world view's row of shafts
    private final Group worldGroup = new Group(worldBox);         //the group makes the scaled size count for layout

    @FXML
    private void initialize() {
        currentFloor.setText("Current Floor");
        motionState.setText("Current Motion State");
        cruisingSpeed.setText("Cruising Speed: 4.0 ft/s");
        acceleration.setText("Acceleration: 2.0 ft/s^2");
        deceleration.setText("Deceleration: 2.0 ft/s^2");
        buildSpeedChart();
        buildSpeedControl();
        clipViewport();
        viewport.heightProperty().addListener((observable, oldValue, newValue) -> updateCamera());
        viewport.widthProperty().addListener((observable, oldValue, newValue) -> updateCamera());
        elevatorSelector.getSelectionModel().selectedIndexProperty().addListener((observable, oldIndex, newIndex) -> {
            int i = newIndex.intValue();
            if (i >= 0 && i < shaftViews.size()) {
                selectCabin(i);
            }
        });
        viewMode.setText("World view");
        viewMode.selectedProperty().addListener((observable, wasOn, isOn) -> {
            worldMode = isOn;
            viewMode.setText(isOn ? "Single view" : "World view");
            if (selectedIndex >= 0) {
                refreshDisplay();
            }
        });
    }

    /** Starts the render loop once. Call after the stage is shown. */
    public void start() {
        AnimationTimer timer = new AnimationTimer() {
            long previousTimeStamp = -1;
            @Override
            public void handle(long now) {
                if (selectedIndex < 0) { return; }
                double elapsed = 0.0;
                if (previousTimeStamp != -1) { elapsed = (now - previousTimeStamp) / 1000000000.0; }
                CabinSnapshot selected = cabins.get(selectedIndex).cabin().snapshot();   //one consistent read per car
                for (int i = 0; i < shaftViews.size(); i++) {
                    if (worldMode || i == selectedIndex) {   //hidden shafts are not drawn
                        CabinSnapshot s = (i == selectedIndex) ? selected : cabins.get(i).cabin().snapshot();
                        shaftViews.get(i).render(s, now);
                    }
                }
                updateLabels(selected);
                updateCamera();
                updateGraph(elapsed * speedScale, selected);
                previousTimeStamp = now;
            }
        };
        timer.start();
    }

    /** Gives the GUI the cars to choose from, builds one shaft per car and shows the first car. */
    public void setCabins(List<CabinView> cabins) {
        this.cabins = List.copyOf(cabins);
        selectedIndex = -1;
        shaftViews.clear();
        for (int i = 0; i < this.cabins.size(); i++) {
            ShaftView shaftView = loadShaft();
            shaftView.show(this.cabins.get(i));
            final int index = i;
            //Clicking a shaft in the world view selects that car
            shaftView.getRoot().setOnMouseClicked(event -> elevatorSelector.getSelectionModel().select(index));
            shaftViews.add(shaftView);
        }
        elevatorSelector.getItems().setAll(this.cabins.stream().map(CabinView::name).toList());
        elevatorSelector.getSelectionModel().selectFirst();
    }

    public void setSpeedHandler(DoubleConsumer handler) {
        this.speedHandler = handler;
        speedSlider.setDisable(handler == null);
    }

    /** The car at this index becomes the selected one: labels and speed graph follow it. */
    private void selectCabin(int index) {
        selectedIndex = index;
        speedSeries.getData().clear();   //restart the speed graph
        graphTime = 0.0;
        nextSampleTime = 0.0;
        timeAxis.setLowerBound(0);
        timeAxis.setUpperBound(graphWindow);
        refreshDisplay();
    }

    /** Puts the right shafts on screen for the current view mode. */
    private void refreshDisplay() {
        viewport.getChildren().clear();
        for (int i = 0; i < shaftViews.size(); i++) {
            shaftViews.get(i).setSelected(worldMode && i == selectedIndex);
        }
        if (worldMode) {
            worldBox.getChildren().clear();
            for (int i = 0; i < shaftViews.size(); i++) {
                shaftViews.get(i).resetCamera();
                shaftViews.get(i).show(cabins.get(i));   //no glide from a stale position
                worldBox.getChildren().add(shaftViews.get(i).getRoot());
            }
            viewport.getChildren().add(worldGroup);
        } else {
            worldBox.getChildren().clear();              //take the shafts out of the row first
            ShaftView selected = shaftViews.get(selectedIndex);
            selected.show(cabins.get(selectedIndex));
            viewport.getChildren().add(selected.getRoot());
        }
        updateCamera();
    }

    private ShaftView loadShaft() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Shaft.fxml"));
            loader.load();
            return loader.getController();
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

    /** Single view: scroll the shaft so the cabin stays centered. World view: scale all shafts to fit. */
    private void updateCamera() {
        if (selectedIndex < 0) {
            return;
        }
        if (worldMode) {
            double fit = Math.min(viewport.getHeight() / ViewScale.shaftHeight,
                    viewport.getWidth() / (shaftViews.size() * shaftColumnWidth));
            double scale = Math.max(fit, 0.01);   //never zero: a zero scale cannot be inverted
            worldBox.setScaleX(scale);
            worldBox.setScaleY(scale);
        } else {
            shaftViews.get(selectedIndex).centerOn(viewport.getHeight());
        }
    }

    private void clipViewport() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
    }
}