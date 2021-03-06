package android_network.hetnet.vpn_service;

/**
 * Created by kaihe on 3/1/17.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.List;

import android_network.hetnet.R;

//import static android_network.hetnet.R.id.tvDPort;

public class AdapterTraffic extends CursorAdapter {
    private static String TAG = "NetGuard.Log";

    private int colID;
    private int colVersion;
    private int colProtocol;
    private int colDaddr;
    private int colDPort;
    private int colTime;
    private int colAllowed;
    private int colBlock;
    private int colSent;
    private int colReceived;
    private int colConnections;

    private int colorText;
    private int colorOn;
    private int colorOff;
    private int iconSize;
    private int colUid;



    public AdapterTraffic(Context context, Cursor cursor) {
        super(context, cursor, 0);
        colID = cursor.getColumnIndex("ID");
        colVersion = cursor.getColumnIndex("version");
        colProtocol = cursor.getColumnIndex("protocol");
        colDaddr = cursor.getColumnIndex("daddr");
        colDPort = cursor.getColumnIndex("dport");
        colTime = cursor.getColumnIndex("time");
        colAllowed = cursor.getColumnIndex("allowed");
        colBlock = cursor.getColumnIndex("block");
        colSent = cursor.getColumnIndex("sent");
        colReceived = cursor.getColumnIndex("received");
        colConnections = cursor.getColumnIndex("connections");
        colUid = cursor.getColumnIndex("uid");


//        TypedValue tv = new TypedValue();
//        context.getTheme().resolveAttribute(R.attr.colorOn, tv, true);
//        colorOn = tv.data;
//        context.getTheme().resolveAttribute(R.attr.colorOff, tv, true);
//        colorOff = tv.data;

        iconSize = Util.dips2pixels(24, context);


    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.trafficlog, parent, false);
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        // Get values
        final long id = cursor.getLong(colID);
        final int version = cursor.getInt(colVersion);
        final int protocol = cursor.getInt(colProtocol);
        final String daddr = cursor.getString(colDaddr);
        final int dport = cursor.getInt(colDPort);
        long time = cursor.getLong(colTime);
        int allowed = cursor.getInt(colAllowed);
        int block = cursor.getInt(colBlock);
        long sent = cursor.isNull(colSent) ? -1 : cursor.getLong(colSent);
        long received = cursor.isNull(colReceived) ? -1 : cursor.getLong(colReceived);
        int connections = cursor.isNull(colConnections) ? -1 : cursor.getInt(colConnections);
        int uid = (cursor.isNull(colUid) ? -1 : cursor.getInt(colUid));

        // Get views
        TextView myTime = (TextView) view.findViewById(R.id.myTime);
//        ImageView ivBlock = (ImageView) view.findViewById(R.id.ivBlock);
        final TextView myDest = (TextView) view.findViewById(R.id.myDest);
//        LinearLayout llTraffic = (LinearLayout) view.findViewById(R.id.llTraffic);
//        TextView tvConnections = (TextView) view.findViewById(R.id.tvConnections);
        TextView myTraffic = (TextView) view.findViewById(R.id.myTraffic);

        ImageView ivIcon = (ImageView) view.findViewById(R.id.myIcon);
//

        // Show time
        myTime.setText(new SimpleDateFormat("HH:mm:ss").format(time));


        myDest.setText(
                Util.getProtocolName(protocol, version, true) +
                        " " + daddr + (dport > 0 ? "/" + dport : ""));
//        myDest.setText(daddr + (dport > 0 ? "/" + dport : ""));


        // Application icon
        ApplicationInfo info = null;
        PackageManager pm = context.getPackageManager();
        String[] pkg = pm.getPackagesForUid(uid);
        if (pkg != null && pkg.length > 0)
            try {
                info = pm.getApplicationInfo(pkg[0], 0);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        if (info == null)
            ivIcon.setImageDrawable(null);
        else if (info.icon == 0)
            Picasso.with(context).load(android.R.drawable.sym_def_app_icon).into(ivIcon);
        else {
            Uri uri = Uri.parse("android.resource://" + info.packageName + "/" + info.icon);
            Picasso.with(context).load(uri).resize(iconSize, iconSize).into(ivIcon);
        }

        if (sent > 1024 * 1204 * 1024L || received > 1024 * 1024 * 1024L)
            myTraffic.setText(context.getString(R.string.msg_gb,
                    (sent > 0 ? sent / (1024 * 1024 * 1024f) : 0),
                    (received > 0 ? received / (1024 * 1024 * 1024f) : 0)));
        else if (sent > 1204 * 1024L || received > 1024 * 1024L)
            myTraffic.setText(context.getString(R.string.msg_mb,
                    (sent > 0 ? sent / (1024 * 1024f) : 0),
                    (received > 0 ? received / (1024 * 1024f) : 0)));
        else
            myTraffic.setText(context.getString(R.string.msg_kb,
                    (sent > 0 ? sent / 1024f : 0),
                    (received > 0 ? received / 1024f : 0)));

    }


}
