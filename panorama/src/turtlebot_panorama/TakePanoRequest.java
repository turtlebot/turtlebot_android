package turtlebot_panorama;

public interface TakePanoRequest extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "test_ros/TakePanoRequest";
  static final java.lang.String _DEFINITION = "# mode for taking the pictures\nuint8 mode\n# rotate, stop, snapshot, rotate, stop, snapshot, ...\nuint8 SNAPANDROTATE=0\n# keep rotating while taking snapshots\nuint8 CONTINUOUS=1\n# stop an ongoing panorama creation\nuint8 STOP=2\n# total angle of panorama picture\nfloat32 pano_angle\n# angle interval when creating the panorama picture in snap&rotate mode, time interval otherwise \nfloat32 snap_interval\n# rotating velocity\nfloat32 rot_vel\n\n";
  static final byte SNAPANDROTATE = 0;
  static final byte CONTINUOUS = 1;
  static final byte STOP = 2;
  byte getMode();
  void setMode(byte value);
  float getPanoAngle();
  void setPanoAngle(float value);
  float getSnapInterval();
  void setSnapInterval(float value);
  float getRotVel();
  void setRotVel(float value);
}
