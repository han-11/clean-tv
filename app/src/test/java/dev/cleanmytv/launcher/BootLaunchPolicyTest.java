package dev.cleanmytv.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class BootLaunchPolicyTest {
    @Test public void newBootMayLaunch() {
        assertTrue(BootLaunchPolicy.eligible(true, 12, 11, 30000));
    }
    @Test public void sameBootNeverLaunchesAgain() {
        assertFalse(BootLaunchPolicy.eligible(true, 12, 12, 30000));
    }
    @Test public void optOutPreventsLaunch() {
        assertFalse(BootLaunchPolicy.eligible(false, 12, 11, 30000));
    }
    @Test public void unknownBootDoesNotLaunch() {
        assertFalse(BootLaunchPolicy.eligible(true, -1, 11, 30000));
    }
    @Test public void lateReconnectDoesNotTakeOverScreen() {
        assertFalse(BootLaunchPolicy.eligible(true, 12, 11, BootLaunchPolicy.WINDOW_MS));
        assertFalse(BootLaunchPolicy.eligible(true, 12, 11, 3600000));
    }
    @Test public void invalidUptimeDoesNotLaunch() {
        assertFalse(BootLaunchPolicy.eligible(true, 12, 11, -1));
    }
}
