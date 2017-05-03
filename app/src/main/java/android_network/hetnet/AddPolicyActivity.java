package android_network.hetnet;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android_network.hetnet.cloud.HttpService;
import android_network.hetnet.cloud.PostTask;
import android_network.hetnet.data.Network;
import android_network.hetnet.vpn_service.DatabaseHelper;


public class AddPolicyActivity extends Activity {

    private String PreUrl= "http://34.201.21.219:8111";

    EditText locationRecord;
    Spinner networks;
    Spinner networks2;
    Spinner appspin;
    private Context mycontext = this;
    Button submitLN;
    Button submitAN;
    Location location;
    HttpService cloudsender;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_addpolicy_pg1);
      getLocation();
      locationRecord = (EditText) findViewById(R.id.LocationRecorder);
      networks = (Spinner) findViewById(R.id.NetworkSpin);
      networks2 = (Spinner) findViewById(R.id.NetworkSpin2);
      appspin = (Spinner) findViewById(R.id.AppSpin);
      List<String> tempnet = getNetworks();
//      ProgressDialog dialog=new ProgressDialog(mycontext);
//      dialog.setMessage("Loading Networks...");
//      dialog.setCancelable(false);
//      dialog.setInverseBackgroundForced(false);
//      dialog.show();
      while(tempnet.size() == 0){
          ;
      }
//      dialog.hide();
      networks.setAdapter(constructSpinner(tempnet));
      networks2.setAdapter(constructSpinner(tempnet));
      appspin.setAdapter(constructSpinner(getApp()));
      submitLN = (Button) findViewById(R.id.LNPolicySubmit);
      submitAN = (Button) findViewById(R.id.ANPolicySubmit);
      submitLN.setOnClickListener(submitLNL);
      submitAN.setOnClickListener(submitANL);
      cloudsender = new HttpService();
  }

  @Override
  public void onBackPressed() {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
  }

  private ArrayAdapter<String> constructSpinner(List<String> rangeset){
      ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(mycontext, android.R.layout.simple_spinner_item, rangeset);
      dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return dataAdapter;
  }

  private List<String> getNetworks(){
      List<String> networks = new ArrayList<>();
      // TODO: Fetch Networks GET, need to modify getallssid API to JSONArray

      URL url;
      HttpURLConnection urlConnection = null;
      try {

          url = new URL("http://34.201.21.219:8111/network/getallssid");

          urlConnection = (HttpURLConnection) url.openConnection();

          InputStream in = urlConnection.getInputStream();

          String resp = readStream(in);

          Log.i("GET Info",resp);
          JSONObject jsonObject = new JSONObject(resp);

          JSONArray array = jsonObject.getJSONArray("ssid");

          for (int i = 0; i < array.length(); i++) {
              String network = array.getJSONObject(i).getString("name");
              networks.add(network);
          }


      } catch (Exception e) {
          e.printStackTrace();
      } finally {
          if (urlConnection != null) {
              urlConnection.disconnect();
          }
      }

      networks.add("Highest Banwidth");
      networks.add("Lowest Latency");
      return networks;
  };

  private List<String> getApp(){
      Set<String> set = new HashSet<>();
      List<String> apps = new ArrayList<>();
      // TODO: Get Apps
      Cursor cursor = DatabaseHelper.getInstance(this).getAccess();
      cursor.moveToFirst();
      while (cursor.isAfterLast() == false)
      {
          int colUid = cursor.getColumnIndex("uid");
          int uid = (cursor.isNull(colUid) ? -1 : cursor.getInt(colUid));
          String appName = this.getPackageManager().getNameForUid(uid);
          set.add(appName);
          cursor.moveToNext();
      }
      apps.addAll(set);
      return apps;
  };

    public void getLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Context context = getApplicationContext();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

  private Button.OnClickListener submitLNL = new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            String url = "http://34.201.21.219:8111/event/setlocpref";
            String locname = locationRecord.getText().toString();
            String network = networks.getSelectedItem().toString();
            String currloc = String.valueOf(location.getLongitude())+","+String.valueOf(location.getLatitude());
            String time = String.valueOf(System.currentTimeMillis());
            String android_id= Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            // TODO: POST TO CLOUD
            Map<String, Object> temp = new HashMap<>();
            temp.put("user_id", "test@columbia.edu");
            temp.put("location_name", locname);
            temp.put("preference", network);
            temp.put("location", currloc);
            temp.put("device_id", android_id);
            temp.put("time", time);
            JSONObject submission = new JSONObject(temp);

            new PostTask().execute(new String[]{url, submission.toString()});

//            try {
//                Log.i("SendCloud",submission.toString());
//                cloudsender.POST(url, submission.toString());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
  };

  private Button.OnClickListener submitANL = new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            String url = "http://34.201.21.219:8111/event/setapppref";
            String network = networks2.getSelectedItem().toString();
            String app = appspin.getSelectedItem().toString();
            String currloc = String.valueOf(location.getLongitude())+","+String.valueOf(location.getLatitude());
            String time = String.valueOf(System.currentTimeMillis());
            String android_id= Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            // TODO: POST TO CLOUD
            Map<String, Object> temp = new HashMap<>();
            temp.put("user_id", "test@columbia.edu");
            temp.put("preference", network);
            temp.put("uid", app);
            temp.put("device_id", android_id);
            temp.put("location", currloc);
            temp.put("time", time);

            JSONObject submission = new JSONObject(temp);

            new PostTask().execute(new String[]{url, submission.toString()});

//            try {
//                Log.i("SendCloud",submission.toString());
//                cloudsender.POST(url, submission.toString());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
  };

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }
}