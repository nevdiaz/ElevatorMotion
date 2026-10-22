package gui;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class SimulatorController {

    @FXML
    private StackPane Viewport;

    @FXML
    private Pane World;

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
    private ToggleButton viewMode;

}
