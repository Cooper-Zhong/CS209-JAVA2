module com.example.lab10 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;


    opens com.example.lab10 to javafx.fxml;
    exports com.example.lab10;
}