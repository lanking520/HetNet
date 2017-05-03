package android_network.hetnet.cloud;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.v4.app.ActivityCompat;
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
import java.util.Set;

import android_network.hetnet.data.DataStoreObject;
import android_network.hetnet.data.Network;
import android_network.hetnet.data.NetworkEvaluation;
import android_network.hetnet.system.ApplicationList;
import android_network.hetnet.system.SystemList;

public class SendCloud extends IntentService {
  private static final String TAG = "SendCloud";

  private String PreUrl= "http://34.201.21.219:8111";
  private Location location;
  private HttpService cloudsender = new HttpService();

  private String android_id;

  public SendCloud() {super("SendCloud");}



  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    NetworkEvaluation eval = (NetworkEvaluation) intent.getSerializableExtra("currentData");
    Date curr= Calendar.getInstance().getTime();
    getLocation();
    android_id= Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
    try {
      // NetworkPoster(tempdata.getListOfNetworks(), curr, android_id, tempdata.getLongitude(), tempdata.getLatitude(), PreUrl+"/network");
      // SystemPoster(tempdata.getSystemList(), curr, android_id, PreUrl+"/uploadappdetl");
      NetworkEvalPoster(android_id,curr, eval.getBandwidth(), eval.getLatency(), eval.getMAC_ADDR(),location.getLongitude(),location.getLatitude(), PreUrl+"/neteval");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void NetworkEvalPoster(String android_id, Date current, String Bandwidth, double latency, String Macaddr, double longitude, double latitude, String url){
    Map<String, Object> temp = new HashMap<>();
    temp.put("Time", current.toString());
    temp.put("Macaddr", Macaddr);
    temp.put("Latency", String.valueOf(latency));
    temp.put("Bandwidth", Bandwidth);
    temp.put("Location", String.valueOf(longitude)+","+String.valueOf(latitude));
    temp.put("device_id",android_id);
    JSONObject submission = new JSONObject(temp);
    try {
      Log.i("SendCloud",submission.toString());
      cloudsender.POST(url, submission.toString());
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

//  private void NetworkPoster(List<Network> networks, Date current, String DeviceID, double Longtitue, double Latitude , String url) throws JSONException {
//    Map<String, Object> temp = new HashMap<>();
//    temp.put("Time", current.toString());
//    temp.put("device_id", DeviceID);
//    temp.put("location", String.valueOf(Longtitue)+","+String.valueOf(Latitude));
//    JSONArray passednetwork = new JSONArray();
//    JSONObject submission = new JSONObject(temp);
//    Set<String> networkClean = new HashSet<>();
//    for(Network net : networks){
//      if(!networkClean.contains(net.getNetworkSSID())){
//        networkClean.add(net.getNetworkSSID());
//        Map<String, Object> tempnet = new HashMap<>();
//        tempnet.put("bandwidth", net.getBandwidth());
//        //tempnet.put("Cost", net.getCost());
//        tempnet.put("ssid", net.getNetworkSSID());
//        tempnet.put("security", net.getSecurityProtocol());
//        //tempnet.put("SignalFrequency", net.getSignalFrequency());
//        //tempnet.put("TimeToConnect", net.getTimeToConnect());
//        tempnet.put("avgss", net.getSignalStrength());
//        passednetwork.put(new JSONObject(tempnet));
//      }
//    }
//    submission.put("Networks", passednetwork);
//
//    try {
//      Log.i("SendCloud",submission.toString());
//      cloudsender.POST(url, submission.toString());
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
//
//  private void SystemPoster(SystemList systems, Date current, String DeviceID, String url) throws JSONException {
//    Map<String, Object> holder = new HashMap<>();
//    holder.put("Time", current.toString());
//    holder.put("device_id", DeviceID);
//    holder.put("type","CPUBat");
//    JSONArray applications = new JSONArray();
//    JSONObject submission = new JSONObject(holder);
//    Map<Integer, ApplicationList> temp = systems.getApplicationList();
//    for(ApplicationList app : temp.values()){
//      Map<String, Object> sys = new HashMap<>();
//      sys.put("ProcessName",app.getProcessName());
//      sys.put("CpuUsage", app.getCpuUsage());
//      //sys.put("RxBytes",app.getRxBytes());
//      //sys.put("TxBytes",app.getTxBytes());
//      //sys.put("PrivateClean",app.getPrivateClean());
//      sys.put("BatteryPercent",app.getBatteryPercent());
//      //sys.put("Uss",app.getUss());
//      //sys.put("Pss",app.getPss());
//      applications.put(new JSONObject(sys));
//    }
//    submission.put("Applications", applications);
//    try {
//      Log.i("SendCloud",submission.toString());
//      cloudsender.POST(url, submission.toString());
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }



}


