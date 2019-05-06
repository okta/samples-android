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
package com.okta.android.samples.custom_sign_in.fragments.mfa_types;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import com.okta.authn.sdk.resource.VerifyFactorRequest;
import com.okta.authn.sdk.resource.VerifyPassCodeFactorRequest;

public class MfaGoogleVerifyCodeFragment extends BaseFragment {
    private String TAG = "MfaGoogleVerifyCode";
    public static final String FACTOR_ID_KEY = "FACTOR_ID_KEY";
    public static final String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    public static final String ACCOUNT_NAME_KEY = "ACCOUNT_NAME_KEY";
    private TextView accountTextView;
    private EditText enterCodeEdittext;
    private Button verifyCodeBtn;

    private String accountName;

    public static Fragment createFragment(String stateToken, Factor factor) {
        MfaGoogleVerifyCodeFragment fragment = new MfaGoogleVerifyCodeFragment();

        Bundle arguments = new Bundle();
        arguments.putString(FACTOR_ID_KEY, factor.getId());
        arguments.putString(STATE_TOKEN_KEY, stateToken);
        arguments.putString(ACCOUNT_NAME_KEY, (String) factor.getProfile().get("credentialId"));

        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.mfa_google_authenticator_code_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.accountTextView = view.findViewById(R.id.account_textview);
        this.enterCodeEdittext = view.findViewById(R.id.enter_code_edittext);
        this.verifyCodeBtn = view.findViewById(R.id.verify_code_btn);

        String factorId = getArguments().getString(FACTOR_ID_KEY);
        String stateToken = getArguments().getString(STATE_TOKEN_KEY);
        this.accountName = getArguments().getString(ACCOUNT_NAME_KEY);

        initView(factorId, stateToken, accountName);
    }

    public void initView(String factorId, String stateToken, String accountName) {
        this.accountTextView.setText(String.format(getString(R.string.mfa_google_authenticator_account), accountName));

        verifyCodeBtn.setOnClickListener((v) -> {
            verifyCode(factorId, stateToken);
        });
    }

    private void verifyCode(String factorId, String stateToken) {
        String code = enterCodeEdittext.getText().toString();
        if(TextUtils.isEmpty(code)) {
            enterCodeEdittext.setError(getString(R.string.empty_field_error));
        } else {
            enterCodeEdittext.setError(null);
        }
        VerifyFactorRequest codeVerifyRequest = authenticationClient.instantiate(VerifyPassCodeFactorRequest.class)
                .setPassCode(code)
                .setStateToken(stateToken);

        verifyFactor(factorId, codeVerifyRequest);
    }

    private void verifyFactor(String factorId, VerifyFactorRequest request) {
        showLoading();
        submit(() -> {
            try {
                authenticationClient.verifyFactor(factorId, request, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        runOnUIThread(() -> {
                            hideLoading();
                            showMessage(String.format(getString(R.string.not_handle_message), authenticationResponse.getStatus().name()));
                        });
                    }

                    @Override
                    public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {
                        runOnUIThread(() -> {
                            hideLoading();
                            showMessage(String.format(getString(R.string.mfa_push_sent), accountName));
                        });
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse successResponse) {
                        runOnUIThread(() -> {
                            hideLoading();
                            sendSessionToken(successResponse.getSessionToken());
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

    private void sendSessionToken(String sessionToken) {
        if(getTargetFragment() != null && (getTargetFragment() instanceof IMFAResult)) {
            ((IMFAResult)getTargetFragment()).onSuccess(sessionToken);
        }
    }

}