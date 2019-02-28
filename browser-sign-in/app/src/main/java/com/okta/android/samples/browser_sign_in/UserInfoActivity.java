package com.okta.android.samples.browser_sign_in;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.appauth.android.OktaAppAuth;

import net.openid.appauth.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

import static com.okta.appauth.android.OktaAppAuth.getInstance;

public class UserInfoActivity extends AppCompatActivity {
    private final String TAG = "UserInfo";
    private OktaAppAuth mOktaAppAuth;
    private OktaProgressDialog oktaProgressDialog;
    private final AtomicReference<JSONObject> mUserInfoJson = new AtomicReference<>();

    private static final String EXTRA_FAILED = "failed";
    private static final String KEY_USER_INFO = "userInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userinfo);
        this.oktaProgressDialog = new OktaProgressDialog(this);
        this.mOktaAppAuth = getInstance(this);

        if (!mOktaAppAuth.isUserLoggedIn()) {
            showMessage(getString(R.string.not_authorized));
            clearData();
            finish();
        }

        findViewById(R.id.userinfo_btn).setOnClickListener(v ->
            fetchUserInfo()
        );

        findViewById(R.id.clear_data_btn).setOnClickListener(v ->
                clearData()
        );

        findViewById(R.id.logout_btn).setOnClickListener(v ->
                logOut()
        );

        if(mOktaAppAuth.hasRefreshToken()) {
            findViewById(R.id.refresh_token).setVisibility(View.VISIBLE);
            findViewById(R.id.refresh_token).setOnClickListener(v ->
                    refreshToken()
            );
        } else {
            findViewById(R.id.refresh_token).setVisibility(View.GONE);
        }

        if (savedInstanceState != null) {
            try {
                mUserInfoJson.set(new JSONObject(savedInstanceState.getString(KEY_USER_INFO)));
            } catch (JSONException ex) {
                showMessage("JSONException: " + ex);
                Log.e(TAG, Log.getStackTraceString(ex));
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mOktaAppAuth.isUserLoggedIn()) {
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
            state.putString(KEY_USER_INFO, mUserInfoJson.toString());
        }
    }

    private void fetchUserInfo() {
        oktaProgressDialog.show(getString(R.string.user_info_loading));
        mOktaAppAuth.getUserInfo(new OktaAppAuth.OktaAuthActionCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    mUserInfoJson.set(jsonObject);
                    displayAuthorizationInfo();
                });
            }

            @Override
            public void onTokenFailure(@NonNull AuthorizationException e) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    mUserInfoJson.set(null);
                    displayAuthorizationInfo();
                    showMessage("TokenFailure: "+e.errorDescription);
                });
            }

            @Override
            public void onFailure(int i, Exception e) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    mUserInfoJson.set(null);
                    displayAuthorizationInfo();
                    showMessage("Failure: "+e.getMessage());
                });
            }
        });
    }

    private void refreshToken() {
        oktaProgressDialog.show(getString(R.string.access_token_loading));
        mOktaAppAuth.refreshAccessToken(new OktaAppAuth.OktaAuthListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(getString(R.string.token_refreshed_successfully));
                    displayAuthorizationInfo();
                });
            }

            @Override
            public void onTokenFailure(@NonNull AuthorizationException e) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(getString(R.string.error_message)+" "+e.errorDescription);
                });
            }
        });
    }

    @MainThread
    private void logOut() {
        Intent completionIntent = new Intent(this, BrowserSignInActivity.class);
        Intent cancelIntent = new Intent(this, UserInfoActivity.class);
        cancelIntent.putExtra(EXTRA_FAILED, true);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        completionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        mOktaAppAuth.signOutFromOkta(this,
                PendingIntent.getActivity(this, 0, completionIntent, 0),
                PendingIntent.getActivity(this, 0, cancelIntent, 0)
        );
    }

    @MainThread
    private void clearData() {
        oktaProgressDialog.show();
        mOktaAppAuth.revoke(new OktaAppAuth.OktaRevokeListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    mOktaAppAuth.clearSession();
                    navigateToStartActivity();
                    finish();
                });
            }

            @Override
            public void onError(AuthorizationException ex) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(getString(R.string.unable_to_clean_data));
                });

            }
        });
    }

    @MainThread
    private void displayAuthorizationInfo() {
        JSONObject user = mUserInfoJson.get();

        ((TextView) findViewById(R.id.accesstoken_textview)).setText(mOktaAppAuth.getTokens().getAccessToken());
        ((TextView) findViewById(R.id.idtoken_textview)).setText(mOktaAppAuth.getTokens().getIdToken());
        ((TextView) findViewById(R.id.refreshtoken_textview)).setText(mOktaAppAuth.getTokens().getRefreshToken());

        if(user == null) {
            return;
        }
        findViewById(R.id.userinfo_container).setVisibility(View.VISIBLE);
        try {
            if(user.has("name")) {
                ((TextView) findViewById(R.id.name_textview)).setText(user.getString("name"));
            }

            if(user.has("preferred_username")) {
                ((TextView) findViewById(R.id.username_textview)).setText(user.getString("preferred_username"));
            }

            if(user.has("locale")) {
                ((TextView) findViewById(R.id.locale_textview)).setText(user.getString("locale"));
            }

            if(user.has("zoneinfo")) {
                ((TextView) findViewById(R.id.zoneinfo_textview)).setText(user.getString("zoneinfo"));
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showMessage(e.getMessage());
        }
    }

    private void navigateToStartActivity() {
        startActivity(new Intent(UserInfoActivity.this, BrowserSignInActivity.class));
    }

    @MainThread
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (mOktaAppAuth != null) {
            mOktaAppAuth.dispose();
            mOktaAppAuth = null;
        }
        super.onDestroy();
    }
}
