package com.geoattend.security;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import com.geoattend.utils.SecurityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MockLocationTest {

    private UiDevice device;
    private Context context;

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testMockLocationDetectionLogic() {
        // Create a simulated mock location
        android.location.Location mockLoc = new android.location.Location("gps");
        mockLoc.setLatitude(12.9716);
        mockLoc.setLongitude(77.5946);
        
        // Android 11+ way to mark as mock (for testing logic)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            mockLoc.setMock(true);
        }

        int riskScore = SecurityManager.getMockRiskScore(mockLoc, context);
        
        // If it's a mock location, risk should be at least 50
        assertTrue("Mock location not detected", riskScore >= 50);
    }
}
