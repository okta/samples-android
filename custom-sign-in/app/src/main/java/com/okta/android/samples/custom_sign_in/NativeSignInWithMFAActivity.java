package com.okta.android.samples.custom_sign_in;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.okta.authn.sdk.resource.Factor;
import com.okta.authn.sdk.resource.VerifyFactorRequest;
import com.okta.authn.sdk.resource.VerifyPassCodeFactorRequest;
import com.okta.sdk.resource.user.factor.FactorProfile;
import com.okta.sdk.resource.user.factor.FactorStatus;
import com.okta.sdk.resource.user.factor.FactorType;
import com.okta.sdk.resource.user.factor.SmsFactorProfile;

import org.w3c.dom.Text;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeSignInWithMFAActivity extends AppCompatActivity {
    private String TAG = "NativeSignInWithMFA";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    AuthenticationClient authenticationClient = null;

    private EditText loginEditText;
    private EditText passwordEditText;
    private Button signInBtn;

    OktaProgressDialog oktaProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_with_mfa);

        oktaProgressDialog = new OktaProgressDialog(this);

        loginEditText = findViewById(R.id.login_edittext);
        passwordEditText = findViewById(R.id.password_edittext);
        signInBtn = findViewById(R.id.sign_in_btn);
        signInBtn.setOnClickListener(v -> {
            signIn();
        });

        init();
    }

    private void init() {
        authenticationClient = AuthenticationClients.builder()
                .setOrgUrl(BuildConfig.BASE_URL)
                .build();
    }

    private void signIn() {
        String login = "imartsekha@lohika.com";//loginEditText.getText().toString();
        String password = "Mayonez1989__";//passwordEditText.getText().toString();
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
                        runOnUiThread(() -> {
                            showMessage(successResponse.toString());
                            oktaProgressDialog.hide();
                        });
                        finish();
                    }

                    @Override
                    public void handleMfaRequired(AuthenticationResponse mfaRequiredResponse) {
                        runOnUiThread(() -> {
                            oktaProgressDialog.hide();
                            showDialogWithPromptFactors(mfaRequiredResponse.getStateToken(), mfaRequiredResponse.getFactors());
                        });
                    }

                    @Override
                    public void handleMfaEnroll(AuthenticationResponse mfaEnroll) {
                        List<Factor> factors = mfaEnroll.getFactors();
                        if(factors == null || factors.isEmpty())
                            throw new IllegalArgumentException("Missed factors or empty");

                        enrollFactor(mfaEnroll.getStateToken(),factors);
                    }

                    @Override
                    public void handleMfaEnrollActivate(AuthenticationResponse mfaEnrollActivate) {
                        super.handleMfaEnrollActivate(mfaEnrollActivate);
                    }

                    @Override
                    public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {
                        super.handleMfaChallenge(mfaChallengeResponse);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    private void challangeFactor(String factorId, String stateToken) {
        KeyboardUtil.hideSoftKeyboard(this);
        oktaProgressDialog.show();
        executor.submit(() -> {
            try {
                authenticationClient.challengeFactor(factorId, stateToken, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        runOnUiThread(() -> {
                            oktaProgressDialog.hide();
                            showMessage(authenticationResponse.toString());
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    private void verifyFactor(String factorId, VerifyFactorRequest request) {
        KeyboardUtil.hideSoftKeyboard(this);
        oktaProgressDialog.show();
        executor.submit(() -> {
            try {
                authenticationClient.verifyFactor(factorId, request, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        runOnUiThread(() -> {
                            oktaProgressDialog.hide();
                            showMessage(authenticationResponse.toString());
                        });
                    }

                    @Override
                    public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {
                        challangeFactor(factorId, mfaChallengeResponse.getStateToken());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    oktaProgressDialog.hide();
                    showMessage(e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    @MainThread
    private void showDialogWithPromptFactors(String stateToken, List<Factor> factors) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(getString(R.string.select_mfa));

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item);
        for(Factor factor : factors) {
            arrayAdapter.add(factor.getType().toString().toUpperCase());
        }

        alertBuilder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        alertBuilder.setAdapter(arrayAdapter, (dialog, which) -> {
            String factorType = arrayAdapter.getItem(which);
            Factor selectedFactor = null;
            for(Factor factor:factors) {
                if(factor.getType().toString().equalsIgnoreCase(factorType)) {
                    selectedFactor = factor;
                    break;
                }
            }
            dialog.dismiss();
            showFactor(stateToken, selectedFactor);
        });
        alertBuilder.show();

    }

    @MainThread
    private void showDialogEnterCodeFromSMS(String factorId, String stateToken, String phoneNumber) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_dialog_enter_sms_code, null);
        dialogBuilder.setView(dialogView);

        TextView titleTextView = dialogView.findViewById(R.id.title_textview);
        titleTextView.setText(String.format(getString(R.string.sms_mfa_title), phoneNumber));
        EditText codeEditText = dialogView.findViewById(R.id.code_edittext);

        Button resendBtn = dialogView.findViewById(R.id.resend_btn);
        resendBtn.setOnClickListener((v) -> {
            VerifyFactorRequest smsVerifyRequest = authenticationClient.instantiate(VerifyPassCodeFactorRequest.class)
                    .setStateToken(stateToken);

            verifyFactor(factorId, smsVerifyRequest);
        });
        Button verifyBtn = dialogView.findViewById(R.id.verify_btn);
        verifyBtn.setOnClickListener((v) -> {
            String code = codeEditText.getText().toString();
            if(TextUtils.isEmpty(code)) {
                codeEditText.setError(getString(R.string.empty_field_error));
            } else {
                codeEditText.setError(null);
            }
            VerifyFactorRequest smsVerifyRequest = authenticationClient.instantiate(VerifyPassCodeFactorRequest.class)
                    .setPassCode(code)
                    .setStateToken(stateToken);

            verifyFactor(factorId, smsVerifyRequest);
        });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void showFactor(String stateToken, Factor factor) {
        switch (factor.getType()) {
            case SMS:
                String factorId = factor.getId();
                String phoneNumber = (String) factor.getProfile().get("phoneNumber");
                showDialogEnterCodeFromSMS(factor.getId(), stateToken, phoneNumber);

                VerifyFactorRequest smsVerifyRequest = authenticationClient.instantiate(VerifyPassCodeFactorRequest.class)
                        .setRememberDevice(true)
                        .setStateToken(stateToken);
                verifyFactor(factorId, smsVerifyRequest);
            break;
            case QUESTION: {
//                String factorId = factor.getId();
                String questionText = (String) factor.getProfile().get("questionText");
                String question = (String) factor.getProfile().get("question");

//                VerifyFactorRequest request = authenticationClient.instantiate(RecoveryQuestionAnswerRequest.class).setStateToken(stateToken).setAnswer("");

//                authenticationClient.verifyFactor(factorId)

//                startActivity(RecoveryQuestionActivity.createIntent(this, questionText,stateToken));
            }
            break;
        }
    }

    private void enrollFactor(String stateToken, List<Factor> factors) {
        Factor firstFactor = factors.get(0);
        FactorProfile factorProfile = null;
        try {
            if(firstFactor.getType() == FactorType.SMS) {

                if(FactorStatus.NOT_SETUP.toString().equalsIgnoreCase(firstFactor.getStatus())) {
                    throw new IllegalArgumentException(FactorType.SMS + " MFA not setup");
                } else {
                    String phoneNumber = (String) firstFactor.getProfile().get("phoneNumber");
                    if(phoneNumber == null) {
                        throw new IllegalArgumentException("Missed phoneNumber in profile");
                    }
                    factorProfile = authenticationClient.instantiate(SmsFactorProfile.class).setPhoneNumber(phoneNumber);
                }
            }

            AuthenticationResponse response = authenticationClient.enrollFactor(firstFactor.getType(), firstFactor.getProvider(), factorProfile, stateToken, new AuthenticationStateHandlerAdapter() {
                @Override
                public void handleUnknown(AuthenticationResponse authenticationResponse) {

                }

                @Override
                public void handleSuccess(AuthenticationResponse successResponse) {
                    super.handleSuccess(successResponse);
                }

                @Override
                public void handleMfaRequired(AuthenticationResponse mfaRequiredResponse) {
                    super.handleMfaRequired(mfaRequiredResponse);
                }

                @Override
                public void handleMfaEnroll(AuthenticationResponse mfaEnroll) {
                    super.handleMfaEnroll(mfaEnroll);
                }

                @Override
                public void handleMfaEnrollActivate(AuthenticationResponse mfaEnrollActivate) {
                    super.handleMfaEnrollActivate(mfaEnrollActivate);
                }

                @Override
                public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {
                    super.handleMfaChallenge(mfaChallengeResponse);
                }
            });
            Log.d(TAG, response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }
}
