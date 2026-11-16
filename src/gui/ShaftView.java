package gui;

import api.CabinSnapshot;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/** Draws one shaft with its cabin. Created by FXMLLoader from Shaft.fxml, one per elevator. */
public class ShaftView {

    @FXML private Pane world;
    @FXML private Rectangle shaft;
    @FXML private Group cabinAssembly;
    @FXML private Rectangle gondola;
    @FXML private Rectangle ceilingSensorLeft;
    @FXML private Rectangle ceilingSensorRight;
    @FXML private Rectangle floorSensorLeft;
    @FXML private Rectangle floorSensorRight;

    private final javafx.scene.control.Label nameLabel = new javafx.scene.control.Label();
    private static final Color sensorsAligned = Color.LIGHTGREEN;
    private static final Color shaftSensorColor = Color.GRAY;
    private static final Color cabinSensorColor = Color.ORANGE;
    private static final Color sensorsFault = Color.RED;
    private static final Color sensorsNudging = Color.YELLOW;   //between orange and lime green
    private static final Color leverColor = Color.DARKRED;
    private static final long blinkNanos = 400_000_000L;        // 0.4 s per on/off phase

    private record FloorMarkers(Rectangle floorLeft, Rectangle floorRight,
                                Rectangle ceilingLeft, Rectangle ceilingRight) {}

    private final List<FloorMarkers> floorMarkers = new ArrayList<>();
    private Line topLever;
    private Line bottomLever;
    private IntSupplier nudgingFloor = () -> 0;   //floor being nudged, or 0
    private double shownFeet = 0;                 //smoothed position that is actually drawn

    @FXML
    private void initialize() {
        buildShaft();
        buildFloors();
        buildLevers();
        placeCabin(0);

        //The following sets up the name label and font and stays readable after world view shrinks
        nameLabel.setStyle("-fx-font-size: 46px; -fx-font-weight: bold;");
        nameLabel.setLayoutX(shaft.getLayoutX());
        nameLabel.setLayoutY(20);
        world.getChildren().add(nameLabel);
    }

    /** The node to put on screen. */
    public Pane getRoot() {
        return world;
    }

    /** Tells this shaft which cabin it draws. Resets the smoothing so the cabin does not glide in. */
    public void show(CabinView view) {
        nudgingFloor = view.nudgingFloor();
        shownFeet = view.cabin().getPosition();
        placeCabin(shownFeet);
    }

    /** Draws one frame from one consistent snapshot. */
    public void render(CabinSnapshot s, long now) {
        shownFeet += (s.position() - shownFeet) * 0.3;   //smooths out some of the jitteriness
        if (Math.abs(s.position() - shownFeet) < 0.001) {
            shownFeet = s.position();
        }
        placeCabin(shownFeet);
        updateSensors(s, now);
    }

    /** Scrolls the shaft so the middle of the cabin sits at the middle of the viewport. */
    public void centerOn(double viewportHeight) {
        double gondolaMiddle = cabinAssembly.getLayoutY() + gondola.getLayoutY() + gondola.getHeight() / 2;
        world.setTranslateY(viewportHeight / 2 - gondolaMiddle);
    }

    private void buildShaft() {
        shaft.setLayoutY(0);
        shaft.setHeight(ViewScale.shaftHeight);
    }

    private void buildFloors() {
        for (int i = 1; i <= ViewScale.floorCount; i++) {
            double floorYPosition = ViewScale.feetToWorldYValue(ViewScale.floorToFeet(i));
            Line floorLine = new Line(shaft.getLayoutX(), floorYPosition,
                    shaft.getLayoutX() + shaft.getWidth(), floorYPosition);
            floorLine.setStroke(Color.GREEN);
            javafx.scene.control.Label floorNumber = new javafx.scene.control.Label(String.valueOf(i));
            floorNumber.setLayoutX(shaft.getLayoutX() - 35);
            floorNumber.setLayoutY(floorYPosition - 10);
            world.getChildren().add(0, floorLine);
            world.getChildren().add(0, floorNumber);
            floorMarkers.add(new FloorMarkers(
                    buildShaftSensor(floorSensorLeft, floorYPosition),
                    buildShaftSensor(floorSensorRight, floorYPosition),
                    buildShaftSensor(ceilingSensorLeft, floorYPosition),
                    buildShaftSensor(ceilingSensorRight, floorYPosition)));
        }
    }

    private Rectangle buildShaftSensor(Rectangle cabinSensor, double floorLineY) {
        double x = cabinAssembly.getLayoutX() + cabinSensor.getLayoutX();
        double y = floorLineY + cabinSensor.getLayoutY() - gondolaBottomOffset();
        Rectangle shaftSensor = new Rectangle(x, y, cabinSensor.getWidth(), cabinSensor.getHeight());
        shaftSensor.setFill(shaftSensorColor);
        shaftSensor.setStroke(Color.BLACK);
        world.getChildren().add(0, shaftSensor);
        return shaftSensor;
    }

    private void buildLevers() {
        topLever = buildLever(ViewScale.floorToFeet(ViewScale.floorCount) + ViewScale.leverMargin);
        bottomLever = buildLever(-ViewScale.leverMargin);
    }

    private Line buildLever(double feet) {
        double y = ViewScale.feetToWorldYValue(feet);
        Line lever = new Line(shaft.getLayoutX(), y, shaft.getLayoutX() + shaft.getWidth(), y);
        lever.setStroke(leverColor);
        lever.setStrokeWidth(3);
        world.getChildren().add(0, lever);
        return lever;
    }

    private double gondolaBottomOffset() {
        return gondola.getLayoutY() + gondola.getHeight();
    }

    private void placeCabin(double feet) {
        double floorLineY = ViewScale.feetToWorldYValue(feet);
        cabinAssembly.setLayoutY(floorLineY - gondolaBottomOffset());
    }

    private void setSensorColor(Rectangle sensor, boolean lit, Color offColor) {
        sensor.setFill(lit ? sensorsAligned : offColor);
    }

    private void blinkSensor(Rectangle sensor, boolean on, Color offColor) {
        sensor.setFill(on ? sensorsFault : offColor);
    }

    private boolean blinkOn(long nowNanos) {
        return (nowNanos / blinkNanos) % 2 == 0;
    }

    private void updateSensors(CabinSnapshot s, long now) {
        boolean anyAligned = s.alignedFloor() != 0;
        for (int i = 1; i <= ViewScale.floorCount; i++) {
            boolean aligned = (s.alignedFloor() == i);
            FloorMarkers markers = floorMarkers.get(i - 1);
            setSensorColor(markers.floorLeft(), aligned, shaftSensorColor);
            setSensorColor(markers.floorRight(), aligned, shaftSensorColor);
            setSensorColor(markers.ceilingLeft(), aligned, shaftSensorColor);
            setSensorColor(markers.ceilingRight(), aligned, shaftSensorColor);
        }
        setSensorColor(floorSensorLeft, anyAligned, cabinSensorColor);
        setSensorColor(floorSensorRight, anyAligned, cabinSensorColor);
        setSensorColor(ceilingSensorLeft, anyAligned, cabinSensorColor);
        setSensorColor(ceilingSensorRight, anyAligned, cabinSensorColor);

        //While the car is nudged onto its target floor, show the in-between color on that
        //floor's sensors and the cabin's sensors. They turn lime green once the car is level.
        int nudging = nudgingFloor.getAsInt();
        if (nudging >= 1 && nudging <= ViewScale.floorCount) {
            FloorMarkers target = floorMarkers.get(nudging - 1);
            target.floorLeft().setFill(sensorsNudging);
            target.floorRight().setFill(sensorsNudging);
            target.ceilingLeft().setFill(sensorsNudging);
            target.ceilingRight().setFill(sensorsNudging);
            floorSensorLeft.setFill(sensorsNudging);
            floorSensorRight.setFill(sensorsNudging);
            ceilingSensorLeft.setFill(sensorsNudging);
            ceilingSensorRight.setFill(sensorsNudging);
        }

        boolean topFault = s.topLimitTripped();
        boolean bottomFault = s.bottomLimitTripped();
        if (topFault || bottomFault) {
            boolean on = blinkOn(now);
            Line trippedLever = topFault ? topLever : bottomLever;
            trippedLever.setStroke(on ? sensorsFault : leverColor);
            FloorMarkers endFloor = floorMarkers.get(topFault ? ViewScale.floorCount - 1 : 0);
            blinkSensor(endFloor.floorLeft(), on, shaftSensorColor);
            blinkSensor(endFloor.floorRight(), on, shaftSensorColor);
            blinkSensor(endFloor.ceilingLeft(), on, shaftSensorColor);
            blinkSensor(endFloor.ceilingRight(), on, shaftSensorColor);
            blinkSensor(floorSensorLeft, on, cabinSensorColor);
            blinkSensor(floorSensorRight, on, cabinSensorColor);
            blinkSensor(ceilingSensorLeft, on, cabinSensorColor);
            blinkSensor(ceilingSensorRight, on, cabinSensorColor);
        }
    }
}