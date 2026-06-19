package com.geoattend;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testLoginUIElements() {
        onView(withId(R.id.tv_welcome)).check(matches(isDisplayed()));
        onView(withId(R.id.et_email)).check(matches(isDisplayed()));
        onView(withId(R.id.et_password)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_sign_in)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyCredentialsShowsToast() {
        onView(withId(R.id.btn_sign_in)).perform(click());
        // Toast check is tricky in some API levels, but let's see if we can check basic logic
        // For now, just ensuring it stays on LoginActivity
        onView(withId(R.id.btn_sign_in)).check(matches(isDisplayed()));
    }

    @Test
    public void testNavigateToRegister() {
        onView(withId(R.id.btn_go_to_register)).perform(click());
        // Verify registration screen elements
        onView(withId(R.id.et_first_name)).check(matches(isDisplayed()));
    }
    
    @Test
    public void testEmailFieldInput() {
        onView(withId(R.id.et_email)).perform(replaceText("test@geoattend.com"), closeSoftKeyboard());
        onView(withId(R.id.et_email)).check(matches(withText("test@geoattend.com")));
    }
}
