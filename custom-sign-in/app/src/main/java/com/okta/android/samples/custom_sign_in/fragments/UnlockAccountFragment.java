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

import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.StartActivity;
import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.FactorType;
import com.okta.authn.sdk.resource.UnlockAccountRequest;

public class UnlockAccountFragment extends BaseFragment {
    private String TAG = "UnlockAccount";

    Button sendEmailBtn = null;
    EditText usernameEditText = null;

    public static UnlockAccountFragment createFragment() {
        UnlockAccountFragment fragment = new UnlockAccountFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.unlock_account_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usernameEditText = view.findViewById(R.id.username_edittext);
        sendEmailBtn = view.findViewById(R.id.send_email_btn);
        sendEmailBtn.setOnClickListener(v -> sendEmail());
    }

    private void sendEmail() {
        String username = usernameEditText.getText().toString();
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError(getString(R.string.empty_field_error));
            return;
        }

        UnlockAccountRequest unlockAccountRequest = authenticationClient.instantiate(UnlockAccountRequest.class)
                .setUsername(username)
                .setFactorType(FactorType.EMAIL);

        unlockAccount(unlockAccountRequest);
    }

    private void unlockAccount(UnlockAccountRequest request) {
        KeyboardUtil.hideSoftKeyboard(getActivity());
        showLoading();
        submit(() -> {
            try {
                AuthenticationResponse response = authenticationClient.unlockAccount(request, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        runOnUIThread(() -> {
                            hideLoading();
                            showMessage(authenticationResponse.toString());
                        });
                    }

                    @Override
                    public void handleRecoveryChallenge(AuthenticationResponse recoveryChallenge) {
                        runOnUIThread(() -> {
                            showMessage(getString(R.string.letter_with_unlock_account_success));
                            hideLoading();
                            navigation.close();
                            startActivity(StartActivity.createIntent(getContext()));
                        });
                    }
                });
            } catch (AuthenticationException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                runOnUIThread(() -> {
                    hideLoading();
                    showMessage(e.getMessage());
                });
            }
        });
    }
}
