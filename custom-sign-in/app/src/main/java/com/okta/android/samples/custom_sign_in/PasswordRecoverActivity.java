package com.okta.android.samples.custom_sign_in;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.android.samples.custom_sign_in.util.OktaProgressDialog;
import com.okta.authn.sdk.AuthenticationStateHandler;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.sdk.resource.user.factor.FactorType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PasswordRecoverActivity extends AppCompatActivity {
    private String TAG = "PasswordReset";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    AuthenticationClient authenticationClient = null;

    Button passwordResetBtn = null;
    EditText loginEditText = null;

    OktaProgressDialog oktaProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recovery);

        oktaProgressDialog = new OktaProgressDialog(this);

        loginEditText = findViewById(R.id.password_edittext);
        passwordResetBtn = findViewById(R.id.reset_password);
        passwordResetBtn.setOnClickListener(v -> passwordReset());

        init();
    }

    private void init() {
        authenticationClient = AuthenticationClients.builder()
                .setOrgUrl(BuildConfig.BASE_URL)
                .build();
    }

    private void passwordReset() {
        if(TextUtils.isEmpty(loginEditText.getText())) {
            loginEditText.setError(getString(R.string.empty_field_error));
        } else {
            loginEditText.setError(null);
        }

        String username = loginEditText.getText().toString();

        KeyboardUtil.hideSoftKeyboard(this);
        oktaProgressDialog.show();
        executor.submit(() -> {
            try {
                AuthenticationResponse response = authenticationClient.recoverPassword(username, FactorType.EMAIL, null, new AuthenticationStateHandler() {
                    @Override
                    public void handleUnauthenticated(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handlePasswordWarning(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handlePasswordExpired(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleRecovery(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleRecoveryChallenge(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handlePasswordReset(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleLockedOut(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleMfaRequired(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleMfaEnroll(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleMfaEnrollActivate(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleMfaChallenge(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse authenticationResponse) {

                    }

                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {

                    }
                });
                runOnUiThread(() -> {
                    showMessage(String.format(getString(R.string.letter_with_reset_link_success), username));
                    oktaProgressDialog.hide();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showMessage(e.getLocalizedMessage());
                    oktaProgressDialog.hide();
                });
                Log.e(TAG, Log.getStackTraceString(e));
            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }
}
