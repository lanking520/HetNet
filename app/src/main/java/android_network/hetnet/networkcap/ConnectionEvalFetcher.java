package android_network.hetnet.networkcap;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import android_network.hetnet.data.NetworkEvaluation;

import static android_network.hetnet.common.Constants.CONNECTION_EVAL_FETCHER;

/**
 * Created by lanking on 26/04/2017.
 */

public class ConnectionEvalFetcher extends IntentService {
    public ConnectionEvalFetcher() {
        super("ConnectionEvalFetcher");
    }

    private String bandwidth;
    private double latency;
    private String MAC_ADDR;
    private String sourceUrl = "http://download.thinkbroadband.com/10MB.zip";

    @Override
    public void onCreate() {
        Log.i("CEL", "Start Handele Connection Fetcher");
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("CEL", "Start Handele Connection Fetcher");
        NetworkEvaluation eval = new NetworkEvaluation();
        downLoadFileFromServer(sourceUrl);
        eval.setBandwidth(this.bandwidth);
        testNetworkLatency();
        eval.setLatency(this.latency);
        getMacId();
        eval.setMAC_ADDR(this.MAC_ADDR);
        EventBus.getDefault().post(new ConnectionResponseEvent(CONNECTION_EVAL_FETCHER, eval, Calendar.getInstance().getTime()));
    }

    public void getMacId() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        MAC_ADDR = wifiInfo.getBSSID();
    }

    private void downLoadFileFromServer(String sourceUrl) {
        Log.v("DEBUG", "sourceUrl: " + sourceUrl);

        InputStream urlInputStream = null;

        URLConnection urlConnection;
        try {
            long start = System.currentTimeMillis();

            //Form a new URL
            URL finalUrl = new URL(sourceUrl);

            urlConnection = finalUrl.openConnection();

            //Get the size of the (file) inputstream from server..
            int contentLength = urlConnection.getContentLength();

            Log.d("1URL", "Streaming from " + sourceUrl + "....");
            DataInputStream stream = new DataInputStream(finalUrl.openStream());

            Log.d("2FILE", "Buffering the received stream(size=" + contentLength + ") ...");
            byte[] buffer = new byte[contentLength];
            stream.readFully(buffer);
            stream.close();
            Log.d("3FILE", "Buffered successfully(Buffer.length=" + buffer.length + ")....");

            long end = System.currentTimeMillis();

            double bandwidth = 10.0 * 8 * 1000 / (end - start);

            Log.i("SPDTST", "bandwidth = " + bandwidth + " Mbps");
            this.bandwidth = String.valueOf(bandwidth);
        } catch (Exception e) {
            Log.e("9ERROR", "Failed to open urlConnection/Stream the connection(From catch block) & returning 'false'..");
            System.out.println("Exception: " + e);
            e.printStackTrace();
        } finally {
            try {
                Log.d("10URL", "Closing urlInputStream... ");
                if (urlInputStream != null) urlInputStream.close();

            } catch (Exception e) {
                Log.e("11ERROR", "Failed to close urlInputStream(From finally block)..");
            }
        }
    }


    private void testNetworkLatency() {
        try {
            URL url = new URL("http://www.google.com");

            long timeSum = 0;
            long beforeTime;
            long afterTime;

            int times = 3;

            double networkLatency;

            for (int i = 0; i < times; i++) {
                beforeTime = System.currentTimeMillis();
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                Object responseObject = urlConnection.getContent();
                afterTime = System.currentTimeMillis();

                timeSum += (afterTime - beforeTime);
            }

            networkLatency = timeSum / (times * 1.0);
            Log.d("CEL", Double.toString(networkLatency));
            this.latency = networkLatency;
        } catch (Exception e) {
            Log.d("CEL", "ERROR: Some Error comes out from testNetwork Latency");
            e.printStackTrace();
        }
    }

}
