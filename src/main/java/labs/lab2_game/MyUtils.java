package labs.lab2_game;

import java.nio.ByteBuffer;

public class MyUtils {
  public static final byte byteToBool(boolean val) {
    return val ? (byte)1 : (byte)0;
  }

  public static final boolean boolToByte(byte val) {
    return val != (byte)0 ? true : false;
  }

  public static class ArrowState {
    public boolean visible;
    public double posX;

    ArrowState() {
      visible = false;
      posX = 0.0;
    }

    ArrowState(boolean visible, double pos) {
      this.visible = visible;
      this.posX = pos;
    }

    ArrowState(byte[] bytes) {
      this(ByteBuffer.wrap(bytes));
    }

    ArrowState(ByteBuffer buffer) {
      visible = boolToByte(buffer.get());
      posX = buffer.getDouble();
    }

    public byte[] generateBytes() {
      byte[] bytes = new byte[9];
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      buffer.put(byteToBool(visible));
      buffer.putDouble(posX);
      return bytes;
    }
  }

  public static class ArrowStateArray {
    public ArrowState[] arr;

    ArrowStateArray(int n) {
      arr = new ArrowState[n];
      for (int i = 0; i < n; i++) {
        arr[i] = new ArrowState();
      }
    }

    ArrowStateArray(byte[] bytes, int offset, int length) {
      this((length <= bytes.length ? length : bytes.length) / 9);
      for (int i = 0; i < arr.length; i++) {
        arr[i] = new ArrowState(ByteBuffer.wrap(bytes, offset+i*9, 9));
      }
    }

    public byte[] generateBytes() {
      byte[] bytes = new byte[9*arr.length];
      for (int i = 0; i < arr.length; i++) {
        System.arraycopy(arr[i].generateBytes(), 0, bytes, i*9, 9);
      }
      return bytes;
    }
  }
}
