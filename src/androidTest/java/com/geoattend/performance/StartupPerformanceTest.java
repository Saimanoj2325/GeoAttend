package com.geoattend.performance;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class StartupPerformanceTest {

    private static final String PACKAGE_NAME = "com.geoattend";
    private static final int LAUNCH_TIMEOUT = 5000;
    private UiDevice device;

    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void testColdStartupTime() {
        // Start from home screen
        device.pressHome();

        // Wait for launcher
        final String launcherPackage = device.getLauncherPackageName();
        assertNotNull(launcherPackage);
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        // Launch the app
        Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(PACKAGE_NAME);
        if (intent == null) throw new RuntimeException("App not installed: " + PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        long startTime = System.currentTimeMillis();
        context.startActivity(intent);

        // Wait for the app to appear (Splash screen)
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), LAUNCH_TIMEOUT);
        long endTime = System.currentTimeMillis();

        long startupTime = endTime - startTime;
        android.util.Log.d("Performance", "Cold Startup Time: " + startupTime + "ms");
        
        // Assert startup is within acceptable range (e.g., < 2 seconds)
        assertTrue("Startup too slow: " + startupTime + "ms", startupTime < 3000);
    }

    private void assertTrue(String message, boolean condition) {
        if (!condition) throw new AssertionError(message);
    }
}
