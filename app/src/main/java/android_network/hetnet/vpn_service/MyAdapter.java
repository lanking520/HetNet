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
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.List;

import android_network.hetnet.R;

public class MyAdapter extends CursorAdapter {
    private static String TAG = "NetGuard.Log";

    private boolean resolve;
    private boolean organization;
    private int colID;
    private int colTime;
    private int colVersion;
    private int colProtocol;
    private int colFlags;
    private int colSAddr;
    private int colSPort;
    private int colDAddr;
    private int colDPort;
    private int colDName;
    private int colUid;
    private int colData;
    private int colAllowed;
    private int colConnection;
    private int colInteractive;
    private int colorOn;
    private int colorOff;
    private int iconSize;
    private InetAddress dns1 = null;
    private InetAddress dns2 = null;
    private InetAddress vpn4 = null;
    private InetAddress vpn6 = null;

    public MyAdapter(Context context, Cursor cursor, boolean resolve, boolean organization) {
        super(context, cursor, 0);
        this.resolve = resolve;
        this.organization = organization;
        colID = cursor.getColumnIndex("ID");
        colTime = cursor.getColumnIndex("time");
        colVersion = cursor.getColumnIndex("version");
        colProtocol = cursor.getColumnIndex("protocol");
        colFlags = cursor.getColumnIndex("flags");
        colSAddr = cursor.getColumnIndex("saddr");
        colSPort = cursor.getColumnIndex("sport");
        colDAddr = cursor.getColumnIndex("daddr");
        colDPort = cursor.getColumnIndex("dport");
        colDName = cursor.getColumnIndex("dname");
        colUid = cursor.getColumnIndex("uid");
        colData = cursor.getColumnIndex("data");
        colAllowed = cursor.getColumnIndex("allowed");
        colConnection = cursor.getColumnIndex("connection");
        colInteractive = cursor.getColumnIndex("interactive");

        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorOn, tv, true);
        colorOn = tv.data;
        context.getTheme().resolveAttribute(R.attr.colorOff, tv, true);
        colorOff = tv.data;

        iconSize = Util.dips2pixels(24, context);

        //Log.e(TAG, "-------data is: " + cursor.getString(colData));

        try {
            List<InetAddress> lstDns = ServiceSinkhole.getDns(context);
            dns1 = (lstDns.size() > 0 ? lstDns.get(0) : null);
            dns2 = (lstDns.size() > 1 ? lstDns.get(1) : null);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            vpn4 = InetAddress.getByName(prefs.getString("vpn4", "10.1.10.1"));
            vpn6 = InetAddress.getByName(prefs.getString("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"));
        } catch (UnknownHostException ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
    }

    public void setResolve(boolean resolve) {
        this.resolve = resolve;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.mylog, parent, false);
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        // Get values
        final long id = cursor.getLong(colID);
        long time = cursor.getLong(colTime);
        int version = (cursor.isNull(colVersion) ? -1 : cursor.getInt(colVersion));
        int protocol = (cursor.isNull(colProtocol) ? -1 : cursor.getInt(colProtocol));
        String flags = cursor.getString(colFlags);
        String saddr = cursor.getString(colSAddr);
        int sport = (cursor.isNull(colSPort) ? -1 : cursor.getInt(colSPort));
        String daddr = cursor.getString(colDAddr);
        int dport = (cursor.isNull(colDPort) ? -1 : cursor.getInt(colDPort));
        String dname = (cursor.isNull(colDName) ? null : cursor.getString(colDName));
        int uid = (cursor.isNull(colUid) ? -1 : cursor.getInt(colUid));


        // Get views
        TextView tvTime = (TextView) view.findViewById(R.id.myTime);
        TextView tvProtocol = (TextView) view.findViewById(R.id.myProtocol);
        TextView tvSAddr = (TextView) view.findViewById(R.id.mySAddr);
        TextView tvSPort = (TextView) view.findViewById(R.id.mySPort);
        final TextView tvDaddr = (TextView) view.findViewById(R.id.myDAddr);
        TextView tvDPort = (TextView) view.findViewById(R.id.myDPort);
        ImageView ivIcon = (ImageView) view.findViewById(R.id.myIcon);

        // Show time
        tvTime.setText(new SimpleDateFormat("HH:mm:ss").format(time));


        // Show protocol name
        tvProtocol.setText(Util.getProtocolName(protocol, version, false));





        // Show source and destination port
        if (protocol == 6 || protocol == 17) {
            tvSPort.setText(sport < 0 ? "" : Integer.toString(sport));
            tvDPort.setText(dport < 0 ? "" : Integer.toString(dport));
        } else {
            tvSPort.setText(sport < 0 ? "" : Integer.toString(sport));
            tvDPort.setText(dport < 0 ? "" : Integer.toString(dport));
        }

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



        // Show source address
        tvSAddr.setText(saddr);

        // Show destination address
        tvDaddr.setText(daddr);


    }


}
