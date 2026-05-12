package com.example.three_dimensional_rubiks_cube;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.*;

public class Stopwatch {

    private final Label timeLabel = new Label("00:00:00:000");
    private final Button startButton = new Button("START");
    private final Button resetButton = new Button("RESET");
    private boolean isRunning = false;

    private int milliseconds = 0, seconds = 0, minutes = 0, hours = 0;
    private Timeline timeline;

    public Stopwatch() {
        // Initialize the "Heartbeat" (1 second intervals)
        timeline = new Timeline(new KeyFrame(Duration.millis(1), e -> updateTime()));
        timeline.setCycleCount(Animation.INDEFINITE);

        // Styling (Equivalent to setFont)
        timeLabel.setStyle("-fx-font-size: 35px; -fx-font-family: 'Verdana';");

        // Setup Button Actions
        startButton.setOnAction(e -> startOrStop());
        resetButton.setOnAction(e -> reset());

        // Prevent buttons from stealing keyboard focus from the cube
        startButton.setFocusTraversable(false);
        resetButton.setFocusTraversable(false);
    }

    private void updateTime() {
        milliseconds+=1;
        if (milliseconds == 1000) {milliseconds = 0; seconds++;}
        if (seconds == 60) { seconds = 0; minutes++; }
        if (minutes == 60) { minutes = 0; hours++; }
        timeLabel.setText(getTime());
    }

    public String getTime() {
        return String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, milliseconds);
    }

    public void startOrStop() {
        if (timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.pause();
            startButton.setText("START");
            isRunning = false;
        } else {
            timeline.play();
            startButton.setText("STOP");
            isRunning = true;
        }
    }

    public void reset() {
        timeline.stop();
        milliseconds = 0; seconds = 0; minutes = 0; hours = 0;
        timeLabel.setText("00:00:00:000");
        startButton.setText("START");
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public VBox getUI() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);

        // 1. THE CONTAINER STYLE (Dark, rounded, with a drop shadow)
        layout.setStyle("-fx-background-color: #3c3f41; " +
                "-fx-background-radius: 15; " +
                "-fx-padding: 20 30 20 30; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 5, 0);");

        // CRITICAL FIX: Stops the VBox from stretching down the whole screen
        layout.setMaxHeight(Region.USE_PREF_SIZE);
        layout.setMaxWidth(Region.USE_PREF_SIZE);

        // 2. THE TIME LABEL
        timeLabel.setStyle("-fx-font-size: 32px; " +
                "-fx-font-family: 'Verdana'; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold;");

        // 3. THE BUTTONS (Side-by-side in an HBox)
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        String btnStyle = "-fx-background-color: #525659; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 8 15 8 15; -fx-font-weight: bold;";
        String btnHover = "-fx-background-color: #6a6e73; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 8 15 8 15; -fx-font-weight: bold;";

        // Apply styles and hover effects
        startButton.setStyle(btnStyle);
        resetButton.setStyle(btnStyle);
        startButton.setCursor(Cursor.HAND);
        resetButton.setCursor(Cursor.HAND);

        startButton.setOnMouseEntered(e -> startButton.setStyle(btnHover));
        startButton.setOnMouseExited(e -> startButton.setStyle(btnStyle));
        resetButton.setOnMouseEntered(e -> resetButton.setStyle(btnHover));
        resetButton.setOnMouseExited(e -> resetButton.setStyle(btnStyle));

        // Add buttons to the horizontal row
        buttonBox.getChildren().addAll(startButton, resetButton);

        // Add the label and the row of buttons to the main layout
        layout.getChildren().addAll(timeLabel, buttonBox);

        return layout;
    }
}
