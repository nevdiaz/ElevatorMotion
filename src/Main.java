import api.Direction;
import assembly.CabinAssembly;
import bus.BusCodes;
import bus.BusStandIn;
import gui.CabinView;
import gui.ElevatorViewController;
import harness.EveryTwoHarness;
import harness.LeverStopHarness;
import harness.NudgeHarness;
import harness.TopAndBottomHarness;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import manager.FloorSequencer;
import manager.SimulationManager;

import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    //Elevator 1 uses the first route, the others use the rest, so every car moves in DEMO mode
    private static final int[][] DEMO_ROUTES = {{2, 10, 4, 1}, {5, 1, 8}, {10, 3}, {7, 2, 9}};
    private enum Mode {DEMO, ROUTE_HARNESS, TOP_BOTTOM_HARNESS, NUDGE_HARNESS, LEVER_TOP_HARNESS, LEVER_BOTTOM_HARNESS, EVERY_TWO_HARNESS}
    private static final Mode MODE = Mode.DEMO; //TODO: Change mode to one of the enums to run it

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/NakatomiTower.fxml"));
        Parent root = loader.load();
        ElevatorViewController controller = loader.getController();

        //Four cars on one bus; each has its own view for the GUI
        BusStandIn bus = new BusStandIn();
        List<CabinAssembly> cars = new ArrayList<>();
        List<CabinView> views = new ArrayList<>();
        for (int n = BusCodes.ELEVATOR_1; n <= BusCodes.ELEVATOR_4; n++) {
            CabinAssembly car = new CabinAssembly(n, bus.connect());
            cars.add(car);
            views.add(new CabinView("Elevator " + n, car.engine(), car.trip()::getNudgingFloor));
        }
        CabinAssembly car1 = cars.get(0);   //the scenarios drive elevator 1

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NakatomiTower");
        primaryStage.show();
        controller.start();
        controller.setCabins(views);

        if (MODE == Mode.ROUTE_HARNESS) {
            harness.TestHarness testHarness = new harness.TestHarness(car1.trip(), car1.engine()::tick);
            testHarness.setBeforeStep(car1.gateway()::update);
            testHarness.start();
        } else {
            //One simulation manager per car; the slider sets the speed of all of them
            List<SimulationManager> managers = new ArrayList<>();
            for (CabinAssembly car : cars) {
                SimulationManager manager = new SimulationManager(car.trip(), car.engine()::tick);
                manager.setBeforeStep(car.gateway()::update);
                managers.add(manager);
            }
            controller.setSpeedHandler(scale -> managers.forEach(m -> m.setTimeScale(scale)));

            //The demo routes only run in DEMO mode, scenarios drive elevator 1 over the bus.
            List<FloorSequencer> sequencers = new ArrayList<>();
            if (MODE == Mode.DEMO) {
                for (int i = 0; i < cars.size(); i++) {
                    sequencers.add(new FloorSequencer(cars.get(i).trip(), DEMO_ROUTES[i], 2.0));
                }
            }

            //Add additional harnesses here. Each one runs before elevator 1's gateway:
            SimulationManager manager1 = managers.get(0);
            Runnable gateway1 = car1.gateway()::update;
            if (MODE == Mode.TOP_BOTTOM_HARNESS) {
                TopAndBottomHarness scenario = new TopAndBottomHarness(bus.connect());
                manager1.setBeforeStep(() -> { scenario.update(); gateway1.run(); });
            } else if (MODE == Mode.NUDGE_HARNESS) {
                NudgeHarness scenario = new NudgeHarness(bus.connect(), car1.trip());
                manager1.setBeforeStep(() -> { scenario.update(); gateway1.run(); });
            } else if (MODE == Mode.LEVER_TOP_HARNESS || MODE == Mode.LEVER_BOTTOM_HARNESS) {
                Direction runaway = (MODE == Mode.LEVER_TOP_HARNESS) ? Direction.Up : Direction.Down;
                LeverStopHarness scenario = new LeverStopHarness(car1.engine(), bus.connect(),
                        BusCodes.ELEVATOR_1, runaway, 40);
                manager1.setBeforeStep(() -> { scenario.update(); gateway1.run(); });
            } else if (MODE == Mode.EVERY_TWO_HARNESS) {
                EveryTwoHarness scenario = new EveryTwoHarness(bus.connect());
                manager1.setBeforeStep(() -> { scenario.update(); gateway1.run(); });
            }

            AnimationTimer timer = new AnimationTimer() {
                long previousFramesTimeStamp = -1;
                @Override
                public void handle(long now) {
                    double elapsedSeconds = 0;
                    if (previousFramesTimeStamp != -1) {
                        elapsedSeconds = (now - previousFramesTimeStamp) / 1000000000.0;
                    }
                    for (SimulationManager manager : managers) {
                        manager.update(now);
                    }
                    for (FloorSequencer sequencer : sequencers) {
                        sequencer.update(elapsedSeconds);
                    }
                    previousFramesTimeStamp = now;
                }
            };
            timer.start();
        }
    }

    public static void main(String[] args) {launch(args);}
}