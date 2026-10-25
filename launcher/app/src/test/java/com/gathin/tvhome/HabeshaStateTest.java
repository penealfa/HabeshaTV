package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class HabeshaStateTest {

    private HabeshaState s;
    private List<String> savedApps;
    private List<String> savedRemoved;
    private int saves;

    private static List<String> ids(String... a) { return new ArrayList<>(Arrays.asList(a)); }

    @Before
    public void setUp() {
        saves = 0;
        s = new HabeshaState((apps, removed) -> { savedApps = apps; savedRemoved = removed; saves++; });
        s.setApps(ids("a", "b", "c", "d", "e"), ids("a", "b", "c"), Collections.<String>emptyList());
    }

    @Test
    public void newAppsJoinTheRingAndRemovedOnesStayOut() {
        s.setApps(ids("a", "b", "c", "d", "e"), ids("c", "a"), ids("d"));
        assertEquals(ids("c", "a", "b", "e"), s.apps());
        assertEquals(ids("d"), s.removed());
        assertEquals(ids("c", "a", "b", "e", "__lib"), s.ring());
    }

    @Test
    public void uninstalledAppsDropOutOfBothLists() {
        s.setApps(ids("a", "b"), ids("z", "a", "b"), ids("y"));
        assertEquals(ids("a", "b"), s.apps());
        assertTrue(s.removed().isEmpty());
    }

    @Test
    public void ringWrapsBothWaysIncludingTheLibraryTile() {
        assertEquals(6, s.ring().size()); // a..e plus the library tile
        s.arrow(-1, 0);
        assertEquals("__lib", s.focusedId());
        s.arrow(1, 0);
        assertEquals("a", s.focusedId());
        assertEquals(0, s.rotation);
        s.arrow(1, 0);
        assertEquals(1, s.rotation);
    }

    @Test
    public void upGoesToTheButtonsAndDownComesBack() {
        s.arrow(0, -1);
        assertEquals(HabeshaState.Zone.TOP, s.zone);
        s.arrow(1, 0);
        s.arrow(1, 0);
        s.arrow(1, 0);
        assertEquals(HabeshaState.BUTTON_SETTINGS, s.topIndex);
        s.arrow(0, 1);
        assertEquals(HabeshaState.Zone.RING, s.zone);
    }

    @Test
    public void downFromTheRingOpensTheLibrary() {
        s.arrow(0, 1);
        assertEquals(HabeshaState.Screen.LIBRARY, s.screen);
        s.back();
        assertEquals(HabeshaState.Screen.HOME, s.screen);
        assertEquals(HabeshaState.Zone.RING, s.zone);
    }

    @Test
    public void okOnAnAppLaunchesItAndOnTheLibraryTileOpensTheLibrary() {
        HabeshaState.Action launch = s.ok();
        assertEquals("a", launch.launchPackage);
        s.arrow(-1, 0);
        HabeshaState.Action none = s.ok();
        assertNull(none.launchPackage);
        assertEquals(HabeshaState.Screen.LIBRARY, s.screen);
    }

    @Test
    public void topButtons() {
        s.arrow(0, -1);
        s.ok(); // Apps
        assertEquals(HabeshaState.Screen.LIBRARY, s.screen);
        s.goHome();
        s.arrow(0, -1);
        s.arrow(1, 0);
        s.ok(); // Wi-Fi
        assertEquals(HabeshaState.Overlay.WIFI, s.overlay);
        s.arrow(0, 1);
        assertEquals("wifi", s.ok().system);
        assertEquals(HabeshaState.Overlay.NONE, s.overlay);
        s.arrow(1, 0);
        assertEquals("settings", s.ok().system);
    }

    @Test
    public void wifiPanelStaysWithinItsRows() {
        s.arrow(0, -1);
        s.arrow(1, 0);
        s.ok();
        for (int i = 0; i < 5; i++) s.arrow(0, 1);
        assertEquals(1, s.overlayIndex);
        for (int i = 0; i < 5; i++) s.arrow(0, -1);
        assertEquals(0, s.overlayIndex);
        s.back();
        assertEquals(HabeshaState.Overlay.NONE, s.overlay);
    }

    @Test
    public void holdOpensTheMenuOnlyOnAnApp() {
        s.hold();
        assertTrue(s.menuOpen);
        s.back();
        s.arrow(-1, 0); // library tile
        s.hold();
        assertFalse(s.menuOpen);
        s.goHome();
        s.arrow(0, -1);
        s.hold();
        assertFalse(s.menuOpen);
    }

    @Test
    public void moveCarriesAnAppAroundTheRingAndSavesEachStep() {
        s.hold();
        s.ok(); // Move
        assertTrue(s.moving);
        s.arrow(1, 0);
        s.arrow(1, 0);
        assertEquals(ids("b", "c", "a", "d", "e"), s.apps());
        assertEquals(2, s.focus);
        assertEquals(ids("b", "c", "a", "d", "e"), savedApps);
        s.arrow(-1, 0);
        assertEquals(ids("b", "a", "c", "d", "e"), s.apps());
        s.ok(); // put it down
        assertFalse(s.moving);
        assertEquals(3, saves);
    }

    @Test
    public void movingWrapsPastTheEndWithoutTheLibraryTile() {
        s.hold();
        s.ok();
        s.arrow(-1, 0);
        assertEquals(ids("e", "b", "c", "d", "a"), s.apps());
        assertEquals(4, s.focus);
    }

    @Test
    public void backPutsAMovedAppDownWithoutUndoing() {
        s.hold();
        s.ok();
        s.arrow(1, 0);
        s.back();
        assertFalse(s.moving);
        assertEquals(ids("b", "a", "c", "d", "e"), s.apps());
    }

    @Test
    public void removeThenAddFromTheLibrary() {
        s.arrow(1, 0); // focus b
        s.hold();
        s.arrow(0, 1); // Remove
        s.ok();
        assertEquals(ids("a", "c", "d", "e"), s.apps());
        assertEquals(ids("b"), s.removed());
        assertEquals("removed", s.toast.amKey);
        assertEquals("b", s.toast.appId);
        assertEquals(ids("a", "c", "d", "e"), savedApps);
        assertEquals(ids("b"), savedRemoved);

        s.openLibrary();
        s.arrow(1, 0); // b is second in library order
        assertEquals(1, s.libraryIndex);
        s.ok();
        assertEquals(ids("a", "c", "d", "e", "b"), s.apps());
        assertTrue(s.removed().isEmpty());
        assertEquals("added", s.toast.amKey);
    }

    @Test
    public void addingAnAppAlreadyOnTheRingJustSaysSo() {
        s.openLibrary();
        s.ok();
        assertEquals("onHome", s.toast.amKey);
        assertEquals(ids("a", "b", "c", "d", "e"), s.apps());
    }

    @Test
    public void removingTheLastFocusedAppKeepsFocusInRange() {
        for (int i = 0; i < 4; i++) s.arrow(1, 0); // focus e, the last app
        s.hold();
        s.arrow(0, 1);
        s.ok();
        assertEquals(ids("a", "b", "c", "d"), s.apps());
        assertEquals("__lib", s.focusedId()); // focus now sits on the library tile
        assertTrue(s.focus <= s.apps().size());
    }

    @Test
    public void menuCancelAndBack() {
        s.hold();
        s.arrow(0, 1);
        s.arrow(0, 1);
        s.arrow(0, 1);
        assertEquals(HabeshaState.MENU_CANCEL, s.menuIndex);
        s.ok();
        assertFalse(s.menuOpen);
        assertEquals(ids("a", "b", "c", "d", "e"), s.apps());
    }

    @Test
    public void libraryGridNavigation() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 13; i++) many.add("p" + i);
        s.setApps(many, many, Collections.<String>emptyList());
        s.openLibrary();
        s.arrow(-1, 0);
        assertEquals(0, s.libraryIndex);
        s.arrow(0, -1);
        assertEquals(0, s.libraryIndex);
        for (int i = 0; i < 9; i++) s.arrow(1, 0);
        assertEquals(5, s.libraryIndex); // stops at the right edge
        s.arrow(0, 1);
        assertEquals(11, s.libraryIndex);
        s.arrow(0, 1);
        assertEquals(12, s.libraryIndex); // short last row: jumps to the last tile
        s.arrow(0, 1);
        assertEquals(12, s.libraryIndex);
        s.arrow(1, 0);
        assertEquals(12, s.libraryIndex);
        s.arrow(0, -1);
        assertEquals(6, s.libraryIndex);
    }

    @Test
    public void idleOnlyOnAPlainHome() {
        assertTrue(s.idleForSaver());
        s.hold();
        assertFalse(s.idleForSaver());
        s.back();
        s.startSaver();
        assertEquals(HabeshaState.Screen.SAVER, s.screen);
        s.goHome();
        assertTrue(s.idleForSaver());
    }

    @Test
    public void emptyRingStillHasTheLibraryTile() {
        s.setApps(Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
        assertEquals(ids("__lib"), s.ring());
        s.hold();
        assertFalse(s.menuOpen);
        s.arrow(1, 0);
        assertEquals("__lib", s.focusedId());
    }
}
