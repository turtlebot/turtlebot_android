package turtlebot_panorama;

public interface TakePano extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "turtlebot_panorama/TakePano";
  static final java.lang.String _DEFINITION = "# mode for taking the pictures\nuint8 mode\n# rotate, stop, snapshot, rotate, stop, snapshot, ...\nuint8 SNAPANDROTATE=0\n# keep rotating while taking snapshots\nuint8 CONTINUOUS=1\n# stop an ongoing panorama creation\nuint8 STOP=2\n# total angle of panorama picture\nfloat32 pano_angle\n# angle interval when creating the panorama picture in snap&rotate mode, time interval otherwise \nfloat32 snap_interval\n# rotating velocity\nfloat32 rot_vel\n\n---\n\nuint8 status\nuint8 STARTED=0\nuint8 IN_PROGRESS=1\nuint8 STOPPED=2";
}
