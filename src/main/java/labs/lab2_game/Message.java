package labs.lab2_game;

import java.nio.ByteBuffer;

public class Message {
  public final static int realMessageMaxSize = 1+4+4*8+2*8;
  public final static int messageMaxSize = (int)Math.pow(2.0, Math.ceil(Math.log(realMessageMaxSize) / Math.log(2)));
  public final static byte GENERIC = 0;
  public final static byte CONNECT = 1; // byte_slot, bytes_name[24]
  public final static byte REJECT = 2; // byte_reason
  //public final static byte JOIN = 3;
  public final static byte EXIT = 4; // byte_slot
  public final static byte READY = 5; // byte_slot
  public final static byte UNREADY = 6; // byte_slot
  public final static byte SYNC = 7; // (bool, double)*4, double_target1, double_target2
  public final static byte SHOOT = 8; // byte_slot
  public final static byte HIT = 9; // byte_slot, byte_points
  //public final static byte PAUSE = 10; // byte_slot // let it both use READY and UNREADY?
  //public final static byte UNPAUSE = 11; // byte_slot
  public final static byte WINNER = 12; // byte_slot

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
      }
      return null;
    }
    public synchronized byte[] handleConnect(Connect message) { return null; }
    public byte[] handleReject(Reject message) { return null; }
    public byte[] handleExit(Exit message) { return null; }
    public byte[] handleReady(Ready message) { return null; }
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
    protected byte reason = 0;

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
        return new byte[1];
      }
      byte[] message = new byte[2];
      message[0] = REJECT;
      message[1] = reason;
      return message;
    }
  }

  public static class Exit extends Generic {
    protected byte slot = 0;

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
        return new byte[1];
      }
      byte[] message = new byte[2];
      message[0] = EXIT;
      message[1] = slot;
      return message;
    }
  }

  public static class Ready extends Generic {
    protected byte slot = 0;

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
        return new byte[1];
      }
      byte[] message = new byte[2];
      message[0] = EXIT;
      message[1] = slot;
      return message;
    }
  }
}
