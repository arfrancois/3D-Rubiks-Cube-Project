module com.example.three_dimensional_rubiks_cube {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires org.jetbrains.annotations;
    //requires javafx.graphics;


    opens com.example.three_dimensional_rubiks_cube to javafx.fxml;
    exports com.example.three_dimensional_rubiks_cube;
}