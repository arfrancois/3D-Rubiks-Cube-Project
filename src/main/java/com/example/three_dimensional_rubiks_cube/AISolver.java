package com.example.three_dimensional_rubiks_cube;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class AISolver {

    protected static HashMap<String, double[]> qTable = new HashMap<>();
    static boolean hasTrained = false;
    static double epsilon = 1.0;  // starts fully random
    // state string → 12 Q-values
    static double learningRate = .1;
    static double discount = 0.95;
    public int moveCount = 0;
    protected CubeApp app;
    private boolean ignoreNewStates = false;

    public AISolver(CubeApp app) {
        this.app = app;
        /*
        CubeApp.CubieState[][][] recordState = saveCubeState();
        if (!hasTrained) train();
        restoreCube(recordState);
         */
        //app.scrambleCubeFaces(2);
    }

    /**
     * Sequence for hashing is as follows: Front, Right, Back, Left, Top, Bottom,
     * all cubies within each face read top to bottom, left to right
     *
     * @param cubeApp app to record cube state from
     * @return String hashing of current state
     */
    public static String getStateString(CubeApp cubeApp) {
        Cubie[][][] threeDArrayAllCubies = cubeApp.threeDArrayAllCubies;

        return cubieFaceToString(cubeApp, 'f') + cubieFaceToString(cubeApp, 'r') + cubieFaceToString(cubeApp, 'b') + cubieFaceToString(cubeApp, 'l') + cubieFaceToString(cubeApp, 'u') + cubieFaceToString(cubeApp, 'd');
    }

    private static String cubieFaceToString(CubeApp app, char face) {
        StringBuilder faceString = new StringBuilder();
        for (int row = -1; row <= 1; row++) {
            for (int col = -1; col <= 1; col++) {
                int x = 0, y = 0, z = 0;

                if (face == 'f') { // Looking at FRONT
                    x = col;
                    y = row;
                    z = -1;
                } else if (face == 'r') { // Looking at RIGHT
                    x = 1;
                    y = row;
                    z = col; // Left side is Front (-z), Right side is Back (+z)
                } else if (face == 'b') { // Looking at BACK
                    x = -col; // Left side is Right face (+x), Right side is Left face (-x)
                    y = row;
                    z = 1;
                } else if (face == 'l') { // Looking at LEFT
                    x = -1;
                    y = row;
                    z = -col; // Left side is Back (+z), Right side is Front (-z)
                } else if (face == 'u') { // Looking at UP (Top edge connects to Back face)
                    x = col;
                    y = -1;
                    z = -row; // Top is Back (+z), Bottom is Front (-z)
                } else if (face == 'd') { // Looking at DOWN (Top edge connects to Front face)
                    x = col;
                    y = 1;
                    z = row; // Top is Front (-z), Bottom is Back (+z)
                }

                // Retrieve the Cubie using your inverted array logic
                Cubie cubie = app.threeDArrayAllCubies[x + 1][-y + 1][-z + 1];
                faceString.append(colorToChar(cubie.getFaceColor(face)));
            }
        }
        return faceString.toString();
    }

    protected static char colorToChar(Color color) {
        if (color.equals(Color.RED)) return 'R';
        if (color.equals(Color.GREEN)) return 'G';
        if (color.equals(Color.BLUE)) return 'B';
        if (color.equals(Color.YELLOW)) return 'Y';
        if (color.equals(Color.DARKORANGE)) return 'O';
        if (color.equals(Color.WHITE)) return 'W';
        return 'e'; //error
    }

    /**
     * Hashes a cube move into a double index value ranging from 0-11,
     * in this order: R, R', L, L', D, D', U, U', B, B', F, F'
     *
     * @param move the move to hash
     * @return the int representation of the cube (ranging from 0-11
     */
    protected static int hashCubeMove(CubeApp.CubeMove move) {
        int axisOffset = switch (move.axis()) {
            case 'x' -> 0;
            case 'y' -> 4;
            default -> 8;
        };
        int dirOffset = move.direction() == 1 ? 0 : 1;
        int faceOffset = move.faceValue() == 1 ? 0 : 2;
        return axisOffset + faceOffset + dirOffset;
    }

    protected static CubeApp.CubeMove getCubeMove(int hashIndex) {
        char axis;
        switch (hashIndex / 4) {
            case 0 -> axis = 'x';
            case 1 -> axis = 'y';
            case 2 -> axis = 'z';
            default -> axis = 'e'; //error
        }
        int details = hashIndex % 4;
        int direction = details % 2 == 0 ? 1 : -1;
        int faceValue = details < 2 ? 1 : -1;
        return new CubeApp.CubeMove(axis, faceValue, direction);
    }

    private static int hashedIndexBestMove(double[] moves) {
        double maxVal = moves[0];
        for (double move : moves) {
            if (move > maxVal) maxVal = move;
        }
        ArrayList<Integer> bestMoves = new ArrayList<>();
        for (int i = 0; i < moves.length; i++) {
            if (moves[i] == maxVal) bestMoves.add(i);
        }
        return bestMoves.get((int) (Math.random() * bestMoves.size()));
    }

    protected static boolean solvedState(String state) {
        char c;
        for (int a = 0; a < 6; a++) {
            c = state.charAt(9 * a);
            for (int i = 1; i < 9; i++) {
                if (c != state.charAt(a * 9 + i)) return false;
            }
        }
        return true;
    }

    protected static String getNewState(String oldState, CubeApp.CubeMove move) {
        StringBuilder newState = new StringBuilder(oldState);
        boolean cw = move.direction() == 1; //clockwise?

        // --- X AXIS MOVES ---
        if (move.axis() == 'x') {
            if (move.faceValue() == 1) { // R Move
                rotateFace(newState, oldState, 9, cw);
                cycleEdges(newState, oldState, cw, new int[]{44, 41, 38}, new int[]{18, 21, 24}, new int[]{53, 50, 47}, new int[]{8, 5, 2});
            } else { // L Move
                rotateFace(newState, oldState, 27, cw);
                cycleEdges(newState, oldState, cw, new int[]{36, 39, 42}, new int[]{0, 3, 6}, new int[]{45, 48, 51}, new int[]{26, 23, 20});
            }
        }

        // --- Y AXIS MOVES ---
        else if (move.axis() == 'y') {
            if (move.faceValue() == -1) { // U Move
                rotateFace(newState, oldState, 36, cw);
                cycleEdges(newState, oldState, cw, new int[]{0, 1, 2}, new int[]{27, 28, 29}, new int[]{18, 19, 20}, new int[]{9, 10, 11});
            } else { // D Move
                rotateFace(newState, oldState, 45, cw);
                cycleEdges(newState, oldState, cw, new int[]{6, 7, 8}, new int[]{15, 16, 17}, new int[]{24, 25, 26}, new int[]{33, 34, 35});

            }
        }

        // --- Z AXIS MOVES ---
        else if (move.axis() == 'z') {
            if (move.faceValue() == -1) { // F Move
                rotateFace(newState, oldState, 0, cw);
                cycleEdges(newState, oldState, cw, new int[]{42, 43, 44}, new int[]{9, 12, 15}, new int[]{47, 46, 45}, new int[]{35, 32, 29});
            } else { // B Move
                rotateFace(newState, oldState, 18, cw);
                cycleEdges(newState, oldState, cw, new int[]{38, 37, 36}, new int[]{27, 30, 33}, new int[]{51, 52, 53}, new int[]{17, 14, 11});

            }
        }

        return newState.toString();
    }

    // Rotates the 9 stickers on the actual face being turned
    private static void rotateFace(StringBuilder newState, String old, int faceStart, boolean cw) {
        // These arrays map "New Position" <- "Old Position"
        int[] cwMap = {6, 3, 0, 7, 4, 1, 8, 5, 2};
        int[] ccwMap = {2, 5, 8, 1, 4, 7, 0, 3, 6};
        int[] map = cw ? cwMap : ccwMap;

        for (int i = 0; i < 9; i++) {
            // Reads from the untouched 'old' string, writes to the 'sb' string
            newState.setCharAt(faceStart + i, old.charAt(faceStart + map[i]));
        }
    }

    // Shifts the 4 edges around the face
    private static void cycleEdges(StringBuilder newState, String old, boolean cw, int[] e1, int[] e2, int[] e3, int[] e4) {
        if (cw) {
            applyCycle(newState, old, e2, e1); // e1 moves to e2
            applyCycle(newState, old, e3, e2); // e2 moves to e3
            applyCycle(newState, old, e4, e3); // e3 moves to e4
            applyCycle(newState, old, e1, e4); // e4 moves to e1
        } else {
            applyCycle(newState, old, e4, e1); // e1 moves to e4
            applyCycle(newState, old, e3, e4); // e4 moves to e3
            applyCycle(newState, old, e2, e3); // e3 moves to e2
            applyCycle(newState, old, e1, e2); // e2 moves to e1
        }
    }

    // Performs the actual character swapping for 3 stickers at a time
    private static void applyCycle(StringBuilder newState, String old, int[] to, int[] from) {
        for (int i = 0; i < 3; i++) {
            newState.setCharAt(to[i], old.charAt(from[i]));
        }
    }

    public static String compareStatesHelper(String beforeState, String afterState) {
        StringBuilder indexesDifferent = new StringBuilder();
        for (int i = 0; i < beforeState.length(); i++) {
            if (beforeState.charAt(i) != afterState.charAt(i))
                indexesDifferent.append(i).append(": ").append(beforeState.charAt(i)).append(" -> ").append(afterState.charAt(i)).append(", ");
        }
        return indexesDifferent.toString();
    }

    public CubeApp.CubeMove getNextMove(String state) {
        double[] moves;// = qTable.getOrDefault(state, new double[12]);

        if (!qTable.containsKey(state) && ignoreNewStates) {
            moveCount++;
            return app.randomCubeMove();
        } else {
            moves = qTable.getOrDefault(state, new double[12]);
        }

        int bestMove = hashedIndexBestMove(moves);

        int nextMove;
        if (Math.random() < epsilon) {
            nextMove = (int) (Math.random() * 12);
        } else {
            nextMove = bestMove;
        }
        moveCount++;
        return getCubeMove(nextMove);
    }

    /**
     * Updating table of q values mapped to old state
     *
     * @param oldState the state of the cube prior to the rotation move
     * @param move     the rotation move transformation from oldState to newState
     * @param newState the new state of the cube after having performed the rotation move
     */
    public void updateQ(String oldState, CubeApp.CubeMove move, String newState) {
        if (!qTable.containsKey(oldState)) {
            qTable.computeIfAbsent(oldState, (k) -> new double[12]);
        }

        double[] moves = qTable.getOrDefault(oldState, new double[12]);
        int hashIndexMove = hashCubeMove(move);
        double reward = getReward(newState);

        double oldQValue = moves[hashIndexMove];
        double[] futureQValues = qTable.getOrDefault(newState, new double[12]);
        double bestFutureVal = futureQValues[hashedIndexBestMove(futureQValues)];

        double newQValue = oldQValue + learningRate * (reward + discount * bestFutureVal - oldQValue);
        moves[hashIndexMove] = newQValue;
        qTable.put(oldState, moves);
    }

    protected double getReward(String state) {
        if (solvedState(state)) return 100;
        return -.01; //decrement Q val a little bit every move until solved
    }

    protected void train(int numOfMovesToTrainFor) {
        int maxSteps = 7;
        int numMovesScrambled = 1;
        double epsilonDecay = 0.995;
        int checkInterval = 200;
        int successThreshold = 75; //only advance to next numMovesScrambled when successThreshold% accuracy reached
        String currentState;
        String newState;
        CubeApp.CubeMove move;
        int ep;

        app.initializeCubeState();
        String solvedState = getStateString(app);

        for (ep = 0; numMovesScrambled <= numOfMovesToTrainFor; ep++) {
            if (ep % checkInterval == 0 && ep > 0) {
                int rate = getSuccessRate(solvedState, numMovesScrambled);
                System.out.println("Episode " + ep + " | Scramble " + numMovesScrambled + " | Success: " + rate + "/100");
                if (rate >= successThreshold) {
                    numMovesScrambled++;
                    maxSteps += 3;
                    epsilon = 1.0;
                    checkInterval = 200 * numMovesScrambled;
                    successThreshold = Math.min(successThreshold + 5, 90);
                    System.out.println("Advancing to " + numMovesScrambled + " moves!");
                }
            }
            currentState = applyRandomMoves(solvedState, numMovesScrambled);
            for (int step = 0; step < maxSteps; step++) {
                move = getNextMove(currentState);
                newState = getNewState(currentState, move);
                updateQ(currentState, move, newState);
                if (solvedState(newState)) break;
                currentState = newState;
            }
            epsilon = Math.max(0.1, epsilon * epsilonDecay);
        }

        System.out.println("AI training complete: " + qTable.size() + " recorded states");
        epsilon = 0;//.04;
        hasTrained = true;
        for (int i = 1; i <= numMovesScrambled; i++) {
            System.out.println("Final success rate for " + i + " Scramble" + (i == 1 ? "" : "s") + ": " + getSuccessRate(solvedState, i) + "/100");
        }
        System.out.println(numberWCommas(ep));


    }

    protected String applyRandomMoves(String state, int numMoves) {
        for (int i = 0; i < numMoves; i++) {
            state = getNewState(state, app.randomCubeMove());
        }
        return state;
    }

    private int getSuccessRate(String solvedState, int numMovesScrambled) {
        ignoreNewStates = true;
        double savedEpsilon = epsilon;
        epsilon = 0;
        int successes = 0;
        String curState;
        String nextState;
        for (int test = 0; test < 100; test++) {
            curState = applyRandomMoves(solvedState, numMovesScrambled);
            for (int steps = 0; steps < numMovesScrambled; steps++) { //letting a little margin to let solver solve cube (3 * number of moves scrambled)
                nextState = getNewState(curState, getNextMove(curState));
                if (solvedState(nextState)) {
                    successes++;
                    break;
                }
                curState = nextState;
            }
        }
        epsilon = savedEpsilon;
        ignoreNewStates = false;
        return successes;
    }

    private void printSuccessRate(int numMovesScrambled) {
        ignoreNewStates = true;
        double savedEpsilon = epsilon;
        epsilon = 0;
        int successes = 0;
        boolean solved;
        String state;
        for (int test = 0; test < 100; test++) {
            app.initializeCubeState();
            app.scrambleCubeFaces(numMovesScrambled);
            solved = false;
            for (int steps = 0; steps < numMovesScrambled; steps++) { //letting a little margin to let solver solve cube (3 * number of moves scrambled)
                state = getStateString(app);
                app.updateColorsCubeMove(getNextMove(state));
                if (solvedState(getStateString(app))) {
                    solved = true;
                    break;
                }
            }
            if (solved) successes++;
        }
        System.out.println("Success rate: " + successes + "/100 -> For " + numMovesScrambled + " scrambled moves!");
        epsilon = savedEpsilon;
        ignoreNewStates = false;
    }

    public String numberWCommas(int number) {
        if (number == 0) return "0";

        String ret = "";
        while (number > 0) {
            int remainder = number % 1000;
            number /= 1000;

            // If there's more of the number left, we need leading zeros
            // for the current segment (e.g., 005 instead of 5)
            String cur = (number > 0) ? String.format("%03d", remainder) : String.valueOf(remainder);

            // Add comma only if we have more digits to the left
            if (ret.length() > 0) {
                ret = cur + "," + ret;
            } else {
                ret = cur;
            }
        }
        return ret;
    }


}
