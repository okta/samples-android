package com.okta.android.samples.browser_sign_in.rxClient;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class EmptyActivityLifeCycle implements Application.ActivityLifecycleCallbacks {
    public EmptyActivityLifeCycle() {
    }

    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    public void onActivityStarted(Activity activity) {
    }

    public void onActivityResumed(Activity activity) {
    }

    public void onActivityPaused(Activity activity) {
    }

    public void onActivityStopped(Activity activity) {
    }

    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    public void onActivityDestroyed(Activity activity) {
    }
}