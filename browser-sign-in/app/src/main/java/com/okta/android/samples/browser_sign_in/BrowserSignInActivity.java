/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.okta.android.samples.browser_sign_in;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.okta.android.samples.browser_sign_in.util.FingerprintDialog;
import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.android.samples.browser_sign_in.util.PreferenceRepository;
import com.okta.android.samples.browser_sign_in.util.SmartLockHelper;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.storage.security.FingerprintUtils;
import com.okta.oidc.storage.security.SimpleBaseEncryptionManager;
import com.okta.oidc.util.AuthorizationException;



public class BrowserSignInActivity extends AppCompatActivity {
    private String TAG = "BrowserSignIn";
    private static final String EXTRA_FAILED = "failed";
    private OktaProgressDialog oktaProgressDialog;

    private WebAuthClient mWebAuth;
    private SessionClient mSessionClient;
    private PreferenceRepository mPreferenceRepository;
    private SmartLockHelper mSmartLockHelper;
    private boolean signInWithFingerprintMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_sign_in);

        oktaProgressDialog = ServiceLocator.provideOktaProgressDialog(this);

        mPreferenceRepository = ServiceLocator.providePreferenceRepository(this);
        mSmartLockHelper = ServiceLocator.provideSmartLockHelper();
        ((Button) findViewById(R.id.browser_sign_in)).setOnClickListener(v -> signIn());

        init();

        Button signInWithFingerprint = findViewById(R.id.fingerprint_browser_sign_in);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            signInWithFingerprint.setEnabled(true);
            signInWithFingerprint.setOnClickListener(v -> signInWithFingerprint());
        } else {
            signInWithFingerprint.setEnabled(false);
        }

        if (mSessionClient.isAuthenticated()) {
            if (mPreferenceRepository.isEnabledSmartLock()) {
                mSmartLockHelper.showSmartLockChooseDialog(this, new FingerprintDialog.FingerprintDialogCallbacks() {
                    @Override
                    public void onFingerprintSuccess(int purpose, FingerprintManager.CryptoObject cryptoObject) {
                        ServiceLocator.provideEncryptionManager(BrowserSignInActivity.this).recreateCipher();
                        showUserInfo(true);
                    }

                    @Override
                    public void onFingerprintCancel() {
                        showMessage("Failed to fingerprint");
                        clearStorage();
                    }
                });
            } else {
                showUserInfo(false);
            }
        } else {
            if (mPreferenceRepository.isEnabledSmartLock()) {
                clearStorage();
            }
        }
    }

    private void clearStorage() {
        try {
            mSessionClient.clear();
            SimpleBaseEncryptionManager simpleEncryptionManager = ServiceLocator.createSimpleEncryptionManager(this);
            ServiceLocator.setEncryptionManager(simpleEncryptionManager);
            mWebAuth.migrateTo(simpleEncryptionManager);
            mPreferenceRepository.enableSmartLock(false);
        } catch (AuthorizationException exception) {
            // Should recreate
            throw new RuntimeException("Need restart the app");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebAuth.isInProgress()) {
            oktaProgressDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        mSmartLockHelper.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void init() {
        mWebAuth = ServiceLocator.provideWebAuthClient(this);
        mSessionClient = mWebAuth.getSessionClient();

        setupCallback();
    }

    void setupCallback() {
        ResultCallback<AuthorizationStatus, AuthorizationException> callback =
                new ResultCallback<AuthorizationStatus, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull AuthorizationStatus status) {
                        Log.d(TAG, "AUTHORIZED");

                        if (status == AuthorizationStatus.AUTHORIZED) {
                            oktaProgressDialog.hide();

                            Log.i(TAG, "User is already authenticated, proceeding " +
                                    "to token activity");
                            showUserInfo(signInWithFingerprintMode);
                        } else if (status == AuthorizationStatus.SIGNED_OUT) {
                            displayAuth();
                        }
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "CANCELED!");
                        oktaProgressDialog.hide();
                        showMessage(getString(R.string.auth_canceled));
                    }

                    @Override
                    public void onError(@Nullable String msg, AuthorizationException error) {
                        oktaProgressDialog.hide();
                        showMessage(getString(R.string.init_error)
                                + ":"
                                + error.error
                                + ":"
                                + error.errorDescription);
                    }
                };

        mWebAuth.registerCallback(callback, this);
    }

    @TargetApi(23)
    private void signInWithFingerprint() {
        FingerprintUtils.SensorState sensorState = FingerprintUtils.checkSensorState(this);
        switch (sensorState) {
            case READY:
                signInWithFingerprintMode = true;
                signIn();
                break;
            case NOT_BLOCKED:
                showMessage(getString(R.string.setup_lockscreen_info_msg));
                break;
            case NOT_SUPPORTED:
                showMessage(getString(R.string.hardware_not_support_fingerprint));
                break;
            case NO_FINGERPRINTS:
                showMessage(getString(R.string.fingerprint_not_enrolled));
                break;
        }
    }


    private void signIn() {
        mWebAuth.signIn(this, null);
    }

    private void displayAuth() {
        findViewById(R.id.auth_container).setVisibility(View.VISIBLE);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showUserInfo(boolean isRequireEnableFingerprint) {
        startActivity(UserInfoActivity.createIntent(this, isRequireEnableFingerprint));
        finish();
    }
}
