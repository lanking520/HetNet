package android_network.hetnet.cloud;

import android.Manifest;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android_network.hetnet.data.Network;
import android_network.hetnet.network.NetworkAdditionalInfo;
import android_network.hetnet.network.SecurityManager;
import android_network.hetnet.vpn_service.DatabaseHelper;

/**
 * Created by lanking on 04/04/2017.
 */

public class AppDataService extends IntentService {
    private String PreUrl= "http://34.201.21.219:8111";
    private String android_id;

    static WifiManager wifiManager;
    TelephonyManager telephonyManager;
    List<Network> networkList = new ArrayList<>();
    boolean wifiDataReceived = false;

    static long startT;
    static long endT;
    static long connectT;

    Location location;

    public AppDataService() {
        super("AppDataService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        getWifiInfo();
        getLTEInfo();
        getLocation();
        //Log.i("NETWORK INFO",networkList.toString());
        Log.i("Location INFO", location.toString());
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
    private void getLTEInfo() {
        // Getting telephony manager for LTE
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String carrierName = telephonyManager.getNetworkOperatorName();

        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();

        // hypothetical value/connection type ie. 3G -> 1Gbps
        int speedMobile = telephonyManager.getNetworkType();

        for (CellInfo cellInfo : cellInfoList) {
            Network network = new Network();

            if (cellInfo instanceof CellInfoLte) {
                if (cellInfo.isRegistered()) {
                    network.setNetworkSSID(carrierName);
                } else {
                    network.setNetworkSSID("Other");
                }

                // signal strength
                network.setSignalStrength(((CellInfoLte) cellInfo).getCellSignalStrength().getLevel());

                // display max value
                network.setBandwidth(speedMobile);

                // TBD
                SecurityManager.checkNetworkConnectivity(network);

                // hardcoded lTE connection immediate
                network.setTimeToConnect(0);

                network.setCost(getCarrierCost(carrierName));

                // hardcoded LTE is not selected
                network.setCurrentNetwork(false);

                networkList.add(network);
            }
        }
    }

    @NonNull
    private Double getCarrierCost(String carrierName) {
        Double cost = 0.0;

        switch (carrierName) {
            case "Fi Network":
                cost = 0.5;
                break;
            case "Verizon":
                cost = 1.0;
                break;
            case "AT&T":
                cost = 2.0;
                break;
            case "Sprint":
                cost = 3.0;
                break;
            case "Tmobile":
                cost = 4.0;
                break;
        }
        return cost;
    }

    private void getWifiInfo() {
        wifiDataReceived = false;

        // Getting the WiFi Manager
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);


        startT = System.currentTimeMillis();
        // Initiate the network scan
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();


        while (!wifiDataReceived) {;}

        try {
            unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("APPData", "Error unregistering receiver in getWifiInfo");
        }
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        // This method is called when number of WiFi connections changed
        public void onReceive(Context context, Intent intent) {

            List<ScanResult> wifiList = wifiManager.getScanResults();
            // gets maximum network speed
            double max_speed = wifiManager.getConnectionInfo().getLinkSpeed();

            for (ScanResult result : wifiList) {

                Network network = new Network();

                //hypothetical value
                network.setBandwidth(max_speed);

                //name
                network.setNetworkSSID(result.SSID);

                //WPA
                network.setSecurityProtocol(result.capabilities);

                //dB
                network.setSignalStrength(result.level);

                //Hz not useful
                network.setSignalFrequency(result.frequency);

                // TBD
                SecurityManager.checkNetworkConnectivity(network);

                // this class not used atm can be removed
                //NetworkAdditionalInfo.getTimeToConnect(network);


                network.setCost(0.0);

                // check if this is current connected network
                isCurrentNet(context, network);

                if (network.isCurrentNetwork()) {
                    endT = System.currentTimeMillis();
                    connectT = endT - startT;

                    network.setTimeToConnect(connectT);
                }
                if (!network.isCurrentNetwork()) {
                    network.setTimeToConnect(-1);
                }

                // check if network requires password
                password(network);

                // separate app for speed calculation so N/A right now
                network.setSpeed(NetworkAdditionalInfo.getNetworkSpeed(network));

                networkList.add(network);

            }

            wifiDataReceived = true;
        }
    };

    public static void isCurrentNet(Context context, Network network) {
        // sets current network variable
        String current_SSID = "";

        // get current connected network
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            // format: "current_SSID"
            current_SSID = activeNetwork.getExtraInfo();

            // check if network name matches current_SSID
            if (current_SSID.contains(network.getNetworkSSID())) {
                network.setCurrentNetwork(true);
                // if protocol is [WPA2-PSK..] and you know password
                network.setPossibleToConnect(true);
            } else {
                network.setCurrentNetwork(false);
            }
        }
    }

    public static void password(Network network) {
        //Checks if network is open (no password required)

        String protocol = network.getSecurityProtocol();

        // if password is needed protocol will be [WPA2-PSK....]
        if (protocol.contains("[WPA2]") || protocol.contains("[WEP]")) {
            network.setPossibleToConnect(true);

        }
    }

//    public static long getTimeToConnect(Network network) {
//        // used when more than one network is available for connection
//        long time = 0;
//
//        WifiInfo w = wifiManager.getConnectionInfo();
//
//        int i = w.getNetworkId();
//
//        wifiManager.disconnect();
//        wifiManager.setWifiEnabled(true);
//        wifiManager.enableNetwork(i, true);
//        wifiManager.reconnect();
//
//        return time;
//    }

    public void getLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Context context = getApplicationContext();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }
}

