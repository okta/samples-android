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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.UserInfoActivity;
import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.android.samples.custom_sign_in.base.IOktaAppAuthClientProvider;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.results.Result;
import com.okta.oidc.util.AuthorizationException;

public class NativeSignInFragment extends BaseFragment {
    private String TAG = "NativeSignIn";

    private EditText loginEditText;
    private EditText passwordEditText;

    private AuthClient mAuthClient;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IOktaAppAuthClientProvider) {
            mAuthClient = ((IOktaAppAuthClientProvider) context).provideOktaAppAuthClient();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loginEditText = view.findViewById(R.id.login_edittext);
        passwordEditText = view.findViewById(R.id.password_edittext);
        view.findViewById(R.id.sign_in_btn).setOnClickListener(v -> {
            signIn();
        });
    }

    @MainThread
    private void showUserInfo() {
        startActivity(UserInfoActivity.createIntent(getContext()));
        navigation.close();
    }

    private void signIn() {
        String login = loginEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(login)) {
            loginEditText.setError(getString(R.string.empty_field_error));
            return;
        } else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.empty_field_error));
            return;
        }


        KeyboardUtil.hideSoftKeyboard(getActivity());
        showLoading();
        submit(() -> {
            try {
                authenticationClient.authenticate(login, password.toCharArray(), null, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        runOnUIThread(() -> {
                            hideLoading();
                            showMessage(String.format(getString(R.string.not_handle_message), authenticationResponse.getStatus().name()));
                        });
                    }

                    @Override
                    public void handleLockedOut(AuthenticationResponse lockedOut) {
                        runOnUIThread(() -> {
                            hideLoading();
                            showLockedAccountMessage(getContext());
                        });
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse successResponse) {
                        String sessionToken = successResponse.getSessionToken();
                        authenticateViaOktaAndroidSDK(sessionToken);
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

    private void authenticateViaOktaAndroidSDK(String sessionToken) {
        this.mAuthClient.signIn(sessionToken, null, new RequestCallback<Result, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull Result result) {
                hideLoading();
                showUserInfo();
            }

            @Override
            public void onError(String s, AuthorizationException e) {
                hideLoading();
                showMessage(e.getLocalizedMessage());
                navigation.close();
            }
        });
    }

    private void showLockedAccountMessage(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.account_lock_out);
        builder.setMessage(R.string.unlock_account_message);
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.cancel();
        });
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            dialog.cancel();
            navigation.present(UnlockAccountFragment.createFragment());
        });
        builder.create().show();
    }

}
