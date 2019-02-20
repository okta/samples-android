package com.okta.android.samples.custom_sign_in.util;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class NavigationHelper implements INavigation {
    private Activity activity;
    private FragmentManager manager;
    private int container;

    public NavigationHelper(Activity activity, FragmentManager fragmentManager, int container) {
        this.activity = activity;
        this.manager = fragmentManager;
        this.container = container;
    }

    @Override
    public void close() {

        this.activity.finish();
    }

    @Override
    public void present(Fragment fragment) {
        this.manager.beginTransaction()
                .replace(container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void push(Fragment fragment) {
        this.manager.beginTransaction()
                .add(container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
