package com.okta.android.samples.custom_sign_in.util;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;

public class KeyboardUtil {
    public static void hideSoftKeyboard(Activity activity) {
        if(activity == null)
            return;
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if(activity.getCurrentFocus() != null && activity.getCurrentFocus().getWindowToken() != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
}
