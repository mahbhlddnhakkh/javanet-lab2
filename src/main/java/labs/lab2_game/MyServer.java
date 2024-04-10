package labs.lab2_game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
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

  private double target1Direction = 1;
  private double target2Direction = 1;

  public boolean gameIsGoing = false;

  private ServerSocket serverSocket;
  //InetAddress ip = null;

  public ServerMessageHandler messageHandler = new ServerMessageHandler(this);
  public ClientHandler[] clients = new ClientHandler[] {null, null, null, null};

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
      String name = new String(message.name, StandardCharsets.UTF_8);
      int slotsLeft = server.clients.length;
      byte freeSlot = 0;
      boolean freeSlotChosen = false;
      for (int i = 0; i < server.clients.length; i++) {
        if (server.clients[i] != null) {
          slotsLeft--;
          if (server.clients[i].getPlayerName().equals(name)) {
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

    @Override
    public byte[] handleReady(Message.Ready message) {
      server.clients[message.slot].ready = 1;
      server.sendToAllPlayers(message.generateByteMessage());
      server.tryStartGame();
      return null;
    }

    @Override
    public byte[] handleUnready(Message.Unready message) {
      server.clients[message.slot].ready = 0;
      server.sendToAllPlayers(message.generateByteMessage());
      return null;
    }
  }

  private static class ClientHandler extends Thread {
    public Socket clientSocket;
    private MyServer server;
    private String name;
    private int score = 0;
    private byte slot = 0;
    private byte ready = 0;
    public DataOutputStream dOut;
    public DataInputStream dInp;

    ClientHandler(Socket socket, MyServer s) {
      clientSocket = socket;
      server = s;
    }

    public String getPlayerName() {
      return name;
    }

    public int getPlayerScore() {
      return score;
    }

    public byte getSlot() {
      return slot;
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
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
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
    //ip = InetAddress.getLocalHost();
    serverSocket = new ServerSocket(Config.port);
    while (true) {
      new ClientHandler(serverSocket.accept(), this).start();
    }
  }

  public synchronized void addPlayer(byte slot, ClientHandler handler) {
    try {
      clients[slot] = handler;
      DataOutputStream dOut = handler.dOut;
      for (int i = 0; i < clients.length; i++) {
        if (clients[i] != null) {
          if (i != slot) {
            clients[i].sendMessage(new Message.Connect(handler.slot, handler.name.getBytes()).generateByteMessage());
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
    for (int i = 0; i < clients.length; i++) {
      if (clients[i] != null) {
        clients[i].sendMessage(message);
      }
    }
  }

  static public void resolveSlot(byte[] message, byte slot) {
    if (message.length > 1) {
      switch (message[0]) {
        case Message.EXIT:
        case Message.READY:
        case Message.UNREADY:
        case Message.SHOOT:
        case Message.HIT:
        case Message.WINNER:
          message[1] = slot;
          break;
      }
    }
  }

  public void sendMessage(byte slot, byte[] message) {
    clients[slot].sendMessage(message);
  }

  public void tryStartGame() {
    
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
