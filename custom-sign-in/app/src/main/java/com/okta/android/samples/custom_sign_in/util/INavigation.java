package com.okta.android.samples.custom_sign_in.util;

import android.support.v4.app.Fragment;

public interface INavigation {
    void close();
    void present(Fragment fragment);
    void push(Fragment fragment);
}
