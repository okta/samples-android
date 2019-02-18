package com.okta.android.samples.custom_sign_in.base;

import com.okta.authn.sdk.client.AuthenticationClient;

public interface IOktaAuthenticationClientProvider {
    AuthenticationClient provideAuthenticationClient();
}
