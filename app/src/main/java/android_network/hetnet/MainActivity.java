package android_network.hetnet;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android_network.hetnet.common.trigger_events.UITriggerEvent;
import android_network.hetnet.policy_engine.PolicyEngine;

import static android_network.hetnet.common.Constants.LOCATION_EVENT_TRACKER;
import static android_network.hetnet.common.Constants.NETWORK_EVENT_TRACKER;
import static android_network.hetnet.common.Constants.POLICY_ENGINE;
import static android_network.hetnet.common.Constants.SYSTEM_EVENT_TRACKER;

public class MainActivity extends Activity {
  private static final int REQUEST_READ_PHONE_STATE = 100;
  private static final int REQUEST_ACCESS_COARSE_LOCATION = 101;
  private static final int REQUEST_ACCESS_NETWORK_STATE = 102;

  TextView eventList;
  TextView networkList;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    eventList = (TextView) findViewById(R.id.event_list);
    networkList = (TextView) findViewById(R.id.network_list);

    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
    }

    EventBus.getDefault().register(this);

    Intent policyEngineService = new Intent(this, PolicyEngine.class);
    this.startService(policyEngineService);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case REQUEST_READ_PHONE_STATE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

          if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
          }
        }
      }
      case REQUEST_ACCESS_COARSE_LOCATION: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);

          if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, REQUEST_ACCESS_NETWORK_STATE);
          }
        }
      }
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onMessageEvent(UITriggerEvent event) {
    switch (event.getEventOriginator()) {
      case NETWORK_EVENT_TRACKER:
      case LOCATION_EVENT_TRACKER:
        eventList.setText(event.getEventName() + " event received from " + event.getEventOriginator() + " at " + event.getTimeOfEvent());
        networkList.setText("");
        break;
      case POLICY_ENGINE:
        eventList.setText("Data received from " + event.getEventOriginator() + " at " + event.getTimeOfEvent());
        networkList.setText(event.getEventName());
        break;
      case SYSTEM_EVENT_TRACKER:
        NotificationCompat.Builder mBuilder =
          new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_icon)
            .setContentTitle("HetNet")
            .setContentText(event.getEventName());

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
        break;
    }
  }
}
