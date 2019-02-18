package com.okta.android.samples.custom_sign_in.base;

import com.okta.appauth.android.OktaAppAuth;

public interface IOktaAppAuthClientProvider {
    OktaAppAuth provideOktaAppAuthClient();
}
