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
import android.support.constraint.ConstraintLayout;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;


public class UserInfoActivity extends AppCompatActivity {
    private final String TAG = "UserInfo";
    private WebAuthClient mWebAuth;
    private SessionClient mSessionClient;
    private OktaProgressDialog oktaProgressDialog;
    private final AtomicReference<UserInfo> mUserInfoJson = new AtomicReference<>();

    private ConstraintLayout userinfoContainer;
    private ConstraintLayout tokensContainer;

    private static final String EXTRA_FAILED = "failed";
    private static final String KEY_USER_INFO = "userInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userinfo);

        this.oktaProgressDialog = new OktaProgressDialog(this);
        mWebAuth = ServiceLocator.provideWebAuthClient(this);
        mSessionClient = mWebAuth.getSessionClient();

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

        if (mSessionClient.getTokens().getRefreshToken() != null) {
            findViewById(R.id.refresh_token).setVisibility(View.VISIBLE);
            findViewById(R.id.refreshtoken_title).setVisibility(View.VISIBLE);
            findViewById(R.id.refreshtoken_textview).setVisibility(View.VISIBLE);
            findViewById(R.id.refresh_token).setOnClickListener(v ->
                    refreshToken()
            );
        } else {
            findViewById(R.id.refreshtoken_textview).setVisibility(View.GONE);
            findViewById(R.id.refreshtoken_title).setVisibility(View.GONE);
            findViewById(R.id.refresh_token).setVisibility(View.GONE);
        }

        if (savedInstanceState != null) {
            try {
                mUserInfoJson.set(new UserInfo(new JSONObject(savedInstanceState.getString(KEY_USER_INFO))));
            } catch (JSONException ex) {
                showMessage("JSONException: " + ex);
                Log.e(TAG, Log.getStackTraceString(ex));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mUserInfoJson.get() == null) {
            fetchUserInfo();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mSessionClient.isAuthenticated()) {
            displayAuthorizationInfo();
        } else {
            showMessage("No authorization state retained - reauthorization required");
            navigateToStartActivity();
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mUserInfoJson.get() != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.get().toString());
        }
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
                displayAuthorizationInfo();
                showMessage("Failure: " + e.getMessage());

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
                showMessage(getString(R.string.error_message) + " " + e.errorDescription);
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
        oktaProgressDialog.show();
        final int requestCount = (mSessionClient.getTokens().getRefreshToken() != null) ? 3 : 2;

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

        mSessionClient.revokeToken(mSessionClient.getTokens().getAccessToken(), requestCallback);
        mSessionClient.revokeToken(mSessionClient.getTokens().getIdToken(), requestCallback);

        if (mSessionClient.getTokens().getRefreshToken() != null)
            mSessionClient.revokeToken(mSessionClient.getTokens().getRefreshToken(), requestCallback);
    }

    private void clearData() {
        mSessionClient.clear();
        navigateToStartActivity();
    }

    private void displayAuthorizationInfo() {
        UserInfo user = mUserInfoJson.get();

        ((TextView) findViewById(R.id.accesstoken_textview)).setText(mSessionClient.getTokens().getAccessToken());
        ((TextView) findViewById(R.id.idtoken_textview)).setText(mSessionClient.getTokens().getIdToken());
        ((TextView) findViewById(R.id.refreshtoken_textview)).setText(mSessionClient.getTokens().getRefreshToken());

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
}
