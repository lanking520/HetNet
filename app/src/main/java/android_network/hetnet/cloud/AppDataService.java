package android_network.hetnet.cloud;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.TrafficStats;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import android_network.hetnet.vpn_service.DatabaseHelper;

/**
 * Created by lanking on 04/04/2017.
 */

public class AppDataService extends IntentService {
    private String PreUrl= "http://34.201.21.219:8111";
    private String android_id;

    public AppDataService() {
        super("AppDataService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            TrafficPoster(PreUrl+"/appdata");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void TrafficPoster(String url) throws JSONException {

        JSONArray jsonArray = new JSONArray();

        Map<String, Object> holder = new HashMap<>();
        Date curr= Calendar.getInstance().getTime();
        holder.put("Time", curr.toString());
        android_id= Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        holder.put("device_id", android_id);
        holder.put("type","APPData");
        JSONObject submission = new JSONObject(holder);
        HashSet<Integer> hash = new HashSet<>();
        Cursor cursor = DatabaseHelper.getInstance(this).getAccess();
        cursor.moveToFirst();
        while (cursor.isAfterLast() == false)
        {
            int colUid = cursor.getColumnIndex("uid");
            int colTime = cursor.getColumnIndex("time");
            int uid = (cursor.isNull(colUid) ? -1 : cursor.getInt(colUid));
            if (hash.contains(uid)) {
                cursor.moveToNext();
            } else {
                hash.add(uid);
                Map<String, Object> json = new HashMap<>();
                long time = cursor.getLong(colTime);
                long now = SystemClock.elapsedRealtime();
                long up = TrafficStats.getUidTxBytes(uid);
                long down = TrafficStats.getUidRxBytes(uid);
                float upload = (float) up * 24 * 3600 * 1000 / 1024f / 1024f / now;
                float download = (float) down * 24 * 3600 * 1000 / 1024f / 1024f / now;
                String appName = this.getPackageManager().getNameForUid(uid);
                Log.i("ADS", "uid =  " + uid + " time = " + time + " appName = " + appName + " upload = " + upload + " download = " + download);

                json.put("uid", uid);
                json.put("time", time);
                json.put("application_package", appName);
                json.put("upload", upload);
                json.put("download", download);

                jsonArray.put(new JSONObject(json));
                cursor.moveToNext();
            }
        }

        submission.put("Applications", jsonArray);


        Log.i("SendCloud", submission.toString());
        HttpService cloudsender = new HttpService();
        try{
            cloudsender.POST(url,submission.toString());
        } catch (Exception e){
            e.printStackTrace();
        }


//        try
//            CloudPoster(PreUrl+"/location", holder.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}

