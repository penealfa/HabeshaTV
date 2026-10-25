package com.gathin.tvhome;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/** What the Wi-Fi button and panel show: the network name, signal, band and address. */
final class HabeshaNet {

    static final class Info {
        final boolean connected;
        final boolean ethernet;
        final String name;   // network name, "Ethernet", or "Wi-Fi" when it is not known
        final int bars;      // 0 to 4
        final String detail; // e.g. "Connected · 5 GHz · 192.168.1.24"

        Info(boolean connected, boolean ethernet, String name, int bars, String detail) {
            this.connected = connected;
            this.ethernet = ethernet;
            this.name = name;
            this.bars = bars;
            this.detail = detail;
        }
    }

    static final Info OFFLINE = new Info(false, false, "Wi-Fi", 0, "Not connected");

    private HabeshaNet() {
    }

    @SuppressWarnings("deprecation")
    static Info read(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo active = cm == null ? null : cm.getActiveNetworkInfo();
            if (active == null || !active.isConnected()) return OFFLINE;
            if (active.getType() == ConnectivityManager.TYPE_ETHERNET) {
                return new Info(true, true, "Ethernet", 4, "Connected · Ethernet");
            }
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm == null ? null : wm.getConnectionInfo();
            if (wi == null) return new Info(true, false, "Wi-Fi", 3, "Connected");
            String ssid = wi.getSSID();
            // Without the location permission Android answers "<unknown ssid>"; the button then just says Wi-Fi.
            if (ssid == null || ssid.contains("unknown ssid")) ssid = "Wi-Fi";
            else if (ssid.length() > 1 && ssid.startsWith("\"") && ssid.endsWith("\"")) ssid = ssid.substring(1, ssid.length() - 1);
            int bars = Math.max(1, WifiManager.calculateSignalLevel(wi.getRssi(), 5));
            int mhz = wi.getFrequency();
            String band = mhz >= 4900 ? "5 GHz" : mhz > 0 ? "2.4 GHz" : null;
            int ip = wi.getIpAddress();
            String addr = ip == 0 ? null : (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
            StringBuilder detail = new StringBuilder("Connected");
            if (band != null) detail.append(" · ").append(band);
            if (addr != null) detail.append(" · ").append(addr);
            return new Info(true, false, ssid, bars, detail.toString());
        } catch (RuntimeException e) {
            return OFFLINE;
        }
    }
}
