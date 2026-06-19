package com.geoattend;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.os.RemoteException;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EdgeCaseTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testOrientationChangeDuringLogin() throws RemoteException {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        
        // Initial state
        onView(withId(R.id.et_email)).check(matches(isDisplayed()));
        
        // Rotate device
        device.setOrientationLeft();
        
        // Check if views are still there and app didn't crash
        onView(withId(R.id.et_email)).check(matches(isDisplayed()));
        onView(withId(R.id.et_password)).check(matches(isDisplayed()));
        
        // Rotate back
        device.setOrientationNatural();
        onView(withId(R.id.et_email)).check(matches(isDisplayed()));
    }
}
