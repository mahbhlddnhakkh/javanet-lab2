package labs.lab2_game;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import javafx.application.Platform;

/**
 * JavaFX App
 */
public class App extends Application {

  private static Scene scene;

  @Override
  public void start(Stage stage) throws IOException {
    //scene = new Scene(loadFXML("primary"), Config.win_w, Config.win_h);
    FXMLLoader loader = new FXMLLoader(App.class.getResource("primary" + ".fxml"));
    scene = new Scene(loader.load(), Config.win_w, Config.win_h);
    stage.setScene(scene);
    stage.setTitle("Fire in a hole!");
    stage.setResizable(false);
    // Without this the process will just live forever
    PrimaryController controller = loader.getController();
    stage.setOnCloseRequest(e -> {
      controller.closeSocket();
      Platform.exit();
      System.exit(0);
    });
    stage.show();
  }

  private static Parent loadFXML(String fxml) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
    return fxmlLoader.load();
  }

  public static void main(String[] args) {
    launch();
  }

}