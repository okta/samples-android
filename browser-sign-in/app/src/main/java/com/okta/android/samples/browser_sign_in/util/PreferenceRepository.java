package com.okta.android.samples.browser_sign_in.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceRepository {
    private final String FILENAME = "PREFERENCES_FILE";
    private final String SMARTLOCK_KEY = "SMARTLOCK_KEY";
    SharedPreferences sharedPreferences;

    public PreferenceRepository(Context context) {
        this.sharedPreferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
    }

    public void enableSmartLock(boolean enable) {
        this.sharedPreferences.edit().putBoolean(SMARTLOCK_KEY, enable).apply();
    }

    public boolean isEnabledSmartLock() {
        return this.sharedPreferences.getBoolean(SMARTLOCK_KEY, false);
    }
}
