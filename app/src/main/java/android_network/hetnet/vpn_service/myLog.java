package android_network.hetnet.vpn_service;

/**
 * Created by kaihe on 3/1/17.
 */
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android_network.hetnet.R;

public class myLog extends AppCompatActivity  {
    private static final String TAG = "NetGuard.Log";

    private boolean running = false;
    private ListView lvLog;
    private AdapterLog adapter;
    private MenuItem menuSearch = null;

    private boolean live;
    private boolean resolve;
    private boolean organization;
    private InetAddress vpn4 = null;
    private InetAddress vpn6 = null;


    private DatabaseHelper.LogChangedListener listener = new DatabaseHelper.LogChangedListener() {
        @Override
        public void onChanged() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logging);
        running = true;


        getSupportActionBar().setTitle("Show Packet Log");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        lvLog = (ListView) findViewById(R.id.lvLog);


        // -- the adapter holds rows of data entries -> AdapterLog class
        adapter = new AdapterLog(this, DatabaseHelper.getInstance(this).getLog(true, true, true, true, true), true, true);
//        adapter.setFilterQueryProvider(new FilterQueryProvider() {
//            public Cursor runQuery(CharSequence constraint) {
//                return DatabaseHelper.getInstance(myLog.this).searchLog(constraint.toString());
//            }
//        });

        lvLog.setAdapter(adapter);


        live = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (live) {
            DatabaseHelper.getInstance(this).addLogChangedListener(listener);
            updateAdapter();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (live)
            DatabaseHelper.getInstance(this).removeLogChangedListener(listener);
    }

    @Override
    protected void onDestroy() {
        running = false;
        adapter = null;
        super.onDestroy();
    }



    private void updateAdapter() {
        if (adapter != null) {
            adapter.changeCursor(DatabaseHelper.getInstance(this).getLog(true, true, true, true, true));
            if (menuSearch != null && menuSearch.isActionViewExpanded()) {
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuSearch);
//                adapter.getFilter().filter(searchView.getQuery().toString());
            }
        }
    }



}

