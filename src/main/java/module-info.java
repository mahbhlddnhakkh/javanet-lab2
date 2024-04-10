module labs.lab2_game {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires javafx.base;

  opens labs.lab2_game to javafx.fxml;

  exports labs.lab2_game;
}
