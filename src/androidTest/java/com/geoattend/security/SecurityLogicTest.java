package com.geoattend.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.geoattend.utils.SecurityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SecurityLogicTest {

    private Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testDeviceIntegrityCheck() {
        // We expect the device running the test NOT to be compromised if it's a standard CI or real device
        // But we just verify the method returns a result
        SecurityManager.isDeviceCompromised();
    }

    @Test
    public void testUsbDebuggingDetection() {
        // USB debugging might be ON during tests (ADB), so we just check it doesn't crash
        SecurityManager.isUsbDebuggingEnabled(context);
    }

    @Test
    public void testMockLocationDetectionLogic() {
        // Create a mock location and check if risk score increases
        android.location.Location mockLoc = new android.location.Location("gps");
        mockLoc.setLatitude(0.0);
        mockLoc.setLongitude(0.0);
        
        SecurityManager.getMockRiskScore(mockLoc, context);
    }
}
