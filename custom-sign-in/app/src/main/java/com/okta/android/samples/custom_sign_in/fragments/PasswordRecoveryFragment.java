package com.okta.android.samples.custom_sign_in.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.authn.sdk.AuthenticationStateHandler;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.sdk.resource.user.factor.FactorType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PasswordRecoveryFragment extends BaseFragment {
    private String TAG = "PasswordRecovery";
    Button passwordResetBtn = null;
    EditText loginEditText = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.password_recovery_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loginEditText = view.findViewById(R.id.password_edittext);
        passwordResetBtn = view.findViewById(R.id.reset_password);
        passwordResetBtn.setOnClickListener(v -> passwordReset());
    }

    private void passwordReset() {
        if(TextUtils.isEmpty(loginEditText.getText())) {
            loginEditText.setError(getString(R.string.empty_field_error));
        } else {
            loginEditText.setError(null);
        }

        String username = loginEditText.getText().toString();

        KeyboardUtil.hideSoftKeyboard(getActivity());
        showLoading();
        submit(() -> {
            try {
                AuthenticationResponse response = authenticationClient.recoverPassword(username, FactorType.EMAIL, null, new AuthenticationStateHandler() {
                    @Override
                    public void handleUnauthenticated(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handlePasswordWarning(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handlePasswordExpired(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleRecovery(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleRecoveryChallenge(AuthenticationResponse authenticationResponse) {
                        loginEditText.post(() -> {
                            String finishMessage = String.format(getString(R.string.letter_with_reset_link_success), username)+"\n"+authenticationResponse.toString();
                            showMessage(finishMessage);
                            hideLoading();
                            navigation.close();
                        });
                    }

                    @Override
                    public void handlePasswordReset(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleLockedOut(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleMfaRequired(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleMfaEnroll(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleMfaEnrollActivate(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleMfaChallenge(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }

                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        showUnhandledStateMessage(authenticationResponse);
                    }
                });
            } catch (Exception e) {
                runOnUIThread(() -> {
                    showMessage(e.getLocalizedMessage());
                    hideLoading();
                });
                Log.e(TAG, Log.getStackTraceString(e));
            }
        });
    }

    private void showUnhandledStateMessage(AuthenticationResponse authenticationResponse) {
        runOnUIThread(() -> {
            showMessage(authenticationResponse.toString());
            hideLoading();
        });
    }

    @Override
    protected void finalize() throws Throwable {

        Log.d("MfaOktaVerify", "Finalize");
        super.finalize();
    }
}
