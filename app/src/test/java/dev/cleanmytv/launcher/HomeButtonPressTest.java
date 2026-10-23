package dev.cleanmytv.launcher;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static dev.cleanmytv.launcher.HomeButtonPress.Result.*;

public final class HomeButtonPressTest {
    private final HomeButtonPress press = new HomeButtonPress();

    @Test public void tapOpensOnlyOnRelease() {
        assertEquals(CONSUME, press.handle(true, true, false, 0, 10, false));
        assertEquals(OPEN_HOME, press.handle(true, false, true, 0, 10, false));
        assertEquals(CONSUME, press.handle(true, false, true, 0, 10, false));
    }

    @Test public void otherKeysAndTheirRepeatsAlwaysPass() {
        for (int repeat = 0; repeat < 10; repeat++)
            assertEquals(PASS, press.handle(false, true, false, repeat, 10, false));
        assertEquals(PASS, press.handle(false, false, true, 0, 10, false));
    }

    @Test public void cancelledPressDoesNotOpenHome() {
        press.handle(true, true, false, 0, 10, false);
        assertEquals(CONSUME, press.handle(true, false, true, 0, 10, true));
    }

    @Test public void repeatsDoNotLaunchRepeatedly() {
        press.handle(true, true, false, 0, 10, false);
        for (int repeat = 1; repeat < 10; repeat++)
            assertEquals(CONSUME, press.handle(true, true, false, repeat, 10, false));
        assertEquals(OPEN_HOME, press.handle(true, false, true, 0, 10, false));
    }

    @Test public void missingOrMismatchedDownDoesNotLaunch() {
        assertEquals(CONSUME, press.handle(true, false, true, 0, 10, false));
        press.handle(true, true, false, 0, 10, false);
        assertEquals(CONSUME, press.handle(true, false, true, 0, 20, false));
    }

    @Test public void interruptDiscardsPendingPress() {
        press.handle(true, true, false, 0, 10, false);
        press.reset();
        assertEquals(CONSUME, press.handle(true, false, true, 0, 10, false));
    }

    @Test public void otherKeyDoesNotCancelPendingHomePress() {
        press.handle(true, true, false, 0, 10, false);
        assertEquals(PASS, press.handle(false, true, false, 0, 11, false));
        assertEquals(OPEN_HOME, press.handle(true, false, true, 0, 10, false));
    }
}
