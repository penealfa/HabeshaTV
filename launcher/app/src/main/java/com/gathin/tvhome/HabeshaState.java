package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigation state of the HabeshaTV home screen: the ring of apps, the three top buttons, the wi-fi panel, the
 * hold-OK menu, move mode, the app library and the saver. Plain Java so it is unit tested; the view only draws it.
 */
final class HabeshaState {

    static final String LIBRARY_ID = "__lib";
    static final int COLUMNS = 6;
    static final int BUTTON_APPS = 0, BUTTON_WIFI = 1, BUTTON_SETTINGS = 2;
    static final int MENU_MOVE = 0, MENU_REMOVE = 1, MENU_THEME = 2, MENU_CANCEL = 3;

    enum Screen { HOME, LIBRARY, SAVER }
    enum Zone { RING, TOP }
    enum Overlay { NONE, WIFI }

    /** What the activity has to do after a key. */
    static final class Action {
        static final Action NONE = new Action(null, null);
        final String launchPackage;
        final String system; // "settings", "wifi", "theme" or null

        private Action(String launchPackage, String system) {
            this.launchPackage = launchPackage;
            this.system = system;
        }

        static Action launch(String pkg) { return new Action(pkg, null); }
        static Action system(String what) { return new Action(null, what); }
    }

    /**
     * A message shown for two seconds: an Amharic key plus an English line. When appId is set the view puts that
     * app's name in front of the English line, e.g. "Spotify" + "added to the mesob".
     */
    static final class Toast {
        final String amKey;
        final String en;
        final String appId;

        Toast(String amKey, String en, String appId) {
            this.amKey = amKey;
            this.en = en;
            this.appId = appId;
        }
    }

    interface Listener {
        /** The order of the ring or the removed set changed and should be saved. */
        void onAppsChanged(List<String> apps, List<String> removed);
    }

    Screen screen = Screen.HOME;
    Zone zone = Zone.RING;
    Overlay overlay = Overlay.NONE;
    int focus;          // index in ring()
    int rotation;       // grows by one per step, turns the weave
    int topIndex;
    int overlayIndex;
    int libraryIndex;
    boolean menuOpen;
    int menuIndex;
    boolean moving;
    Toast toast;

    private final List<String> apps = new ArrayList<>();
    private final List<String> removed = new ArrayList<>();
    private final List<String> library = new ArrayList<>(); // every installed app, stable order
    private final Listener listener;

    HabeshaState(Listener listener) {
        this.listener = listener;
    }

    /**
     * Installed apps in library order, with the saved ring order and the removed set. Apps that are new (in neither
     * list) join the end of the ring, so a freshly installed app is visible straight away.
     */
    void setApps(List<String> installed, List<String> savedOrder, List<String> savedRemoved) {
        library.clear();
        library.addAll(installed);
        apps.clear();
        removed.clear();
        for (String id : savedOrder) if (installed.contains(id) && !apps.contains(id)) apps.add(id);
        for (String id : savedRemoved) if (installed.contains(id) && !apps.contains(id) && !removed.contains(id)) removed.add(id);
        for (String id : installed) if (!apps.contains(id) && !removed.contains(id)) apps.add(id);
        focus = Math.min(focus, ring().size() - 1);
        libraryIndex = Math.min(libraryIndex, Math.max(0, library.size() - 1));
    }

    List<String> apps() { return apps; }
    List<String> removed() { return removed; }
    List<String> library() { return library; }

    /** The apps followed by the "all apps" tile. */
    List<String> ring() {
        List<String> r = new ArrayList<>(apps);
        r.add(LIBRARY_ID);
        return r;
    }

    String focusedId() {
        List<String> r = ring();
        return r.get(Math.max(0, Math.min(focus, r.size() - 1)));
    }

    boolean canHold() {
        return screen == Screen.HOME && overlay == Overlay.NONE && !menuOpen && !moving
                && zone == Zone.RING && focus < apps.size();
    }

    int overlayRows() { return 2; }

    void arrow(int dx, int dy) {
        if (screen == Screen.LIBRARY) {
            libraryArrow(dx, dy);
            return;
        }
        if (overlay != Overlay.NONE) {
            if (dy < 0) overlayIndex = Math.max(0, overlayIndex - 1);
            if (dy > 0) overlayIndex = Math.min(overlayRows() - 1, overlayIndex + 1);
            return;
        }
        if (menuOpen) {
            if (dy < 0) menuIndex = Math.max(0, menuIndex - 1);
            if (dy > 0) menuIndex = Math.min(MENU_CANCEL, menuIndex + 1);
            return;
        }
        if (moving) {
            if (dx == 0) return;
            int n = apps.size();
            int j = dx < 0 ? (focus - 1 + n) % n : (focus + 1) % n;
            String swap = apps.get(focus);
            apps.set(focus, apps.get(j));
            apps.set(j, swap);
            focus = j;
            rotation += dx < 0 ? -1 : 1;
            changed();
            return;
        }
        if (zone == Zone.TOP) {
            if (dx < 0 && topIndex > 0) topIndex--;
            else if (dx > 0 && topIndex < 2) topIndex++;
            else if (dy > 0) zone = Zone.RING;
            return;
        }
        int n = ring().size();
        if (dx < 0) { focus = (focus - 1 + n) % n; rotation--; }
        else if (dx > 0) { focus = (focus + 1) % n; rotation++; }
        else if (dy < 0) zone = Zone.TOP;
        else if (dy > 0) openLibrary();
    }

    private void libraryArrow(int dx, int dy) {
        int n = library.size();
        if (n == 0) return;
        int i = libraryIndex;
        if (dx < 0 && i % COLUMNS > 0) i--;
        else if (dx > 0 && i % COLUMNS < COLUMNS - 1 && i + 1 < n) i++;
        else if (dy < 0 && i >= COLUMNS) i -= COLUMNS;
        else if (dy > 0) {
            if (i + COLUMNS < n) i += COLUMNS;
            else if (i / COLUMNS < (n - 1) / COLUMNS) i = n - 1;
        }
        libraryIndex = i;
    }

    /** OK pressed (a short press). */
    Action ok() {
        if (screen == Screen.LIBRARY) {
            if (library.isEmpty()) return Action.NONE;
            String id = library.get(libraryIndex);
            if (removed.contains(id)) {
                removed.remove(id);
                apps.add(id);
                changed();
                toast = new Toast("added", "added to the mesob", id);
            } else {
                toast = new Toast("onHome", "is already on the mesob", id);
            }
            return Action.NONE;
        }
        if (overlay == Overlay.WIFI) {
            overlay = Overlay.NONE;
            if (overlayIndex == 1) return Action.system("wifi");
            toast = new Toast(null, "Wi-Fi", null);
            return Action.NONE;
        }
        if (menuOpen) {
            if (menuIndex == MENU_MOVE) { menuOpen = false; moving = true; }
            else if (menuIndex == MENU_REMOVE) removeFocused();
            else if (menuIndex == MENU_THEME) return Action.system("theme"); // the menu stays open to try the next look
            else menuOpen = false;
            return Action.NONE;
        }
        if (moving) {
            moving = false;
            return Action.NONE;
        }
        if (zone == Zone.TOP) {
            if (topIndex == BUTTON_SETTINGS) return Action.system("settings");
            if (topIndex == BUTTON_APPS) openLibrary();
            else { overlay = Overlay.WIFI; overlayIndex = 0; }
            return Action.NONE;
        }
        String id = focusedId();
        if (LIBRARY_ID.equals(id)) {
            openLibrary();
            return Action.NONE;
        }
        toast = new Toast("opening", null, id);
        return Action.launch(id);
    }

    /** OK held on an app in the ring: the move, remove and cancel menu. */
    void hold() {
        if (!canHold()) return;
        menuOpen = true;
        menuIndex = 0;
    }

    void back() {
        if (overlay != Overlay.NONE) overlay = Overlay.NONE;
        else if (menuOpen) menuOpen = false;
        else if (moving) moving = false;
        else if (screen == Screen.LIBRARY) { screen = Screen.HOME; zone = Zone.RING; }
        else if (zone == Zone.TOP) zone = Zone.RING;
    }

    void goHome() {
        screen = Screen.HOME;
        zone = Zone.RING;
        overlay = Overlay.NONE;
        menuOpen = false;
        moving = false;
    }

    void openLibrary() {
        screen = Screen.LIBRARY;
        libraryIndex = 0;
    }

    void startSaver() {
        screen = Screen.SAVER;
        overlay = Overlay.NONE;
        menuOpen = false;
        moving = false;
    }

    /** Idle on the plain home screen, nothing open. */
    boolean idleForSaver() {
        return screen == Screen.HOME && overlay == Overlay.NONE && !menuOpen && !moving;
    }

    private void removeFocused() {
        String id = apps.remove(focus);
        removed.add(id);
        focus = Math.min(focus, apps.size());
        menuOpen = false;
        changed();
        toast = new Toast("removed", "removed from the mesob", id);
    }

    private void changed() {
        if (listener != null) listener.onAppsChanged(new ArrayList<>(apps), new ArrayList<>(removed));
    }
}
