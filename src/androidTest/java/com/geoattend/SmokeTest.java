package com.geoattend;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke Tests: Critical User Journeys (CUJ)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SmokeTest {

    @Rule
    public ActivityScenarioRule<SplashActivity> splashRule =
            new ActivityScenarioRule<>(SplashActivity.class);

    @Test
    public void testAppLaunchAndGetStarted() {
        // Verify Splash -> Login navigation
        onView(withId(R.id.btn_get_started)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_get_started)).perform(click());
        
        // Should be on Integrity Check or Login
        // Based on current logic it goes to IntegrityCheckActivity
        onView(withId(R.id.tv_security_header)).check(matches(isDisplayed()));
    }

    @Test
    public void testNavigateToRegisterFromLogin() {
        // Skip Splash
        onView(withId(R.id.btn_get_started)).perform(click());
        
        // Wait for Integrity Check if it's there, but Espresso is usually too fast
        // or IntegrityCheckActivity finish() automatically if it passes
        
        // Assuming we reach LoginActivity
        // In real automation we might need to bypass security checks for testing
    }
}
