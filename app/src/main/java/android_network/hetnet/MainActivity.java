package android_network.hetnet;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import android_network.hetnet.cloud.AppDataService;
import android_network.hetnet.common.trigger_events.TriggerEvent;
import android_network.hetnet.common.trigger_events.UITriggerEvent;
import android_network.hetnet.data.PolicyEngineData;
import android_network.hetnet.data.PolicyVector;
import android_network.hetnet.policy_engine.PolicyEngine;
import android_network.hetnet.ui.TabFragment.OnFragmentInteractionListener;
import android_network.hetnet.ui.TabFragment.TabFragment;
import android_network.hetnet.vpn_service.ActivitySettings;
import android_network.hetnet.vpn_service.ActivityStats;
import android_network.hetnet.vpn_service.AdapterRule;
import android_network.hetnet.vpn_service.DatabaseHelper;
import android_network.hetnet.vpn_service.Receiver;
import android_network.hetnet.vpn_service.Rule;
import android_network.hetnet.vpn_service.ServiceSinkhole;
import android_network.hetnet.vpn_service.ActivityTraffic;
import android_network.hetnet.vpn_service.Util;

import static android_network.hetnet.common.Constants.LOCATION_EVENT_TRACKER;
import static android_network.hetnet.common.Constants.NETWORK_EVENT_TRACKER;
import static android_network.hetnet.common.Constants.POLICY_ENGINE;
import static android_network.hetnet.common.Constants.SYSTEM_EVENT_TRACKER;


public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener, SharedPreferences.OnSharedPreferenceChangeListener {
  private static final String TAG = "MainActivity";

  private static final int REQUEST_READ_PHONE_STATE = 100;
  private static final int REQUEST_ACCESS_COARSE_LOCATION = 101;
  private static final int REQUEST_ACCESS_NETWORK_STATE = 102;

  private boolean running = false;
  private ImageView ivIcon;
  private ImageView ivQueue;
  private SwitchCompat swEnabled;
  private ImageView ivMetered;
  private SwipeRefreshLayout swipeRefresh;
  private AdapterRule adapter = null;
  private MenuItem menuSearch = null;
  private AlertDialog dialogFirst = null;
  private AlertDialog dialogVpn = null;
  private AlertDialog dialogDoze = null;
  private AlertDialog dialogLegend = null;
  private AlertDialog dialogAbout = null;

//  private IAB iab = null;

  private static final int REQUEST_VPN = 1;
  private static final int REQUEST_INVITE = 2;
  private static final int REQUEST_LOGCAT = 3;
  public static final int REQUEST_ROAMING = 4;

  private static final int MIN_SDK = Build.VERSION_CODES.ICE_CREAM_SANDWICH;

  public static final String ACTION_RULES_CHANGED = "eu.faircode.netguard.ACTION_RULES_CHANGED";
  public static final String ACTION_QUEUE_CHANGED = "eu.faircode.netguard.ACTION_QUEUE_CHANGED";
  public static final String EXTRA_REFRESH = "Refresh";
  public static final String EXTRA_SEARCH = "Search";
  public static final String EXTRA_APPROVE = "Approve";
  public static final String EXTRA_LOGCAT = "Logcat";
  public static final String EXTRA_CONNECTED = "Connected";
  public static final String EXTRA_METERED = "Metered";
  public static final String EXTRA_SIZE = "Size";

  private String m_event_log;

  //UI elements
  private TextView m_eventList;
  private TextView m_policyRuleVector;
  private TextView m_currentStateVector;

  FragmentManager fragmentManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "Create version=" + Util.getSelfVersionName(this) + "/" + Util.getSelfVersionCode(this));
    Util.logExtras(getIntent());

    if (Build.VERSION.SDK_INT < MIN_SDK) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.android);
      return;
    }

    Util.setTheme(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    m_eventList = (TextView) findViewById(R.id.event_list);
    m_policyRuleVector = (TextView) findViewById(R.id.policy_rule_vector);
    m_currentStateVector = (TextView) findViewById(R.id.current_state_vector);

    fragmentManager = getSupportFragmentManager();
    FragmentTransaction firstTransaction = fragmentManager.beginTransaction();
    firstTransaction.replace(R.id.containerView, new TabFragment()).commit();

    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
    }

    //register to event bus
    EventBus.getDefault().register(this);

    Intent policyEngineService = new Intent(this, PolicyEngine.class);
    this.startService(policyEngineService);

    //###################################### Policy Engine End

    Intent AppDataTransService = new Intent(this, AppDataService.class);
    PendingIntent pintent = PendingIntent.getService(this, 0, AppDataTransService, 0);
    AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 10*1000, pintent);

    //###################################### Data Transmission End
    running = true;

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    boolean enabled = prefs.getBoolean("enabled", false);
    boolean initialized = prefs.getBoolean("initialized", false);

    // Upgrade
    Receiver.upgrade(initialized, this);

    if (!getIntent().hasExtra(EXTRA_APPROVE)) {
      if (enabled)
        ServiceSinkhole.start("UI", this);
      else
        ServiceSinkhole.stop("UI", this);
    }

    // Action bar
    final View actionView = getLayoutInflater().inflate(R.layout.actionmain, null, false);
//    ivIcon = (ImageView) actionView.findViewById(R.id.ivIcon);
    ivQueue = (ImageView) actionView.findViewById(R.id.ivQueue);
//    swEnabled = (SwitchCompat) actionView.findViewById(R.id.swEnabled);
    ivMetered = (ImageView) actionView.findViewById(R.id.ivMetered);

    // Icon
//    ivIcon.setOnLongClickListener(new View.OnLongClickListener() {
//      @Override
//      public boolean onLongClick(View view) {
//        menu_about();
//        return true;
//      }
//    });

    // Title
    getSupportActionBar().setTitle(null);

    // Netguard is busy
    ivQueue.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        int location[] = new int[2];
        actionView.getLocationOnScreen(location);
        Toast toast = Toast.makeText(MainActivity.this, R.string.msg_queue, Toast.LENGTH_LONG);
        toast.setGravity(
                Gravity.TOP | Gravity.LEFT,
                location[0] + ivQueue.getLeft(),
                Math.round(location[1] + ivQueue.getBottom() - toast.getView().getPaddingTop()));
        toast.show();
        return true;
      }
    });

    // On/off switch
    try {
      final Intent prepare = VpnService.prepare(MainActivity.this);
      if (prepare == null) {
        Log.i(TAG, "Prepare done");
        onActivityResult(REQUEST_VPN, RESULT_OK, null);
      } else {
        // Show dialog
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View view = inflater.inflate(R.layout.vpn, null, false);
        if (running) {
          Log.i(TAG, "Start intent=" + prepare);
          try {
            // com.android.vpndialogs.ConfirmDialog required
            startActivityForResult(prepare, REQUEST_VPN);
          } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            onActivityResult(REQUEST_VPN, RESULT_CANCELED, null);
            prefs.edit().putBoolean("enabled", false).apply();
          }
        }
//        dialogVpn = new AlertDialog.Builder(MainActivity.this)
//                .setView(view)
//                .setCancelable(false)
//                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                  @Override
//                  public void onClick(DialogInterface dialog, int which) {
//                    if (running) {
//                      Log.i(TAG, "Start intent=" + prepare);
//                      try {
//                        // com.android.vpndialogs.ConfirmDialog required
//                        startActivityForResult(prepare, REQUEST_VPN);
//                      } catch (Throwable ex) {
//                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
//                        onActivityResult(REQUEST_VPN, RESULT_CANCELED, null);
//                        prefs.edit().putBoolean("enabled", false).apply();
//                      }
//                    }
//                  }
//                })
//                .setOnDismissListener(new DialogInterface.OnDismissListener() {
//                  @Override
//                  public void onDismiss(DialogInterface dialogInterface) {
//                    dialogVpn = null;
//                  }
//                })
//                .create();
//        dialogVpn.show();
      }
    } catch (Throwable ex) {
      // Prepare failed
      Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
      prefs.edit().putBoolean("enabled", false).apply();
    }


    if (enabled)
      checkDoze();

    // Network is metered
    ivMetered.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        int location[] = new int[2];
        actionView.getLocationOnScreen(location);
        Toast toast = Toast.makeText(MainActivity.this, R.string.msg_metered, Toast.LENGTH_LONG);
        toast.setGravity(
                Gravity.TOP | Gravity.LEFT,
                location[0] + ivMetered.getLeft(),
                Math.round(location[1] + ivMetered.getBottom() - toast.getView().getPaddingTop()));
        toast.show();
        return true;
      }
    });

    getSupportActionBar().setDisplayShowCustomEnabled(true);
    getSupportActionBar().setCustomView(actionView);

    // Disabled warning
//    TextView tvDisabled = (TextView) findViewById(R.id.tvDisabled);
//    tvDisabled.setVisibility(enabled ? View.GONE : View.VISIBLE);

//        // Application list
//        RecyclerView rvApplication = (RecyclerView) findViewById(R.id.rvApplication);
//        rvApplication.setHasFixedSize(true);
//        rvApplication.setLayoutManager(new LinearLayoutManager(this));
//        adapter = new AdapterRule(this);
//        rvApplication.setAdapter(adapter);

    // Swipe to refresh
//    TypedValue tv = new TypedValue();
//    getTheme().resolveAttribute(R.attr.colorPrimary, tv, true);
//    swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
//    swipeRefresh.setColorSchemeColors(Color.WHITE, Color.WHITE, Color.WHITE);
//    swipeRefresh.setProgressBackgroundColorSchemeColor(tv.data);
//    swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//      @Override
//      public void onRefresh() {
//        Rule.clearCache(MainActivity.this);
//        ServiceSinkhole.reload("pull", MainActivity.this);
//        updateApplicationList(null);
//      }
//    });

    // Hint usage
//        final LinearLayout llUsage = (LinearLayout) findViewById(R.id.llUsage);
//        Button btnUsage = (Button) findViewById(R.id.btnUsage);
//        boolean hintUsage = prefs.getBoolean("hint_usage", true);
//        llUsage.setVisibility(hintUsage ? View.VISIBLE : View.GONE);
//        btnUsage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                prefs.edit().putBoolean("hint_usage", false).apply();
//                llUsage.setVisibility(View.GONE);
//                showHints();
//            }
//        });
//        showHints();

    // Listen for preference changes
    prefs.registerOnSharedPreferenceChangeListener(this);

    prefs.edit().putBoolean("manage_system", true).apply();

    prefs.edit().putBoolean("log_app", true).apply();

    prefs.edit().putBoolean("filter", true).apply();

    // Listen for rule set changes
    IntentFilter ifr = new IntentFilter(ACTION_RULES_CHANGED);
    LocalBroadcastManager.getInstance(this).registerReceiver(onRulesChanged, ifr);

    // Listen for queue changes
    IntentFilter ifq = new IntentFilter(ACTION_QUEUE_CHANGED);
    LocalBroadcastManager.getInstance(this).registerReceiver(onQueueChanged, ifq);

    // Listen for added/removed applications
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
    intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
    intentFilter.addDataScheme("package");
    registerReceiver(packageChangedReceiver, intentFilter);

    // First use
//        boolean admob = prefs.getBoolean("admob", false);
//        if (!initialized || !admob) {
//            // Create view
//            LayoutInflater inflater = LayoutInflater.from(this);
//            View view = inflater.inflate(R.layout.first, null, false);
//
//            TextView tvFirst = (TextView) view.findViewById(R.id.tvFirst);
//            TextView tvAdmob = (TextView) view.findViewById(R.id.tvAdmob);
//            tvFirst.setMovementMethod(LinkMovementMethod.getInstance());
//            tvAdmob.setMovementMethod(LinkMovementMethod.getInstance());
//
//            // Show dialog
//            dialogFirst = new AlertDialog.Builder(this)
//                    .setView(view)
//                    .setCancelable(false)
//                    .setPositiveButton(R.string.app_agree, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            if (running) {
//                                prefs.edit().putBoolean("initialized", true).apply();
//                                prefs.edit().putBoolean("admob", true).apply();
//                            }
//                        }
//                    })
//                    .setNegativeButton(R.string.app_disagree, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            if (running)
//                                finish();
//                        }
//                    })
//                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
//                        @Override
//                        public void onDismiss(DialogInterface dialogInterface) {
//                            dialogFirst = null;
//                        }
//                    })
//                    .create();
//            dialogFirst.show();
//        }

    // Fill application list
    updateApplicationList(getIntent().getStringExtra(EXTRA_SEARCH));

    // Update IAB SKUs
//        try {
//            iab = new IAB(new IAB.Delegate() {
//                @Override
//                public void onReady(IAB iab) {
//                    try {
//                        iab.updatePurchases();
//
//                        if (!IAB.isPurchased(ActivityPro.SKU_LOG, MainActivity.this))
//                            prefs.edit().putBoolean("log", false).apply();
//                        if (!IAB.isPurchased(ActivityPro.SKU_THEME, MainActivity.this)) {
//                            if (!"teal".equals(prefs.getString("theme", "teal")))
//                                prefs.edit().putString("theme", "teal").apply();
//                        }
//                        if (!IAB.isPurchased(ActivityPro.SKU_NOTIFY, MainActivity.this))
//                            prefs.edit().putBoolean("install", false).apply();
//                        if (!IAB.isPurchased(ActivityPro.SKU_SPEED, MainActivity.this))
//                            prefs.edit().putBoolean("show_stats", false).apply();
//                    } catch (Throwable ex) {
//                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
//                    } finally {
//                        iab.unbind();
//                    }
//                }
//            }, this);
//            iab.bind();
//        } catch (Throwable ex) {
//            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
//        }
//
//        // Initialize ads
//        initAds();

    // Handle intent
    checkExtras(getIntent());

  }



  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case REQUEST_READ_PHONE_STATE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

          if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
          }
        }
      }
      case REQUEST_ACCESS_COARSE_LOCATION: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);

          if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, REQUEST_ACCESS_NETWORK_STATE);
          }
        }
      }
      case REQUEST_ROAMING: {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
          ServiceSinkhole.reload("permission granted", this);
      }
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onMessageEvent(TriggerEvent event) {
    m_event_log += (event.toString() + "\t");
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onMessageEvent(UITriggerEvent event) {
    //TODO:change this to log policy vector
    switch (event.getEventOriginator()) {
      case NETWORK_EVENT_TRACKER:
      case LOCATION_EVENT_TRACKER:
        m_eventList.setText(event.getEvent() + " event received from " + event.getEventOriginator() + " at " + event.getTimeOfEvent());
        break;
      case POLICY_ENGINE:
        PolicyVector ruleVector = ((PolicyEngineData) (event.getEvent())).getRuleVector();
        PolicyVector currentStateVector = ((PolicyEngineData) (event.getEvent())).getCurrentStateVector();
        String ruleVectorString = getRuleVectorToString(ruleVector);
        String currentStateVectorString = getCurrentVectorToString(currentStateVector);

        m_policyRuleVector.setText(ruleVectorString);
        m_currentStateVector.setText(currentStateVectorString);
        break;
      case SYSTEM_EVENT_TRACKER:
        NotificationCompat.Builder mBuilder =
          new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_icon)
            .setContentTitle("HetNet")
            .setContentText(event.getEvent().toString());

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
        break;
      default:
        Log.e(TAG, "Wrong event from: " + event.getEventOriginator());
    }
  }

  private String getRuleVectorToString(PolicyVector data) {
    StringBuilder builder_rule = new StringBuilder();
    builder_rule.append("Policy Rule Vector: <");
    builder_rule.append(data.getApplicationID()).append(" , ");
    builder_rule.append(data.getApplicationType()).append(" , ");
    builder_rule.append(data.getLatitude()).append(" , ");
    builder_rule.append(data.getLongitude());
    builder_rule.append(">");

    return String.valueOf(builder_rule);
  }

  private String getCurrentVectorToString(PolicyVector data) {
    StringBuilder builder_current = new StringBuilder();
    builder_current.append("Current State Vector: <");
    builder_current.append(data.getApplicationID()).append(" , ");
    builder_current.append(data.getApplicationType()).append(" , ");
    builder_current.append(data.getLatitude()).append(" , ");
    builder_current.append(data.getLongitude());
    builder_current.append(">");

    return String.valueOf(builder_current);
  }


  @Override
  public void onFragmentInteraction(Uri uri) {
  }

  public void showAddPolicyPage(View view) {
    Intent intent = new Intent(this, AddPolicyActivity.class);
    startActivity(intent);
  }

  @Override
  public void onBackPressed() {
    moveTaskToBack(true);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Log.i(TAG, "New intent");
    Util.logExtras(intent);
    super.onNewIntent(intent);

    setIntent(intent);

    if (Build.VERSION.SDK_INT >= MIN_SDK) {
      if (intent.hasExtra(EXTRA_REFRESH))
        updateApplicationList(intent.getStringExtra(EXTRA_SEARCH));
      else
        updateSearch(intent.getStringExtra(EXTRA_SEARCH));
      checkExtras(intent);
    }
  }

  @Override
  protected void onResume() {
    Log.i(TAG, "Resume");

    DatabaseHelper.getInstance(this).addAccessChangedListener(accessChangedListener);
    if (adapter != null)
      adapter.notifyDataSetChanged();

    // Ads
//        if (!IAB.isPurchasedAny(this) && Util.hasPlayServices(this))
//            enableAds();
//        else
//            disableAds();

    super.onResume();
  }

  @Override
  protected void onPause() {
    Log.i(TAG, "Pause");
    super.onPause();

    DatabaseHelper.getInstance(this).removeAccessChangedListener(accessChangedListener);

//        disableAds();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    Log.i(TAG, "Config");
    super.onConfigurationChanged(newConfig);

//        disableAds();
//        if (!IAB.isPurchasedAny(this) && Util.hasPlayServices(this))
//            enableAds();
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "Destroy");

    if (Build.VERSION.SDK_INT < MIN_SDK) {
      super.onDestroy();
      return;
    }

    running = false;
    adapter = null;

    PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

    LocalBroadcastManager.getInstance(this).unregisterReceiver(onRulesChanged);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(onQueueChanged);
    unregisterReceiver(packageChangedReceiver);

    if (dialogFirst != null) {
      dialogFirst.dismiss();
      dialogFirst = null;
    }
    if (dialogVpn != null) {
      dialogVpn.dismiss();
      dialogVpn = null;
    }
    if (dialogDoze != null) {
      dialogDoze.dismiss();
      dialogDoze = null;
    }
    if (dialogLegend != null) {
      dialogLegend.dismiss();
      dialogLegend = null;
    }
    if (dialogAbout != null) {
      dialogAbout.dismiss();
      dialogAbout = null;
    }

//        if (iab != null) {
//            iab.unbind();
//            iab = null;
//        }

    super.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
//    Log.i(TAG, "onActivityResult request=" + requestCode + " result=" + requestCode + " ok=" + (resultCode == RESULT_OK));
    Util.logExtras(data);

    if (requestCode == REQUEST_VPN) {
      // Handle VPN approval
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      prefs.edit().putBoolean("enabled", resultCode == RESULT_OK).apply();
      if (resultCode == RESULT_OK) {
        ServiceSinkhole.start("prepared", this);
        checkDoze();
      } else if (resultCode == RESULT_CANCELED)
        Toast.makeText(this, R.string.msg_vpn_cancelled, Toast.LENGTH_LONG).show();

    } else if (requestCode == REQUEST_INVITE) {
      // Do nothing

    } else if (requestCode == REQUEST_LOGCAT) {
      // Send logcat by e-mail
      if (resultCode == RESULT_OK) {
        Uri target = data.getData();
        if (data.hasExtra("org.openintents.extra.DIR_PATH"))
          target = Uri.parse(target + "/logcat.txt");
        Log.i(TAG, "Export URI=" + target);
        Util.sendLogcat(target, this);
      }

    } else {
      Log.w(TAG, "Unknown activity result request=" + requestCode);
      super.onActivityResult(requestCode, resultCode, data);
    }
  }


  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String name) {
    Log.i(TAG, "Preference " + name + "=" + prefs.getAll().get(name));
    if ("enabled".equals(name)) {
      // Get enabled
      boolean enabled = prefs.getBoolean(name, false);

      // Display disabled warning
//      TextView tvDisabled = (TextView) findViewById(R.id.tvDisabled);
//      tvDisabled.setVisibility(enabled ? View.GONE : View.VISIBLE);

      // Check switch state
//      SwitchCompat swEnabled = (SwitchCompat) getSupportActionBar().getCustomView().findViewById(R.id.swEnabled);
//      if (swEnabled.isChecked() != enabled)
//        swEnabled.setChecked(enabled);

    } else if ("whitelist_wifi".equals(name) ||
            "screen_on".equals(name) ||
            "screen_wifi".equals(name) ||
            "whitelist_other".equals(name) ||
            "screen_other".equals(name) ||
            "whitelist_roaming".equals(name) ||
            "show_user".equals(name) ||
            "show_system".equals(name) ||
            "show_nointernet".equals(name) ||
            "show_disabled".equals(name) ||
            "sort".equals(name) ||
            "imported".equals(name)) {
      updateApplicationList(null);

      final LinearLayout llWhitelist = (LinearLayout) findViewById(R.id.llWhitelist);
      boolean screen_on = prefs.getBoolean("screen_on", true);
      boolean whitelist_wifi = prefs.getBoolean("whitelist_wifi", false);
      boolean whitelist_other = prefs.getBoolean("whitelist_other", false);
      boolean hintWhitelist = prefs.getBoolean("hint_whitelist", true);
//      llWhitelist.setVisibility(!(whitelist_wifi || whitelist_other) && screen_on && hintWhitelist ? View.VISIBLE : View.GONE);

    } else if ("manage_system".equals(name)) {
      invalidateOptionsMenu();
      updateApplicationList(null);

      LinearLayout llSystem = (LinearLayout) findViewById(R.id.llSystem);
      boolean system = prefs.getBoolean("manage_system", true);
      boolean hint = prefs.getBoolean("hint_system", true);
//      llSystem.setVisibility(!system && hint ? View.VISIBLE : View.GONE);

    } else if ("theme".equals(name) || "dark_theme".equals(name))
      recreate();
  }

  private DatabaseHelper.AccessChangedListener accessChangedListener = new DatabaseHelper.AccessChangedListener() {
    @Override
    public void onChanged() {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (adapter != null && adapter.isLive())
            adapter.notifyDataSetChanged();
        }
      });
    }
  };

  private BroadcastReceiver onRulesChanged = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i(TAG, "Received " + intent);
      Util.logExtras(intent);

      if (adapter != null)
        if (intent.hasExtra(EXTRA_CONNECTED) && intent.hasExtra(EXTRA_METERED)) {
          ivIcon.setImageResource(Util.isNetworkActive(MainActivity.this)
                  ? R.drawable.ic_security_white_24dp
                  : R.drawable.ic_security_white_24dp_60);
          if (intent.getBooleanExtra(EXTRA_CONNECTED, false)) {
            if (intent.getBooleanExtra(EXTRA_METERED, false))
              adapter.setMobileActive();
            else
              adapter.setWifiActive();
//            ivMetered.setVisibility(Util.isMeteredNetwork(MainActivity.this) ? View.VISIBLE : View.INVISIBLE);
          } else {
            adapter.setDisconnected();
//            ivMetered.setVisibility(View.INVISIBLE);
          }
        } else
          updateApplicationList(null);
    }
  };

  private BroadcastReceiver onQueueChanged = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i(TAG, "Received " + intent);
      Util.logExtras(intent);
      int size = intent.getIntExtra(EXTRA_SIZE, -1);
//      ivIcon.setVisibility(size == 0 ? View.VISIBLE : View.GONE);
//      ivQueue.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
    }
  };

  private BroadcastReceiver packageChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i(TAG, "Received " + intent);
      Util.logExtras(intent);
      updateApplicationList(null);
    }
  };

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {


    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);

    return true;
  }

  private void markPro(MenuItem menu, String sku) {
    if (true) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      boolean dark = prefs.getBoolean("dark_theme", false);
      SpannableStringBuilder ssb = new SpannableStringBuilder("  " + menu.getTitle());
      ssb.setSpan(new ImageSpan(this, dark ? R.drawable.ic_shopping_cart_white_24dp : R.drawable.ic_shopping_cart_black_24dp), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      menu.setTitle(ssb);
    }
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Log.i(TAG, "Menu=" + item.getTitle());

    // Handle item selection
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    switch (item.getItemId()) {

      case R.id.menu_traffic:
        startActivity(new Intent(this, ActivityTraffic.class));
        return true;

//      case R.id.menu_settings:
//        startActivity(new Intent(this, ActivitySettings.class));
//        return true;

      case R.id.menu_statistics:
        startActivity(new Intent(this, ActivityStats.class));
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

//  private void showHints() {
//    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//    boolean hintUsage = prefs.getBoolean("hint_usage", true);
//
//    // Hint white listing
//    final LinearLayout llWhitelist = (LinearLayout) findViewById(R.id.llWhitelist);
//    Button btnWhitelist = (Button) findViewById(R.id.btnWhitelist);
//    boolean whitelist_wifi = prefs.getBoolean("whitelist_wifi", false);
//    boolean whitelist_other = prefs.getBoolean("whitelist_other", false);
//    boolean hintWhitelist = prefs.getBoolean("hint_whitelist", true);
////    llWhitelist.setVisibility(!(whitelist_wifi || whitelist_other) && hintWhitelist && !hintUsage ? View.VISIBLE : View.GONE);
//    btnWhitelist.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View view) {
//        prefs.edit().putBoolean("hint_whitelist", false).apply();
////        llWhitelist.setVisibility(View.GONE);
//      }
//    });
//
//    // Hint push messages
//    final LinearLayout llPush = (LinearLayout) findViewById(R.id.llPush);
//    Button btnPush = (Button) findViewById(R.id.btnPush);
//    boolean hintPush = prefs.getBoolean("hint_push", true);
////    llPush.setVisibility(hintPush && !hintUsage ? View.VISIBLE : View.GONE);
//    btnPush.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View view) {
//        prefs.edit().putBoolean("hint_push", false).apply();
////        llPush.setVisibility(View.GONE);
//      }
//    });
//
//    // Hint system applications
//    final LinearLayout llSystem = (LinearLayout) findViewById(R.id.llSystem);
//    Button btnSystem = (Button) findViewById(R.id.btnSystem);
//    boolean system = prefs.getBoolean("manage_system", false);
//    boolean hintSystem = prefs.getBoolean("hint_system", true);
////    llSystem.setVisibility(!system && hintSystem ? View.VISIBLE : View.GONE);
//    btnSystem.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View view) {
//        prefs.edit().putBoolean("hint_system", false).apply();
////        llSystem.setVisibility(View.GONE);
//      }
//    });
//  }

//    private void initAds() {
//        // https://developers.google.com/android/reference/com/google/android/gms/ads/package-summary
//        MobileAds.initialize(getApplicationContext(), getString(R.string.ad_app_id));
//
//        final LinearLayout llAd = (LinearLayout) findViewById(R.id.llAd);
//        TextView tvAd = (TextView) findViewById(R.id.tvAd);
//        final AdView adView = (AdView) findViewById(R.id.adView);
//
//        SpannableString content = new SpannableString(getString(R.string.title_pro_ads));
//        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
//        tvAd.setText(content);
//
////        tvAd.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                startActivity(new Intent(MainActivity.this, ActivityPro.class));
////            }
////        });
//
//        adView.setAdListener(new AdListener() {
//            @Override
//            public void onAdLoaded() {
//                Log.i(TAG, "Ad loaded");
//                llAd.setVisibility(View.GONE);
//            }
//
//            @Override
//            public void onAdFailedToLoad(int errorCode) {
//                llAd.setVisibility(View.VISIBLE);
//                switch (errorCode) {
//                    case AdRequest.ERROR_CODE_INTERNAL_ERROR:
//                        Log.w(TAG, "Ad load error=INTERNAL_ERROR");
//                        break;
//                    case AdRequest.ERROR_CODE_INVALID_REQUEST:
//                        Log.w(TAG, "Ad load error=INVALID_REQUEST");
//                        break;
//                    case AdRequest.ERROR_CODE_NETWORK_ERROR:
//                        Log.w(TAG, "Ad load error=NETWORK_ERROR");
//                        break;
//                    case AdRequest.ERROR_CODE_NO_FILL:
//                        Log.w(TAG, "Ad load error=NO_FILL");
//                        break;
//                    default:
//                        Log.w(TAG, "Ad load error=" + errorCode);
//                }
//            }
//
//            @Override
//            public void onAdOpened() {
//                Log.i(TAG, "Ad opened");
//            }
//
//            @Override
//            public void onAdClosed() {
//                Log.i(TAG, "Ad closed");
//            }
//
//            @Override
//            public void onAdLeftApplication() {
//                Log.i(TAG, "Ad left app");
//            }
//        });
//    }
//
//    private void enableAds() {
//        RelativeLayout rlAd = (RelativeLayout) findViewById(R.id.rlAd);
//        LinearLayout llAd = (LinearLayout) findViewById(R.id.llAd);
//        final AdView adView = (AdView) findViewById(R.id.adView);
//
//        rlAd.setVisibility(View.VISIBLE);
//        llAd.setVisibility(View.VISIBLE);
//
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    AdRequest adRequest = new AdRequest.Builder()
//                            .addTestDevice(getString(R.string.ad_test_device_id))
//                            .build();
//                    adView.loadAd(adRequest);
//                } catch (Throwable ex) {
//                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
//                }
//            }
//        }, 1000);
//    }

//    private void disableAds() {
//        RelativeLayout rlAd = (RelativeLayout) findViewById(R.id.rlAd);
//        AdView adView = (AdView) findViewById(R.id.adView);
//
//        rlAd.setVisibility(View.GONE);
//
//        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) adView.getLayoutParams();
//        RelativeLayout parent = (RelativeLayout) adView.getParent();
//        parent.removeView(adView);
//
//        adView.destroy();
//        adView = new AdView(this);
//        adView.setAdSize(AdSize.SMART_BANNER);
//        adView.setAdUnitId(getString(R.string.ad_banner_unit_id));
//        adView.setId(R.id.adView);
//        adView.setLayoutParams(params);
//        parent.addView(adView);
//    }

  private void checkExtras(Intent intent) {
    // Approve request
    if (intent.hasExtra(EXTRA_APPROVE)) {
      Log.i(TAG, "Requesting VPN approval");
      swEnabled.toggle();
    }

    if (intent.hasExtra(EXTRA_LOGCAT)) {
      Log.i(TAG, "Requesting logcat");
      Intent logcat = getIntentLogcat();
      if (logcat.resolveActivity(getPackageManager()) != null)
        startActivityForResult(logcat, REQUEST_LOGCAT);
    }
  }

  private void updateApplicationList(final String search) {
    Log.i(TAG, "Update search=" + search);

    new AsyncTask<Object, Object, List<Rule>>() {
      private boolean refreshing = true;

//      @Override
//      protected void onPreExecute() {
//        swipeRefresh.post(new Runnable() {
//          @Override
//          public void run() {
//            if (refreshing)
//              swipeRefresh.setRefreshing(true);
//          }
//        });
//      }

      @Override
      protected List<Rule> doInBackground(Object... arg) {
        return Rule.getRules(false, MainActivity.this);
      }

      @Override
      protected void onPostExecute(List<Rule> result) {
        if (running) {
          if (adapter != null) {
            adapter.set(result);
            updateSearch(search);
          }

//          if (swipeRefresh != null) {
//            refreshing = false;
//            swipeRefresh.setRefreshing(false);
//          }
        }
      }
    }.execute();
  }

  private void updateSearch(String search) {
    if (menuSearch != null) {
      SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuSearch);
      if (search == null) {
        if (menuSearch.isActionViewExpanded())
          adapter.getFilter().filter(searchView.getQuery().toString());
      } else {
        MenuItemCompat.expandActionView(menuSearch);
        searchView.setQuery(search, true);
      }
    }
  }

  private void checkDoze() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      final Intent doze = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
      if (Util.batteryOptimizing(this) && getPackageManager().resolveActivity(doze, 0) != null) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("nodoze", false)) {
          LayoutInflater inflater = LayoutInflater.from(this);
          View view = inflater.inflate(R.layout.doze, null, false);
          final CheckBox cbDontAsk = (CheckBox) view.findViewById(R.id.cbDontAsk);
          dialogDoze = new AlertDialog.Builder(this)
                  .setView(view)
                  .setCancelable(true)
                  .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      prefs.edit().putBoolean("nodoze", cbDontAsk.isChecked()).apply();
                      startActivity(doze);
                    }
                  })
                  .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      prefs.edit().putBoolean("nodoze", cbDontAsk.isChecked()).apply();
                    }
                  })
                  .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                      dialogDoze = null;
                      checkDataSaving();
                    }
                  })
                  .create();
          dialogDoze.show();
        } else
          checkDataSaving();
      } else
        checkDataSaving();
    }
  }

  private void checkDataSaving() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      final Intent settings = new Intent(
              Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS,
              Uri.parse("package:" + getPackageName()));
      if (Util.dataSaving(this) && getPackageManager().resolveActivity(settings, 0) != null) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("nodata", false)) {
          LayoutInflater inflater = LayoutInflater.from(this);
          View view = inflater.inflate(R.layout.datasaving, null, false);
          final CheckBox cbDontAsk = (CheckBox) view.findViewById(R.id.cbDontAsk);
          dialogDoze = new AlertDialog.Builder(this)
                  .setView(view)
                  .setCancelable(true)
                  .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      prefs.edit().putBoolean("nodata", cbDontAsk.isChecked()).apply();
                      startActivity(settings);
                    }
                  })
                  .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      prefs.edit().putBoolean("nodata", cbDontAsk.isChecked()).apply();
                    }
                  })
                  .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                      dialogDoze = null;
                    }
                  })
                  .create();
          dialogDoze.show();
        }
      }
    }
  }

//  private void menu_legend() {
//    TypedValue tv = new TypedValue();
//    getTheme().resolveAttribute(R.attr.colorOn, tv, true);
//    int colorOn = tv.data;
//    getTheme().resolveAttribute(R.attr.colorOff, tv, true);
//    int colorOff = tv.data;
//
//    // Create view
//    LayoutInflater inflater = LayoutInflater.from(this);
//    View view = inflater.inflate(R.layout.legend, null, false);
//    ImageView ivWifiOn = (ImageView) view.findViewById(R.id.ivWifiOn);
//    ImageView ivWifiOff = (ImageView) view.findViewById(R.id.ivWifiOff);
//    ImageView ivOtherOn = (ImageView) view.findViewById(R.id.ivOtherOn);
//    ImageView ivOtherOff = (ImageView) view.findViewById(R.id.ivOtherOff);
//    ImageView ivScreenOn = (ImageView) view.findViewById(R.id.ivScreenOn);
//    ImageView ivHostAllowed = (ImageView) view.findViewById(R.id.ivHostAllowed);
//    ImageView ivHostBlocked = (ImageView) view.findViewById(R.id.ivHostBlocked);
//    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//      Drawable wrapWifiOn = DrawableCompat.wrap(ivWifiOn.getDrawable());
//      Drawable wrapWifiOff = DrawableCompat.wrap(ivWifiOff.getDrawable());
//      Drawable wrapOtherOn = DrawableCompat.wrap(ivOtherOn.getDrawable());
//      Drawable wrapOtherOff = DrawableCompat.wrap(ivOtherOff.getDrawable());
//      Drawable wrapScreenOn = DrawableCompat.wrap(ivScreenOn.getDrawable());
//      Drawable wrapHostAllowed = DrawableCompat.wrap(ivHostAllowed.getDrawable());
//      Drawable wrapHostBlocked = DrawableCompat.wrap(ivHostBlocked.getDrawable());
//
//      DrawableCompat.setTint(wrapWifiOn, colorOn);
//      DrawableCompat.setTint(wrapWifiOff, colorOff);
//      DrawableCompat.setTint(wrapOtherOn, colorOn);
//      DrawableCompat.setTint(wrapOtherOff, colorOff);
//      DrawableCompat.setTint(wrapScreenOn, colorOn);
//      DrawableCompat.setTint(wrapHostAllowed, colorOn);
//      DrawableCompat.setTint(wrapHostBlocked, colorOff);
//    }
//
//
//    // Show dialog
//    dialogLegend = new AlertDialog.Builder(this)
//            .setView(view)
//            .setCancelable(true)
//            .setOnDismissListener(new DialogInterface.OnDismissListener() {
//              @Override
//              public void onDismiss(DialogInterface dialogInterface) {
//                dialogLegend = null;
//              }
//            })
//            .create();
//    dialogLegend.show();
//  }

  private void menu_about() {
    // Create view
    LayoutInflater inflater = LayoutInflater.from(this);
    View view = inflater.inflate(R.layout.about, null, false);
    TextView tvVersionName = (TextView) view.findViewById(R.id.tvVersionName);
    TextView tvVersionCode = (TextView) view.findViewById(R.id.tvVersionCode);
    Button btnRate = (Button) view.findViewById(R.id.btnRate);
    TextView tvLicense = (TextView) view.findViewById(R.id.tvLicense);
    TextView tvAdmob = (TextView) view.findViewById(R.id.tvAdmob);

    // Show version
    tvVersionName.setText(Util.getSelfVersionName(this));
    if (!Util.hasValidFingerprint(this))
      tvVersionName.setTextColor(Color.GRAY);
    tvVersionCode.setText(Integer.toString(Util.getSelfVersionCode(this)));

//        // Handle license
//        tvLicense.setMovementMethod(LinkMovementMethod.getInstance());
//        tvAdmob.setMovementMethod(LinkMovementMethod.getInstance());
//        tvAdmob.setVisibility(IAB.isPurchasedAny(this) ? View.GONE : View.VISIBLE);

    // Handle logcat
    view.setOnClickListener(new View.OnClickListener() {
      private short tap = 0;
      private Toast toast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

      @Override
      public void onClick(View view) {
        tap++;
        if (tap == 7) {
          tap = 0;
          toast.cancel();

          Intent intent = getIntentLogcat();
          if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQUEST_LOGCAT);

        } else if (tap > 3) {
          toast.setText(Integer.toString(7 - tap));
          toast.show();
        }
      }
    });

    // Handle rate
//    btnRate.setVisibility(getIntentRate(this).resolveActivity(getPackageManager()) == null ? View.GONE : View.VISIBLE);
    btnRate.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivity(getIntentRate(MainActivity.this));
      }
    });

    // Show dialog
    dialogAbout = new AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .setOnDismissListener(new DialogInterface.OnDismissListener() {
              @Override
              public void onDismiss(DialogInterface dialogInterface) {
                dialogAbout = null;
              }
            })
            .create();
    dialogAbout.show();
  }

//  private static Intent getIntentInvite(Context context) {
//    Intent intent = new Intent("com.google.android.gms.appinvite.ACTION_APP_INVITE");
//    intent.setPackage("com.google.android.gms");
//    intent.putExtra("com.google.android.gms.appinvite.TITLE", context.getString(R.string.menu_invite));
//    intent.putExtra("com.google.android.gms.appinvite.MESSAGE", context.getString(R.string.msg_try));
//    intent.putExtra("com.google.android.gms.appinvite.BUTTON_TEXT", context.getString(R.string.msg_try));
//    // com.google.android.gms.appinvite.DEEP_LINK_URL
//    return intent;
//  }

  private static Intent getIntentRate(Context context) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName()));
    if (intent.resolveActivity(context.getPackageManager()) == null)
      intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName()));
    return intent;
  }

  private static Intent getIntentSupport() {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse("https://github.com/M66B/NetGuard/blob/master/FAQ.md"));
    return intent;
  }

  private Intent getIntentLogcat() {
    Intent intent;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      if (Util.isPackageInstalled("org.openintents.filemanager", this)) {
        intent = new Intent("org.openintents.action.PICK_DIRECTORY");
      } else {
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=org.openintents.filemanager"));
      }
    } else {
      intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("text/plain");
      intent.putExtra(Intent.EXTRA_TITLE, "logcat.txt");
    }
    return intent;
  }
}


