package com.example.three_dimensional_rubiks_cube;

import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.HashMap;
import java.util.Map;

public class Cubie extends Group {
    public int x, y, z;
    private final Map<Character, Box> stickers = new HashMap<>();

    /**
     * Initializes a Cubie based on its position within the cube,
     * sorting the colors accordingly
     *
     * @param size the size of the cube
     * @param x    the horizontal (back to front) location within the cube
     * @param y    the horizontal (right to left) location within the cube
     * @param z    the vertical location within the cube
     */
    public Cubie(double size, int x, int y, int z) {
        Box box = new Box(size, size, size);
        box.setMaterial(new PhongMaterial(Color.BLACK));
        this.getChildren().add(box);


        this.x = x;
        this.y = y;
        this.z = z;

        this.setTranslateX(x * (size + 2));
        this.setTranslateY(y * (size + 2));
        this.setTranslateZ(z * (size + 2));

        addSticker('u', new Rotate(-90, Rotate.X_AXIS), size);
        addSticker('d', new Rotate(90, Rotate.X_AXIS), size);
        addSticker('f', new Rotate(0, Rotate.X_AXIS), size);
        addSticker('b', new Rotate(180, Rotate.X_AXIS), size);
        addSticker('r', new Rotate(-90, Rotate.Y_AXIS), size);
        addSticker('l', new Rotate(90, Rotate.Y_AXIS), size);

        initializeColors();


    }

    /**
     * Adding black color sticker to cubie face
     * @param face the face to add the color too
     * @param rotation
     * @param size size of cubie
     */
    private void addSticker(char face, Rotate rotation, double size) {
        Box sticker = new Box(size * 0.8, size * 0.8, 1);
        sticker.setMaterial(new PhongMaterial(Color.BLACK));

        Translate offset = new Translate(0, 0, -size / 1.99); //1.99 to prevent flickering
        sticker.getTransforms().addAll(rotation, offset);


        stickers.put(face, sticker);
        this.getChildren().add(sticker);
    }

    /**
     * Sets the color to the face of the cubie
     * @param face face to have new color
     * @param newColor color to put on face
     */
    public void setFaceColor(char face, Color newColor) {
        Box s = stickers.get(face);
        if (s != null) {
            ((PhongMaterial) s.getMaterial()).setDiffuseColor(newColor);
        }
    }

    /**
     * returns color of face demanded
     * @param face the face demanded
     * @return color of face demanded
     */
    public Color getFaceColor(char face) {
        Box s = stickers.get(face);
        if (s != null) {
            return (Color) ((PhongMaterial) s.getMaterial()).getDiffuseColor();
        }
        return Color.BLACK;
    }

    public boolean isEdge(char axis) {
        if (axis == 'x') return (y == 0 && z != 0) || (y != 0 && z == 0);
        if (axis == 'y') return (x == 0 && z != 0) || (x != 0 && z == 0);
        if (axis == 'z') return (x == 0 && y != 0) || (x != 0 && y == 0);
        return false;
    }

    /**
     * returns coordinate of the axis demanded
     * @param axis axis wanted
     * @return coordinate of the axis demanded
     */
    public int getCoordinate(char axis) {
        if (axis == 'x') return x;
        if (axis == 'y') return y;
        if (axis == 'z') return z;
        return 2;
    }

    public void changeCoordinate(char axis, int newCoordinate) {
        if (axis == 'x') {
            this.x = newCoordinate;
        } else if (axis == 'y') {
            this.y = newCoordinate;
        } else if (axis == 'z') {
            this.z = newCoordinate;
        }
    }

    /**
     * Initialize all colors of the cubie
     */
    private void initializeColors() {
        if (z == -1) {
            stickers.get('f').setMaterial(new PhongMaterial(Color.GREEN));
            stickers.get('b').setMaterial(new PhongMaterial(Color.BLACK));
        }
        if (z == 1) {
            stickers.get('b').setMaterial(new PhongMaterial(Color.BLUE));
            stickers.get('f').setMaterial(new PhongMaterial(Color.BLACK));
        }
        if (y == -1) {
            stickers.get('u').setMaterial(new PhongMaterial(Color.YELLOW));
            stickers.get('d').setMaterial(new PhongMaterial(Color.BLACK));
        }
        if (y == 1) {
            stickers.get('d').setMaterial(new PhongMaterial(Color.WHITE));
            stickers.get('u').setMaterial(new PhongMaterial(Color.BLACK));
        }
        if (x == 1) {
            stickers.get('r').setMaterial(new PhongMaterial(Color.DARKORANGE));
            stickers.get('l').setMaterial(new PhongMaterial(Color.BLACK));
        }
        if (x == -1) {
            stickers.get('l').setMaterial(new PhongMaterial(Color.RED));
            stickers.get('r').setMaterial(new PhongMaterial(Color.BLACK));
        }
    }

    @Override
    public String toString() {
        return AISolver.colorToChar(getFaceColor('f')) + " " + AISolver.colorToChar(getFaceColor('r')) + " " +AISolver.colorToChar(getFaceColor('b')) + " " +AISolver.colorToChar(getFaceColor('l')) + " " +AISolver.colorToChar(getFaceColor('u')) +" " +AISolver.colorToChar(getFaceColor('d'));
    }
}