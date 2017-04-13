package android_network.hetnet.vpn_service;

/**
 * Created by kaihe on 3/1/17.
 */
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import android_network.hetnet.R;

public class ActivityTraffic extends AppCompatActivity {
    private static final String TAG = "NetGuard.Log";

    private ListView lvLog;
    private AdapterTraffic adapter;
    private MenuItem menuSearch = null;

    private boolean live;



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


        getSupportActionBar().setTitle("Show Traffic Log");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        lvLog = (ListView) findViewById(R.id.lvLog);


        // -- the adapter holds rows of data entries -> AdapterLog class
        adapter = new AdapterTraffic(this, DatabaseHelper.getInstance(this).getAccess());
//        adapter.setFilterQueryProvider(new FilterQueryProvider() {
//            public Cursor runQuery(CharSequence constraint) {
//                return DatabaseHelper.getInstance(ActivityTraffic.this).searchLog(constraint.toString());
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
        adapter = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mylogging, menu);


        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_traffic_clear:
                new AsyncTask<Object, Object, Object>() {
                    @Override
                    protected Object doInBackground(Object... objects) {
                        DatabaseHelper.getInstance(ActivityTraffic.this).clearAccess();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object result) {
                        updateAdapter();
                    }
                }.execute();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void updateAdapter() {
        if (adapter != null) {
            adapter.changeCursor(DatabaseHelper.getInstance(this).getAccess());
//            if (menuSearch != null && menuSearch.isActionViewExpanded()) {
//                SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuSearch);
////                adapter.getFilter().filter(searchView.getQuery().toString());
//            }
        }
    }



}

