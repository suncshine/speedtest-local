package com.fdossena.speedtest.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.fdossena.speedtest.core.Speedtest;
import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.config.TelemetryConfig;
import com.fdossena.speedtest.core.serverSelector.TestPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;

import android.content.pm.ActivityInfo;
import android.app.UiModeManager;
import android.content.res.Configuration;
import dev.local.speedtest.R;

import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "LibreSpeed_Main";
    private static final String PREFS_NAME = "LibreSpeedPrefs";
    private static final String PREF_SERVER_URL = "server_url";
    private static final String PREF_CACHED_IP_PREFIX = "cached_ip_prefix";
    private static final String PREF_LAST_SUCCESSFUL_SERVER_URL = "last_successful_server_url";

    private String currentServerUrl = ""; // Track the current server URL
    private static Speedtest st = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Application starting");

        // Detect if running on TV
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            Log.d(TAG, "onCreate: Running on TV");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            Log.d(TAG, "onCreate: Running on Phone/Tablet");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_main);
        
        // 立即开始初始化逻辑，不再显示 page_splash 和等待 1.5s
        Log.d(TAG, "onCreate: Initializing components directly");
        
        new Thread() {
            public void run() {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    final ImageView v = findViewById(R.id.testBackground);
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeResource(getResources(), R.drawable.testbackground, options);
                    int ih = options.outHeight, iw = options.outWidth;
                    if (4 * ih * iw > 16 * 1024 * 1024) throw new Exception("Too big");
                    options.inJustDecodeBounds = false;
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int vh = displayMetrics.heightPixels, vw = displayMetrics.widthPixels;
                    double desired = Math.max(vw, vh) * 0.7;
                    double scale = desired / Math.max(iw, ih);
                    final Bitmap b = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.testbackground, options), (int) (iw * scale), (int) (ih * scale), true);
                    runOnUiThread(() -> v.setImageBitmap(b));
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to load background", t);
                }

                String currentIpPrefix = getLocalIPv4Prefix();
                String cachedIpPrefix = getCachedIpprefix();
                if (currentIpPrefix != null && currentIpPrefix.equals(cachedIpPrefix)) {
                    String lastSuccessfulServerUrl = getLastSuccessfulServerURL();
                    if (!lastSuccessfulServerUrl.isEmpty()) {
                        currentServerUrl = lastSuccessfulServerUrl;
                    }
                }
                if (currentIpPrefix != null) {
                    cacheIpprefix(currentIpPrefix);
                }

                Log.d(TAG, "Init background tasks complete, calling page_init");
                page_init();
            }
        }.start();
    }

    private String[] getLocalIPv4Parts() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddress = networkInterface.getInetAddresses(); enumIpAddress.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddress.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ipAddress = inetAddress.getHostAddress();
                        if (ipAddress == null) continue;
                        String[] parts = ipAddress.split("\\.");
                        if (parts.length == 4) return parts;
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error getting IP parts", ex);
        }
        return null;
    }

    private String getLocalIPv4Prefix() {
        String[] ipParts = getLocalIPv4Parts();
        if (ipParts != null && ipParts.length >= 3) {
            return ipParts[0] + "." + ipParts[1] + "." + ipParts[2];
        }
        return null;
    }

    private String getCachedServerURL() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_SERVER_URL, "");
    }

    private void cacheServerURL(String url) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_SERVER_URL, url);
        editor.apply();
    }

    private String getLastSuccessfulServerURL() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_LAST_SUCCESSFUL_SERVER_URL, "");
    }

    private void cacheLastSuccessfulServerURL(String url) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_LAST_SUCCESSFUL_SERVER_URL, url);
        editor.apply();
    }

    private String getCachedIpprefix() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_CACHED_IP_PREFIX, "");
    }

    private void cacheIpprefix(String ipPrefix) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_CACHED_IP_PREFIX, ipPrefix);
        editor.apply();
    }

    private void page_init() {
        Log.d(TAG, "page_init: Starting");
        new Thread() {
            @Override
            public void run() {
                runOnUiThread(() -> transition(R.id.page_init, TRANSITION_LENGTH));
                
                // Watchdog timer (5s)
                new Thread() {
                    public void run() {
                        try { sleep(5000); } catch (Exception e) {}
                        if (currentPage == R.id.page_init) {
                            Log.w(TAG, "Watchdog triggered! Forcing transition to server select.");
                            runOnUiThread(() -> page_serverSelect(null, new TestPoint[0]));
                        }
                    }
                }.start();

                final TextView t = findViewById(R.id.init_text);
                runOnUiThread(() -> t.setText(R.string.init_init));
                
                try {
                    String c = readFileFromAssets("SpeedtestConfig.json");
                    JSONObject o = new JSONObject(c);
                    SpeedtestConfig config = new SpeedtestConfig(o);
                    
                    c = readFileFromAssets("TelemetryConfig.json");
                    o = new JSONObject(c);
                    TelemetryConfig telemetryConfig = new TelemetryConfig(o);
                    
                    if (telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_DISABLED)) {
                        runOnUiThread(() -> hideView(R.id.privacy_open));
                    }
                    
                    if (st != null) {
                        try { st.abort(); } catch (Throwable e) {}
                    }
                    st = new Speedtest();
                    st.setSpeedtestConfig(config);
                    st.setTelemetryConfig(telemetryConfig);

                    final String testOrder = config.getTest_order();
                    runOnUiThread(() -> {
                        if (!testOrder.contains("D")) hideView(R.id.dlArea);
                        if (!testOrder.contains("U")) hideView(R.id.ulArea);
                        if (!testOrder.contains("P")) hideView(R.id.pingArea);
                        if (!testOrder.contains("I")) hideView(R.id.ipInfo);
                    });
                    
                    Log.d(TAG, "Init complete, moving to server selection");
                    runOnUiThread(() -> page_serverSelect(null, new TestPoint[0]));
                } catch (final Throwable e) {
                    Log.e(TAG, "Init failed", e);
                    st = null;
                    runOnUiThread(() -> {
                        transition(R.id.page_fail, TRANSITION_LENGTH);
                        ((TextView) findViewById(R.id.fail_text)).setText(getString(R.string.initFail_configError) + ": " + e.getMessage());
                        final Button b = findViewById(R.id.fail_button);
                        b.setText(R.string.initFail_retry);
                        b.setOnClickListener(v -> {
                            page_init();
                            b.setOnClickListener(null);
                        });
                    });
                }
            }
        }.start();
    }

    private void page_serverSelect(TestPoint selected, TestPoint[] servers) {
        Log.d(TAG, "page_serverSelect: Showing");
        transition(R.id.page_serverSelect, TRANSITION_LENGTH);
        reinitOnResume = true;

        String[] ipParts = getLocalIPv4Parts();
        if (ipParts != null && ipParts.length == 4) {
            ((EditText) findViewById(R.id.ip_segment_1)).setText(ipParts[0]);
            ((EditText) findViewById(R.id.ip_segment_2)).setText(ipParts[1]);
            ((EditText) findViewById(R.id.ip_segment_3)).setText(ipParts[2]);
            ((EditText) findViewById(R.id.ip_segment_4)).setText("10");
            ((EditText) findViewById(R.id.port_input)).setText("8989");
        }

        final Button b = findViewById(R.id.start);
        b.setOnClickListener(v -> {
            reinitOnResume = false;
            String ipPart1 = ((EditText) findViewById(R.id.ip_segment_1)).getText().toString().trim();
            String ipPart2 = ((EditText) findViewById(R.id.ip_segment_2)).getText().toString().trim();
            String ipPart3 = ((EditText) findViewById(R.id.ip_segment_3)).getText().toString().trim();
            String ipPart4 = ((EditText) findViewById(R.id.ip_segment_4)).getText().toString().trim();
            String portStr = ((EditText) findViewById(R.id.port_input)).getText().toString().trim();

            if (ipPart1.isEmpty() || ipPart2.isEmpty() || ipPart3.isEmpty() || ipPart4.isEmpty() || portStr.isEmpty()) {
                ((TextView) findViewById(R.id.selectServer_message)).setText("Please fill all fields");
                return;
            }

            String serverUrl = "http://" + ipPart1 + "." + ipPart2 + "." + ipPart3 + "." + ipPart4 + ":" + portStr;
            String baseUrl = serverUrl.replaceAll("/$", "");
            String serverName = extractServerName(serverUrl);

            TestPoint customServer = new TestPoint(serverName, baseUrl, "/garbage.php", "/empty.php", "/empty.php", "/getIP.php");
            page_test(customServer);
            b.setOnClickListener(null);
        });
        
        findViewById(R.id.privacy_open).setOnClickListener(v -> page_privacy());
    }

    private String extractServerName(String url) {
        try {
            java.net.URL netUrl = new java.net.URL(url);
            return netUrl.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private void page_privacy() {
        transition(R.id.page_privacy, TRANSITION_LENGTH);
        reinitOnResume = false;
        ((WebView) findViewById(R.id.privacy_policy)).loadUrl(getString(R.string.privacy_policy));
        findViewById(R.id.privacy_close).setOnClickListener(v -> {
            transition(R.id.page_serverSelect, TRANSITION_LENGTH);
            reinitOnResume = true;
        });
    }

    private void page_test(final TestPoint selected) {
        Log.d(TAG, "page_test: Starting test on " + selected.getServer());
        currentServerUrl = selected.getServer();
        transition(R.id.page_test, TRANSITION_LENGTH);
        st.setSelectedServer(selected);
        
        ((TextView) findViewById(R.id.serverName)).setText(selected.getName());
        ((TextView) findViewById(R.id.dlText)).setText(format(0));
        ((TextView) findViewById(R.id.ulText)).setText(format(0));
        ((TextView) findViewById(R.id.pingText)).setText(format(0));
        ((TextView) findViewById(R.id.jitterText)).setText(format(0));
        ((ProgressBar) findViewById(R.id.dlProgress)).setProgress(0);
        ((ProgressBar) findViewById(R.id.ulProgress)).setProgress(0);
        ((GaugeView) findViewById(R.id.dlGauge)).setValue(0);
        ((GaugeView) findViewById(R.id.ulGauge)).setValue(0);
        ((TextView) findViewById(R.id.ipInfo)).setText("");
        
        final View endTestArea = findViewById(R.id.endTestArea);
        ViewGroup.LayoutParams p = endTestArea.getLayoutParams();
        p.height = 0;
        endTestArea.setLayoutParams(p);
        findViewById(R.id.shareButton).setVisibility(View.GONE);

        st.start(new Speedtest.SpeedtestHandler() {
            @Override
            public void onDownloadUpdate(final double dl, final double progress) {
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.dlText)).setText(progress == 0 ? "..." : format(dl));
                    ((GaugeView) findViewById(R.id.dlGauge)).setValue(progress == 0 ? 0 : mbpsToGauge(dl));
                    ((ProgressBar) findViewById(R.id.dlProgress)).setProgress((int) (100 * progress));
                });
            }

            @Override
            public void onUploadUpdate(final double ul, final double progress) {
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.ulText)).setText(progress == 0 ? "..." : format(ul));
                    ((GaugeView) findViewById(R.id.ulGauge)).setValue(progress == 0 ? 0 : mbpsToGauge(ul));
                    ((ProgressBar) findViewById(R.id.ulProgress)).setProgress((int) (100 * progress));
                });
            }

            @Override
            public void onPingJitterUpdate(final double ping, final double jitter, final double progress) {
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.pingText)).setText(progress == 0 ? "..." : format(ping));
                    ((TextView) findViewById(R.id.jitterText)).setText(progress == 0 ? "..." : format(jitter));
                });
            }

            @Override
            public void onIPInfoUpdate(final String ipInfo) {
                runOnUiThread(() -> ((TextView) findViewById(R.id.ipInfo)).setText(ipInfo));
            }

            @Override
            public void onTestIDReceived(final String id, final String shareURL) {
                if (shareURL == null || shareURL.isEmpty()) return;
                runOnUiThread(() -> {
                    Button shareButton = findViewById(R.id.shareButton);
                    shareButton.setVisibility(View.VISIBLE);
                    shareButton.setOnClickListener(v -> {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.putExtra(Intent.EXTRA_TEXT, shareURL);
                        startActivity(Intent.createChooser(share, getString(R.string.test_share)));
                    });
                });
            }

            @Override
            public void onEnd() {
                Log.d(TAG, "Test ended successfully");
                if (!currentServerUrl.isEmpty()) {
                    cacheLastSuccessfulServerURL(currentServerUrl);
                    cacheServerURL(currentServerUrl);
                }
                runOnUiThread(() -> {
                    final Button restartButton = findViewById(R.id.restartButton);
                    restartButton.setOnClickListener(v -> page_init());
                });
                
                final long startT = System.currentTimeMillis(), endT = startT + TRANSITION_LENGTH;
                new Thread() {
                    public void run() {
                        while (System.currentTimeMillis() < endT) {
                            final double f = (double) (System.currentTimeMillis() - startT) / (double) (TRANSITION_LENGTH);
                            runOnUiThread(() -> {
                                ViewGroup.LayoutParams p = endTestArea.getLayoutParams();
                                p.height = (int) (400 * f); // Animation
                                endTestArea.setLayoutParams(p);
                            });
                            try { sleep(10); } catch (Exception e) {}
                        }
                    }
                }.start();
            }

            @Override
            public void onCriticalFailure(String err) {
                Log.e(TAG, "Test failed: " + err);
                runOnUiThread(() -> {
                    transition(R.id.page_fail, TRANSITION_LENGTH);
                    ((TextView) findViewById(R.id.fail_text)).setText(getString(R.string.testFail_err));
                    findViewById(R.id.fail_button).setOnClickListener(v -> page_init());
                });
            }
        });
    }

    private String format(double d) {
        Locale l = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? getResources().getConfiguration().getLocales().get(0) : getResources().getConfiguration().locale;
        if (d < 10) return String.format(l, "%.2f", d);
        if (d < 100) return String.format(l, "%.1f", d);
        return "" + Math.round(d);
    }

    private int mbpsToGauge(double s) {
        return (int) (1000 * (1 - (1 / (Math.pow(1.3, Math.sqrt(s))))));
    }

    private String readFileFromAssets(String name) throws Exception {
        BufferedReader b = new BufferedReader(new InputStreamReader(getAssets().open(name)));
        StringBuilder ret = new StringBuilder();
        String s;
        while ((s = b.readLine()) != null) ret.append(s);
        return ret.toString();
    }

    private void hideView(int id) {
        View v = findViewById(id);
        if (v != null) v.setVisibility(View.GONE);
    }

    private boolean reinitOnResume = false;
    @Override
    protected void onResume() {
        super.onResume();
        if (reinitOnResume) {
            reinitOnResume = false;
            page_init();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (st != null) st.abort(); } catch (Throwable t) {}
    }

    @Override
    public void onBackPressed() {
        if (currentPage == R.id.page_privacy) transition(R.id.page_serverSelect, TRANSITION_LENGTH);
        else super.onBackPressed();
    }

    private int currentPage = -1;
    private boolean transitionBusy = false;
    private final int TRANSITION_LENGTH = 300;

    private void transition(final int page, final int duration) {
        if (transitionBusy) {
            new Thread() {
                public void run() {
                    try { sleep(10); } catch (Exception e) {}
                    transition(page, duration);
                }
            }.start();
            return;
        }
        transitionBusy = true;
        if (page == currentPage) {
            transitionBusy = false;
            return;
        }
        final ViewGroup oldPage = currentPage == -1 ? null : (ViewGroup) findViewById(currentPage);
        final ViewGroup newPage = page == -1 ? null : (ViewGroup) findViewById(page);
        
        new Thread() {
            public void run() {
                long t = System.currentTimeMillis(), endT = t + duration;
                runOnUiThread(() -> {
                    if (newPage != null) { newPage.setAlpha(0); newPage.setVisibility(View.VISIBLE); }
                });
                while (System.currentTimeMillis() < endT) {
                    final float f = (float) (endT - System.currentTimeMillis()) / (float) duration;
                    runOnUiThread(() -> {
                        if (newPage != null) newPage.setAlpha(1 - f);
                        if (oldPage != null) oldPage.setAlpha(f);
                    });
                    try { sleep(10); } catch (Exception e) {}
                }
                currentPage = page;
                runOnUiThread(() -> {
                    if (oldPage != null) { oldPage.setAlpha(0); oldPage.setVisibility(View.INVISIBLE); }
                    if (newPage != null) newPage.setAlpha(1);
                    transitionBusy = false;
                });
            }
        }.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
            View v = getCurrentFocus();
            if (v != null) { v.performClick(); return true; }
        }
        return super.onKeyDown(keyCode, event);
    }
}
