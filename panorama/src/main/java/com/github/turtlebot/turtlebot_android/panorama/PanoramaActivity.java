package com.github.turtlebot.turtlebot_android.panorama;

import org.ros.address.InetAddressFactory;
import org.ros.android.MessageCallable;
import com.github.ros_java.android_apps.application_management.RosAppActivity;
import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;


public class PanoramaActivity extends RosAppActivity implements NodeMain
{
  private ImageView imgView;
  private Toast   lastToast;
  private ConnectedNode node;
  private final MessageCallable<Bitmap, sensor_msgs.CompressedImage> callable =
      new ScaledBitmapFromCompressedImage(2);


  public PanoramaActivity()
  {
    super("PanoramaActivity", "PanoramaActivity");
  }

  /************************************************************
    Android code:
    Activity life cycle and GUI management
   ************************************************************/

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    setDefaultRobotName(getString(R.string.default_robot));
    setDefaultAppName(getString(R.string.default_app));
    setDashboardResource(R.id.top_bar);
    setMainWindowResource(R.layout.main);

    super.onCreate(savedInstanceState);
    buildView(false);

    // TODO Tricky solution to the StrictMode; the recommended way is by using AsyncTask
    if (android.os.Build.VERSION.SDK_INT > 9) {
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
    }
  }

  @Override
  protected void onStop()
  {
    super.onStop();
  }

  @Override
  protected void onRestart()
  {
    super.onRestart();
  }

  @Override
  protected void onPause()
  {
    super.onPause();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    // TODO this is not called now, so we cannot flip the screen
    Log.e("PanoramaActivity", "onConfigurationChanged");
    super.onConfigurationChanged(newConfig);

    buildView(true);
  }

  private void buildView(boolean rebuild)
  {
    CheckBox prevContCheck = null;
    SeekBar  prevSpeedBar  = null;
    SeekBar  prevAngleBar  = null;
    SeekBar  prevIntervBar = null;
    Drawable prevDrawable  = null;

    if (rebuild == true)
    {
      // If we are rebuilding GUI (probably because the screen was rotated) we must save widgets'
      // previous content, as setContentView will destroy and replace them with new instances
      prevContCheck = (CheckBox)findViewById(R.id.checkBox_continuous);
      prevSpeedBar  =  (SeekBar)findViewById(R.id.seekBar_speed);
      prevAngleBar  =  (SeekBar)findViewById(R.id.seekBar_angle);
      prevIntervBar =  (SeekBar)findViewById(R.id.seekBar_interval);
      prevDrawable  = imgView.getDrawable();
    }

    // Register input controls callbacks
    Button backButton = (Button) findViewById(R.id.back_button);
    backButton.setOnClickListener(backButtonListener);

    Button startButton = (Button)findViewById(R.id.button_start);
    startButton.setOnClickListener(startButtonListener);

    Button stopButton  = (Button)findViewById(R.id.button_stop);
    stopButton.setOnClickListener(stopButtonListener);

    CheckBox contCheck = (CheckBox)findViewById(R.id.checkBox_continuous);
    contCheck.setOnCheckedChangeListener(contCheckListener);
    if (rebuild == true)
      contCheck.setChecked(prevContCheck.isChecked());

    SeekBar speedBar  = (SeekBar)findViewById(R.id.seekBar_speed);
    speedBar.setOnSeekBarChangeListener(speedBarListener);
    if (rebuild == true)
      speedBar.setProgress(prevSpeedBar.getProgress());

    SeekBar angleBar  = (SeekBar)findViewById(R.id.seekBar_angle);
    angleBar.setOnSeekBarChangeListener(angleBarListener);
    if (rebuild == true)
      angleBar.setProgress(prevAngleBar.getProgress());

    SeekBar intervBar = (SeekBar)findViewById(R.id.seekBar_interval);
    intervBar.setOnSeekBarChangeListener(intervalBarListener);
    if (rebuild == true)
      intervBar.setProgress(prevIntervBar.getProgress());

    // Take a reference to the image view to show incoming panoramic pictures
    imgView = (ImageView)findViewById(R.id.imageView_panorama);
    if (rebuild == true)
      imgView.setImageDrawable(prevDrawable);
  }

  /**
   * Call Toast on UI thread.
   * @param message Message to show on toast.
   */
  public void showToast(final String message)
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run() {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
      }
    });
  }

  /************************************************************
    ROS code:
    NodeMain implementation and service call code
   ************************************************************/

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor)
  {
    super.init(nodeMainExecutor);

    NodeConfiguration nodeConfiguration =
      NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());

    nodeMainExecutor.execute(this, nodeConfiguration.setNodeName("android/video_view"));
  }

  protected void callService(byte mode)
  {
    if (node == null)
    {
      Log.e("PanoramaActivity", "Still doesn't have a connected node");
      return;
    }

    ServiceClient<turtlebot_panorama.TakePanoRequest, turtlebot_panorama.TakePanoResponse> serviceClient;
    try
    {
      NameResolver appNameSpace = getAppNameSpace();
      String srvTopic = appNameSpace.resolve("turtlebot_panorama/take_pano").toString();
      serviceClient = node.newServiceClient(srvTopic, turtlebot_panorama.TakePano._TYPE);
    }
    catch (ServiceNotFoundException e)
    {
      Log.e("PanoramaActivity", "Service not found: " + e.getMessage());
      Toast.makeText(getBaseContext(), "Panorama service not found", Toast.LENGTH_LONG).show();
      return;
    }
    final turtlebot_panorama.TakePanoRequest request = serviceClient.newMessage();

    SeekBar ang_speed   = (SeekBar)findViewById(R.id.seekBar_speed);
    SeekBar pano_angle  = (SeekBar)findViewById(R.id.seekBar_angle);
    SeekBar snap_interv = (SeekBar)findViewById(R.id.seekBar_interval);
    CheckBox continuous = (CheckBox)findViewById(R.id.checkBox_continuous);
    if (continuous.isChecked() == true)
      request.setSnapInterval(snap_interv.getProgress()/100.0f); // convert to seconds
    else
      request.setSnapInterval(snap_interv.getProgress());

    request.setRotVel((float)((ang_speed.getProgress()*Math.PI)/180.0)); // convert to radians/s
    request.setPanoAngle(pano_angle.getProgress());
    request.setMode(mode);

    serviceClient.call(request, new ServiceResponseListener<turtlebot_panorama.TakePanoResponse>() {
      @Override
      public void onSuccess(turtlebot_panorama.TakePanoResponse response) {
        Log.i("PanoramaActivity", "Service result: success (status " + response.getStatus() + ")");
        node.getLog().info(String.format("Service result %d",  response.getStatus()));
        if (request.getMode() == turtlebot_panorama.TakePanoRequest.STOP)
          showToast("Take panorama stopped.");
        else
          showToast("Take panorama started.");
      }

      @Override
      public void onFailure(RemoteException e) {
        Log.e("PanoramaActivity", "Service result: failure (" + e.getMessage() + ")");
        node.getLog().info(String.format("Service result: failure (%s)", e.getMessage()));
        showToast("Take panorama failed");
      }
    });
  }

  @Override
  public void onStart(ConnectedNode connectedNode)
  {
    Log.d("PanoramaActivity", connectedNode.getName() + " node started");
    node = connectedNode;

    NameResolver appNameSpace = getAppNameSpace();
    String panoImgTopic = appNameSpace.resolve("turtlebot_panorama/panorama/compressed").toString();

    Subscriber<sensor_msgs.CompressedImage> subscriber =
        connectedNode.newSubscriber(panoImgTopic, sensor_msgs.CompressedImage._TYPE);
    subscriber.addMessageListener(new MessageListener<sensor_msgs.CompressedImage>() {
      @Override
      public void onNewMessage(final sensor_msgs.CompressedImage message) {
        imgView.post(new Runnable() {
          @Override
          public void run() {
            imgView.setImageBitmap(callable.call(message));
          }
        });
        imgView.postInvalidate();
      }
    });
  }

  @Override
  public void onError(Node n, Throwable e)
  {
    Log.e("PanoramaActivity", n.getName() + " node error: " + e.getMessage());
  }

  @Override
  public void onShutdown(Node n)
  {
    Log.d("PanoramaActivity", n.getName() + " node shuting down...");
  }

  @Override
  public void onShutdownComplete(Node n)
  {
    Log.d("PanoramaActivity", n.getName() + " node shutdown completed");
  }

  @Override
  public GraphName getDefaultNodeName()
  {
    return GraphName.of("android/panorama");
  }

  /************************************************************
     Android code:
     Anonymous implementation for input controls callbacks
   ************************************************************/

  private final OnClickListener backButtonListener = new OnClickListener()
  {
    @Override
    public void onClick(View v)
    {
      onBackPressed();
    }
  };

  private final OnClickListener startButtonListener = new OnClickListener()
  {
    @Override
    public void onClick(View v)
    {
      CheckBox continuous = (CheckBox)findViewById(R.id.checkBox_continuous);

      if (continuous.isChecked() == true)
        callService(turtlebot_panorama.TakePanoRequest.CONTINUOUS);
      else
        callService(turtlebot_panorama.TakePanoRequest.SNAPANDROTATE);
    }
  };

  private final OnClickListener stopButtonListener = new OnClickListener()
  {
    @Override
    public void onClick(View v)
    {
      callService(turtlebot_panorama.TakePanoRequest.STOP);
    }
  };

  private final OnCheckedChangeListener contCheckListener = new OnCheckedChangeListener()
  {
    @Override
    public void onCheckedChanged(CompoundButton cb, boolean checked)
    {
      SeekBar snap_interv = (SeekBar)findViewById(R.id.seekBar_interval);
      if (checked == true)
      {
        // continuous rotation; snap interval represents milliseconds between snaps
        snap_interv.setMax(5000);
        snap_interv.setProgress(Math.round((snap_interv.getProgress()*5000)/90));
      }
      else
      {
        // Snap and rotate; snap interval represents degrees between snaps
        snap_interv.setProgress(Math.round((snap_interv.getProgress()*90)/5000));
        snap_interv.setMax(90);
      }
    }
  };

  private final OnSeekBarChangeListener speedBarListener = new OnSeekBarChangeListener()
  {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
      if (lastToast == null)
        lastToast = Toast.makeText(getBaseContext(), progress + " deg/s", Toast.LENGTH_SHORT);
      else
        lastToast.setText(progress + " deg/s");

      lastToast.show();
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }
  };

  private final OnSeekBarChangeListener angleBarListener = new OnSeekBarChangeListener()
  {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
      if (lastToast == null)
        lastToast = Toast.makeText(getBaseContext(), progress + " degrees", Toast.LENGTH_SHORT);
      else
        lastToast.setText(progress + " degrees");

      lastToast.show();
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }
  };

  private final OnSeekBarChangeListener intervalBarListener = new OnSeekBarChangeListener()
  {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
      if (seekBar.getMax() > 360) // continuous rotation; snap interval represents milliseconds between snaps
      {
        if (lastToast == null)
          lastToast = Toast.makeText(getBaseContext(), (progress/100)/10.0 + " seconds", Toast.LENGTH_SHORT);
        else
          lastToast.setText((progress/100)/10.0 + " seconds");
      }
      else                        // snap and rotate; snap interval represents degrees between snaps
      {
        if (lastToast == null)
          lastToast = Toast.makeText(getBaseContext(), progress + " degrees", Toast.LENGTH_SHORT);
        else
          lastToast.setText(progress + " degrees");
      }
      lastToast.show();
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
    @Override public void onStopTrackingTouch(SeekBar seekBar) { }
  };
}
