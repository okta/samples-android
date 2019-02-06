package com.okta.android.samples.custom_sign_in;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.android.samples.custom_sign_in.util.OktaProgressDialog;
import com.okta.appauth.android.AuthenticationError;
import com.okta.appauth.android.OktaAppAuth;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import com.okta.authn.sdk.resource.AuthenticationResponse;

import net.openid.appauth.AuthorizationException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeSignInActivity extends AppCompatActivity {
    private String TAG = "NativeSignIn";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    AuthenticationClient authenticationClient = null;
    private OktaAppAuth mOktaAuth;


    private EditText loginEditText;
    private EditText passwordEditText;

    OktaProgressDialog oktaProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        oktaProgressDialog = new OktaProgressDialog(this);

        loginEditText = findViewById(R.id.login_edittext);
        passwordEditText = findViewById(R.id.password_edittext);
        findViewById(R.id.sign_in_btn).setOnClickListener(v -> {
            signIn();
        });

        init();
    }

    private void init() {
        hideLoginForm();
        oktaProgressDialog.show();
        authenticationClient = AuthenticationClients.builder()
                .setOrgUrl(BuildConfig.BASE_URL)
                .build();


        mOktaAuth = OktaAppAuth.getInstance(getApplicationContext());

        mOktaAuth.init(
                getApplicationContext(),
                new OktaAppAuth.OktaAuthListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            oktaProgressDialog.hide();
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
                            oktaProgressDialog.hide();
                            showMessage(getString(R.string.init_error)
                                    + ":"
                                    + ex.errorDescription);
                        });
                    }
                },
                getResources().getColor(R.color.colorPrimary));
    }

    @MainThread
    private void showLoginForm() {
        findViewById(R.id.container).setVisibility(View.VISIBLE);
    }

    @MainThread
    private void hideLoginForm() {
        findViewById(R.id.container).setVisibility(View.GONE);
    }

    @MainThread
    private void showUserInfo() {
        startActivity(new Intent(this, UserInfoActivity.class));
        finish();
    }

    private void signIn() {
        String login = "imartsekha@lohika.com";//loginEditText.getText().toString();
        String password = "Mayonez1989_";//passwordEditText.getText().toString();
        if (TextUtils.isEmpty(login)) {
            loginEditText.setError(getString(R.string.empty_field_error));
            return;
        } else if(TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.empty_field_error));
            return;
        }


        KeyboardUtil.hideSoftKeyboard(this);
        oktaProgressDialog.show();
        executor.submit(() -> {
            try {
                authenticationClient.authenticate(login, password.toCharArray(), null, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        runOnUiThread(() -> {
                            oktaProgressDialog.hide();
                            showMessage(authenticationResponse.toString());
                        });
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse successResponse) {
                        String sessionToken = successResponse.getSessionToken();
                        authenticateViaOktaAndroidSDK(sessionToken);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(e.getMessage());
                });
                Log.e(TAG, Log.getStackTraceString(e));
            }
        });
    }

    private void authenticateViaOktaAndroidSDK(String sessionToken) {
        mOktaAuth.authenticate(sessionToken, new OktaAppAuth.OktaNativeAuthListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showUserInfo();
                });
            }

            @Override
            public void onTokenFailure(@NonNull AuthenticationError authenticationError) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(authenticationError.getLocalizedMessage());
                });
                finish();
            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }
}
