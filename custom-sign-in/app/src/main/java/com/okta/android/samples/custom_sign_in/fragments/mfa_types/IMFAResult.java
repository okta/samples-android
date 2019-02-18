package com.okta.android.samples.custom_sign_in.fragments.mfa_types;

public interface IMFAResult {
    void onSuccess(String sessionToken);
}
