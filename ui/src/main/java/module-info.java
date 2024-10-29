module com.javadruid.bluez.phone.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires com.javadruid.bluez.phone.lib;

    opens com.javadruid.bluez.phone.ui to javafx.fxml;
    exports com.javadruid.bluez.phone.ui;
}
