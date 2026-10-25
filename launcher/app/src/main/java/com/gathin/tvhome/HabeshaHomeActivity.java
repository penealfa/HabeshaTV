package com.gathin.tvhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The HabeshaTV home screen. Keys go to {@link HabeshaState}, {@link HabeshaHomeView} draws it, and this class does
 * what needs a Context: launching apps, saving the order, the clock, the idle saver and Wi-Fi status.
 */
public class HabeshaHomeActivity extends Activity {

    private static final long IDLE_MS = 60_000;
    private static final long TOAST_MS = 2_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private HabeshaState state;
    private HabeshaHomeView view;
    private LinkedHashMap<String, HabeshaApps.AppInfo> installed = new LinkedHashMap<>();

    private boolean swallowOkUp; // the OK that woke the saver should not also click
    private boolean resumed;

    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { reloadApps(); }
    };

    private final Runnable idleRunnable = new Runnable() {
        @Override public void run() {
            if (resumed && state.idleForSaver()) startSaver();
        }
    };

    private final Runnable clearToast = new Runnable() {
        @Override public void run() {
            state.toast = null;
            view.invalidate();
        }
    };

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            view.setTime(System.currentTimeMillis());
            handler.postDelayed(this, state.screen == HabeshaState.Screen.SAVER ? 1_000 : 5_000);
        }
    };

    private final Runnable wifiTick = new Runnable() {
        @Override public void run() {
            view.setWifi(HabeshaNet.read(HabeshaHomeActivity.this));
            handler.postDelayed(this, 20_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        state = new HabeshaState((apps, removed) -> HabeshaApps.saveLists(this, apps, removed));
        view = new HabeshaHomeView(this, state, HabeshaTheme.byName(HabeshaApps.savedTheme(this)));
        view.setTime(System.currentTimeMillis());
        setContentView(view);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packages, f);
        reloadApps();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packages);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        reloadApps();
        view.setTime(System.currentTimeMillis());
        handler.post(tick);
        handler.post(wifiTick);
        bumpIdle();
    }

    @Override
    protected void onPause() {
        resumed = false;
        state.toast = null; // its timer is cancelled below, so a message must not be left on screen
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    /** Home pressed while already home: back to the ring. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            state.goHome();
            view.invalidate();
            bumpIdle();
        }
    }

    private void reloadApps() {
        installed = HabeshaApps.load(this);
        List<String> ids = new ArrayList<>(installed.keySet());
        state.setApps(ids, HabeshaApps.savedList(this, "order"), HabeshaApps.savedList(this, "removed"));
        view.setApps(installed);
        // Icons take a moment on this hardware, so they arrive after the first frame.
        final LinkedHashMap<String, HabeshaApps.AppInfo> snapshot = installed;
        new Thread(() -> {
            HabeshaApps.loadIcons(getApplicationContext(), snapshot);
            view.postInvalidate();
        }, "habesha-icons").start();
    }

    private void bumpIdle() {
        handler.removeCallbacks(idleRunnable);
        handler.postDelayed(idleRunnable, IDLE_MS);
    }

    private void startSaver() {
        state.startSaver();
        view.invalidate();
    }

    private void showToastTimer() {
        handler.removeCallbacks(clearToast);
        if (state.toast != null) handler.postDelayed(clearToast, TOAST_MS);
    }

    // ------------------------------------------------------------------ keys

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        bumpIdle();
        if (state.screen == HabeshaState.Screen.SAVER) {
            if (event.getRepeatCount() == 0) {
                state.goHome();
                if (isOk(keyCode)) swallowOkUp = true;
                view.invalidate();
            }
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: state.arrow(-1, 0); break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: state.arrow(1, 0); break;
            case KeyEvent.KEYCODE_DPAD_UP: state.arrow(0, -1); break;
            case KeyEvent.KEYCODE_DPAD_DOWN: state.arrow(0, 1); break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                if (event.getRepeatCount() > 0) return true;
                if (state.canHold()) {
                    // A short press launches, a long press opens the menu: Android tells us which on key up or
                    // through onKeyLongPress, using the same timeout as every other app on this TV.
                    event.startTracking();
                    return true;
                }
                doOk();
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                state.back();
                break;
            case KeyEvent.KEYCODE_MENU:
                cycleTheme(true);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
        view.invalidate();
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (isOk(keyCode) && state.canHold()) {
            state.hold();
            view.invalidate();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isOk(keyCode)) return super.onKeyUp(keyCode, event);
        if (swallowOkUp) {
            swallowOkUp = false;
            return true;
        }
        // A press that became a long press arrives here cancelled: the menu is already open, so do nothing.
        if (event.isTracking() && !event.isCanceled()) doOk();
        return true;
    }

    private static boolean isOk(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_BUTTON_A;
    }

    private void doOk() {
        HabeshaState.Action action = state.ok();
        if (action.launchPackage != null) {
            Intent intent = HabeshaApps.launchIntent(this, action.launchPackage);
            if (intent != null) {
                try {
                    startActivity(intent);
                } catch (RuntimeException e) {
                    state.toast = new HabeshaState.Toast(null, "Could not open this app", null);
                }
            } else {
                state.toast = new HabeshaState.Toast(null, "Could not open this app", null);
            }
        } else if ("theme".equals(action.system)) {
            cycleTheme(false); // from the hold-OK menu: the row shows the name, so no message
        } else if (action.system != null) {
            openSystem(action.system);
        }
        showToastTimer();
        view.invalidate();
    }

    private void openSystem(String what) {
        String action = "wifi".equals(what) ? Settings.ACTION_WIFI_SETTINGS : Settings.ACTION_SETTINGS;
        try {
            startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException ignored) {
                state.toast = new HabeshaState.Toast(null, "Settings are not available", null);
            }
        }
    }

    /** The Theme row (and the Menu key, on remotes that have one) steps through the four looks and remembers it. */
    private void cycleTheme(boolean announce) {
        String next = HabeshaTheme.next(view.theme().name);
        HabeshaApps.saveTheme(this, next);
        view.setTheme(HabeshaTheme.byName(next));
        if (announce) {
            state.toast = new HabeshaState.Toast(null, "Theme: " + next, null);
            showToastTimer();
        }
        view.invalidate();
    }
}
