package com.okta.android.samples.custom_sign_in;


import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.okta.android.samples.browser_sign_in.BrowserSignInActivity;
import com.okta.android.samples.browser_sign_in.BuildConfig;
import com.okta.android.samples.browser_sign_in.R;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SmartLockTest {

    private UiDevice mDevice;

    private static final String CHROME_STABLE = "com.android.chrome";
    private static final String SAMPLE_APP = "com.okta.oidc.example";
    private static final String SETTINGS_APP = "com.android.settings";

    private static final int TRANSITION_TIMEOUT = 2000;
    private static final int NETWORK_TIMEOUT = 5000;

    //web page resource ids
    private static final String ID_USERNAME = "okta-signin-username";
    private static final String ID_PASSWORD = "okta-signin-password";
    private static final String ID_SUBMIT = "okta-signin-submit";
    private static final String ID_NO_THANKS = "com.android.chrome:id/negative_button";
    private static final String ID_ACCEPT = "com.android.chrome:id/terms_accept";
    private static final String ID_CLOSE_BROWSER = "com.android.chrome:id/close_button";

    private static final String ID_PROGRESS_BAR =
            "com.okta.android.samples.browser_sign_in:id/progress_horizontal";

    private static String PASSWORD = BuildConfig.PASSWORD;
    private static String USERNAME = BuildConfig.USERNAME;
    private static String PINCODE = BuildConfig.PINCODE;

    @Rule
    public ActivityTestRule<BrowserSignInActivity> activityTestRule =
            new ActivityTestRule<>(BrowserSignInActivity.class);

    @Before
    public void setUp() {
        mDevice =  UiDevice.getInstance(getInstrumentation());
    }

    private void acceptChromePrivacyOption() throws UiObjectNotFoundException {
        UiSelector selector = new UiSelector();
        UiObject accept = mDevice.findObject(selector.resourceId(ID_ACCEPT));
        accept.waitForExists(TRANSITION_TIMEOUT);
        if (accept.exists()) {
            accept.click();
        }

        UiObject noThanks = mDevice.findObject(selector.resourceId(ID_NO_THANKS));
        noThanks.waitForExists(TRANSITION_TIMEOUT);
        if (noThanks.exists()) {
            noThanks.click();
        }
    }

    private UiObject getProgressBar() {
        return mDevice.findObject(new UiSelector().resourceId(ID_PROGRESS_BAR));
    }


    private void customTabInteraction(boolean enterUserName) throws UiObjectNotFoundException {
        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);
        acceptChromePrivacyOption();
        UiSelector selector = new UiSelector();
        if (enterUserName) {
            UiObject username = mDevice.findObject(selector.resourceId(ID_USERNAME));
            username.setText(USERNAME);
            UiObject password = mDevice.findObject(selector.resourceId(ID_PASSWORD));
            password.setText(PASSWORD);
        }
        UiObject signIn = mDevice.findObject(selector.resourceId(ID_SUBMIT));
        signIn.click();
        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
    }

    @Test
    public void test1_verifySmartLockToggle() throws UiObjectNotFoundException, IOException {
        onView(withId(R.id.browser_sign_in)).perform(click());
        customTabInteraction(true);
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        onView(withId(R.id.smartlock_ebable))
                .perform(click());

        onView(withText("Yes"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withText("Unlock Screen"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());

        mDevice.wait(Until.findObject(By.pkg(SETTINGS_APP)), TRANSITION_TIMEOUT);
        UiSelector selector = new UiSelector();
        UiObject passwordText = mDevice.findObject(selector.focused(true));
        passwordText.setText(PINCODE);
        mDevice.pressEnter();
        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        waitFor(onView(withId(R.id.email_title)), matches(withText("devex@okta.com")), 1000);
    }

    private void waitFor(ViewInteraction viewInteraction, ViewAssertion viewAssertion, long timeout) {

        PollingTimeoutIdler idler = new PollingTimeoutIdler(viewInteraction, viewAssertion, timeout);
        IdlingRegistry.getInstance().register(idler);

        viewInteraction.check(viewAssertion);

        IdlingRegistry.getInstance().unregister(idler);
    }


}
