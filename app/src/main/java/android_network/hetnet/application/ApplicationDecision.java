package android_network.hetnet.application;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import android_network.hetnet.cloud.HttpService;

/**
 * Created by lanking on 05/05/2017.
 */

public class ApplicationDecision extends IntentService {
    public ApplicationDecision() {super("ApplicationDecision");}
    Handler HN = new Handler();
    HttpService cloudsender = new HttpService();
    private String PreUrl= "http://34.201.21.219:8111";
    private Location location;

    private class DisplayToast implements Runnable {

        String TM = "";

        public DisplayToast(String toast){
            TM = toast;
        }

        public void run(){
            Toast.makeText(getApplicationContext(), TM, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        getLocation();
        String appname = (String) intent.getSerializableExtra("appname");
        HN.post(new DisplayToast("Current Foreground Application: "+appname));
        Map<String, String> param = new HashMap<>();
        String android_id= Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        String MAC = wifiManager.getConnectionInfo().getBSSID();

        param.put("device_id",android_id);
        param.put("curr_net", MAC);
        param.put("location",String.valueOf(location.getLongitude())+","+String.valueOf(location.getLatitude()));
        param.put("uid",appname);
        try {
            String decision = cloudsender.GET(PreUrl+"/event/getmacidbyprefbyuidloc",param);
            HN.post(new DisplayToast("Switch Decision: "+ decision));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void getLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Context context = getApplicationContext();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }
}
