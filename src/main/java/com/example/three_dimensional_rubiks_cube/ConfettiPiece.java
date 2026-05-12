package com.example.three_dimensional_rubiks_cube;

import javafx.scene.paint.Color;

public class ConfettiPiece {
    static final double GRAVITY = .18;
    double x, y, vx, vy, angle, rotateSpeed, scale;
    Color color;
    double width = 8, height = 12;
    boolean leftPiece;

    ConfettiPiece(double screenWidth, double screenHeight) {
        this.leftPiece = Math.random() < 0.5;
        this.x = leftPiece ? 0 : screenWidth;
        this.y = screenHeight + 20;
        this.vy = -(8 + Math.random() * 15);
        this.vx = (1 + Math.random() * 21) * (leftPiece ? 1 : -1);

        this.rotateSpeed = Math.random() * 10;
        this.angle = Math.random() * 360;
        this.color = Color.hsb(Math.random() * 360, 0.8, 0.9);
        scale = .5 + Math.random();
    }

    void update() {
        x += vx;
        y += vy;
        vy += GRAVITY;
        vx *= .984; //air resistance
        angle += rotateSpeed;
    }
}
