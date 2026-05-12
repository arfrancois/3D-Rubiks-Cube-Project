package com.example.three_dimensional_rubiks_cube;

import javafx.animation.*;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.PickResult;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.shape.Box;
import javafx.scene.canvas.Canvas;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import static com.example.three_dimensional_rubiks_cube.AISolver.*;

public class CubeApp extends Application {
    private final Map<KeyCode, CubeMove> controls = new HashMap<>();
    //scenes
    protected StackPane globalRoot;
    protected BorderPane mainLayout;
    protected Scene scene;
    protected Canvas confettiCanvas;
    protected VBox winUI;
    protected StackPane centerStack;
    protected VBox optionsDrawer;
    protected VBox sideBar;
    protected MenuBar menuBar;
    protected StackPane loadingOverlay;
    //cube state
    protected boolean firstFaceCompleted = false;
    protected int firstFaceCompletedMoves = -1;
    protected boolean whiteCrossCompleted = false;
    protected int whiteCrossCompletedMoves = -1;
    protected String firstColorCompleted;
    protected HashMap<Character, Character> orientationMap = new HashMap<>();
    //special modes
    protected boolean playGroundMode = false;
    protected boolean immersiveMode = false;
    protected boolean sideBarAlreadyHidden = false;
    protected boolean menuBarAlreadyHidden = false;
    protected boolean alreadyScrambled = false;
    //rotation logic
    protected boolean isAlreadyRotating = false;
    protected int numberOfRotations = 0;
    protected boolean isMenuOpen = false;
    protected boolean stopWatchVisible = true;
    //confetti
    protected ArrayList<ConfettiPiece> pieces = new ArrayList<>();
    protected AnimationTimer confettiEngine;
    //pro mode
    protected int observationSeconds = 15;
    protected boolean aiActive = false;
    protected RotateTransition currentRotation;
    //cube
    Rotate xRotate = new Rotate(0, Rotate.X_AXIS);
    Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);
    ArrayList<Cubie> allCubies = new ArrayList<>();
    Group cubeGroup = new Group();
    Cubie[][][] threeDArrayAllCubies = new Cubie[3][3][3];
    //stopwatch
    Stopwatch stopwatch = new Stopwatch();
    protected VBox stopwatchUI = stopwatch.getUI();
    //cube settings
    Slider sizeSlider = new Slider(15, 85, 60);
    Slider xSlider = new Slider(-65, 65, 0);
    Slider ySlider = new Slider(-50, 50, 0);
    Slider animationSpeedSlider = new Slider(15, 600, 150);
    int animationSpeed = 150;
    private double lastMouseX, lastMouseY;
    private double angleX = 0;
    private double angleY = 0;
    private double cubeSize = 60;
    //AI logic
    private AISolver aiSolver;
    private boolean aiTraining;

    public static String colorToString(Color color) {
        if (color.equals(Color.RED)) return "red";
        if (color.equals(Color.GREEN)) return "green";
        if (color.equals(Color.BLUE)) return "blue";
        if (color.equals(Color.YELLOW)) return "yellow";
        if (color.equals(Color.DARKORANGE)) return "orange";
        if (color.equals(Color.WHITE)) return "white";
        return null;
    }

    @Override
    public void start(Stage stage) {

        initializeOrientationMap();
        initializeCubeState();
        initializeControls();

        SubScene cubeSubScene = new SubScene(cubeGroup, 100, 100, true, SceneAntialiasing.BALANCED);
        cubeSubScene.setFill(Color.TRANSPARENT);
        setUpCamera(cubeSubScene);

        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: lightsalmon;");

        centerStack = new StackPane(cubeSubScene);
        cubeSubScene.widthProperty().bind(centerStack.widthProperty());
        cubeSubScene.heightProperty().bind(centerStack.heightProperty());

        mainLayout.setCenter(centerStack);
        centerStack.getChildren().add(stopwatchUI);
        StackPane.setAlignment(stopwatchUI, Pos.TOP_RIGHT);
        StackPane.setMargin(stopwatchUI, new Insets(30));

        globalRoot = new StackPane();
        globalRoot.getChildren().add(mainLayout);

        setUpSideBar();
        createMenuBar();

        cubeGroup.getTransforms().addAll(xRotate, yRotate);

        scene = new Scene(globalRoot, 1080, 720);
        mouseEvents(cubeSubScene);
        keyboardEvents(scene);

        stage.setTitle("Aramis' 3D Rubik's Cube Project");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    private void mouseEvents(SubScene scene) {
        //Mouse Drag Logic
        scene.setOnMousePressed(event -> {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });


        scene.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - lastMouseX;
            double deltaY = event.getSceneY() - lastMouseY;

            angleY -= deltaX * 0.2;
            angleX += deltaY * 0.2;

            xRotate.setAngle(angleX);
            yRotate.setAngle(angleY);

            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });
        //Going to "Cheat" and recolor cube based on where pressed
        scene.setOnMouseClicked(event -> {
            if (AISolver.hasTrained) return;
            if (event.isStillSincePress()) {

                // This looks into the 3D space to see what was hit
                PickResult result = event.getPickResult();
                Node intersectedNode = result.getIntersectedNode();

                if (intersectedNode instanceof Box clickedSticker) { //specific sticker that was clicked
                    Cubie cubieClicked = (Cubie) clickedSticker.getParent();
                    angleX = 0;
                    angleY = 0;
                    xRotate.setAngle(angleX);
                    yRotate.setAngle(angleY);
                    if (cubieClicked.x == 0 && cubieClicked.y == 0) {
                        if (cubieClicked.z == -1) return;
                        updateColors(allCubies, 'y', -1);
                        updateColors(allCubies, 'y', -1);
                        rotateOrientationY(-1);
                        rotateOrientationY(-1);
                    } else if (cubieClicked.x == 0 && cubieClicked.z == 0) {
                        int dir = cubieClicked.y == -1 ? 1 : -1;
                        updateColors(allCubies, 'x', dir);
                        rotateOrientationX(dir);
                    } else if (cubieClicked.z == 0 && cubieClicked.y == 0) {
                        int dir = cubieClicked.x == 1 ? 1 : -1;
                        updateColors(allCubies, 'y', dir);
                        rotateOrientationY(dir);
                    }
                }
            }
        });

        //sensor logic
        scene.setOnMouseMoved(e -> {
            if (immersiveMode) {
                double mouseX = e.getSceneX();
                if (sideBarAlreadyHidden && mouseX <= 50) {
                    showSideBarAnimation();
                    sideBarAlreadyHidden = false;
                } else if (!sideBarAlreadyHidden && mouseX > sideBar.getWidth() + 10) {
                    hideSideBarAnimation();
                    sideBarAlreadyHidden = true;
                    optionsDrawer.setVisible(false);
                }
                double mouseY = e.getSceneY();
                if (menuBarAlreadyHidden && mouseY <= 35) {
                    showMenuBarAnimation();
                    menuBarAlreadyHidden = false;
                } else if (!menuBarAlreadyHidden && mouseY > menuBar.getHeight() + 20) {
                    hideMenuBarAnimation();
                    menuBarAlreadyHidden = true;
                }
            }
        });

    }

    private void keyboardEvents(Scene scene) {
        if (aiActive) return;
        scene.setOnKeyPressed(event -> {
            if (isMenuOpen || (winUI != null && globalRoot.getChildren().contains(winUI))) {
                return;
            }
            KeyCode keyCode = event.getCode();
            try {
                if (keyCode == KeyCode.SPACE || keyCode == KeyCode.ENTER) {
                    stopwatch.startOrStop();//stop/start timer if space or enter pressed
                } else if (event.isControlDown()) {
                    if (keyCode == KeyCode.S) {
                        startAISolver();
                    } else if (!event.isShiftDown()) {
                        if (keyCode == KeyCode.R) {
                            resetCube();
                        }
                    }
                } else {
                    CubeMove cubeMove = controls.get(keyCode);

                    int finalDirection = cubeMove.direction() * (event.isShiftDown() ? 1 : -1);
                    rotateFace(new CubeMove(cubeMove.axis(), cubeMove.faceValue(), finalDirection));

                    if (stopWatchVisible && !stopwatch.isRunning())
                        stopwatch.startOrStop();//start timer with first cube move
                }
            } catch (Exception e) {
                //IO.println("Error with: " + keyCode);
            }
        });
    }

    public void rotateFace(CubeMove cubeMove) {
        if (isAlreadyRotating) return;
        isAlreadyRotating = true;
        Group tempGroup = new Group();
        ArrayList<Cubie> movingCubies = new ArrayList<>();
        for (Cubie cubie : allCubies) {
            if (cubie.getCoordinate(cubeMove.axis) == cubeMove.faceValue) {
                movingCubies.add(cubie);
            }
        }

        tempGroup.getChildren().addAll(movingCubies);

        cubeGroup.getChildren().add(tempGroup);

        currentRotation = new RotateTransition(Duration.millis(animationSpeed), tempGroup);
        currentRotation.setAxis(cubeMove.axis == 'x' ? Rotate.X_AXIS : (cubeMove.axis == 'y' ? Rotate.Y_AXIS : Rotate.Z_AXIS));

        currentRotation.setByAngle(-90 * cubeMove.direction * cubeMove.faceValue);

        currentRotation.setOnFinished(e -> {
            numberOfRotations++;
            isAlreadyRotating = false;
            updateColorsCubeMove(cubeMove);
            cubeGroup.getChildren().removeAll(movingCubies);
            for (Cubie cubie : movingCubies) {
                cubeGroup.getChildren().add(cubie);
            }
            if (!playGroundMode && alreadyScrambled) {
                if (isCubeSolved()) {
                    aiActive = false;
                    System.out.println("Cube solved!");
                    solvedCubeAnimation(stopwatch.getTime());
                    alreadyScrambled = false;
                    if (stopwatch.isRunning()) stopwatch.startOrStop();
                } else if (!firstFaceCompleted && isFirstFaceDone()) {
                    firstFaceCompleted = true;
                    firstFaceCompletedMoves = numberOfRotations;
                    System.out.println("First face done in " + firstFaceCompletedMoves + " moves!");
                    System.out.println("First face done: " + firstColorCompleted);
                } else if (!whiteCrossCompleted && isWhiteCrossDone()) {
                    whiteCrossCompleted = true;
                    whiteCrossCompletedMoves = numberOfRotations;
                    System.out.println("White cross done in " + whiteCrossCompletedMoves + " moves!");
                }
            }
            if (aiActive && !aiTraining) {
                triggerNextAIMove();
            }
        });
        currentRotation.play();
    }

    private void updateColors(ArrayList<Cubie> cubies, char axis, int direction) {
        record Colors(int x, int y, int z, Color u, Color d, Color f, Color b, Color r, Color l) {

            Colors(Cubie cubie, Color u, Color d, Color f, Color b, Color r, Color l) {
                this(cubie.x, cubie.y, cubie.z, u, d, f, b, r, l);
            }
        }

        ArrayList<Colors> colorsAllCubies = new ArrayList<>();
        Color u, d, f, b, r, l;

        for (Cubie cubie : cubies) {
            Cubie previousCubie;
            if (axis == 'x') {
                previousCubie = threeDArrayAllCubies[cubie.x + 1][-1 * cubie.z * direction + 1][cubie.y * direction + 1]; // (Inverse - since want old cubie)
                u = previousCubie.getFaceColor(direction == -1 ? 'f' : 'b');
                d = previousCubie.getFaceColor(direction == 1 ? 'f' : 'b');
                f = previousCubie.getFaceColor(direction == 1 ? 'u' : 'd');
                b = previousCubie.getFaceColor(direction == -1 ? 'u' : 'd');
                r = previousCubie.getFaceColor('r');
                l = previousCubie.getFaceColor('l');
            } else if (axis == 'y') {
                previousCubie = threeDArrayAllCubies[(-1) * cubie.z * direction + 1][-1 * cubie.y + 1][-1 * cubie.x * direction + 1];
                u = previousCubie.getFaceColor('u');
                d = previousCubie.getFaceColor('d');
                f = previousCubie.getFaceColor(direction == 1 ? 'r' : 'l');
                b = previousCubie.getFaceColor(direction == -1 ? 'r' : 'l');
                r = previousCubie.getFaceColor(direction == -1 ? 'f' : 'b');
                l = previousCubie.getFaceColor(direction == 1 ? 'f' : 'b');
            } else {
                previousCubie = threeDArrayAllCubies[cubie.y * direction + 1][cubie.x * direction + 1][-1 * cubie.z + 1];
                u = previousCubie.getFaceColor(direction == -1 ? 'r' : 'l');
                d = previousCubie.getFaceColor(direction == 1 ? 'r' : 'l');
                f = previousCubie.getFaceColor('f');
                b = previousCubie.getFaceColor('b');
                r = previousCubie.getFaceColor(direction == 1 ? 'u' : 'd');
                l = previousCubie.getFaceColor(direction == -1 ? 'u' : 'd');
            }
            colorsAllCubies.add(new Colors(cubie, u, d, f, b, r, l));
        }

        Cubie cubie;
        for (Colors allCubies : colorsAllCubies) {
            cubie = threeDArrayAllCubies[allCubies.x + 1][-1 * allCubies.y + 1][-1 * allCubies.z + 1];
            cubie.setFaceColor('u', allCubies.u);
            cubie.setFaceColor('d', allCubies.d);
            cubie.setFaceColor('f', allCubies.f);
            cubie.setFaceColor('b', allCubies.b);
            cubie.setFaceColor('r', allCubies.r);
            cubie.setFaceColor('l', allCubies.l);
        }
    }

    /**
     * More efficient, updates all colors concerned by cubeMove
     *
     * @param cubeMove cube move performed
     */
    public void updateColorsCubeMove(CubeMove cubeMove) {

        record Colors(int x, int y, int z, Color u, Color d, Color f, Color b, Color r, Color l) {
        }

        ArrayList<Colors> colorsAllCubies = new ArrayList<>();
        Color u, d, f, b, r, l;
        int x, y, z;
        int direction = cubeMove.direction * cubeMove.faceValue;

        Cubie previousCubie;
        for (int m = -1; m < 2; m++) {
            for (int n = -1; n < 2; n++) {
                if (cubeMove.axis == 'x') {
                    x = cubeMove.faceValue + 1;
                    y = m * direction + 1;
                    z = (-1) * n * direction + 1;
                    previousCubie = threeDArrayAllCubies[x][y][z]; // (Inverse - since want old cubie)
                    u = previousCubie.getFaceColor(direction == 1 ? 'f' : 'b');
                    d = previousCubie.getFaceColor(direction == -1 ? 'f' : 'b');
                    f = previousCubie.getFaceColor(direction == -1 ? 'u' : 'd');
                    b = previousCubie.getFaceColor(direction == 1 ? 'u' : 'd');
                    r = previousCubie.getFaceColor('r');
                    l = previousCubie.getFaceColor('l');
                    colorsAllCubies.add(new Colors(x, (-1) *n + 1, (-1) *m + 1, u, d, f, b, r, l));
                } else if (cubeMove.axis == 'y') {
                    x = m * direction + 1;
                    y = (-1) * cubeMove.faceValue + 1;
                    z = n * direction + 1;
                    previousCubie = threeDArrayAllCubies[x][y][z];
                    u = previousCubie.getFaceColor('u');
                    d = previousCubie.getFaceColor('d');
                    f = previousCubie.getFaceColor(direction == -1 ? 'r' : 'l');
                    b = previousCubie.getFaceColor(direction == 1 ? 'r' : 'l');
                    r = previousCubie.getFaceColor(direction == 1 ? 'f' : 'b');
                    l = previousCubie.getFaceColor(direction == -1 ? 'f' : 'b');
                    colorsAllCubies.add(new Colors(n + 1, y, (-1) *m + 1, u, d, f, b, r, l));
                } else if (cubeMove.axis == 'z') {
                    x = (-1) * m * direction + 1;
                    y = (-1) * n * direction + 1;
                    z = (-1) * cubeMove.faceValue + 1;
                    previousCubie = threeDArrayAllCubies[x][y][z];
                    u = previousCubie.getFaceColor(direction == 1 ? 'r' : 'l');
                    d = previousCubie.getFaceColor(direction == -1 ? 'r' : 'l');
                    f = previousCubie.getFaceColor('f');
                    b = previousCubie.getFaceColor('b');
                    r = previousCubie.getFaceColor(direction == -1 ? 'u' : 'd');
                    l = previousCubie.getFaceColor(direction == 1 ? 'u' : 'd');
                    colorsAllCubies.add(new Colors(n + 1, (-1) *m + 1, z, u, d, f, b, r, l));
                }
            }
        }
        Cubie cubie;
        for (Colors allCubies : colorsAllCubies) {
            cubie = threeDArrayAllCubies[allCubies.x][allCubies.y][allCubies.z];
            cubie.setFaceColor('u', allCubies.u);
            cubie.setFaceColor('d', allCubies.d);
            cubie.setFaceColor('f', allCubies.f);
            cubie.setFaceColor('b', allCubies.b);
            cubie.setFaceColor('r', allCubies.r);
            cubie.setFaceColor('l', allCubies.l);
        }

    }

    private void initializeControls() {
        controls.put(KeyCode.R, new CubeMove('x', 1, 1));
        controls.put(KeyCode.L, new CubeMove('x', -1, 1));
        controls.put(KeyCode.F, new CubeMove('z', -1, 1));
        controls.put(KeyCode.B, new CubeMove('z', 1, 1));
        controls.put(KeyCode.U, new CubeMove('y', -1, 1));
        controls.put(KeyCode.D, new CubeMove('y', 1, 1));
    }

    private void initializeOrientationMap() {
        orientationMap.put('f', 'G'); // Green faces front
        orientationMap.put('b', 'B'); // Blue faces back
        orientationMap.put('u', 'Y'); // Yellow faces up
        orientationMap.put('d', 'W'); // White faces down
        orientationMap.put('r', 'O'); // Orange faces right
        orientationMap.put('l', 'R'); // Red faces left
    }

    protected void initializeCubeState() {
        cubeGroup.getChildren().clear();
        allCubies.clear();
        // Create the 3x3x3 grid
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Cubie cubie = new Cubie(cubeSize, x, y, z);
                    threeDArrayAllCubies[x + 1][-1 * y + 1][-1 * z + 1] = cubie;
                    cubeGroup.getChildren().add(cubie);
                    allCubies.add(cubie);
                    cubie.setCursor(Cursor.HAND);
                }
            }
        }
        applyOrientation();
    }

    private void applyOrientation() {
        char greenTarget = getFaceOfColor('G'); // where is green in current orientation
        char yellowTarget = getFaceOfColor('Y'); // where is yellow in current orientation
        switch (greenTarget) {
            case 'b' -> {
                updateColors(allCubies, 'y', -1);
                updateColors(allCubies, 'y', -1);
                checkYellow('z', 1, yellowTarget);
                /*switch (yellowTarget) {
                    case 'r' -> updateColors(allCubies, 'z', 1);
                    case 'l' -> updateColors(allCubies, 'z', -1);
                    case 'd' -> { updateColors(allCubies, 'z', 1);
                        updateColors(allCubies, 'z', 1); }
                }
                 */

            }
            case 'l' -> {
                updateColors(allCubies, 'y', 1);
                checkYellow('x', -1, yellowTarget);
                /*switch (yellowTarget) {
                    case 'b' -> updateColors(allCubies, 'x', -1);
                    case 'f' -> updateColors(allCubies, 'x', 1);
                    case 'd' -> { updateColors(allCubies, 'x', 1);
                        updateColors(allCubies, 'x', 1); }
                 */
            }
            case 'r' -> {
                updateColors(allCubies, 'y', -1);
                checkYellow('x', -1, yellowTarget);
                /*switch (yellowTarget) {
                    case 'b' -> updateColors(allCubies, 'x', -1);
                    case 'f' -> updateColors(allCubies, 'x', 1);
                    case 'd' -> { updateColors(allCubies, 'x', 1);
                        updateColors(allCubies, 'x', 1); }
                }
                 */

            }
            case 'u' -> {
                updateColors(allCubies, 'x', -1);
                if (yellowTarget != 'b') checkYellow('y', 1, yellowTarget);
                /*switch (yellowTarget) {
                    case 'f' -> {
                        updateColors(allCubies, 'y', 1);
                        updateColors(allCubies, 'y', 1);
                    }
                    case 'r' -> updateColors(allCubies, 'y', 1);
                    case 'l' -> updateColors(allCubies, 'y', -1);
                }
                 */

            }
            case 'd' -> {
                updateColors(allCubies, 'x', 1);
                if (yellowTarget != 'f') checkYellow('y', -1, yellowTarget);
                /*switch (yellowTarget) {
                    case 'b' -> {
                        updateColors(allCubies, 'y', 1);
                        updateColors(allCubies, 'y', 1);
                    }
                    case 'r' -> updateColors(allCubies, 'y', -1);
                    case 'l' ->  updateColors(allCubies, 'y', 1);
                }
                 */
            }
            case 'f' -> {
                checkYellow('z', 1, yellowTarget);
                /*switch (yellowTarget) {
                    case 'r' -> updateColors(allCubies, 'z', 1);
                    case 'l' -> updateColors(allCubies, 'z', -1);
                    case 'd' -> { updateColors(allCubies, 'z', 1);
                        updateColors(allCubies, 'z', 1); }
                }
                 */
            }
        }

    }

    private void checkYellow(char axis, int face, char yellowTarget) {
        switch (yellowTarget) {
            case 'b' -> {
                updateColors(allCubies, axis, face);
                if (axis =='y') updateColors(allCubies, axis, face);
            }
            case 'f' -> {
                updateColors(allCubies, axis, -face);
                if (axis =='y') updateColors(allCubies, axis, -face);
            }
            case 'd' -> { updateColors(allCubies, axis, 1);
                updateColors(allCubies, axis, 1); }
            case 'r' -> updateColors(allCubies, axis, face);
            case 'l' ->  updateColors(allCubies, axis, -face);
        }
    }

    private char getFaceOfColor(char color) {
        for (Map.Entry<Character, Character> entry : orientationMap.entrySet()) {
            if (entry.getValue() == color) return entry.getKey();
        }
        return 'f'; // shouldn't happen
    }

    private void rotateOrientationX(int direction) {
        // direction 1: u->f, f->d, d->b, b->u
        // direction -1: f->u, u->b, b->d, d->f
        char u = orientationMap.get('u');
        char f = orientationMap.get('f');
        char d = orientationMap.get('d');
        char b = orientationMap.get('b');
        if (direction == 1) {
            orientationMap.put('f', u);
            orientationMap.put('d', f);
            orientationMap.put('b', d);
            orientationMap.put('u', b);
        } else {
            orientationMap.put('u', f);
            orientationMap.put('b', u);
            orientationMap.put('d', b);
            orientationMap.put('f', d);
        }
        // r and l don't change on x rotation
    }

    private void rotateOrientationY(int direction) {
        // direction 1: f->r, r->b, b->l, l->f
        char f = orientationMap.get('f');
        char r = orientationMap.get('r');
        char b = orientationMap.get('b');
        char l = orientationMap.get('l');
        if (direction == -1) {
            orientationMap.put('r', f);
            orientationMap.put('b', r);
            orientationMap.put('l', b);
            orientationMap.put('f', l);
        } else {
            orientationMap.put('l', f);
            orientationMap.put('b', l);
            orientationMap.put('r', b);
            orientationMap.put('f', r);
        }
        // u and d don't change on y rotation
    }

    private void setUpCamera(SubScene cubeSubScene) {
        //Camera setup
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-600);
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        cubeSubScene.setCamera(camera);
    }

    private void createMenuBar() {
        menuBar = new MenuBar();
        Menu viewMenu = new Menu("View");
        Menu helpMenu = new Menu("Help");

        MenuItem resetCamera = new MenuItem("Reset Camera");
        resetCamera.setOnAction(e -> {
            angleX = 0;
            angleY = 0;
            xRotate.setAngle(0);
            yRotate.setAngle(0);
        });

        MenuItem immersiveModeItem = new MenuItem("Toggle Immersive Mode On");
        immersiveModeItem.setOnAction(e -> {
            setImmersiveMode(!immersiveMode);
            immersiveModeItem.setText(immersiveMode ? "Toggle Immersive Mode Off" : "Toggle Immersive Mode On");
        });


        MenuItem stopWatch = new MenuItem("Show/Hide Stopwatch");
        stopWatch.setOnAction(e -> {
            stopWatchVisible = !stopWatchVisible;
            makeStopWatchDisappearOrAppear();
        });

        MenuItem helpWithCommands = new MenuItem("Help With Commands");
        helpWithCommands.setOnAction(e -> showHelpOverlay());


        viewMenu.getItems().add(resetCamera);
        viewMenu.getItems().add(immersiveModeItem);
        viewMenu.getItems().add(stopWatch);
        helpMenu.getItems().add(helpWithCommands);
        menuBar.getMenus().add(viewMenu);
        menuBar.getMenus().add(helpMenu);

        centerStack.getChildren().add(menuBar);
        StackPane.setAlignment(menuBar, Pos.TOP_LEFT);
    }

    private void showHelpOverlay() {
        isMenuOpen = true;
        boolean stopWatchPaused = stopwatch.isRunning();
        if (stopWatchPaused) {
            stopwatch.startOrStop();
        }
        // 1. Create a darkened background (the "scrim")
        VBox overlay = new VBox(20);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-padding: 40;");
        overlay.setAlignment(Pos.CENTER);

        // 2. Content
        Label title = new Label("CONTROL GUIDE");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 30px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        addControlRow(grid, 0, "R / Shift+R", "Right Face (R'/R)");
        addControlRow(grid, 1, "L / Shift+L", "Left Face (L'/L)");
        addControlRow(grid, 2, "U / Shift+U", "Top Face (U'/U)");
        addControlRow(grid, 3, "D / Shift+D", "Bottom Face (D'/D)");
        addControlRow(grid, 4, "F / Shift+F", "Front Face (F'/F)");
        addControlRow(grid, 5, "B / Shift+B", "Back Face (B'/B)");
        addControlRow(grid, 6, "Space / Enter", "Start/Stop Timer");

        Button closeBtn = new Button("Back to Cube");
        closeBtn.setCursor(Cursor.HAND);
        closeBtn.setOnAction(e -> {
            globalRoot.getChildren().remove(overlay);
            isMenuOpen = false;
            if (stopWatchPaused) stopwatch.startOrStop();
        });

        overlay.getChildren().addAll(title, grid, closeBtn);

        // 3. Add to the stack (it will appear on top of everything)
        globalRoot.getChildren().addLast(overlay);
    }

    private void addControlRow(GridPane g, int row, String keys, String action) {
        Label l1 = new Label(keys);
        l1.setStyle("-fx-text-fill: #00ff00; -fx-font-family: 'Monospaced';");
        Label l2 = new Label(action);
        l2.setStyle("-fx-text-fill: white;");
        g.addRow(row, l1, l2);
    }

    private void setUpSideBar() {
        // --- 1. THE FIXED SIDEBAR (Left BorderPane) ---
        sideBar = new VBox(20);
        sideBar.setPrefWidth(50);
        sideBar.setAlignment(Pos.TOP_CENTER);
        // Sleek dark sidebar
        sideBar.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 15 0 0 0;");

        // --- 2. THE FLOATING DRAWER (Inside CenterStack) ---
        optionsDrawer = new VBox(20);
        optionsDrawer.setVisible(false);
        optionsDrawer.setMaxWidth(260); // Constrain width so it doesn't cover the whole cube

        // Modern lighter gray for the drawer to contrast with the sidebar
        optionsDrawer.setStyle("-fx-background-color: #3c3f41; -fx-padding: 25; " + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 5, 0);"); // Adds a nice shadow

        setupMainMenu();

        // --- 4. THE TOGGLE BUTTON ---
        Button toggleBtn = new Button("≡");
        toggleBtn.setCursor(Cursor.HAND);
        toggleBtn.setFocusTraversable(false);
        // Style the hamburger menu to blend in
        toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; " + "-fx-font-size: 24px; -fx-padding: 0;");

        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), toggleBtn);

        toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle("-fx-background-color: #3c3f41; -fx-text-fill: white; -fx-font-size: 24px; -fx-padding: 0;"));
        toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 24px; -fx-padding: 0;"));

        toggleBtn.setOnAction(e -> {
            optionsDrawer.setVisible(!optionsDrawer.isVisible());
        });

        sideBar.getChildren().add(toggleBtn);

        // --- 5. PLACEMENT ---
        // The sidebar pushes the center layout slightly
        mainLayout.setLeft(sideBar);

        // The drawer floats ON TOP of the cube inside the center stack
        StackPane.setAlignment(optionsDrawer, Pos.CENTER_LEFT);
        centerStack.getChildren().add(optionsDrawer);
    }

    private void showCubeSettingsMenu() {
        optionsDrawer.getChildren().clear();

        Region spacer = new Region();
        spacer.setPrefHeight(15);

        // B. Settings "Card" (Wraps the slider so it doesn't float around)
        VBox sizeCard = new VBox(10);
        sizeCard.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 8; -fx-padding: 15;");

        Label sizeTitle = new Label("Cube Size");
        sizeTitle.setTextFill(Color.WHITE);
        sizeTitle.setStyle("-fx-font-weight: bold;");

        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMajorTickUnit(15);
        // Custom CSS to make the slider look a bit cleaner
        sizeSlider.setStyle("-fx-control-inner-background: #525659;");

        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            cubeSize = newVal.doubleValue();
            refreshCube();
        });

        sizeCard.getChildren().addAll(sizeTitle, sizeSlider);

        // --- Cube Position Controls ---
        VBox positionCard = new VBox(10);
        positionCard.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 8; -fx-padding: 15;");

        Label positionTitle = new Label("Cube Position (X / Y)");
        positionTitle.setTextFill(Color.WHITE);
        positionTitle.setStyle("-fx-font-weight: bold;");

// X-Axis Slider (Left / Right)
        xSlider.setShowTickMarks(true);
        xSlider.setStyle("-fx-control-inner-background: #525659;");

// Bind the slider directly to the cubeGroup's X position
        cubeGroup.translateXProperty().bind(xSlider.valueProperty());

// Y-Axis Slider (Up / Down)
        ySlider.setShowTickMarks(true);
        ySlider.setStyle("-fx-control-inner-background: #525659;");

// Bind the slider directly to the cubeGroup's Y position
        cubeGroup.translateYProperty().bind(ySlider.valueProperty().multiply(-1));

        positionCard.getChildren().addAll(positionTitle, xSlider, ySlider);

        VBox animationSpeedCard = new VBox(10);
        positionCard.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 8; -fx-padding: 15;");

        animationSpeedSlider.setShowTickLabels(true);
        animationSpeedSlider.setMajorTickUnit(animationSpeed);
        animationSpeedSlider.setStyle("-fx-tick-label-fill: white; -fx-font-size:15px;");
        animationSpeedSlider.setSnapToTicks(true);
        animationSpeedSlider.setShowTickMarks(true);

        Label animationSpeedTitle = new Label("Animation Speed: " + animationSpeed + "ms");
        animationSpeedTitle.setTextFill(Color.WHITE);
        animationSpeedTitle.setStyle("-fx-font-weight: bold;");
        animationSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            this.animationSpeed = newVal.intValue();
            animationSpeedTitle.setText("Animation Speed: " + newVal.intValue() + "ms");
        });

        animationSpeedCard.getChildren().addAll(animationSpeedTitle, animationSpeedSlider);

        String baseStyle = "-fx-background-color: #525659; -fx-text-fill: white; " + "-fx-background-radius: 15; -fx-padding: 10 20 10 20; -fx-font-weight: bold;";
        String hoverStyle = "-fx-background-color: #6a6e73; -fx-text-fill: white; " + "-fx-background-radius: 15; -fx-padding: 10 20 10 20; -fx-font-weight: bold;";

        Label resetCubeSettings = new Label("Reset Cube Settings");

        setOptionDrawerLabelStyles(baseStyle, hoverStyle, resetCubeSettings);
        resetCubeSettings.setOnMouseClicked(e -> {
            cubeSize = 60;
            sizeSlider.setValue(60);
            xSlider.setValue(0);
            ySlider.setValue(0);
            animationSpeed = 150;
            animationSpeedSlider.setValue(animationSpeed);
        });


        Label backButton = new Label("< Back");


        setOptionDrawerLabelStyles(baseStyle, hoverStyle, backButton);

        backButton.setOnMouseClicked(e -> {
            optionsDrawer.getChildren().clear();
            setupMainMenu();
        });

        optionsDrawer.getChildren().addAll(spacer, sizeCard, positionCard, animationSpeedCard, resetCubeSettings, backButton);
    }

    private void setOptionDrawerLabelStyles(String baseStyle, String hoverStyle, Label resetCubeSettings) {
        resetCubeSettings.setStyle(baseStyle);
        resetCubeSettings.setCursor(Cursor.HAND);
        resetCubeSettings.setMaxWidth(Double.MAX_VALUE); // Makes the button stretch to fit the drawer
        resetCubeSettings.setAlignment(Pos.CENTER);

        resetCubeSettings.setOnMouseEntered(e -> resetCubeSettings.setStyle(hoverStyle));
        resetCubeSettings.setOnMouseExited(e -> resetCubeSettings.setStyle(baseStyle));
    }

    private void setupMainMenu() {
        Region spacer = new Region();
        spacer.setPrefHeight(15);

        Label aiLabel = new Label("AI Solver");
        String baseStyle = "-fx-background-color: #525659; -fx-text-fill: white; " + "-fx-background-radius: 15; -fx-padding: 10 20 10 20; -fx-font-weight: bold;";
        String hoverStyle = "-fx-background-color: #6a6e73; -fx-text-fill: white; " + "-fx-background-radius: 15; -fx-padding: 10 20 10 20; -fx-font-weight: bold;";

        setOptionDrawerLabelStyles(baseStyle, hoverStyle, aiLabel);
        aiLabel.setOnMouseClicked(e -> {
            startAISolver();
        });

        Label cubeSettings = new Label("Cube Settings");

        setOptionDrawerLabelStyles(baseStyle, hoverStyle, cubeSettings);
        cubeSettings.setOnMouseClicked(e -> {
            showCubeSettingsMenu();
        });

        Label resetCube = new Label("Reset Cube");

        setOptionDrawerLabelStyles(baseStyle, hoverStyle, resetCube);
        resetCube.setOnMouseClicked(e -> resetCube());

        Label scrambleCube = new Label("Scramble Cube");

        setOptionDrawerLabelStyles(baseStyle, hoverStyle, scrambleCube);
        scrambleCube.setOnMouseClicked(e -> {
            scrambleCube();
        });

        Label proMode = new Label("Pro Mode");

        setOptionDrawerLabelStyles(baseStyle, hoverStyle, proMode);
        proMode.setOnMouseClicked(e -> {
            proMode();
        });

        CheckBox playgroundMode = new CheckBox("Playground Mode");
        playgroundMode.setTextFill(Color.WHITE);
        playgroundMode.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");

        Tooltip guide = new Tooltip("Playground Mode Guide\n" + "- Turns off solved state checks\n" + "- Removes Timer (can put it back on in view settings)\n" + "- Dark Background\n" + "- Hides all distractions");

        guide.setStyle("-fx-background-color: #3d3c3c; -fx-text-fill: lightblue; -fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
        guide.setShowDelay(Duration.millis(200));
        playgroundMode.setTooltip(guide);

        playgroundMode.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            this.playGroundMode = isSelected;

            if (isSelected) {
                mainLayout.setStyle("-fx-background-color: black;");
                setImmersiveMode(true);
            } else if (wasSelected) {
                mainLayout.setStyle("-fx-background-color: lightsalmon;");
                setImmersiveMode(false);
            }
        });

        // Add elements to the drawer
        optionsDrawer.getChildren().addAll(spacer, cubeSettings, resetCube, scrambleCube, proMode, aiLabel, playgroundMode);
    }

    protected void resetCube() {
        if (currentRotation != null) currentRotation.stop();
        isAlreadyRotating = false;

        xSlider.setValue(0);
        ySlider.setValue(0);
        cubeSize = 60;
        sizeSlider.setValue(60);
        angleX = 0;
        angleY = 0;
        xRotate.setAngle(0);
        yRotate.setAngle(0);
        allCubies.clear();
        animationSpeed = 150;
        animationSpeedSlider.setValue(animationSpeed);
        cubeGroup.getChildren().clear();
        initializeCubeState();
        numberOfRotations = 0;
        whiteCrossCompleted = false;
        firstFaceCompleted = false;
        whiteCrossCompletedMoves = -1;
        firstFaceCompletedMoves = -1;
        aiActive = false;
    }

    protected void refreshCube() {
        restoreCube(saveCubeState());
    }

    protected CubieState[][][] saveCubeState() {

        CubeApp.CubieState[][][] memoryBank = new CubeApp.CubieState[3][3][3];

        for (int x = 0; x < 3; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Cubie oldCubie = threeDArrayAllCubies[x][(-1) * y + 1][(-1) * z + 1];
                    if (oldCubie != null) {
                        memoryBank[x][-y + 1][-z + 1] = new CubeApp.CubieState(oldCubie.getFaceColor('u'), oldCubie.getFaceColor('d'), oldCubie.getFaceColor('f'), oldCubie.getFaceColor('b'), oldCubie.getFaceColor('r'), oldCubie.getFaceColor('l'));
                    }
                }
            }
        }
        return memoryBank;
    }

    protected void restoreCube(CubieState[][][] memoryBank) {
        allCubies.clear();
        cubeGroup.getChildren().clear();
        initializeCubeState();

        for (int x = 0; x < 3; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Cubie newCubie = threeDArrayAllCubies[x][(-1) * y + 1][(-1) * z + 1];
                    CubieState savedState = memoryBank[x][-y + 1][-z + 1];

                    if (newCubie != null && savedState != null) {
                        newCubie.setFaceColor('u', savedState.u());
                        newCubie.setFaceColor('d', savedState.d());
                        newCubie.setFaceColor('f', savedState.f());
                        newCubie.setFaceColor('b', savedState.b());
                        newCubie.setFaceColor('r', savedState.r());
                        newCubie.setFaceColor('l', savedState.l());
                    }
                }
            }
        }
    }

    public void scrambleCube() {
        for (int i = 0; i < Math.random() * 10 + 30; i++) { //only need to scramble ~20 times for good 3x3x3 cube scrambling
            updateColorsCubeMove(randomCubeMove());
        }
        alreadyScrambled = true;
    }

    /**
     * Scrambles the cube for numberOfMoves number of random rotations
     *
     * @param numberOfMoves number of random rotations
     */
    public void scrambleCubeFaces(int numberOfMoves) {
        for (int i = 0; i < numberOfMoves; i++) {
            updateColorsCubeMove(randomCubeMove());
        }
    }

    public CubeMove randomCubeMove() {
        char randomAxis = randomFace();
        int randomFace = Math.random() > .5 ? -1 : 1;
        int randomDirection = Math.random() > .5 ? -1 : 1;
        return new CubeMove(randomAxis, randomFace, randomDirection);

    }

    private char randomFace() {
        char[] axes = {'x', 'y', 'z'};
        return axes[(int) (Math.random() * 3)];
    }

    public boolean isCubeSolved() {
        Color frontRef = getCenterColor('z', -1, 'f');
        Color backRef = getCenterColor('z', 1, 'b');
        Color upRef = getCenterColor('y', -1, 'u');
        Color downRef = getCenterColor('y', 1, 'd');
        Color rightRef = getCenterColor('x', 1, 'r');
        Color leftRef = getCenterColor('x', -1, 'l');

        // Check Front Face
        if (!isFaceUniform('z', -1, frontRef)) return false;
        // Check Back Face
        if (!isFaceUniform('z', 1, backRef)) return false;
        // Check Top Face
        if (!isFaceUniform('y', -1, upRef)) return false;
        // Check Bottom Face
        if (!isFaceUniform('y', 1, downRef)) return false;
        //check Right Face
        if (!isFaceUniform('x', 1, rightRef)) return false;
        //check Left Face
        return isFaceUniform('x', -1, leftRef);
    }

    public boolean isFirstFaceDone() {
        ArrayList<Color> colorsSolved = new ArrayList<>();

        Color frontRef = getCenterColor('z', -1, 'f');
        Color backRef = getCenterColor('z', 1, 'b');
        Color upRef = getCenterColor('y', -1, 'u');
        Color downRef = getCenterColor('y', 1, 'd');
        Color rightRef = getCenterColor('x', 1, 'r');
        Color leftRef = getCenterColor('x', -1, 'l');

        // Check Front Face
        if (isFaceUniform('z', -1, frontRef)) colorsSolved.add(frontRef);
        // Check Back Face
        if (isFaceUniform('z', 1, backRef)) colorsSolved.add(backRef);
        // Check Top Face
        if (isFaceUniform('y', -1, upRef)) colorsSolved.add(upRef);
        // Check Bottom Face
        if (isFaceUniform('y', 1, downRef)) colorsSolved.add(downRef);
        //check Right Face
        if (isFaceUniform('x', 1, rightRef)) colorsSolved.add(rightRef);
        //check Left Face
        if (isFaceUniform('x', -1, leftRef)) colorsSolved.add(leftRef);

        if (colorsSolved.isEmpty()) return false;

        StringBuilder firstColor = new StringBuilder();
        for (int i = 0; i < colorsSolved.size(); i++) {
            firstColor.append(colorToString(colorsSolved.get(i)));
            if (i != colorsSolved.size() - 1) firstColor = new StringBuilder(firstColor.append(" and "));
        }
        if (colorsSolved.size() > 1) firstColor.append(" solved at the same time!");
        firstColorCompleted = firstColor.toString();
        return true;
    }

    private boolean isFaceUniform(char axis, int face, Color color) {
        for (Cubie cubie : allCubies) {
            if (cubie.getCoordinate(axis) == face) {
                if (!cubie.getFaceColor(getFaceChar(axis, face)).equals(color)) return false;
            }
        }
        return true;
    }

    public boolean isWhiteCrossDone() {
        CubeFace whiteFace = getWhiteCenterFace();
        if (whiteFace == null) return false;
        char whiteAxis = whiteFace.axis;
        int whitePos = whiteFace.face;
        char whiteFaceSideChar = getFaceChar(whiteAxis, whitePos);

        for (Cubie cubie : allCubies) {
            if (cubie.getCoordinate(whiteAxis) == whitePos && cubie.isEdge(whiteAxis)) {
                if (!cubie.getFaceColor(whiteFaceSideChar).equals(Color.WHITE)) return false;
                if (!isSideMatched(cubie, whiteAxis)) return false;
            }
        }
        return true;
    }

    private boolean isSideMatched(Cubie cubie, char whiteAxis) {
        char sideAxis;
        int sidePos = 0;

        if (whiteAxis == 'y') { // White is Top/Bottom
            if (cubie.x != 0) {
                sideAxis = 'x';
                sidePos = cubie.x;
            } else {
                sideAxis = 'z';
                sidePos = cubie.z;
            }
        } else if (whiteAxis == 'x') { // White is Left/Right
            if (cubie.y != 0) {
                sideAxis = 'y';
                sidePos = cubie.y;
            } else {
                sideAxis = 'z';
                sidePos = cubie.z;
            }
        } else { // White is Front/Back
            if (cubie.x != 0) {
                sideAxis = 'x';
                sidePos = cubie.x;
            } else {
                sideAxis = 'y';
                sidePos = cubie.y;
            }
        }

        char sideFaceChar = getFaceChar(sideAxis, sidePos);
        Color stickerColor = cubie.getFaceColor(sideFaceChar);
        Color centerColor = getCenterColor(sideAxis, sidePos, sideFaceChar);

        return stickerColor.equals(centerColor);
    }

    private char getFaceChar(char axis, int pos) {
        if (axis == 'x') return (pos == 1) ? 'r' : 'l';
        if (axis == 'y') return (pos == 1) ? 'd' : 'u';
        if (axis == 'z') return (pos == 1) ? 'b' : 'f';
        return ' ';
    }

    private CubeFace getWhiteCenterFace() {
        for (Cubie cubie : allCubies) {
            if (cubie.x != 0 && cubie.y == 0 && cubie.z == 0) {
                if (cubie.getFaceColor(cubie.x == 1 ? 'r' : 'l').equals(Color.WHITE)) return new CubeFace('x', cubie.x);
            }
            if (cubie.x == 0 && cubie.y != 0 && cubie.z == 0) {
                if (cubie.getFaceColor(cubie.y == 1 ? 'd' : 'u').equals(Color.WHITE)) return new CubeFace('y', cubie.y);
            }
            if (cubie.x == 0 && cubie.y == 0 && cubie.z != 0) {
                if (cubie.getFaceColor(cubie.z == 1 ? 'b' : 'f').equals(Color.WHITE)) return new CubeFace('z', cubie.z);
            }
        }
        return null; //shouldn't happen
    }

    private Color getCenterColor(char axis, int pos, char face) {
        for (Cubie c : allCubies) {
            if (axis == 'z' && c.z == pos && c.x == 0 && c.y == 0) return c.getFaceColor(face);
            if (axis == 'y' && c.y == pos && c.x == 0 && c.z == 0) return c.getFaceColor(face);
            if (axis == 'x' && c.x == pos && c.y == 0 && c.z == 0) return c.getFaceColor(face);
        }
        return Color.BLACK; // Should never happen
    }

    private void setImmersiveMode(boolean active) {
        immersiveMode = active;
        if (stopWatchVisible == active) {
            stopWatchVisible = !active;
            makeStopWatchDisappearOrAppear();
        }

        if (active) {
            hideSideBarAnimation();
            sideBarAlreadyHidden = true;
            hideMenuBarAnimation();
            menuBarAlreadyHidden = true;
            optionsDrawer.setVisible(false);
        } else {
            showSideBarAnimation();
            sideBarAlreadyHidden = false;
            showMenuBarAnimation();
            menuBarAlreadyHidden = false;
        }

    }

    private void showSideBarAnimation() {
        TranslateTransition sideSlide = new TranslateTransition(Duration.millis(200), sideBar);
        sideSlide.setToX(0);
        sideSlide.play();
    }

    private void hideSideBarAnimation() {
        TranslateTransition sideSlide = new TranslateTransition(Duration.millis(300), sideBar);
        sideSlide.setToX(-(sideBar.getWidth() + 10));
        sideSlide.play();
    }

    private void showMenuBarAnimation() {
        TranslateTransition sideSlide = new TranslateTransition(Duration.millis(300), menuBar);
        sideSlide.setToY(0);
        sideSlide.play();
    }

    private void hideMenuBarAnimation() {
        TranslateTransition sideSlide = new TranslateTransition(Duration.millis(300), menuBar);
        sideSlide.setToY(-(menuBar.getHeight() + 10));
        sideSlide.play();

    }

    private void solvedCubeAnimation(String finalTime) {
        startConfetti();
        showWindowUISolvedCube(finalTime);
    }

    private void showWindowUISolvedCube(String finalTime) {
        isMenuOpen = true;
        winUI = new VBox(50);

        winUI.setAlignment(Pos.CENTER);
        winUI.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        winUI.setFocusTraversable(true);

        Label crown = new Label("👑");
        crown.setStyle("-fx-font-size: 100px;");

        Label congrats = new Label("CONGRATULATIONS!");
        congrats.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 40px; -fx-font-weight: bold;");

        Label timeLabel = new Label("Time (hr:min:sec:ms): " + finalTime);
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-family: 'Monospaced';");

        Label numberOfRotationsLabel = new Label("Total Number Of Rotations Performed: " + numberOfRotations);
        numberOfRotationsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Monospaced';");

        Label firstFaceCompletedLabel = new Label(firstFaceCompletedMoves == -1 ? "(Did not go through and complete first face)" : "First face done in " + firstFaceCompletedMoves + " moves!\n" + "First face done: " + firstColorCompleted);
        firstFaceCompletedLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Monospaced';");

        Label numberOfRotationWhiteCross = new Label(whiteCrossCompletedMoves == -1 ? "(Did not go through and complete the white cross)" : "White cross done in " + whiteCrossCompletedMoves + " movess");
        numberOfRotationWhiteCross.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Monospaced';");

        Button resetBtn = new Button("Play Again");
        resetBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 10 30;");
        resetBtn.setCursor(Cursor.HAND);

        resetBtn.setOnAction(e -> {
            globalRoot.getChildren().remove(winUI);
            removeConfetti();
            resetCube();
            stopwatch.reset();
            confettiEngine.stop();
            pieces.clear();
            isMenuOpen = false;
        });

        winUI.getChildren().addAll(crown, congrats, timeLabel, numberOfRotationsLabel, firstFaceCompletedLabel, numberOfRotationWhiteCross, resetBtn);

        winUI.setOnKeyPressed(e -> {
            e.consume();
            addRoundOfConfetti();
        });

        globalRoot.getChildren().add(winUI);

        winUI.requestFocus();

    }

    private void startConfetti() {
        pieces = new ArrayList<>();
        confettiCanvas = new Canvas(scene.getWidth(), scene.getHeight());
        globalRoot.getChildren().add(confettiCanvas);
        addRoundOfConfetti();
        confettiEngine = new AnimationTimer() {
            @Override
            public void handle(long now) {
                GraphicsContext gc = confettiCanvas.getGraphicsContext2D();
                gc.clearRect(0, 0, confettiCanvas.getWidth(), confettiCanvas.getHeight());

                ConfettiPiece p;
                for (int i = 0; i < pieces.size(); i++) {
                    p = pieces.get(i);
                    if (p.y > scene.getHeight() && p.vy > 0) {
                        pieces.remove(i);
                        i--;
                        continue;
                    }
                    p.update();
                    gc.setFill(p.color);

                    gc.save();
                    gc.translate(p.x, p.y);
                    gc.rotate(p.angle);
                    gc.fillRect(-p.width * p.scale / 2, -p.height * p.scale / 2, p.width * p.scale, p.height * p.scale);
                    gc.restore();
                }
            }
        };
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(350), e -> addRoundOfConfetti()));
        timeline.setCycleCount(10);
        timeline.play();


        confettiEngine.start();
    }

    private void showLoadingOverlay(Task<Void> task) {
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");

        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(100, 100);
        pi.progressProperty().bind(task.progressProperty());

        Label statsLabel = new Label("0%");
        statsLabel.setStyle("-fx-text-fill: lightblue; -fx-font-size: 24px; -fx-font-weight: bold;");
        statsLabel.setTranslateY(80);

        statsLabel.textProperty().bind(task.messageProperty());

        Label trainingLabel = new Label("AI is training... Please wait.");
        trainingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
        trainingLabel.setTranslateY(120);

        Button returnToCubeBtn = new Button("Background Training");
        returnToCubeBtn.setCursor(Cursor.HAND);
        returnToCubeBtn.setTranslateY(180);
        returnToCubeBtn.setOnAction(e -> {
            hideLoadingOverlay();
        });

        Button cancelBtn = new Button("Stop Training");
        cancelBtn.setCursor(Cursor.HAND);
        cancelBtn.setTranslateY(230);
        cancelBtn.setOnAction(e -> {
            aiSolver.stopTraining(); // See Step 3
            hideLoadingOverlay();
        });

        loadingOverlay.getChildren().addAll(pi, statsLabel, trainingLabel, returnToCubeBtn, cancelBtn);
        globalRoot.getChildren().add(loadingOverlay);
    }

    private void hideLoadingOverlay() {
        globalRoot.getChildren().remove(loadingOverlay);
    }

    private void addRoundOfConfetti() {
        for (int i = 0; i < 400; i++)
            pieces.add(new ConfettiPiece(confettiCanvas.getWidth(), confettiCanvas.getHeight()));
    }

    private void removeConfetti() {
        globalRoot.getChildren().remove(confettiCanvas);
    }

    private void proMode() {
        isMenuOpen = true;
        VBox observePhase = new VBox(20);
        observePhase.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-padding: 40;");
        observePhase.setAlignment(Pos.CENTER);
        observePhase.setMouseTransparent(true);
        scrambleCube();

        Label title = new Label("OBSERVATION PHASE");
        title.setStyle("-fx-text-fill: #d5dab2; -fx-font-size: 30px; -fx-font-weight: bold;");

        Label timeLeft = new Label(String.valueOf(observationSeconds));
        timeLeft.setStyle("-fx-text-fill: white; -fx-font-size: 100px");

        observePhase.getChildren().addAll(title, timeLeft);
        globalRoot.getChildren().add(observePhase);

        setImmersiveMode(true);
        stopWatchVisible = true;
        makeStopWatchDisappearOrAppear();

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            observationSeconds--;
            timeLeft.setText(String.valueOf(observationSeconds));
            if (observationSeconds <= 5) {
                timeLeft.setStyle("-fx-text-fill: red; -fx-font-size: 100px; -fx-font-weight: bold;");
            }
        }));

        countdown.setCycleCount(15);
        countdown.setOnFinished(e -> {
            globalRoot.getChildren().remove(observePhase);
            isMenuOpen = false;
            stopwatch.reset();
            stopwatch.startOrStop();
            animationSpeed = 100;
        });
        countdown.play();
    }

    private void makeStopWatchDisappearOrAppear() {
        TranslateTransition sideSlide = new TranslateTransition(Duration.millis(300), stopwatchUI);
        sideSlide.setToX(stopWatchVisible ? 0 : stopwatchUI.getWidth() + 50);
        sideSlide.play();
    }

    public void startAISolver() {
        aiActive = true;
        aiSolver = new AISolver(this);
        int numOfMovesToTrainFor = 5;
        if (!AISolver.hasTrained) {
            aiTraining = true;
            boolean stopwatchWasRunning = stopwatch.isRunning();
            if (stopwatchWasRunning) stopwatch.startOrStop();

            CubeApp.CubieState[][][] recordState = saveCubeState();
            initializeCubeState();
            String solvedState = getStateString(this);
            Task<Void> trainingTask = new Task<>() {
                @Override
                protected Void call() {
                    aiSolver.train(numOfMovesToTrainFor, solvedState, (successRate, numMovesScrambled) -> {
                        updateProgress(successRate, 100);
                        updateMessage("Training Move: " + numMovesScrambled + " -> Success Rate: " + successRate + "%");
                    });
                    return null;
                }
            };
            showLoadingOverlay(trainingTask);
            trainingTask.setOnSucceeded(e -> {
                restoreCube(recordState);
                hideLoadingOverlay();
                if (stopwatchWasRunning) stopwatch.startOrStop();
                aiTraining = false;
                triggerNextAIMove();

            });
            new Thread(trainingTask).start();
        } else {
            this.animationSpeed = 130;
            triggerNextAIMove();
        }
    }

    protected void triggerNextAIMove() {
        if (!aiActive || isCubeSolved()) {
            aiActive = false;
            return;
        }
        CubeMove cubeMove = aiSolver.getNextMove(getStateString(this));
        if (cubeMove != null) rotateFace(cubeMove);
    }

    private record CubeFace(char axis, int face) {
    }

    public record CubeMove(char axis, int faceValue, int direction) {
        @Override
        public @NotNull String toString() {
            if (axis == 'x') {
                if (faceValue == 1) return "R" + (direction == -1 ? "'" : "");
                else return "L" + (direction == -1 ? "'" : "");
            } else if (axis == 'y') {
                if (faceValue == -1) return "U" + (direction == -1 ? "'" : "");
                else return "D" + (direction == -1 ? "'" : "");
            } else {
                if (faceValue == -1) return "F" + (direction == -1 ? "'" : "");
                else return "B" + (direction == -1 ? "'" : "");
            }
        }
    }

    record CubieState(Color u, Color d, Color f, Color b, Color r, Color l) {
    }

}