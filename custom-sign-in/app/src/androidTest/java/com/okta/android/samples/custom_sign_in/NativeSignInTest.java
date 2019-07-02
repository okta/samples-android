package com.okta.android.samples.custom_sign_in;


import android.os.Build;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assume.assumeTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NativeSignInTest {

    private static final String TAG = NativeSignInTest.class.getSimpleName();
    private UiDevice mDevice;


    public static final int TRANSITION_TIMEOUT = 2000;
    private static final int NETWORK_TIMEOUT = 5000;

    private static final String ID_PROGRESS_BAR =
            "com.okta.android.samples.browser_sign_in:id/progress_horizontal";
    private static final String ID_CLEAR_BTN = "com.okta.oidc.example:id/clear_data_btn";

    @Rule
    public ActivityTestRule<StartActivity> activityBrowserRule =
            new ActivityTestRule<>(StartActivity.class);

    @Before
    public void setUp() {
        mDevice =  UiDevice.getInstance(getInstrumentation());
    }

    private UiObject getProgressBar() {
        return mDevice.findObject(new UiSelector().resourceId(ID_PROGRESS_BAR));
    }

    private UiObject getClearButton() {
        return mDevice.findObject(new UiSelector().resourceId(ID_CLEAR_BTN));
    }

    @Test
    public void test1_signIn() {
        assumeTrue(
                "Can only run on API Level 24 or later because AuthenticationAPI " +
                        "requires Java 8",
                Build.VERSION.SDK_INT >= 24
        );
        onView(withId(R.id.native_sing_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data_btn)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data_btn)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.native_sing_in)).perform(click());

        onView(withId(R.id.login_edittext)).check(matches(isDisplayed()));
        onView(withId(R.id.login_edittext)).perform(click(), replaceText(BuildConfig.USERNAME));

        onView(withId(R.id.password_edittext)).check(matches(isDisplayed()));
        onView(withId(R.id.password_edittext)).perform(click(), replaceText(BuildConfig.PASSWORD));

        onView(withId(R.id.sign_in_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_btn)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        //check if get profile is visible
        getClearButton().waitForExists(TRANSITION_TIMEOUT);
        onView(withId(R.id.clear_data_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.smartlock_ebable)).check(matches(isNotChecked()));
    }
}
