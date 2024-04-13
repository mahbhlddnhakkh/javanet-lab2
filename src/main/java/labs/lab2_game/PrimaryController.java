package labs.lab2_game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.paint.ImagePattern;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import labs.lab2_game.Message.MessageHandler;

public class PrimaryController {

  private int port = 0;
  Socket socket;
  byte slot = 0;

  @FXML
  private HBox MainFrame;

  @FXML
  private VBox MainGameFrame;

  @FXML
  private HBox ButtonsFrame;

  @FXML
  private Pane GamePane;

  @FXML
  private VBox ScoreFrame;

  @FXML
  private ImageView Finger1;
  @FXML
  private ImageView Finger2;
  @FXML
  private ImageView Finger3;
  @FXML
  private ImageView Finger4;

  private ImageView[] Fingers;

  @FXML
  private ImageView Hand1;
  @FXML
  private ImageView Hand2;
  @FXML
  private ImageView Hand3;
  @FXML
  private ImageView Hand4;

  private ImageView[] Hands;

  @FXML
  private Circle Player1Circle;
  @FXML
  private Circle Player2Circle;
  @FXML
  private Circle Player3Circle;
  @FXML
  private Circle Player4Circle;

  private Circle[] PlayerCircles;

  @FXML
  private Circle Target1Circle;

  @FXML
  private Line Target1Line;

  @FXML
  private Circle Target2Circle;

  @FXML
  private Line Target2Line;

  @FXML
  private Polygon ArrowPoly1;
  @FXML
  private Polygon ArrowPoly2;
  @FXML
  private Polygon ArrowPoly3;
  @FXML
  private Polygon ArrowPoly4;

  private Polygon[] ArrowPolys;

  // @FXML
  // private Circle TmpHitbox;

  @FXML
  private VBox ScoreFramePlayer1;
  @FXML
  private VBox ScoreFramePlayer2;
  @FXML
  private VBox ScoreFramePlayer3;
  @FXML
  private VBox ScoreFramePlayer4;

  private VBox[] ScoreFramesPlayer;

  @FXML
  private Button StartGameBtn;

  @FXML
  private Button ReadyBtn;

  @FXML
  private Button ShootBtn;

  @FXML
  private Button PauseBtn;

  private boolean isPaused = false;

  private boolean isReady = false;

  private boolean isGameGoing = false;

  private ClientMessageHandler clientMessageHandler = new ClientMessageHandler(this);
  
  DataOutputStream dOut;
  DataInputStream dInp;

  // For server
  public double[][] arrowsPos;
  public double[] target1Pos;
  public double[] target1PosStart;
  public double[] target1PosEnd;
  public double[] target2Pos;
  public double[] target2PosStart;
  public double[] target2PosEnd;
  public double gamePaneWidth;

  class NumberField extends TextField {
    @Override
    public void replaceText(int start, int end, String text) {
      if (text.matches("[0-9]*")) {
        super.replaceText(start, end, text);
      }
    }
  
    @Override
    public void replaceSelection(String text) {
      if (text.matches("[0-9]*")) {
        super.replaceSelection(text);
      }
    }
  }

  public static final void buttonSetTrueVisibility(Button button, boolean value) {
    button.setManaged(value);
    button.setVisible(value);
  }

  private static class ClientMessageHandler extends MessageHandler {
    private PrimaryController controller;

    ClientMessageHandler(PrimaryController controller) {
      this.controller = controller;
    }

    @Override
    public synchronized byte[] handleConnect(Message.Connect message) {
      if (!message.isGood())
        return null;
      Platform.runLater(() -> {
        controller.initialize_prepare();
        controller.addPlayer(message.slot, new String(message.name, StandardCharsets.UTF_8));
      });
      return null;
    }

    @Override
    public byte[] handleReject(Message.Reject message) {
      Platform.runLater(() -> {
        if (!message.isGood())
          return;
        switch (message.reason) {
          case Message.Reject.NAME_EXIST:
            controller.createInfoPopup(null, "Имя уже существует на сервере");
            break;
          case Message.Reject.GAME_FULL:
            controller.createInfoPopup(null, "Сервер заполнен");
            break;
          case Message.Reject.GAME_GOING:
          controller.createInfoPopup(null, "Игра уже идёт");
            break;
        }
      });
      return null;
    }

    @Override
    public byte[] handleExit(Message.Exit message) {
      Platform.runLater(() -> {
        if (!message.isGood()) {
          return;
        }
        controller.removePlayer(message.slot);
      });
      return null;
    }

    @Override
    public byte[] handleReady(Message.Ready message) {
      Platform.runLater(() -> {
        if (!controller.isGameGoing)
          controller.readyPlayer(message.slot);
      });
      return null;
    }

    @Override
    public byte[] handleUnready(Message.Unready message) {
      Platform.runLater(() -> {
        if (!controller.isGameGoing)
          controller.unreadyPlayer(message.slot);
      });
      return null;
    }

    @Override
    public byte[] handleGameBegin(Message.GameBegin message) {
      Platform.runLater(() -> {
        controller.isGameGoing = true;
        controller.initialize_game();
      });
      return null;
    }
    
    @Override
    public byte[] handleSync(Message.Sync message) {
      Platform.runLater(() -> {
        controller.sync_game(message.arrows.arr, message.target1PosY, message.target2PosY);
      });
      return null;
    }

    @Override
    public byte[] handleShoot(Message.Shoot message) {
      Platform.runLater(() -> {
        controller.increment_shots_count(message.slot);
      });
      return null;
    }

    @Override
    public byte[] handleScoreSync(Message.ScoreSync message) {
      Platform.runLater(() -> {
        controller.set_score(message.slot, message.score);
      });
      return null;
    }

    @Override
    public byte[] handlePlayerWon(Message.PlayerWon message) {
      Platform.runLater(() -> {
        controller.declare_winner(message.slot);
      });
      return null;
    }

    @Override
    public byte[] handlePause(Message.Pause message) {
      Platform.runLater(() -> {
        controller.player_paused(message.slot);
      });
      return null;
    }

    @Override
    public byte[] handleUnpause(Message.Unpause message) {
      Platform.runLater(() -> {
        controller.player_unpaused(message.slot);
      });
      return null;
    } 
  }

  private void initialize_dynamic_pos() {
    Target1Circle.setTranslateX(ButtonsFrame.getPrefWidth() * 0.7);
    Target1Circle.setTranslateY(GamePane.getPrefHeight() / 2 - Target1Circle.getRadius() / 2);
    Target1Line.setStartX(Target1Circle.getTranslateX());
    Target1Line.setStartY(7);
    Target1Line.setEndX(Target1Circle.getTranslateX());
    Target1Line.setEndY(GamePane.getPrefHeight() - 11);

    Target2Circle.setTranslateX(ButtonsFrame.getPrefWidth() * 0.9);
    Target2Circle.setTranslateY(GamePane.getPrefHeight() / 2 - Target2Circle.getRadius() / 2);
    Target2Line.setStartX(Target2Circle.getTranslateX());
    Target2Line.setStartY(Target1Line.getStartY());
    Target2Line.setEndX(Target2Circle.getTranslateX());
    Target2Line.setEndY(Target1Line.getEndY());

    for (int i = 0; i < ArrowPolys.length; i++) {
      ArrowPolys[i].setTranslateX(0);
      ArrowPolys[i].setVisible(false);
    }
  }

  private void initialize_start() {
    resetScore();
    for (int i = 0; i < PlayerCircles.length; i++) {
      PlayerCircles[i].setVisible(false);
      Fingers[i].setVisible(false);
      Hands[i].setVisible(false);
      ScoreFramesPlayer[i].setVisible(false);
      ArrowPolys[i].setTranslateX(0);
      ArrowPolys[i].setVisible(false);
    }
    isGameGoing = false;
    isPaused = false;
    isReady = false;
    Target1Circle.setVisible(false);
    Target2Circle.setVisible(false);
    buttonSetTrueVisibility(StartGameBtn, true);
    buttonSetTrueVisibility(ReadyBtn, false);
    buttonSetTrueVisibility(ShootBtn, false);
    buttonSetTrueVisibility(PauseBtn, false);
  }

  private void initialize_prepare() {
    Target1Circle.setVisible(false);
    Target2Circle.setVisible(false);
    buttonSetTrueVisibility(StartGameBtn, false);
    buttonSetTrueVisibility(ReadyBtn, true);
    buttonSetTrueVisibility(ShootBtn, false);
    buttonSetTrueVisibility(PauseBtn, false);
  }

  public void initialize_game() {
    resetScore();
    ReadyBtn.setText("Готов");
    isReady = false;
    PauseBtn.setText("Пауза");
    isPaused = false;
    Target1Circle.setVisible(true);
    Target2Circle.setVisible(true);
    buttonSetTrueVisibility(StartGameBtn, false);
    buttonSetTrueVisibility(ReadyBtn, false);
    buttonSetTrueVisibility(ShootBtn, true);
    buttonSetTrueVisibility(PauseBtn, true);
  }

  @FXML
  public void initialize() throws IOException {
    ScoreFramesPlayer = new VBox[] { ScoreFramePlayer1, ScoreFramePlayer2, ScoreFramePlayer3, ScoreFramePlayer4 };
    PlayerCircles = new Circle[] { Player1Circle, Player2Circle, Player3Circle, Player4Circle };
    ArrowPolys = new Polygon[] { ArrowPoly1, ArrowPoly2, ArrowPoly3, ArrowPoly4 };
    Fingers = new ImageView[] { Finger1, Finger2, Finger3, Finger4 };
    Hands = new ImageView[] { Hand1, Hand2, Hand3, Hand4 };
    String[] GoofyImgsFilenames = new String[] { "Easy.png", "Normal.png", "Hard.png", "Harder.png" };

    MainFrame.setPrefWidth(Config.win_w);
    ScoreFrame.setPrefWidth(ScoreFrame.getPrefWidth() * 1.25);
    ButtonsFrame.setPrefHeight(48);
    ButtonsFrame.setPrefWidth(Config.win_w - ScoreFrame.getPrefWidth());
    GamePane.setPrefWidth(ButtonsFrame.getPrefWidth());
    GamePane.setPrefHeight(Config.win_h - ButtonsFrame.getPrefHeight());
    String cssTranslate = "-fx-border-color: black;\n" + "-fx-border-insets: 5;\n" + "-fx-border-width: 1;\n";
    MainGameFrame.setStyle(cssTranslate);
    ButtonsFrame.setStyle(cssTranslate);
    GamePane.setStyle(cssTranslate);
    ScoreFrame.setStyle(cssTranslate);

    resetScore();
    for (int i = 0; i < PlayerCircles.length; i++) {
      PlayerCircles[i].setRadius(Config.player_radius);
      PlayerCircles[i].setTranslateX(Config.player_radius * 1.5);
      PlayerCircles[i].setTranslateY(GamePane.getPrefHeight() * (i + 1) / (PlayerCircles.length * 1.25) - Config.player_radius / 2);
      PlayerCircles[i].setFill(new ImagePattern(getImage(GoofyImgsFilenames[i])));
    }
    for (int i = 0; i < Fingers.length; i++) {
      Fingers[i].setImage(getImage("finger.png"));
      Fingers[i].setFitWidth(57);
      Fingers[i].setTranslateY(PlayerCircles[i].getTranslateY() - 8);
      Fingers[i].setTranslateX(Config.player_radius * 3);
    }
    for (int i = 0; i < Hands.length; i++) {
      Hands[i].setImage(getImage("Stophand.png"));
      Hands[i].setFitHeight(Config.player_radius*2);
      Hands[i].setTranslateY(PlayerCircles[i].getTranslateY() - Config.player_radius);
      Hands[i].setTranslateX(Config.player_radius * 2.5);
    }
    for (int i = 0; i < ScoreFramesPlayer.length; i++) {
      Circle goofyAhhFace = (Circle) ScoreFramesPlayer[i].getChildren().get(0);
      goofyAhhFace.setRadius(12);
      goofyAhhFace.setFill(new ImagePattern(getImage(GoofyImgsFilenames[i])));
    }

    Target1Circle.setRadius(Config.target_radius);
    Target1Circle.setFill(new ImagePattern(getImage("EasyDemon.png")));

    Target2Circle.setRadius(Config.target_radius / 2);
    Target2Circle.setFill(new ImagePattern(getImage("ExtremeDemon.png")));

    initialize_dynamic_pos();
    initialize_start();

    for (int i = 0; i < ArrowPolys.length; i++) {
      Double[] arrow_line_start_end = new Double[] {
          PlayerCircles[i].getTranslateX() + PlayerCircles[i].getRadius() + 70, PlayerCircles[i].getTranslateY(),
          PlayerCircles[i].getTranslateX() + PlayerCircles[i].getRadius() + 70 + Config.arrow_length, PlayerCircles[i].getTranslateY()
      };
      ArrowPolys[i].getPoints().clear();
      ArrowPolys[i].getPoints().addAll(new Double[] {
          arrow_line_start_end[0], arrow_line_start_end[1] - Config.arrow_width / 2,
          arrow_line_start_end[2], arrow_line_start_end[3] - Config.arrow_width / 2,
          // top left corner
          arrow_line_start_end[2], arrow_line_start_end[3] - Config.arrow_hitbox_radius * Math.sqrt(0.75),
          arrow_line_start_end[2] + Config.arrow_hitbox_radius * 1.5, arrow_line_start_end[3],
          // down left corner
          arrow_line_start_end[2], arrow_line_start_end[3] + Config.arrow_hitbox_radius * Math.sqrt(0.75),
          arrow_line_start_end[2], arrow_line_start_end[3] + Config.arrow_width / 2,
          arrow_line_start_end[0], arrow_line_start_end[1] + Config.arrow_width / 2,
      });
    }
    arrowsPos = new double[PlayerCircles.length][2];
    for (int i = 0; i < PlayerCircles.length; i++) {
      arrowsPos[i][0] = ArrowPolys[i].getPoints().get(2) + Config.arrow_hitbox_radius / 2;
      arrowsPos[i][1] = PlayerCircles[i].getTranslateY();
      // new double[] {ArrowPoly1.getPoints().get(2) + Config.arrow_hitbox_radius / 2,
      // Player1Circle.getTranslateY()},
    }
    target1Pos = new double[] { Target1Circle.getTranslateX(), Target1Circle.getTranslateY() };
    target1PosStart = new double[] { Target1Line.getStartX(), Target1Line.getStartY() };
    target1PosEnd = new double[] { Target1Line.getEndX(), Target1Line.getEndY() };

    target2Pos = new double[] { Target2Circle.getTranslateX(), Target2Circle.getTranslateY() };
    target2PosStart = new double[] { Target2Line.getStartX(), Target2Line.getStartY() };
    target2PosEnd = new double[] { Target2Line.getEndX(), Target2Line.getEndY() };

    gamePaneWidth = GamePane.getPrefWidth();
  }

  private void resetScore() {
    for (int i = 0; i < ScoreFramesPlayer.length; i++) {
      Label playerScoreLabel = (Label)ScoreFramesPlayer[i].getChildren().get(4);
      Label playerShotsLabel = (Label)ScoreFramesPlayer[i].getChildren().get(6);
      playerScoreLabel.setText("0");
      playerShotsLabel.setText("0");
    }
  }

  private static Image getImage(String path) throws IOException {
    return new Image(PrimaryController.class.getResource(path).toString());
  }

  public void addPlayer(byte slot, String name) {
    PlayerCircles[slot].setVisible(true);
    Fingers[slot].setVisible(false);
    Hands[slot].setVisible(false);
    ScoreFramesPlayer[slot].setVisible(true);
    Label playerNameLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(2);
    playerNameLabel.setText(name);
    ArrowPolys[slot].setTranslateX(0);
    ArrowPolys[slot].setVisible(false);
  }

  public void removePlayer(byte slot) {
    PlayerCircles[slot].setVisible(false);
    Fingers[slot].setVisible(false);
    Hands[slot].setVisible(false);
    ScoreFramesPlayer[slot].setVisible(false);
    Label playerScoreLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(4);
    Label playerShotsLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(6);
    playerScoreLabel.setText("0");
    playerShotsLabel.setText("0");
    ArrowPolys[slot].setTranslateX(0);
    ArrowPolys[slot].setVisible(false);
  }

  public void readyPlayer(byte slot) {
    Fingers[slot].setVisible(true);
  }

  public void unreadyPlayer(byte slot) {
    Fingers[slot].setVisible(false);
  }

  public void sync_game(MyUtils.ArrowState[] arrows, double target1Pos, double target2Pos) {
    for (int i = 0; i < arrows.length; i++) {
      ArrowPolys[i].setVisible(arrows[i].visible);
      ArrowPolys[i].setTranslateX(arrows[i].posX - arrowsPos[i][0]); // because arrows[i].posX is a hitbox position, not arrow's
    }
    Target1Circle.setTranslateY(target1Pos);
    Target2Circle.setTranslateY(target2Pos);
  }

  public void set_score(byte slot, byte score) {
    Label playerScoreLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(4);
    playerScoreLabel.setText("" + score);
  }

  public void increment_shots_count(byte slot) {
    Label playerShotsLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(6);
    playerShotsLabel.setText("" + (Integer.parseInt(playerShotsLabel.getText()) + 1));
  }

  public void declare_winner(byte slot) {
    initialize_prepare();
    Label playerNameLabel = (Label)ScoreFramesPlayer[slot].getChildren().get(2);
    for (int i = 0; i < PlayerCircles.length; i++) {
      unreadyPlayer((byte)i);
    }
    isGameGoing = false;
    createInfoPopup(null, "Игрок " + playerNameLabel.getText() + " победил!");
  }

  public void player_paused(byte slot) {
    Fingers[slot].setVisible(false);
    Hands[slot].setVisible(true);
  }

  public void player_unpaused(byte slot) {
    Fingers[slot].setVisible(true);
    Hands[slot].setVisible(false);
  }

  @FXML
  private void startGame() {
    port = 0;
    Stage dialog = new Stage();
    VBox mainBox = new VBox();
    mainBox.setAlignment(Pos.CENTER);

    HBox portBox = new HBox();
    portBox.setAlignment(Pos.CENTER);
    portBox.getChildren().add(new Label("Порт"));
    NumberField portField = new NumberField();
    portField.setText(String.valueOf(Config.port));
    portBox.getChildren().add(portField);
    mainBox.getChildren().add(portBox);

    HBox nameBox = new HBox();
    nameBox.setAlignment(Pos.CENTER);
    nameBox.getChildren().add(new Label("Имя"));
    TextField nameField = new TextField();
    nameField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
        if (nameField.getText().length() > Config.name_max_length) {
          String s = nameField.getText().substring(0, Config.name_max_length);
          nameField.setText(s);
        }
      }
    });
    nameBox.getChildren().add(nameField);
    mainBox.getChildren().add(nameBox);

    HBox posBox = new HBox();
    posBox.setAlignment(Pos.CENTER);
    posBox.getChildren().add(new Label("Позиция"));
    NumberField posField = new NumberField();
    posField.setText("0");
    posBox.getChildren().add(posField);
    mainBox.getChildren().add(posBox);

    HBox btnsBox = new HBox();
    btnsBox.setAlignment(Pos.CENTER);
    Button okayButton = new Button();
    okayButton.setText("Войти");
    okayButton.setOnAction(value -> {
      String name = nameField.getText().trim();
      if (name.length() == 0) {
        createInfoPopup(mainBox.getScene().getWindow(), "Неверное имя");
        return;
      }
      try {
        this.port = Integer.valueOf(portField.getText());
        if (port <= 0) {
          createInfoPopup(MainFrame.getScene().getWindow(), "Неверный номер порта");
          return;
        }
      } catch (NumberFormatException e) {
        createInfoPopup(MainFrame.getScene().getWindow(), "Неверный номер порта");
        return;
      }
      dialog.close();
    });
    btnsBox.getChildren().add(okayButton);
    Button closeButton = new Button();
    closeButton.setText("Закрыть");
    closeButton.setOnAction(value -> {
      dialog.close();
    });
    btnsBox.getChildren().add(closeButton);
    mainBox.getChildren().add(btnsBox);

    Group dialogGroup = new Group();
    dialogGroup.getChildren().add(mainBox);
    dialog.setScene(new Scene(dialogGroup));
    dialog.initOwner(MainFrame.getScene().getWindow());
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.showAndWait();
    if (port == 0) {
      return;
    }
    try {
      try {
      socket = new Socket("127.0.0.1", port);
      } catch (ConnectException e) {
        createInfoPopup(null, "В соединении отказано");
        port = 0;
        return;
      }
      dOut = new DataOutputStream(socket.getOutputStream());
      dInp = new DataInputStream(socket.getInputStream());
      dOut.write(new Message.Connect(Integer.valueOf(posField.getText()).byteValue(), nameField.getText().getBytes()).generateByteMessage());
      new Thread(() -> {
        byte[] message = new byte[Message.messageMaxSize];
        boolean flag = true;
        while (flag) {
          try {
            int ret = dInp.read(message, 0, message.length);
            if (ret == -1) {
              port = 0;
              initialize_start();
              flag = false;
              Platform.runLater(() -> {
                createInfoPopup(null, "Сервер закрылся");
              });
            }
          } catch (IOException e) {
            e.printStackTrace();
            flag = false;
          }
          System.out.println(message[0]);
          clientMessageHandler.handleMessage(message, Message.GENERIC);
          message[0] = Message.GENERIC;
        }
      }).start();
    } catch (IOException e) {
      port = 0;
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ee) {}
      }
      e.printStackTrace();
    }
  }

  public void createInfoPopup(Window window, String text) {
    Stage dialog = new Stage();
    VBox mainBox = new VBox();
    mainBox.setAlignment(Pos.CENTER);
    mainBox.getChildren().add(new Label(text));
    Button okButton = new Button();
    okButton.setText("Закрыть");
    okButton.setOnAction(value -> {
      dialog.close();
    });
    mainBox.getChildren().add(okButton);
    Group dialogGroup = new Group();
    dialogGroup.getChildren().add(mainBox);
    dialog.setScene(new Scene(dialogGroup));
    if (window == null)
      dialog.initOwner(MainFrame.getScene().getWindow());
    else
    dialog.initOwner(window);
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.showAndWait();
  }

  @FXML
  private void ready() {
    if (isReady) {
      sendMessage(new Message.Unready((byte)0).generateByteMessage());
      ReadyBtn.setText("Готов");
    } else {
      ReadyBtn.setText("Не готов");
      sendMessage(new Message.Ready((byte)0).generateByteMessage());
    }
    isReady = !isReady;
  }

  @FXML
  private void shoot() {
    sendMessage(new Message.Shoot((byte)0).generateByteMessage());
  }

  @FXML
  private void pauseGame() {
    if (isPaused) {
      sendMessage(new Message.Unpause((byte)0).generateByteMessage());
      PauseBtn.setText("Пауза");
    } else {
      sendMessage(new Message.Pause((byte)0).generateByteMessage());
      PauseBtn.setText("Продолжить");
    }
    isPaused = !isPaused;
  }

  public synchronized void sendMessage(byte[] msg) {
    try {
      dOut.write(msg);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void closeSocket() {
    if (socket != null && socket.isConnected()) {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
