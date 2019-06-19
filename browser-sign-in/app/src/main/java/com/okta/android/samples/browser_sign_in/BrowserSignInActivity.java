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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.android.samples.browser_sign_in.util.PreferenceRepository;
import com.okta.android.samples.browser_sign_in.util.SmartLockHelper;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;


public class BrowserSignInActivity extends AppCompatActivity {
    private String TAG = "BrowserSignIn";
    private OktaProgressDialog oktaProgressDialog;

    private WebAuthClient mWebAuth;
    private SessionClient mSessionClient;
    private PreferenceRepository mPreferenceRepository;
    private SmartLockHelper mSmartLockHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_sign_in);

        oktaProgressDialog = ServiceLocator.provideOktaProgressDialog(this);

        mPreferenceRepository = ServiceLocator.providePreferenceRepository(this);
        mSmartLockHelper = ServiceLocator.provideSmartLockHelper();
        findViewById(R.id.browser_sign_in).setOnClickListener(v -> signIn());

        init();

        if (mSessionClient.isAuthenticated()) {
            if (mPreferenceRepository.isEnabledSmartLock()) {
                if (!SmartLockHelper.isKeyguardSecure(this)) {
                    clearStorage();
                } else {
                    EncryptionManager encryptionManager = ServiceLocator.provideEncryptionManager(BrowserSignInActivity.this);
                    mSmartLockHelper.showSmartLockChooseDialog(this, new SmartLockHelper.FingerprintCallback(this, encryptionManager) {
                        @Override
                        protected void onSuccess() {
                            if (encryptionManager.isValidKeys()) {
                                showUserInfo();
                            } else {
                                clearStorage();
                            }
                        }

                        @Override
                        public void onFingerprintError(String error) {
                            super.onFingerprintError(error);
                            showMessage(error);
                            clearStorage();
                        }

                        @Override
                        public void onFingerprintCancel() {
                            super.onFingerprintCancel();
                            showMessage(getString(R.string.cancel));
                        }
                    }, encryptionManager.getCipher());
                }
            } else {
                showUserInfo();
            }
        } else {
            clearStorage();
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
                        if (status == AuthorizationStatus.AUTHORIZED) {
                            oktaProgressDialog.hide();
                            showUserInfo();
                        } else if (status == AuthorizationStatus.SIGNED_OUT) {
                            displayAuth();
                        }
                    }

                    @Override
                    public void onCancel() {
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
                        switch (error.code) {
                            case AuthorizationException.EncryptionErrors.KEYGUARD_AUTHENTICATION_ERROR:
                            case AuthorizationException.EncryptionErrors.ENCRYPT_ERROR:
                            case AuthorizationException.EncryptionErrors.DECRYPT_ERROR:
                                EncryptionManager encryptionManager = ServiceLocator.provideEncryptionManager(
                                        BrowserSignInActivity.this);
                                mSmartLockHelper.showSmartLockChooseDialog(
                                        BrowserSignInActivity.this,
                                        new SmartLockHelper.FingerprintCallback(
                                                BrowserSignInActivity.this,
                                                encryptionManager) {
                                            @Override
                                            protected void onSuccess() {
                                                signIn();
                                            }
                                        }, encryptionManager.getCipher());
                                break;
                            case AuthorizationException.EncryptionErrors.INVALID_KEYS_ERROR:
                                clearStorage();
                                break;
                        }
                    }
                };

        mWebAuth.registerCallback(callback, this);
    }

    private void signIn() {
        mWebAuth.signIn(this, null);
    }

    private void clearStorage() {
        mSessionClient.clear();
        ServiceLocator.provideEncryptionManager(this).removeKeys();
        try {
            DefaultEncryptionManager simpleEncryptionManager = ServiceLocator.createSimpleEncryptionManager(this);
            ServiceLocator.setEncryptionManager(simpleEncryptionManager);
            mWebAuth.migrateTo(simpleEncryptionManager);
            mPreferenceRepository.enableSmartLock(false);
        } catch (Exception e) {
            showMessage(e.getMessage());
        }
    }

    private void displayAuth() {
        findViewById(R.id.auth_container).setVisibility(View.VISIBLE);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showUserInfo() {
        startActivity(UserInfoActivity.createIntent(this));
        finish();
    }
}
