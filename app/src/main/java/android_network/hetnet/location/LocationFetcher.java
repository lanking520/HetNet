package android_network.hetnet.location;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android_network.hetnet.cloud.HttpService;

import static android_network.hetnet.common.Constants.LOCATION_LIST_FETCHER;

public class LocationFetcher extends IntentService {
  public LocationFetcher() {
    super("LocationFetcher");
  }


  Handler HN = new Handler();
  HttpService cloudsender = new HttpService();
  private String PreUrl= "http://34.201.21.219:8111";

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
    WifiConfiguration wifiConfig = new WifiConfiguration();
    WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    Context context = getApplicationContext();
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
      || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      HN.post(new DisplayToast("Location Changed to: "+String.valueOf(location.getLongitude())+","+String.valueOf(location.getLatitude())));
      EventBus.getDefault().post(new LocationResponseEvent(LOCATION_LIST_FETCHER, location, Calendar.getInstance().getTime()));
      Map<String, String> param = new HashMap<>();
      String android_id= Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
//      WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
      String MAC = wifiManager.getConnectionInfo().getBSSID();

      param.put("device_id",android_id);
      param.put("curr_net", MAC);
      param.put("location",String.valueOf(location.getLongitude())+","+String.valueOf(location.getLatitude()));
      try {
        String result = cloudsender.GET(PreUrl+"/event/getmacidbyprefbyloc",param);
        if (result.charAt(10) != '"')
          HN.post(new DisplayToast("Suggested Network: "+result));
        Log.i("DEC", result.substring(10, result.indexOf(',') - 1));
        String ssid = result.substring(10, result.indexOf(',') - 1);

        wifiConfig.SSID = String.format("\"%s\"", ssid);
        if (ssid.equals("Columbia University") || ssid.equals("Kai He's iPhone") || ssid.equals("Shen'sIphone") || ssid.equals("CSORapartment-5G") || ssid.equals("CSORapartment")) {
          wifiConfig.SSID = String.format("\"%s\"", ssid);
          if (ssid.equals("Kai He's iPhone")) {
            wifiConfig.preSharedKey = String.format("\"%s\"", "15691855825");
          } else if (ssid.equals("Shen'sIphone")) {
            wifiConfig.preSharedKey = String.format("\"%s\"", "personalWifi2017");
          } else if (ssid.equals("CSORapartment-5G") || ssid.equals("CSORapartment")) {
            wifiConfig.preSharedKey = String.format("\"%s\"", "XL5TSTTWRV695VKS");
          }


          int netId = wifiManager.addNetwork(wifiConfig);
          wifiManager.disconnect();
          wifiManager.enableNetwork(netId, true);
          wifiManager.reconnect();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
