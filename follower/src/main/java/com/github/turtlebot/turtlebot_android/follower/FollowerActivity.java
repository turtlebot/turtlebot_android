package com.github.turtlebot.turtlebot_android.follower;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;
import org.ros.android.view.RosImageView;
import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class FollowerActivity extends RosAppActivity
{
  private Toast    lastToast;
  private ConnectedNode node;
  private RosImageView<sensor_msgs.CompressedImage> cameraView;
  private static final String cameraTopic = "camera/rgb/image_color/compressed_throttle";


  public FollowerActivity()
  {
    super("FollowerActivity", "FollowerActivity");
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    setDefaultMasterName(getString(R.string.default_robot));
    setDefaultAppName(getString(R.string.default_app));
    setDashboardResource(R.id.top_bar);
    setMainWindowResource(R.layout.main);

    super.onCreate(savedInstanceState);

    cameraView = (RosImageView<sensor_msgs.CompressedImage>)findViewById(R.id.image);
    cameraView.setMessageType(sensor_msgs.CompressedImage._TYPE);
    cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

    // Register input controls callbacks
    Button backButton = (Button) findViewById(R.id.back_button);
    backButton.setOnClickListener(backButtonListener);

    ImageButton startButton = (ImageButton)findViewById(R.id.button_start);
    startButton.setOnClickListener(startButtonListener);

    ImageButton stopButton = (ImageButton)findViewById(R.id.button_stop);
    stopButton.setOnClickListener(stopButtonListener);

    // TODO Tricky solution to the StrictMode; the recommended way is by using AsyncTask
    if (android.os.Build.VERSION.SDK_INT > 9) {
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
    }
  }


  @Override
  protected void init(NodeMainExecutor nodeMainExecutor)
  {
    super.init(nodeMainExecutor);

    NodeConfiguration nodeConfiguration =
      NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());

    // Execute camera view node
    cameraView.setTopicName(getMasterNameSpace().resolve(cameraTopic).toString());
    nodeMainExecutor.execute(cameraView, nodeConfiguration.setNodeName("android/camera_view"));

    // Execute another node just to allow calling services; I suppose there will be a shortcut for this in the future
    nodeMainExecutor.execute(new AbstractNodeMain()
    {
      @Override
      public GraphName getDefaultNodeName()
      {
        return GraphName.of("android/follower");
      }

      @Override
      public void onStart(final ConnectedNode connectedNode)
      {
        node = connectedNode;
      }
    }, nodeConfiguration.setNodeName("android/follower"));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    menu.add(0,0,0,R.string.stop_app);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()){
    case 0:
        onDestroy();
        break;
    }
    return true;
  }

  private void callService(byte newState)
  {
    if (node == null)
    {
      Log.e("FollowerActivity", "Still doesn't have a connected node");
      return;
    }

    ServiceClient<turtlebot_msgs.SetFollowStateRequest, turtlebot_msgs.SetFollowStateResponse> serviceClient;
    try
    {
        NameResolver appNameSpace = getMasterNameSpace();
        String srvTopic = appNameSpace.resolve("turtlebot_follower/change_state").toString();
      serviceClient = node.newServiceClient(srvTopic, turtlebot_msgs.SetFollowState._TYPE);
    }
    catch (ServiceNotFoundException e)
    {
      Log.e("FollowerActivity", "Service not found: " + e.getMessage());
      Toast.makeText(getBaseContext(), "Change follower state service not found", Toast.LENGTH_LONG).show();
      return;
    }
    final turtlebot_msgs.SetFollowStateRequest request = serviceClient.newMessage();
    request.setState(newState);

    serviceClient.call(request, new ServiceResponseListener<turtlebot_msgs.SetFollowStateResponse>() {
      @Override
      public void onSuccess(turtlebot_msgs.SetFollowStateResponse response) {
        Log.i("FollowerActivity", "Service result " + response.getResult());
        if (request.getState() == turtlebot_msgs.SetFollowStateRequest.STOPPED)
          showToast("Follower stopped");
        else
          showToast("Follower started");
      }

      @Override
      public void onFailure(RemoteException e) {
        Log.e("FollowerActivity", "Service result: failure (" + e.getMessage() + ")");
        showToast("Change follower state failed");
      }
    });
  }

  /**
   * Call Toast on UI thread.
   * @param message Message to show on toast.
   */
  private void showToast(final String message)
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run() {
        if (lastToast != null)
          lastToast.cancel();

        lastToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
        lastToast.show();
      }
    });
  }

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
      callService(turtlebot_msgs.SetFollowStateRequest.FOLLOW);
    }
  };

  private final OnClickListener stopButtonListener = new OnClickListener()
  {
    @Override
    public void onClick(View v)
    {
      callService(turtlebot_msgs.SetFollowStateRequest.STOPPED);
    }
  };
}
