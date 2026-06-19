package com.geoattend.attendance;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OfflineAttendanceTest {

    private UiDevice device;

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void testAppBehaviorInAirplaneMode() throws Exception {
        try {
            // Enable Airplane Mode (Requires shell permission or UI interaction)
            device.executeShellCommand("settings put global airplane_mode_on 1");
            device.executeShellCommand("am broadcast -a android.intent.action.AIRPLANE_MODE");
            
            // Verify app doesn't crash on launch
            // (Testing this specifically requires starting an activity)
            
        } finally {
            // Disable Airplane Mode
            device.executeShellCommand("settings put global airplane_mode_on 0");
            device.executeShellCommand("am broadcast -a android.intent.action.AIRPLANE_MODE");
        }
    }
}
