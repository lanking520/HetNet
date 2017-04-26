package android_network.hetnet.networkcap;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;

import android_network.hetnet.network.NetworkTriggerEvent;

import static android_network.hetnet.common.Constants.CONNECTION_EVAL_LISTENER;
import static android_network.hetnet.common.Constants.NETWORK_EVENT_TRACKER;

/**
 * Created by lanking on 26/04/2017.
 */

public class ConnectionListener extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(manager.getActiveNetworkInfo() != null) {
                EventBus.getDefault().post(new NetworkTriggerEvent(CONNECTION_EVAL_LISTENER, "Connection Changed", Calendar.getInstance().getTime()));
            }
        }
    };

}
