package turtlebot_panorama;

public interface TakePanoResponse extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "test_ros/TakePanoResponse";
  static final java.lang.String _DEFINITION = "\nuint8 status\nuint8 STARTED=0\nuint8 IN_PROGRESS=1\nuint8 STOPPED=2";
  static final byte STARTED = 0;
  static final byte IN_PROGRESS = 1;
  static final byte STOPPED = 2;
  byte getStatus();
  void setStatus(byte value);
}
