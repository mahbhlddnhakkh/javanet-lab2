package labs.lab2_game;

import java.nio.ByteBuffer;

public class Message {
  public final static int realMessageMaxSize = 1+4+4*8+2*8;
  public final static int messageMaxSize = (int)Math.pow(2.0, Math.ceil(Math.log(realMessageMaxSize) / Math.log(2)));
  public final static byte GENERIC = 0;
  public final static byte CONNECT = 1; // byte_slot, bytes_name[24]
  public final static byte REJECT = 2; // byte_reason
  public final static byte EXIT = 3; // byte_slot
  public final static byte READY = 4; // byte_slot
  public final static byte UNREADY = 5; // byte_slot
  public final static byte GAME_BEGIN = 6; // nothing
  public final static byte SYNC = 7; // (bool, double)*4, double_target1, double_target2
  public final static byte SHOOT = 8; // byte_slot
  public final static byte SCORE_SYNC = 9; // byte_slot, byte_score
  public final static byte PLAYER_WON = 10; // byte_slot
  public final static byte PAUSE = 11; // byte_slot
  public final static byte UNPAUSE = 12; // byte_slot

  public static class MessageHandler {
    byte[] handleMessage(byte[] msg, byte expect) {
      if (msg == null || msg.length == 0) {
        return null;
      }
      switch (msg[0]) {
        case CONNECT:
          if (expect == GENERIC || expect == CONNECT)
            return handleConnect(new Connect(msg));
          break;
        case REJECT:
          if (expect == GENERIC || expect == REJECT)
            return handleReject(new Reject(msg));
          break;
        case EXIT:
          if (expect == GENERIC || expect == EXIT)
            return handleExit(new Exit(msg));
          break;
        case READY:
          if (expect == GENERIC || expect == READY)
            return handleReady(new Ready(msg));
          break;
        case UNREADY:
          if (expect == GENERIC || expect == UNREADY)
            return handleUnready(new Unready(msg));
          break;
        case GAME_BEGIN:
          if (expect == GENERIC || expect == GAME_BEGIN)
            return handleGameBegin(new GameBegin(msg));
          break;
        case SYNC:
          if (expect == GENERIC || expect == SYNC)
            return handleSync(new Sync(msg));
          break;
        case SHOOT:
          if (expect == GENERIC || expect == SHOOT)
            return handleShoot(new Shoot(msg));
          break;
        case SCORE_SYNC:
          if (expect == GENERIC || expect == SCORE_SYNC)
            return handleScoreSync(new ScoreSync(msg));
          break;
        case PLAYER_WON:
          if (expect == GENERIC || expect == PLAYER_WON)
            return handlePlayerWon(new PlayerWon(msg));
          break;
        case PAUSE:
          if (expect == GENERIC || expect == PAUSE)
            return handlePause(new Pause(msg));
          break;
        case UNPAUSE:
          if (expect == GENERIC || expect == UNPAUSE)
            return handleUnpause(new Unpause(msg));
          break;
      }
      return null;
    }
    public synchronized byte[] handleConnect(Connect message) { return null; }
    public byte[] handleReject(Reject message) { return null; }
    public byte[] handleExit(Exit message) { return null; }
    public byte[] handleReady(Ready message) { return null; }
    public byte[] handleUnready(Unready message) { return null; }
    public byte[] handleGameBegin(GameBegin message) { return null; }
    public byte[] handleSync(Sync message) { return null; }
    public byte[] handleShoot(Shoot message) { return null; }
    public byte[] handleScoreSync(ScoreSync message) { return null; }
    public byte[] handlePlayerWon(PlayerWon message) { return null; }
    public byte[] handlePause(Pause message) { return null; }
    public byte[] handleUnpause(Unpause message) { return null; }
  }

  public static class Generic {
    protected boolean good = true;

    Generic() {}

    Generic(byte[] msg) {
      checkMsgSize(msg);
      checkFirstByte(msg);
      interpretBuffer(msg, 1);
    }

    public int getGoodMsgSize() {
      return 0;
    }

    public void checkMsgSize(byte[] msg) {
      if (!isGood() || (msg.length - 1) < getGoodMsgSize()) {
        setBad();
      }
    }

    public boolean isGood() {
      return good;
    }

    public void setBad() {
      good = false;
    }

    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != GENERIC) {
        setBad();
      }
    }

    public void interpretBuffer(byte[] msg, int offset) {}

    public byte[] generateByteMessage() {
      byte[] message = new byte[messageMaxSize];
      message[0] = GENERIC;
      return message;
    }
  }

  public static class Connect extends Generic {
    protected byte slot;
    protected byte[] name;

    Connect(byte slot, byte[] name) {
      super();
      this.slot = slot;
      this.name = name;
    }

    Connect(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 1 + Config.name_max_length;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != CONNECT) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      ByteBuffer buffer = ByteBuffer.wrap(msg);
      slot = buffer.get(offset);
      name = new byte[Config.name_max_length];
      for (int i = offset + 1; i < getGoodMsgSize()+offset; i++) {
        name[i - offset - 1] = buffer.get(i);
      }
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[1];
      }
      // https://stackoverflow.com/questions/33810346/how-do-i-convert-mixed-java-data-types-into-a-java-byte-array
      ByteBuffer message = ByteBuffer.allocate(messageMaxSize);
      message.put(CONNECT);
      message.put(slot);
      message.put(name);
      return message.array();
    }
  }

  public static class Reject extends Generic {
    public final static byte NAME_EXIST = 0;
    public final static byte GAME_FULL = 1;
    public final static byte GAME_GOING = 2;
    protected byte reason;

    Reject(byte reason) {
      super();
      this.reason = reason;
    }

    Reject(byte[] msg) {
      super(msg);
    }
    @Override
    public int getGoodMsgSize() {
      return 1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != REJECT) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      reason = msg[offset];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = REJECT;
      message[1] = reason;
      return message;
    }
  }

  public static class Exit extends Generic {
    protected byte slot;

    Exit(byte slot) {
      super();
      this.slot = slot;
    }

    Exit(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != EXIT) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      slot = msg[offset];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = EXIT;
      message[1] = slot;
      return message;
    }
  }

  public static class Ready extends Generic {
    protected byte slot;

    Ready(byte slot) {
      super();
      this.slot = slot;
    }

    Ready(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != READY) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      slot = msg[offset];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = READY;
      message[1] = slot;
      return message;
    }
  }

  public static class Unready extends Generic {
    protected byte slot;

    Unready(byte slot) {
      super();
      this.slot = slot;
    }

    Unready(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != UNREADY) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      slot = msg[offset];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = UNREADY;
      message[1] = slot;
      return message;
    }
  }

  public static class GameBegin extends Generic {
    GameBegin() {
      super();
    }

    GameBegin(byte[] msg) {
      super(msg);
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != GAME_BEGIN) {
        setBad();
      }
    }

    @Override
    public byte[] generateByteMessage() {
      byte[] message = new byte[messageMaxSize];
      message[0] = GAME_BEGIN;
      return message;
    }
  }

  public static class Sync extends Generic {

    protected MyUtils.ArrowStateArray arrows;
    protected double target1PosY;
    protected double target2PosY;

    // More constructors?
    Sync(MyUtils.ArrowStateArray arrows, double target1Pos, double target2Pos) {
      this.arrows = arrows;
      this.target1PosY = target1Pos;
      this.target2PosY = target2Pos;
    }

    Sync(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return messageMaxSize-1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != SYNC) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      arrows = new MyUtils.ArrowStateArray(msg, offset, 9*4);
      ByteBuffer buffer = ByteBuffer.wrap(msg);
      target1PosY = buffer.getDouble(9*4+offset);
      target2PosY = buffer.getDouble(9*4+offset+8);
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      ByteBuffer message = ByteBuffer.allocate(messageMaxSize);
      message.put(SYNC);
      message.put(arrows.generateBytes());
      message.putDouble(target1PosY);
      message.putDouble(target2PosY);
      return message.array();
    }
  }

  public static class Shoot extends Generic {
    protected byte slot;

    Shoot(byte slot) {
      super();
      this.slot = slot;
    }

    Shoot(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != SHOOT) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      slot = msg[offset];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = SHOOT;
      message[1] = slot;
      return message;
    }
  }

  public static class ScoreSync extends Generic {
    protected byte slot;
    protected byte score;

    ScoreSync(byte slot, byte score) {
      super();
      this.slot = slot;
      this.score = score;
    }

    ScoreSync(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 2;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != SCORE_SYNC) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      slot = msg[offset];
      score = msg[offset+1];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = SCORE_SYNC;
      message[1] = slot;
      message[2] = score;
      return message;
    }
  }

  public static class PlayerWon extends Generic {
    protected byte slot;

    PlayerWon(byte slot) {
      super();
      this.slot = slot;
    }

    PlayerWon(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != PLAYER_WON) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      slot = msg[offset];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = PLAYER_WON;
      message[1] = slot;
      return message;
    }
  }

  public static class Pause extends Generic {
    protected byte slot;

    Pause(byte slot) {
      super();
      this.slot = slot;
    }

    Pause(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != PAUSE) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      slot = msg[offset];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = PAUSE;
      message[1] = slot;
      return message;
    }
  }

  public static class Unpause extends Generic {
    protected byte slot;

    Unpause(byte slot) {
      super();
      this.slot = slot;
    }

    Unpause(byte[] msg) {
      super(msg);
    }

    @Override
    public int getGoodMsgSize() {
      return 1;
    }

    @Override
    public void checkFirstByte(byte[] msg) {
      if (!isGood() || msg[0] != UNPAUSE) {
        setBad();
      }
    }

    @Override
    public void interpretBuffer(byte[] msg, int offset) {
      if (!isGood()) {
        return;
      }
      slot = msg[offset];
    }

    @Override
    public byte[] generateByteMessage() {
      if (!isGood()) {
        return new byte[messageMaxSize];
      }
      byte[] message = new byte[messageMaxSize];
      message[0] = UNPAUSE;
      message[1] = slot;
      return message;
    }
  }
}
