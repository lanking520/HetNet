package android_network.hetnet.cloud;

import android.app.IntentService;
import android.content.Intent;
import android.provider.Settings.Secure;
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
import android_network.hetnet.system.ApplicationList;
import android_network.hetnet.system.SystemList;

public class SendCloud extends IntentService {
  private static final String TAG = "SendCloud";

  private String PreUrl= "http://34.201.21.219:8111";
  private String android_id;
  private HttpService cloudsender = new HttpService();

  public SendCloud() {super("SendCloud");}



  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    android_id= Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
    ArrayList<DataStoreObject> dataStoreObjectList = (ArrayList) intent.getSerializableExtra("currentData");
    DataStoreObject tempdata = dataStoreObjectList.get(0);
    Date curr= Calendar.getInstance().getTime();
    try {
      NetworkPoster(tempdata.getListOfNetworks(), curr, android_id, tempdata.getLongitude(), tempdata.getLatitude(), PreUrl+"/network");
      //SystemPoster(tempdata.getSystemList(), curr, android_id, PreUrl+"/uploadappdetl");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void NetworkPoster(List<Network> networks, Date current, String DeviceID, double Longtitue, double Latitude , String url) throws JSONException {
    Map<String, Object> temp = new HashMap<>();
    temp.put("Time", current.toString());
    temp.put("device_id", DeviceID);
    temp.put("location", String.valueOf(Longtitue)+","+String.valueOf(Latitude));
    JSONArray passednetwork = new JSONArray();
    JSONObject submission = new JSONObject(temp);
    Set<String> networkClean = new HashSet<>();
    for(Network net : networks){
      if(!networkClean.contains(net.getNetworkSSID())){
        networkClean.add(net.getNetworkSSID());
        Map<String, Object> tempnet = new HashMap<>();
        tempnet.put("bandwidth", net.getBandwidth());
        //tempnet.put("Cost", net.getCost());
        tempnet.put("ssid", net.getNetworkSSID());
        tempnet.put("security", net.getSecurityProtocol());
        //tempnet.put("SignalFrequency", net.getSignalFrequency());
        //tempnet.put("TimeToConnect", net.getTimeToConnect());
        tempnet.put("avgss", net.getSignalStrength());
        passednetwork.put(new JSONObject(tempnet));
      }
    }
    submission.put("Networks", passednetwork);

    try {
      Log.i("SendCloud",submission.toString());
      cloudsender.POST(url, submission.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void SystemPoster(SystemList systems, Date current, String DeviceID, String url) throws JSONException {
    Map<String, Object> holder = new HashMap<>();
    holder.put("Time", current.toString());
    holder.put("device_id", DeviceID);
    holder.put("type","CPUBat");
    JSONArray applications = new JSONArray();
    JSONObject submission = new JSONObject(holder);
    Map<Integer, ApplicationList> temp = systems.getApplicationList();
    for(ApplicationList app : temp.values()){
      Map<String, Object> sys = new HashMap<>();
      sys.put("ProcessName",app.getProcessName());
      sys.put("CpuUsage", app.getCpuUsage());
      //sys.put("RxBytes",app.getRxBytes());
      //sys.put("TxBytes",app.getTxBytes());
      //sys.put("PrivateClean",app.getPrivateClean());
      sys.put("BatteryPercent",app.getBatteryPercent());
      //sys.put("Uss",app.getUss());
      //sys.put("Pss",app.getPss());
      applications.put(new JSONObject(sys));
    }
    submission.put("Applications", applications);
    try {
      Log.i("SendCloud",submission.toString());
      cloudsender.POST(url, submission.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}


