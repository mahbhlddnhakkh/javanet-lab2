package labs.lab2_game;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import javafx.application.Platform;

public class FakeServerApp extends Application {

  private static Scene scene;
  public static FakeServerApp self = null;
  public static PrimaryController controller = null;

  @Override
  public void start(Stage stage) throws IOException {
    self = this;
    FXMLLoader loader = new FXMLLoader(App.class.getResource("primary.fxml"));
    scene = new Scene(loader.load(), Config.win_w, Config.win_h);
    stage.setScene(scene);
    controller = loader.getController();
    stage.setResizable(false);
    // Without this the process will just live forever
    Platform.exit();
    stage.show();
  }
}
