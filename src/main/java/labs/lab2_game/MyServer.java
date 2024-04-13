package labs.lab2_game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import labs.lab2_game.Message.MessageHandler;

public class MyServer {
  private double[][] arrowsPos;
  private double[] target1Pos;
  private double[] target1PosStart;
  private double[] target1PosEnd;
  private double[] target2Pos;
  private double[] target2PosStart;
  private double[] target2PosEnd;
  private double gamePaneWidth;

  private Message.Sync sync;

  private double target1Direction = 1;
  private double target2Direction = 1;

  public boolean gameIsGoing = false;

  public boolean gameIsPaused = false;

  private ServerSocket serverSocket;

  public ServerMessageHandler messageHandler = new ServerMessageHandler(this);
  public ClientHandler[] clients = new ClientHandler[] {null, null, null, null};
  public Thread gameThread;

  private static class ServerMessageHandler extends MessageHandler {
    private MyServer server;

    ServerMessageHandler(MyServer server) {
      this.server = server;
    }

    @Override
    public synchronized byte[] handleConnect(Message.Connect message) {
      if (!message.isGood()) {
        return null;
      }
      if (server.gameIsGoing) {
        return new Message.Reject(Message.Reject.GAME_GOING).generateByteMessage();
      }
      String name = new String(message.name, StandardCharsets.UTF_8).trim();
      int slotsLeft = server.clients.length;
      byte freeSlot = 0;
      boolean freeSlotChosen = false;
      for (int i = 0; i < server.clients.length; i++) {
        if (server.clients[i] != null) {
          slotsLeft--;
          if (server.clients[i].name.equals(name)) {
            return new Message.Reject(Message.Reject.NAME_EXIST).generateByteMessage();
          }
        } else {
          if (!freeSlotChosen) {
            freeSlot = (byte)i;
          }
          if (freeSlot >= message.slot) {
            freeSlotChosen = true;
          }
        }
      }
      if (slotsLeft == 0) {
        return new Message.Reject(Message.Reject.GAME_FULL).generateByteMessage();
      }
      return new Message.Connect(freeSlot, message.name.clone()).generateByteMessage();
    }

    @Override
    public byte[] handleExit(Message.Exit message) {
      server.deletePlayer(message.slot);
      return null;
    }

    private synchronized void setReadyUnready(byte slot, byte ready, byte[] msg) {
      if (!server.gameIsGoing) {
        server.clients[slot].ready = ready;
        server.sendToAllPlayers(msg);
        server.tryStartGame();
      }
    }

    @Override
    public byte[] handleReady(Message.Ready message) {
      setReadyUnready(message.slot, (byte)1, message.generateByteMessage());
      return null;
    }

    @Override
    public byte[] handleUnready(Message.Unready message) {
      setReadyUnready(message.slot, (byte)0, message.generateByteMessage());
      return null;
    }

    @Override
    public byte[] handleShoot(Message.Shoot message) {
      if (server.gameIsGoing && !server.gameIsPaused) {
        server.clients[message.slot].shootingState = true;
        server.sendToAllPlayers(message.generateByteMessage());
      }
      return null;
    }

    private synchronized void setPauseUnpause(byte slot, byte action, byte[] msg) {
      if (server.gameIsGoing) {
        server.clients[slot].pause = action;
        server.sendToAllPlayers(msg);
        boolean flag = true;
        for (int i = 0; i < server.clients.length; i++) {
          if (server.clients[i] != null && server.clients[i].pause != (byte)0) {
            server.gameIsPaused = true;
            flag = false;
            break;
          }
        }
        if (flag && server.gameIsPaused) {
          server.gameIsPaused = false;
          synchronized (server) {
            server.notify();
          }
        }
      }
    }

    @Override
    public byte[] handlePause(Message.Pause message) {
      setPauseUnpause(message.slot, (byte)1, message.generateByteMessage());
      return null;
    }

    @Override
    public byte[] handleUnpause(Message.Unpause message) {
      setPauseUnpause(message.slot, (byte)0, message.generateByteMessage());
      return null;
    }
  }

  private static class ClientHandler extends Thread {
    public Socket clientSocket;
    private MyServer server;
    private String name;
    private byte score = 0;
    private byte slot = 0;
    private byte ready = 0;
    private byte pause = 0;
    public boolean shootingState = false;
    public DataOutputStream dOut;
    public DataInputStream dInp;

    ClientHandler(Socket socket, MyServer s) {
      clientSocket = socket;
      server = s;
    }

    public synchronized void sendMessage(byte[] message) {
      try {
        dOut.write(message);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void run() {
      try {
        dOut = new DataOutputStream(clientSocket.getOutputStream());
        dInp = new DataInputStream(clientSocket.getInputStream());
        byte[] message = new byte[Message.messageMaxSize];
        dInp.read(message, 0, message.length);
        byte[] response = server.messageHandler.handleMessage(message, Message.CONNECT);
        if (response == null || response[0] == Message.REJECT) {
          if (response != null)
            dOut.write(response);
          clientSocket.close();
          return;
        }
        Message.Connect clientInfo = new Message.Connect(response);
        slot = clientInfo.slot;
        name = new String(clientInfo.name, StandardCharsets.UTF_8);
        server.addPlayer(slot, this);
        boolean flag = true;
        while (flag) {
          try {
            int ret = dInp.read(message, 0, message.length);
            if (ret == -1) {
              flag = false;
            }
            System.out.println("-----");
            System.out.println(slot);
            System.out.println(message[0]);
            System.out.println("-----");
          } catch (IOException e) {
            e.printStackTrace();
            flag = false;
          }
          MyServer.resolveSlot(message, slot);
          server.messageHandler.handleMessage(message, Message.GENERIC);
          message[0] = Message.GENERIC;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      server.deletePlayer(slot);
    }
  }

  public void initialize() throws IOException {
    // evil hack
    javafx.application.Application.launch(FakeServerApp.class);
    arrowsPos = FakeServerApp.controller.arrowsPos;
    target1Pos = FakeServerApp.controller.target1Pos;
    target1PosStart = FakeServerApp.controller.target1PosStart;
    target1PosEnd = FakeServerApp.controller.target1PosEnd;
    target2Pos = FakeServerApp.controller.target2Pos;
    target2PosStart = FakeServerApp.controller.target2PosStart;
    target2PosEnd = FakeServerApp.controller.target2PosEnd;
    gamePaneWidth = FakeServerApp.controller.gamePaneWidth;
    resetSync();
    serverSocket = new ServerSocket(Config.port);
    while (true) {
      new ClientHandler(serverSocket.accept(), this).start();
    }
  }

  public void resetSync() {
    sync = new Message.Sync(new MyUtils.ArrowStateArray(clients.length), target1Pos[1], target2Pos[1]);
    for (int i = 0; i < clients.length; i++) {
      sync.arrows.arr[i].posX = arrowsPos[i][0];
    }
  }

  public synchronized void addPlayer(byte slot, ClientHandler handler) {
    try {
      clients[slot] = handler;
      DataOutputStream dOut = handler.dOut;
      for (int i = 0; i < clients.length; i++) {
        if (clients[i] != null) {
          if (i != slot) {
            //clients[i].sendMessage(new Message.Connect(handler.slot, handler.name.getBytes()).generateByteMessage());
            sendMessage((byte)i, new Message.Connect(handler.slot, handler.name.getBytes()).generateByteMessage());
          }
          dOut.write(new Message.Connect(clients[i].slot, clients[i].name.getBytes()).generateByteMessage());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void deletePlayer(byte slot) {
    if (clients[slot] != null) {
      try {
        clients[slot].clientSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      clients[slot] = null;
      sendToAllPlayers(new Message.Exit(slot).generateByteMessage());
    }
  }

  public void sendToAllPlayers(byte[] message) {
    if (message[0] != Message.SYNC) {
      System.out.println("|||||");
      System.out.println("ALL");
      System.out.println(message[0]);
      System.out.println("|||||");
    }
    for (int i = 0; i < clients.length; i++) {
      if (clients[i] != null) {
        clients[i].sendMessage(message);
      }
    }
  }

  public boolean arePlayersLeft() {
    for (int i = 0; i < clients.length; i++) {
      if (clients[i] != null) {
        return true;
      }
    }
    return false;
  }

  static public final void resolveSlot(byte[] message, byte slot) {
    if (message.length > 1) {
      switch (message[0]) {
        case Message.EXIT:
        case Message.READY:
        case Message.UNREADY:
        case Message.SHOOT:
        case Message.SCORE_SYNC:
        case Message.PLAYER_WON:
        case Message.PAUSE:
        case Message.UNPAUSE:
          message[1] = slot;
          break;
      }
    }
  }

  public synchronized void playerWon(byte slot) {
    if (gameIsGoing) {
      resetSync();
      sendToAllPlayers(sync.generateByteMessage());
      gameIsGoing = false;
      gameIsPaused = false;
      sendToAllPlayers(new Message.PlayerWon(slot).generateByteMessage());
      for (int i = 0; i < clients.length; i++) {
        if (clients[i] != null) {
          clients[i].ready = 0;
          clients[i].pause = 0;
          clients[i].score = 0;
        }
      }
    }
  }

  public void sendMessage(byte slot, byte[] message) {
    System.out.println("|||||");
    System.out.println(slot);
    System.out.println(message[0]);
    System.out.println("|||||");
    clients[slot].sendMessage(message);
  }

  public synchronized void tryStartGame() {
    if (gameIsGoing) {
      return;
    }
    boolean flag = true;
    for (int i = 0; i < clients.length; i++) {
      if (clients[i] != null && clients[i].ready == (byte)0) {
        flag = false;
        break;
      }
    }
    if (!flag) {
      return;
    }
    gameIsGoing = true;
    gameThread = new Thread(() -> {
      while(gameIsGoing) {
        if (gameIsPaused) {
          try {
            synchronized(this) {
              this.wait();
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        for (int i = 0; i < clients.length; i++) {
          if (clients[i] != null && clients[i].shootingState) {
            sync.arrows.arr[i].visible = true;
            double dx = sync.arrows.arr[i].posX - target1Pos[0];
            double dy = arrowsPos[i][1] - sync.target1PosY;
            byte points = -1;
            if (Math.sqrt(dx*dx + dy*dy) <= Config.arrow_hitbox_radius + Config.target_radius) {
              points = 1;
            }
            dx = sync.arrows.arr[i].posX - target2Pos[0];
            dy = arrowsPos[i][1] - sync.target2PosY;
            if (points == -1 && (Math.sqrt(dx*dx + dy*dy) <= Config.arrow_hitbox_radius + Config.target_radius / 2)) {
              points = 2;
            }
            if (points == -1 && (sync.arrows.arr[i].posX + Config.arrow_hitbox_radius > gamePaneWidth)) {
              points = 0;
            }
            switch (points) {
              case -1:
                sync.arrows.arr[i].posX += Config.arrow_speed;
                break;
              case 0:
                sync.arrows.arr[i].posX = arrowsPos[i][0];
                sync.arrows.arr[i].visible = false;
                clients[i].shootingState = false;
                break;
              default:
                sync.arrows.arr[i].posX = arrowsPos[i][0];
                sync.arrows.arr[i].visible = false;
                clients[i].score += points;
                clients[i].shootingState = false;
                sendToAllPlayers(new Message.ScoreSync((byte)i, clients[i].score).generateByteMessage());
                if (clients[i].score >= Config.final_score) {
                  playerWon((byte)i);
                  return;
                }
                break;
            }
          }
        }
        sync.target1PosY += Config.target_speed * target1Direction;
        if (sync.target1PosY > target1PosEnd[1] - Config.target_radius) {
          target1Direction = -1;
          sync.target1PosY = target1PosEnd[1] - Config.target_radius;
        }
        if (sync.target1PosY < target1PosStart[1] + Config.target_radius) {
          target1Direction = 1;
          sync.target1PosY = target1PosStart[1] + Config.target_radius;
        }
        sync.target2PosY += Config.target_speed * target2Direction * 2;
        if (sync.target2PosY > target2PosEnd[1] - Config.target_radius / 2) {
          target2Direction = -1;
          sync.target2PosY = target2PosEnd[1] - Config.target_radius / 2;
        }
        if (sync.target2PosY < target2PosStart[1] + Config.target_radius / 2) {
          target2Direction = 1;
          sync.target2PosY = target2PosStart[1] + Config.target_radius / 2;
        }
        sendToAllPlayers(sync.generateByteMessage());
        try {
          Thread.sleep(Config.sleep_time);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        gameIsGoing = arePlayersLeft();
      }
      resetSync();
    });
    sendToAllPlayers(new Message.GameBegin().generateByteMessage());
    gameThread.start();
  }

  public static void main(String[] args) {
    MyServer server = new MyServer();
    try {
      server.initialize();
      server.serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
