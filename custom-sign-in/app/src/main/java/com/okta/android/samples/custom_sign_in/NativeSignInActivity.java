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
package com.okta.android.samples.custom_sign_in;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.fragment.app.Fragment;

import com.okta.android.samples.custom_sign_in.base.ContainerActivity;
import com.okta.android.samples.custom_sign_in.base.IOktaAppAuthClientProvider;
import com.okta.android.samples.custom_sign_in.fragments.NativeSignInFragment;
import com.okta.android.samples.custom_sign_in.fragments.NativeSignInWithMFAFragment;
import com.okta.android.samples.custom_sign_in.util.PreferenceRepository;
import com.okta.android.samples.custom_sign_in.util.SmartLockHelper;
import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.storage.security.EncryptionManager;

public class NativeSignInActivity extends ContainerActivity implements IOktaAppAuthClientProvider {
    private String TAG = "NativeSignInActivity";

    private static String MODE_KEY = "MODE_KEY";

    private AuthClient mAuth;
    private SessionClient mSessionClient;
    private PreferenceRepository mPreferenceRepository;
    private SmartLockHelper mSmartLockHelper;


    enum MODE {
        NATIVE_SIGN_IN,
        NATIVE_SIGN_IN_WITH_MFA,
        UNKNOWN
    }


    public static Intent createNativeSignIn(Context context) {
        Intent intent = new Intent(context, NativeSignInActivity.class);
        intent.putExtra(MODE_KEY, NativeSignInActivity.MODE.NATIVE_SIGN_IN.ordinal());
        return intent;
    }

    public static Intent createNativeSignInWithMFA(Context context) {
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

        mPreferenceRepository = ServiceLocator.providePreferenceRepository(this);
        mSmartLockHelper = ServiceLocator.provideSmartLockHelper();

        init();
    }

    private void init() {
        mAuth = ServiceLocator.provideAuthClient(this);
        mSessionClient = mAuth.getSessionClient();

        if (mSessionClient.isAuthenticated()) {
            if (mPreferenceRepository.isEnabledSmartLock()) {
                if (!SmartLockHelper.isKeyguardSecure(this)) {
                    clearStorage();
                } else {
                    EncryptionManager encryptionManager = ServiceLocator.provideEncryptionManager(NativeSignInActivity.this);
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
            showLoginForm();
        }
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
    public void clearStorage() {
        mSessionClient.clear();
        ServiceLocator.provideEncryptionManager(this).removeKeys();
        try {
            DefaultEncryptionManager simpleEncryptionManager = ServiceLocator.createSimpleEncryptionManager(this);
            ServiceLocator.setEncryptionManager(simpleEncryptionManager);
            mAuth.migrateTo(simpleEncryptionManager);
            mPreferenceRepository.enableSmartLock(false);
        } catch (Exception e) {
            showMessage(e.getMessage());
        }
    }

    @Override
    public AuthClient provideOktaAppAuthClient() {
        return mAuth;
    }
}
