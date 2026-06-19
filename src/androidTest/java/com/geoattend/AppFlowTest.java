package com.geoattend;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.view.View;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppFlowTest {

    @Rule
    public ActivityScenarioRule<SplashActivity> activityRule =
            new ActivityScenarioRule<>(SplashActivity.class);

    @Test
    public void testSplashToLoginFlow() {
        waitFor(2500); // Wait for Splash animations
        onView(withId(R.id.tv_title)).check(matches(withText("GeoAttend")));
        onView(withId(R.id.btn_get_started)).perform(click());
        onView(withId(R.id.btn_sign_in)).check(matches(isDisplayed()));
    }

    @Test
    public void testRegistrationValidation() {
        waitFor(2500); // Wait for Splash animations
        // Go to Register
        onView(withId(R.id.btn_get_started)).perform(click());
        onView(withId(R.id.btn_go_to_register)).perform(click());

        // Fill fields
        onView(withId(R.id.et_first_name)).perform(replaceText("Test"), closeSoftKeyboard());
        onView(withId(R.id.et_last_name)).perform(replaceText("User"), closeSoftKeyboard());
        onView(withId(R.id.et_email)).perform(replaceText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.et_employee_id)).perform(replaceText("EMP-001"), closeSoftKeyboard());
        
        // Test scroll to find other fields
        onView(withId(R.id.et_password)).perform(scrollTo(), replaceText("password123"), closeSoftKeyboard());
        onView(withId(R.id.et_confirm_password)).perform(scrollTo(), replaceText("password123"), closeSoftKeyboard());

        // Check terms (Already visible outside ScrollView)
        onView(withId(R.id.cb_terms)).perform(click());

        // Verify button is displayed
        onView(withId(R.id.btn_create_account)).check(matches(isDisplayed()));
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
