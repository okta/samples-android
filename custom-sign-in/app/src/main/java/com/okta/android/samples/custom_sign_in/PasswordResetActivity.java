package com.okta.android.samples.custom_sign_in;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.android.samples.custom_sign_in.util.OktaProgressDialog;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.AuthenticationStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PasswordResetActivity extends AppCompatActivity {
    private String TAG = "PasswordReset";
    private static String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    private static String PASSWORD_POLICY_KEY = "PASSWORD_POLICY_KEY";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    AuthenticationClient authenticationClient = null;
    private String stateToken = null;
    private Map<String,Integer> passwordPolicy = null;

    Button passwordResetBtn = null;
    TextView passwordPolicyTextView = null;
    EditText passwordEditText = null;
    EditText repeatPasswordEditText = null;
    OktaProgressDialog oktaProgressDialog = null;


    public static Intent createIntent(Context context, String stateToken,@Nullable Map<String, Integer> passwordPolicy) {
        Intent intent = new Intent(context, PasswordResetActivity.class);
        intent.putExtra(STATE_TOKEN_KEY, stateToken);
        if(passwordPolicy != null) {
            intent.putExtra(PASSWORD_POLICY_KEY, new HashMap<>(passwordPolicy));
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        oktaProgressDialog = new OktaProgressDialog(this);

        passwordPolicyTextView = findViewById(R.id.password_policy);
        passwordEditText = findViewById(R.id.password_edittext);
        repeatPasswordEditText = findViewById(R.id.repeat_password_edittext);
        passwordResetBtn = findViewById(R.id.reset_password);
        passwordResetBtn.setOnClickListener(v -> passwordReset());

        stateToken = getIntent().getStringExtra(STATE_TOKEN_KEY);
        try {
            passwordPolicy = (HashMap<String, Integer>) getIntent().getSerializableExtra(PASSWORD_POLICY_KEY);
        } catch (Exception e) {
            passwordPolicy = null;
            Log.e(TAG, Log.getStackTraceString(e));
        }
        showPasswordPolicy();

        init();
    }

    private void init() {
        authenticationClient = AuthenticationClients.builder()
                .setOrgUrl(BuildConfig.BASE_URL)
                .build();
    }


    private void showPasswordPolicy() {
        if(passwordPolicy != null) {
            String passwordPolicyText = "";
            for(String key : passwordPolicy.keySet()) {
                Object value = passwordPolicy.get(key);
                String item = "";
                if(value instanceof String) {
                    item += value;
                } else if(value instanceof Integer) {
                    item += Integer.toString((Integer) value);
                } else if(value instanceof Boolean) {
                    item += String.valueOf((Boolean)value);
                }
                passwordPolicyText = passwordPolicyText.concat(key+":"+item+"\n");
            }
            passwordPolicyTextView.setText(passwordPolicyText);
        }
    }

    private void passwordReset() {
        String password = passwordEditText.getText().toString();
        String repeatPassword = repeatPasswordEditText.getText().toString();
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.empty_field_error));
            return;
        } else if(TextUtils.isEmpty(repeatPassword)) {
            repeatPasswordEditText.setError(getString(R.string.empty_field_error));
            return;
        }

        if(!password.equalsIgnoreCase(repeatPassword)) {
            showMessage(getString(R.string.password_not_equal));
            return;
        }

        KeyboardUtil.hideSoftKeyboard(this);
        oktaProgressDialog.show();
        executor.submit(() -> {
            try {
                AuthenticationResponse response = authenticationClient.resetPassword(password.toCharArray(), stateToken, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {  }
                });

                if(AuthenticationStatus.SUCCESS != response.getStatus())
                    throw new IllegalArgumentException("Missed status or response not success");

                // Get profile Map
                Map<String, String> profile = response.getUser().getProfile();

                startActivity(new Intent(this, StartActivity.class));
                runOnUiThread(() -> {
                    showMessage(getString(R.string.password_reset_successfully));
                    oktaProgressDialog.hide();
                });
                finish();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(e.getMessage());
                });
                Log.e(TAG, Log.getStackTraceString(e));
            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }
}