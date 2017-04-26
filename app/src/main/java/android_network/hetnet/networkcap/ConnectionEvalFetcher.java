package android_network.hetnet.networkcap;

import android.app.IntentService;
import android.content.Intent;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;

import android_network.hetnet.data.NetworkEvaluation;

import static android_network.hetnet.common.Constants.CONNECTION_EVAL_FETCHER;

/**
 * Created by lanking on 26/04/2017.
 */

public class ConnectionEvalFetcher extends IntentService {
    public ConnectionEvalFetcher(){super("ConnectionEvalFetcher");}
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        NetworkEvaluation eval = new NetworkEvaluation();
        // TODO: Implement fetcher for things we need
        EventBus.getDefault().post(new ConnectionResponseEvent(CONNECTION_EVAL_FETCHER, eval, Calendar.getInstance().getTime()));
    }
}
