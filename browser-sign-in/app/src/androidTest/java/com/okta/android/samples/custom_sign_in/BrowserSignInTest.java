package com.okta.android.samples.custom_sign_in;


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
import com.okta.oidc.AuthenticationPayload;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BrowserSignInTest {

    private static final String TAG = BrowserSignInTest.class.getSimpleName();
    private UiDevice mDevice;

    private static final String CHROME_STABLE = "com.android.chrome";
    private static final String SAMPLE_APP = "com.okta.android.samples.browser_sign_in";
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
    public ActivityTestRule<BrowserSignInActivity> activityBrowserRule =
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
        }
        UiObject password = mDevice.findObject(selector.resourceId(ID_PASSWORD));
        password.setText(PASSWORD);
        UiObject signIn = mDevice.findObject(selector.resourceId(ID_SUBMIT));
        signIn.click();
        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
    }

    private void signInIfNotAlready() {
        onView(withId(R.id.clear_data_btn)).withFailureHandler((error, viewMatcher) -> {
            test3_signInWithSession();
        }).check(matches(isDisplayed()));
    }

    private void unlockKeystore() throws UiObjectNotFoundException {
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
    }

    @Test
    public void test1_signInNoSession() throws UiObjectNotFoundException {
        onView(withId(R.id.browser_sign_in_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.browser_sign_in_btn)).perform(click());

        customTabInteraction(true);
        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.clear_data_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.smartlock_enable)).check(matches(isNotChecked()));

    }

    @Test
    public void test2_clearData() {
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        signInIfNotAlready();

        onView(withId(R.id.clear_data_btn))
                .perform(click());

        onView(withId(R.id.browser_sign_in_btn)).check(matches(isDisplayed()));
    }

    @Test
    public void test3_signInWithSession() {
        onView(withId(R.id.container)).perform(swipeUp());
        onView(withId(R.id.browser_sign_in_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.browser_sign_in_btn)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);
        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.clear_data_btn)).check(matches(isDisplayed()));

    }


    @Test
    public void test4_refreshToken() {
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        signInIfNotAlready();

        onView(withId(R.id.view_token_btn)).perform(click());
        onView(withId(R.id.refresh_token)).check(matches(isDisplayed()));
        onView(withId(R.id.refresh_token)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withText(R.string.token_refreshed_successfully)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }

    @Test
    public void test5_revokeTokens() {
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        signInIfNotAlready();
        onView(withId(R.id.revoke_tokens_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.revoke_tokens_btn)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withText(R.string.tokens_revoke_success)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }

    @Test
    public void test6_signOutOfOkta() {
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        signInIfNotAlready();
        onView(withId(R.id.logout_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.logout_btn)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);
        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);

        onView(withText(R.string.sign_out_success)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }

    @Test
    public void testA_cancelSignIn() throws UiObjectNotFoundException {
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.clear_data_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.clear_data_btn)).perform(click());
        onView(withId(R.id.browser_sign_in_btn)).check(matches(isDisplayed()));

        onView(withId(R.id.browser_sign_in_btn)).perform(click());
        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        UiSelector selector = new UiSelector();
        UiObject closeBrowser = mDevice.findObject(selector.resourceId(ID_CLOSE_BROWSER));
        closeBrowser.click();

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        onView(withText(R.string.auth_canceled)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }

    // In some reason Chrome tabs doesn't redirect user to app, when provide additional parameters or login hint.
    // @Test
    public void testB_signInWithPayload() throws UiObjectNotFoundException {
        activityBrowserRule.getActivity().mPayload = new AuthenticationPayload.Builder()
                .setLoginHint("devex@okta.com")
                .addParameter("max_age", "5000")
                .build();

        onView(withId(R.id.browser_sign_in_btn)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data_btn)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data_btn)).perform(click());
        }).check(matches(isDisplayed()));

        onView(withId(R.id.browser_sign_in_btn)).perform(click());

        customTabInteraction(false);

        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        //check if get profile is visible
        onView(withId(R.id.clear_data_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.smartlock_enable)).check(matches(isNotChecked()));
    }

    @Test
    public void testC_enableSmartLock() throws UiObjectNotFoundException {
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.browser_sign_in_btn)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data_btn)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data_btn)).perform(click());
        }).check(matches(isDisplayed()));

        onView(withId(R.id.browser_sign_in_btn)).perform(click());

        customTabInteraction(true);

        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        //check if get profile is visible
        onView(withId(R.id.smartlock_enable)).check(matches(isNotChecked()));
        onView(withId(R.id.smartlock_enable)).perform(click());

        onView(withText("Yes"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());

        unlockKeystore();

        onView(withId(R.id.smartlock_enable)).check(matches(isChecked()));
        onView(withId(R.id.smartlock_enable)).check(matches(not(isEnabled())));
    }

    @Test
    public void testC_refreshTokenAfterExpiredSmartLockValidityTime() throws UiObjectNotFoundException, InterruptedException {
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        unlockKeystore();

        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        //check if get profile is visible
        onView(withId(R.id.smartlock_enable)).check(matches(isChecked()));
        onView(withId(R.id.smartlock_enable)).check(matches(not(isEnabled())));

        Thread.sleep(15000);

        onView(withId(R.id.view_token_btn)).perform(click());
        onView(withId(R.id.refresh_token)).check(matches(isDisplayed()));
        onView(withId(R.id.refresh_token)).perform(click());

        unlockKeystore();

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withText(R.string.token_refreshed_successfully)).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }

}
