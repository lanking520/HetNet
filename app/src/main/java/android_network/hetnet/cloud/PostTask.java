package android_network.hetnet.cloud;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by lanking on 03/05/2017.
 */

public class PostTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... datas) {
        String url = datas[0];
        String data = datas[1];
        HttpService sender = new HttpService();
        try {
            sender.POST(url, data);
            return "Success!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed!";
        }
    }

    protected void onPostExecute(String result) {
        if(result.equals("Success!")){
            Log.i("Async","Post Success!");
        }
        else{
            Log.i("Async","Post Failed!");
        }
    }

}
