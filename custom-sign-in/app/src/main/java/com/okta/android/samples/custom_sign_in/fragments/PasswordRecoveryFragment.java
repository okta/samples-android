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
package com.okta.android.samples.custom_sign_in.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandler;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.FactorType;

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
                        runOnUIThread(() -> {
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
            } catch (AuthenticationException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                runOnUIThread(() -> {
                    showMessage(e.getLocalizedMessage());
                    hideLoading();
                });
            }
        });
    }

    private void showUnhandledStateMessage(AuthenticationResponse authenticationResponse) {
        runOnUIThread(() -> {
            showMessage(String.format(getString(R.string.not_handle_message), authenticationResponse.getStatus().name()));
            hideLoading();
        });
    }
}
