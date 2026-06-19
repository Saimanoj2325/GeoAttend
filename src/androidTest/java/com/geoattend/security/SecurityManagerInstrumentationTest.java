package com.geoattend.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.location.Location;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.geoattend.utils.SecurityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SecurityManagerInstrumentationTest {

    private Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testMockRiskScore_nullLocation() {
        int score = SecurityManager.getMockRiskScore(null, context);
        assertEquals(0, score);
    }

    @Test
    public void testMockRiskScore_realLocation() {
        Location location = new Location("gps");
        location.setLatitude(12.9716);
        location.setLongitude(77.5946);
        
        int score = SecurityManager.getMockRiskScore(location, context);
        // Score should be 0 if developer options are off and it's not a mock provider
        // But we don't know the device state, so we just check it doesn't crash
        assertTrue(score >= 0);
    }

    @Test
    public void testIsUsbDebuggingEnabled() {
        // This will depend on the device running the test
        SecurityManager.isUsbDebuggingEnabled(context);
    }

    @Test
    public void testIntegrityVerdict() {
        String verdict = SecurityManager.getIntegrityVerdict();
        assertNotNull(verdict);
    }

    private void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError();
    }
}
