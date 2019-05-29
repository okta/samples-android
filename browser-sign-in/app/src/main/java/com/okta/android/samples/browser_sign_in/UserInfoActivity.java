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
import android.support.constraint.ConstraintLayout;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.okta.android.samples.browser_sign_in.util.FingerprintDialog;
import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.android.samples.browser_sign_in.util.PreferenceRepository;
import com.okta.android.samples.browser_sign_in.util.SmartLockHelper;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.storage.security.FingerprintUtils;
import com.okta.oidc.storage.security.SimpleBaseEncryptionManager;
import com.okta.oidc.storage.security.SmartLockBaseEncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

import static com.okta.oidc.util.AuthorizationException.*;

public class UserInfoActivity extends AppCompatActivity {
    private final String TAG = "UserInfo";
    private static final String ASK_FINGERPRINT = "ASK_FINGERPRINT";
    private WebAuthClient mWebAuth;
    private SessionClient mSessionClient;

    private OktaProgressDialog oktaProgressDialog;
    private SmartLockHelper mSmartLockHelper;
    private final AtomicReference<UserInfo> mUserInfoJson = new AtomicReference<>();

    private ConstraintLayout userinfoContainer;
    private ConstraintLayout tokensContainer;

    private static final String EXTRA_FAILED = "failed";
    private static final String KEY_USER_INFO = "userInfo";

    private EncryptionManager mEncryptionManager;
    private PreferenceRepository mPreferenceRepository;

    private boolean isRequireEnableFingerprint = false;

    public static Intent createIntent(Context context, boolean isRequireEnableFingerprint) {
        Intent intent = new Intent(context, UserInfoActivity.class);
        intent.putExtra(ASK_FINGERPRINT, isRequireEnableFingerprint);
        return intent;
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

        userinfoContainer = findViewById(R.id.userinfo_container);
        tokensContainer = findViewById(R.id.tokens_container);
        userinfoContainer.setVisibility(View.GONE);
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

        findViewById(R.id.refresh_token).setVisibility(View.VISIBLE);
        findViewById(R.id.refreshtoken_title).setVisibility(View.VISIBLE);
        findViewById(R.id.refreshtoken_textview).setVisibility(View.VISIBLE);
        findViewById(R.id.refresh_token).setOnClickListener(v ->
                refreshToken()
        );

        isRequireEnableFingerprint = getIntent().getBooleanExtra(ASK_FINGERPRINT, false);

        if (savedInstanceState != null && savedInstanceState.getString(KEY_USER_INFO) != null) {
            try {
                mUserInfoJson.set(new UserInfo(new JSONObject(savedInstanceState.getString(KEY_USER_INFO))));
            } catch (JSONException ex) {
                showMessage("JSONException: " + ex);
                Log.e(TAG, Log.getStackTraceString(ex));
            }
        }

        mEncryptionManager = ServiceLocator.provideEncryptionManager(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isRequireEnableFingerprint &&
                !mPreferenceRepository.isEnabledSmartLock()) {
            showDialogWithAllowToProtectDataByFingerprint();
        } else {
            continuePresentationData();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mUserInfoJson.get() != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.get().toString());
        }
    }

    private void continuePresentationData() {
        if (mSessionClient.isAuthenticated()) {
//            if (mEncryptionManager.isAuthenticateUser()) {
                displayAuthorizationInfo();
                if (mUserInfoJson.get() == null) {
                    fetchUserInfo();
                }
//            } else {
//                mSmartLockHelper.showSmartLockChooseDialog(this, new FingerprintDialog.FingerPrintCallback(this, mEncryptionManager) {
//                    @Override
//                    protected void onSuccess() {
//                        continuePresentationData();
//                    }
//                });
//            }
        } else {
            showMessage("No authorization state retained - re-authorization required");
            navigateToStartActivity();
            finish();
        }

    }

    private void showDialogWithAllowToProtectDataByFingerprint() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setCancelable(false);
        alertBuilder.setTitle(R.string.protect_by_smartlock);
        alertBuilder.setMessage(R.string.ask_to_protect_by_smartlock);
        alertBuilder.setPositiveButton(R.string.yes, (dialog, which) -> {
            dialog.dismiss();
            try {
                FingerprintUtils.SensorState sensorState = FingerprintUtils.checkSensorState(this);
                switch (sensorState) {
                    case READY:
                        mSmartLockHelper.showSmartLockChooseDialog(this, new FingerprintDialog.FingerPrintCallback(this, mEncryptionManager) {
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
                                continuePresentationData();
                            }
                        });
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
            } catch (Exception e) {
                showMessage(e.getMessage());
            }
        });
        alertBuilder.setNegativeButton(R.string.no, (dialog, which) -> {
            continuePresentationData();
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
                displayAuthorizationInfo();
            }

            @Override
            public void onError(String s, AuthorizationException e) {
                oktaProgressDialog.hide();
                mUserInfoJson.set(null);
                showMessage("Failure fetch user profile: " + e.error + ":" + e.errorDescription);
                switch (e.code) {
                    case EncryptionErrors.DECRYPT_ERROR:
                    case EncryptionErrors.ENCRYPT_ERROR:
                        mSmartLockHelper.showSmartLockChooseDialog(UserInfoActivity.this, new FingerprintDialog.FingerPrintCallback(UserInfoActivity.this, mEncryptionManager) {
                            @Override
                            protected void onSuccess() {
                                fetchUserInfo();
                            }
                        });
                        break;
                    case EncryptionErrors.INVALID_KEYS_ERROR:
                        handleInvalidKeys();
                        break;
                }
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
                displayAuthorizationInfo();
            }

            @Override
            public void onError(String s, AuthorizationException e) {
                oktaProgressDialog.hide();
                showMessage(getString(R.string.error_message) + " : " + e.errorDescription + " : " + e.error);
                switch (e.code) {
                    case EncryptionErrors.DECRYPT_ERROR:
                    case EncryptionErrors.ENCRYPT_ERROR:
                        mSmartLockHelper.showSmartLockChooseDialog(UserInfoActivity.this, new FingerprintDialog.FingerPrintCallback(UserInfoActivity.this, mEncryptionManager) {
                            @Override
                            protected void onSuccess() {
                                refreshToken();
                            }
                        });
                        break;
                    case EncryptionErrors.INVALID_KEYS_ERROR:
                        handleInvalidKeys();
                        break;
                }
            }
        });
    }

    private void showDetailsView() {
        userinfoContainer.setVisibility(View.VISIBLE);
        tokensContainer.setVisibility(View.GONE);
    }

    private void showTokensView() {
        userinfoContainer.setVisibility(View.GONE);
        tokensContainer.setVisibility(View.VISIBLE);
    }

    private void logOut() {
        mWebAuth.signOutOfOkta(this);
    }

    private void revokeTokens() {
        mEncryptionManager.removeKeys();
    }

    private void handleInvalidKeys() {
        mSessionClient.clear();
        ServiceLocator.provideEncryptionManager(this).removeKeys();
        try {
            SimpleBaseEncryptionManager simpleEncryptionManager = ServiceLocator.createSimpleEncryptionManager(this);
            ServiceLocator.setEncryptionManager(simpleEncryptionManager);
            mWebAuth.migrateTo(simpleEncryptionManager);
        } catch (Exception e){
            showMessage(e.getMessage());
        }
        navigateToStartActivity();
    }

    private void clearData() {
        mSessionClient.clear();
        navigateToStartActivity();
    }

    private void displayAuthorizationInfo() {
        UserInfo user = mUserInfoJson.get();
        Tokens tokens;
        try {
            tokens = mSessionClient.getTokens();
        } catch (RuntimeException exception) {
            mSmartLockHelper.showSmartLockChooseDialog(this, new FingerprintDialog.FingerPrintCallback(this, mEncryptionManager) {
                @Override
                protected void onSuccess() {
                    displayAuthorizationInfo();
                }
            });
            return;
        }

        ((TextView) findViewById(R.id.accesstoken_textview)).setText(tokens.getAccessToken());
        ((TextView) findViewById(R.id.idtoken_textview)).setText(tokens.getIdToken());
        ((TextView) findViewById(R.id.refreshtoken_textview)).setText(tokens.getRefreshToken());

        if (tokens.getRefreshToken() == null) {
            findViewById(R.id.refreshtoken_textview).setVisibility(View.GONE);
            findViewById(R.id.refreshtoken_title).setVisibility(View.GONE);
            findViewById(R.id.refresh_token).setVisibility(View.GONE);
        }

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
}
