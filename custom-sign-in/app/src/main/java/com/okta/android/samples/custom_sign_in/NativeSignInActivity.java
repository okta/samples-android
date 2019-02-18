package com.okta.android.samples.custom_sign_in;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.okta.android.samples.custom_sign_in.base.ContainerActivity;
import com.okta.android.samples.custom_sign_in.base.IOktaAppAuthClientProvider;
import com.okta.android.samples.custom_sign_in.fragments.NativeSignInFragment;
import com.okta.android.samples.custom_sign_in.fragments.NativeSignInWithMFAFragment;
import com.okta.appauth.android.OktaAppAuth;

import net.openid.appauth.AuthorizationException;

public class NativeSignInActivity extends ContainerActivity implements IOktaAppAuthClientProvider {
    private String TAG = "NativeSignInActivity";

    private static String MODE_KEY = "MODE_KEY";
    private OktaAppAuth mOktaAuth;

    enum MODE {
        NATIVE_SIGN_IN,
        NATIVE_SIGN_IN_WITH_MFA,
        UNKNOWN
    }


    public static Intent createNativeSingIn(Context context) {
        Intent intent = new Intent(context, NativeSignInActivity.class);
        intent.putExtra(MODE_KEY, NativeSignInActivity.MODE.NATIVE_SIGN_IN.ordinal());
        return intent;
    }

    public static Intent createNativeSingInWithMFA(Context context) {
        Intent intent = new Intent(context, NativeSignInActivity.class);
        intent.putExtra(MODE_KEY, NativeSignInActivity.MODE.NATIVE_SIGN_IN_WITH_MFA.ordinal());
        return intent;
    }

    private Fragment getFragmentByModeId(int id, Intent intent){
        if(id == NativeSignInActivity.MODE.NATIVE_SIGN_IN.ordinal()) {
            return new NativeSignInFragment();
        } else if(id == NativeSignInActivity.MODE.NATIVE_SIGN_IN_WITH_MFA.ordinal()) {
            return new NativeSignInWithMFAFragment();
        } else {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
    }

    private void init() {
        show();

        mOktaAuth = OktaAppAuth.getInstance(getApplicationContext());

        mOktaAuth.init(
                getApplicationContext(),
                new OktaAppAuth.OktaAuthListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            hide();
                            if (mOktaAuth.isUserLoggedIn()) {

                                Log.i(TAG, "User is already authenticated, proceeding " +
                                        "to token activity");
                                showUserInfo();
                            } else {
                                Log.i(TAG, "Login activity setup finished");
                                showLoginForm();
                            }
                        });
                    }

                    @Override
                    public void onTokenFailure(@NonNull AuthorizationException ex) {
                        runOnUiThread(() -> {
                            hide();
                            showMessage(getString(R.string.init_error)
                                    + ":"
                                    + ex.errorDescription);
                        });
                    }
                },
                getResources().getColor(R.color.colorPrimary));
    }

    @MainThread
    private void showUserInfo() {
        startActivity(new Intent(this, UserInfoActivity.class));
        finish();
    }

    private void showLoginForm() {
        Fragment fragment = getFragmentByModeId(getIntent().getIntExtra(MODE_KEY, -1), getIntent());
        if(fragment != null) {
            this.navigation.present(fragment);
        }
    }

    @Override
    public OktaAppAuth provideOktaAppAuthClient() {
        return mOktaAuth;
    }
}
