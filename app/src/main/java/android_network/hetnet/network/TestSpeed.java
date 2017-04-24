package android_network.hetnet.network;

/**
 * Created by kaihe on 4/24/17.
 */

// Can be called by:
// TestSpeed testSpeed = new TestSpeed();
// testSpeed.startDownLoad(this, "http://download.thinkbroadband.com/10MB.zip", "10MB.zip");

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import android.content.Context;
import android.util.Log;

public class TestSpeed {

    //MAKE SURE YOU HAVE SET INTERNET PERMISSION ....

    public void startDownLoad(Context context, String sourceUrl,String destinationPath)
    {
        new DownLoadFileThread(context, sourceUrl, destinationPath).start();
    }

    private static class DownLoadFileThread extends Thread{

        Context context=null;
        String sourceUrl=null;
        String destinationPath=null;

        /*
         * Make sure sourceUrl ends with a file to download and not a folder!.
         *  Same with destinationPath.
         *  Ex:
         *
         *  sourceUrl="http://www.tempurl.com/image1.jpg"
         *  destinationPath="data/data/<package-name>/files/image1.jpg"
         */
        public DownLoadFileThread(Context context, String sourceUrl,String destinationPath)
        {
            this.context=context;
            this.sourceUrl=sourceUrl;
            this.destinationPath=destinationPath;
        }

        @Override
        public void run()
        {
            downLoadFileFromServer();
        }


        public boolean downLoadFileFromServer()
        {
            Log.v("DEBUG", "sourceUrl: "+sourceUrl);
            Log.v("DEBUG", "destinationPath: "+destinationPath);

            InputStream urlInputStream=null;

            URLConnection urlConnection ;

            try
            {

                long start = System.currentTimeMillis();

                //Form a new URL
                URL finalUrl =new URL(sourceUrl);

                urlConnection = finalUrl.openConnection();

                //Get the size of the (file) inputstream from server..
                int contentLength=urlConnection.getContentLength();

                Log.d("1URL","Streaming from "+sourceUrl+ "....");
                DataInputStream stream = new DataInputStream(finalUrl.openStream());

                Log.d("2FILE","Buffering the received stream(size="+contentLength+") ...");
                byte[] buffer = new byte[contentLength];
                stream.readFully(buffer);
                stream.close();
                Log.d("3FILE","Buffered successfully(Buffer.length="+buffer.length+")....");

                long end = System.currentTimeMillis();

                double bandwidth = 10.0 * 8 * 1000 / (end - start);

                Log.i("SPDTST", "bandwidth = " + bandwidth + " Mbps");

                if (buffer.length>0)
                {
                    try
                    {
                        Log.d("4FILE","Creating the new file..");
                        FileOutputStream fos = context.openFileOutput(destinationPath, Context.MODE_PRIVATE);
                        Log.d("5FILE","Writing from buffer to the new file..");
                        fos.write(buffer);
                        fos.flush();
                        fos.close();
                        Log.d("6.1FILE","Created the new file & returning 'true'..");
                        return true;
                    }
                    catch (Exception e)
                    {
                        Log.e("7ERROR", "Could not create new file(Path="+destinationPath+") ! & returning 'false'.......");
                        e.printStackTrace();
                        return false;
                                 /*Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();*/
                    }
                }
                else
                {
                    //Could not download the file...
                    Log.e("8ERROR", "Buffer size is zero ! & returning 'false'.......");
                    return false;
                }
            }
            catch (Exception e)
            {
                Log.e("9ERROR", "Failed to open urlConnection/Stream the connection(From catch block) & returning 'false'..");
                System.out.println("Exception: " + e);
                e.printStackTrace();
                return false;
            }
            finally
            {
                try
                {
                    Log.d("10URL", "Closing urlInputStream... ");
                    if (urlInputStream != null) urlInputStream.close();

                }
                catch (Exception e)
                {
                    Log.e("11ERROR", "Failed to close urlInputStream(From finally block)..");
                }
            }
        }
    }

}
