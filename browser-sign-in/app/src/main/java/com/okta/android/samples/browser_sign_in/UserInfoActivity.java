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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.android.samples.browser_sign_in.util.PreferenceRepository;
import com.okta.android.samples.browser_sign_in.util.SmartLockHelper;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.storage.security.SmartLockBaseEncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

import static com.okta.oidc.util.AuthorizationException.EncryptionErrors;

public class UserInfoActivity extends AppCompatActivity {
    private final String TAG = "UserInfo";
    private WebAuthClient mWebAuth;
    private SessionClient mSessionClient;

    private OktaProgressDialog oktaProgressDialog;
    private SmartLockHelper mSmartLockHelper;
    private final AtomicReference<UserInfo> mUserInfoJson = new AtomicReference<>();

    private ConstraintLayout userInfoContainer;
    private ConstraintLayout tokensContainer;
    private Switch smartLockChecker;

    private static final String KEY_USER_INFO = "userInfo";

    private EncryptionManager mEncryptionManager;
    private PreferenceRepository mPreferenceRepository;

    public static Intent createIntent(Context context) {
        return new Intent(context, UserInfoActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userinfo);

        this.oktaProgressDialog = ServiceLocator.provideOktaProgressDialog(this);
        this.mSmartLockHelper = ServiceLocator.provideSmartLockHelper();
        mWebAuth = ServiceLocator.provideWebAuthClient(this);
        mSessionClient = mWebAuth.getSessionClient();

        mPreferenceRepository = ServiceLocator.providePreferenceRepository(this);

        if (!mSessionClient.isAuthenticated()) {
            showMessage(getString(R.string.not_authorized));
            clearData();
            finish();
        }

        mWebAuth.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull AuthorizationStatus authorizationStatus) {
                showMessage(getString(R.string.sign_out_success));
            }

            @Override
            public void onCancel() {
                showMessage(getString(R.string.sign_out_canceled));
            }

            @Override
            public void onError(@Nullable String s, @Nullable AuthorizationException e) {
                showMessage(getString(R.string.error_message) + " : " + e.errorDescription + " : " + e.error);
                handleEncryptionError(e, () -> logOut());
            }
        }, this);

        userInfoContainer = findViewById(R.id.userinfo_container);
        tokensContainer = findViewById(R.id.tokens_container);
        smartLockChecker = findViewById(R.id.smartlock_ebable);
        smartLockChecker.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && smartLockChecker.isEnabled()) {
                showDialogWithAllowToProtectDataByFingerprint();
            }
        });
        userInfoContainer.setVisibility(View.GONE);
        tokensContainer.setVisibility(View.GONE);

        findViewById(R.id.view_detail_btn).setOnClickListener(v ->
                showDetailsView()
        );

        findViewById(R.id.view_token_btn).setOnClickListener(v ->
                showTokensView()
        );

        findViewById(R.id.clear_data_btn).setOnClickListener(v ->
                clearData()
        );

        findViewById(R.id.revoke_tokens_btn).setOnClickListener(v ->
                revokeTokens()
        );

        findViewById(R.id.logout_btn).setOnClickListener(v ->
                logOut()
        );
        findViewById(R.id.refresh_token).setOnClickListener(v ->
                refreshToken()
        );

        findViewById(R.id.refresh_token).setVisibility(View.VISIBLE);
        findViewById(R.id.refreshtoken_title).setVisibility(View.VISIBLE);
        findViewById(R.id.refreshtoken_textview).setVisibility(View.VISIBLE);

        if (savedInstanceState != null && savedInstanceState.getString(KEY_USER_INFO) != null) {
            try {
                mUserInfoJson.set(new UserInfo(new JSONObject(savedInstanceState.getString(KEY_USER_INFO))));
            } catch (JSONException ex) {
                showMessage("JSONException: " + ex);
                Log.e(TAG, Log.getStackTraceString(ex));
            }
        }

        mEncryptionManager = ServiceLocator.provideEncryptionManager(this);
        showSmartLockInfo();
        showUserData();
    }

    private void showSmartLockInfo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || mPreferenceRepository.isEnabledSmartLock()) {
            smartLockChecker.setEnabled(false);
            smartLockChecker.setChecked(mPreferenceRepository.isEnabledSmartLock());
        } else {
            smartLockChecker.setEnabled(true);
            smartLockChecker.setChecked(false);
        }
    }

    private void handleEncryptionError(AuthorizationException e, OnSuccessListener listener) {
        switch (e.code) {
            case EncryptionErrors.KEYGUARD_AUTHENTICATION_ERROR:
            case EncryptionErrors.ENCRYPT_ERROR:
            case EncryptionErrors.DECRYPT_ERROR:
                mSmartLockHelper.showSmartLockChooseDialog(this, new SmartLockHelper.FingerprintCallback(UserInfoActivity.this, mEncryptionManager) {
                    @Override
                    protected void onSuccess() {
                        listener.onSuccess();
                    }
                }, mEncryptionManager.getCipher());
                break;
            case EncryptionErrors.INVALID_KEYS_ERROR:
                showMessage("Failure fetch user profile: " + e.error + ":" + e.errorDescription);
                handleInvalidKeys();
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mUserInfoJson.get() != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.get().toString());
        }
    }

    private void showUserData() {
        if (mSessionClient.isAuthenticated()) {
            if (!mEncryptionManager.isValidKeys()) {
                handleInvalidKeys();
                return;
            }
            if (mEncryptionManager.isUserAuthenticatedOnDevice()) {
                displayAuthorizationInfo();
                if (mUserInfoJson.get() == null) {
                    fetchUserInfo();
                }
            } else {
                mSmartLockHelper.showSmartLockChooseDialog(this, new SmartLockHelper.FingerprintCallback(this, mEncryptionManager) {
                    @Override
                    protected void onSuccess() {
                        showUserData();
                    }
                }, mEncryptionManager.getCipher());
            }
        } else {
            showMessage("No authorization state retained - re-authorization required");
            navigateToStartActivity();
            finish();
        }

    }

    private void showDialogWithAllowToProtectDataByFingerprint() {
        if (!SmartLockHelper.isKeyguardSecure(this)) {
            showMessage(getString(R.string.setup_lockscreen_info_msg));
            showSmartLockInfo();
            return;
        }

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setCancelable(false);
        alertBuilder.setTitle(R.string.protect_by_smartlock);
        alertBuilder.setMessage(R.string.ask_to_protect_by_smartlock);
        alertBuilder.setPositiveButton(R.string.yes, (dialog, which) -> {
            dialog.dismiss();
            oktaProgressDialog.show();
            mSmartLockHelper.showSmartLockChooseDialog(this, new SmartLockHelper.FingerprintCallback(this, mEncryptionManager) {
                @Override
                protected void onSuccess() {
                    try {
                        SmartLockBaseEncryptionManager smartLockBaseEncryptionManager = ServiceLocator.createSmartLockEncryptionManager(UserInfoActivity.this);
                        smartLockBaseEncryptionManager.recreateCipher();
                        mWebAuth.migrateTo(smartLockBaseEncryptionManager);
                        mPreferenceRepository.enableSmartLock(true);
                        mEncryptionManager = smartLockBaseEncryptionManager;
                    } catch (AuthorizationException exception) {
                        mSessionClient.clear();
                        finish();
                    }
                    oktaProgressDialog.hide();
                    showSmartLockInfo();
                    showUserData();
                }

                @Override
                public void onFingerprintError(String error) {
                    super.onFingerprintError(error);
                    showMessage(error);
                    showSmartLockInfo();
                    oktaProgressDialog.hide();
                }

                @Override
                public void onFingerprintCancel() {
                    super.onFingerprintCancel();
                    showMessage(getString(R.string.operation_cancel));
                    showSmartLockInfo();
                    oktaProgressDialog.hide();
                }
            }, mEncryptionManager.getCipher());
        });
        alertBuilder.setNegativeButton(R.string.no, (dialog, which) -> {
            showSmartLockInfo();
            dialog.dismiss();
        });
        alertBuilder.show();
    }

    private void fetchUserInfo() {
        oktaProgressDialog.show(getString(R.string.user_info_loading));

        mSessionClient.getUserProfile(new RequestCallback<UserInfo, AuthorizationException>() {
            @Override
            public void onSuccess(UserInfo userInfo) {
                oktaProgressDialog.hide();
                mUserInfoJson.set(userInfo);
                updateUserInfo(userInfo);
            }

            @Override
            public void onError(String s, AuthorizationException e) {
                oktaProgressDialog.hide();
                mUserInfoJson.set(null);
                showMessage("Failure fetch user profile: " + e.error + ":" + e.errorDescription);
                handleEncryptionError(e, () -> fetchUserInfo());
            }
        });
    }

    private void refreshToken() {
        oktaProgressDialog.show(getString(R.string.access_token_loading));

        mSessionClient.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
            @Override
            public void onSuccess(Tokens tokens) {
                oktaProgressDialog.hide();
                showMessage(getString(R.string.token_refreshed_successfully));
                updateTokens(tokens);
            }

            @Override
            public void onError(String s, AuthorizationException e) {
                oktaProgressDialog.hide();
                showMessage(getString(R.string.error_message) + " : " + e.errorDescription + " : " + e.error);
                handleEncryptionError(e, () -> refreshToken());
            }
        });
    }

    private void showDetailsView() {
        userInfoContainer.setVisibility(View.VISIBLE);
        tokensContainer.setVisibility(View.GONE);
    }

    private void showTokensView() {
        userInfoContainer.setVisibility(View.GONE);
        tokensContainer.setVisibility(View.VISIBLE);
    }

    private void logOut() {
        mWebAuth.signOutOfOkta(this);
    }

    private void revokeTokens() {
        Tokens tokens;
        try {
            tokens = mSessionClient.getTokens();
        } catch (AuthorizationException exception) {
            handleEncryptionError(exception, () -> revokeTokens());
            return;
        }
        oktaProgressDialog.show();
        final int requestCount = (tokens.getRefreshToken() != null) ? 3 : 2;

        RequestCallback<Boolean, AuthorizationException> requestCallback = new RequestCallback<Boolean, AuthorizationException>() {
            volatile int localRequestCount = requestCount;

            @Override
            public synchronized void onSuccess(Boolean aBoolean) {
                localRequestCount--;
                if (localRequestCount == 0) {
                    onComplete();
                }
            }

            @Override
            public synchronized void onError(String s, AuthorizationException e) {
                localRequestCount = -1;

                oktaProgressDialog.hide();
                showMessage(getString(R.string.unable_to_revoke_tokens));
            }

            private void onComplete() {
                oktaProgressDialog.hide();
                showMessage(getString(R.string.tokens_revoke_success));
            }
        };

        mSessionClient.revokeToken(tokens.getAccessToken(), requestCallback);
        mSessionClient.revokeToken(tokens.getIdToken(), requestCallback);

        if (tokens.getRefreshToken() != null)
            mSessionClient.revokeToken(tokens.getRefreshToken(), requestCallback);
        navigateToStartActivity();
    }

    private void handleInvalidKeys() {
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
        navigateToStartActivity();
    }

    private void clearData() {
        mSessionClient.clear();
        navigateToStartActivity();
    }

    private void updateTokens(Tokens tokens) {
        ((TextView) findViewById(R.id.accesstoken_textview)).setText(tokens.getAccessToken());
        ((TextView) findViewById(R.id.idtoken_textview)).setText(tokens.getIdToken());
        ((TextView) findViewById(R.id.refreshtoken_textview)).setText(tokens.getRefreshToken());

        if (tokens.getRefreshToken() == null) {
            findViewById(R.id.refreshtoken_textview).setVisibility(View.GONE);
            findViewById(R.id.refreshtoken_title).setVisibility(View.GONE);
            findViewById(R.id.refresh_token).setVisibility(View.GONE);
        }
    }

    private void updateUserInfo(UserInfo user) {
        if (user == null) {
            return;
        }
        try {
            if (user.get("name") != null) {
                ((TextView) findViewById(R.id.name_textview)).setText((String) user.get("name"));
            }
            if (user.get("given_name") != null) {
                ((TextView) findViewById(R.id.welcome_title)).setText(String.format(getString(R.string.welcome_title), (String) user.get("given_name")));
            }

            if (user.get("preferred_username") != null) {
                ((TextView) findViewById(R.id.username_textview)).setText((String) user.get("preferred_username"));
                ((TextView) findViewById(R.id.email_title)).setText((String) user.get("preferred_username"));
            }

            if (user.get("locale") != null) {
                ((TextView) findViewById(R.id.locale_textview)).setText((String) user.get("locale"));
            }

            if (user.get("zoneinfo") != null) {
                ((TextView) findViewById(R.id.zoneinfo_textview)).setText((String) user.get("zoneinfo"));
                ((TextView) findViewById(R.id.timezone_value)).setText((String) user.get("zoneinfo"));
            }
            if (user.get("updated_at") != null) {
                long time = Long.parseLong(user.getRaw().getString("updated_at"));
                ((TextView) findViewById(R.id.last_update_value)).setText(getLastUpdateString(time));
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showMessage(e.getMessage());
        }
    }

    private void displayAuthorizationInfo() {
        UserInfo user = mUserInfoJson.get();
        Tokens tokens;
        try {
            tokens = mSessionClient.getTokens();
        } catch (AuthorizationException exception) {
            handleEncryptionError(exception, () -> displayAuthorizationInfo());
            return;
        }

        updateTokens(tokens);
        updateUserInfo(user);
    }

    private void navigateToStartActivity() {
        startActivity(new Intent(UserInfoActivity.this, BrowserSignInActivity.class));
        finish();
    }

    private String getLastUpdateString(long lastUpdate) {
        return DateUtils.getRelativeTimeSpanString(lastUpdate * 1000, System.currentTimeMillis(), DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        mSmartLockHelper.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    interface OnSuccessListener {
        void onSuccess();
    }
}
