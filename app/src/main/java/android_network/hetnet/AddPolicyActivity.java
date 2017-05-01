package android_network.hetnet;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
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

import java.util.ArrayList;
import java.util.List;

import android_network.hetnet.cloud.HttpService;

public class AddPolicyActivity extends Activity {

    EditText locationRecord;
    Spinner networks;
    Spinner networks2;
    Spinner appspin;
    Context mycontext;
    Button submitLN;
    Button submitAN;
    Location location;
    HttpService cloudsender;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_addpolicy_pg1);
      locationRecord = (EditText) findViewById(R.id.LocationRecorder);
      networks = (Spinner) findViewById(R.id.NetworkSpin);
      networks2 = (Spinner) findViewById(R.id.NetworkSpin2);
      appspin = (Spinner) findViewById(R.id.AppSpin);
      List<String> tempnet = getNetworks();
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
      // TODO: Fetch Networks GET
      networks.add("Highest Banwidth");
      networks.add("Lowest Latency");
      return networks;
  };

  private List<String> getApp(){
      List<String> app = new ArrayList<>();
      // TODO: Get Apps
      return app;
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
            String locname = locationRecord.getText().toString();
            String network = networks.getSelectedItem().toString();
            String currloc = String.valueOf(location.getLongitude())+","+String.valueOf(location.getLatitude());
            // TODO: POST TO CLOUD
        }
  };

  private Button.OnClickListener submitANL = new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            String network = networks2.getSelectedItem().toString();
            String app = appspin.getSelectedItem().toString();
            //TODO: POST TO CLOUD
        }
  };
}