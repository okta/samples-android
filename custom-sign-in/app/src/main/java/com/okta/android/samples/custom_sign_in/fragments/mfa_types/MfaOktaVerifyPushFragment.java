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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import com.okta.authn.sdk.resource.VerifyFactorRequest;
import com.okta.authn.sdk.resource.VerifyPushFactorRequest;
import com.okta.sdk.resource.user.factor.FactorResultType;

import java.util.concurrent.TimeUnit;

public class MfaOktaVerifyPushFragment extends BaseFragment {
    private String TAG = "MfaOktaVerifyPush";
    public static final String FACTOR_ID_KEY = "FACTOR_ID_KEY";
    public static final String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    public static final String DEVICE_NAME_KEY = "DEVICE_NAME_KEY";
    private TextView deviceTextView;
    private TextView rejectStatusTextview;
    private Button sendPushBtn;

    private String deviceName;

    private final int CHECK_STATUS_DELAY = 5;

    public static Fragment createFragment(String stateToken, Factor factor) {
        MfaOktaVerifyPushFragment fragment = new MfaOktaVerifyPushFragment();

        Bundle arguments = new Bundle();
        arguments.putString(FACTOR_ID_KEY, factor.getId());
        arguments.putString(STATE_TOKEN_KEY, stateToken);
        arguments.putString(DEVICE_NAME_KEY, (String) factor.getProfile().get("name"));

        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.mfa_okta_verify_push_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.deviceTextView = view.findViewById(R.id.device_textview);
        this.rejectStatusTextview = view.findViewById(R.id.reject_status_textview);
        this.sendPushBtn = view.findViewById(R.id.send_push_btn);

        String factorId = getArguments().getString(FACTOR_ID_KEY);
        String stateToken = getArguments().getString(STATE_TOKEN_KEY);
        this.deviceName = getArguments().getString(DEVICE_NAME_KEY);

        initView(factorId, stateToken, deviceName);
    }

    public void initView(String factorId, String stateToken, String deviceName) {
        this.deviceTextView.setText(String.format(getString(R.string.mfa_push_device), deviceName));

        sendPushBtn.setOnClickListener((v) -> {
            sendPush(factorId, stateToken);
        });

        setPushNotSentState();
    }

    private void sendPush(String factorId, String stateToken) {
        VerifyFactorRequest pushVerifyRequest = authenticationClient.instantiate(VerifyPushFactorRequest.class)
                .setStateToken(stateToken);

        verifyFactor(factorId, pushVerifyRequest, true);
    }

    private void checkPushStatus(String factorId, String stateToken) {
        VerifyFactorRequest pushVerifyRequest = authenticationClient.instantiate(VerifyPushFactorRequest.class)
                .setStateToken(stateToken);

        verifyFactor(factorId, pushVerifyRequest, false);
    }

    private void runPushStatusChecking(String factorId, String stateToken) {
        schedule(() -> checkPushStatus(factorId, stateToken), CHECK_STATUS_DELAY, TimeUnit.SECONDS);
    }

    private void setPushSentState() {
        sendPushBtn.setEnabled(false);
        sendPushBtn.setText(getString(R.string.mfa_push_sent_btn));
    }

    private void setPushNotSentState() {
        rejectStatusTextview.setVisibility(View.GONE);
        sendPushBtn.setEnabled(true);
        sendPushBtn.setText(getString(R.string.mfa_push_btn));
    }

    private void setRejectState() {
        setPushNotSentState();
        rejectStatusTextview.setVisibility(View.VISIBLE);
        this.rejectStatusTextview.setText(getString(R.string.mfa_push_reject_title));
    }

    private void verifyFactor(String factorId, VerifyFactorRequest request, Boolean showLoading) {
        if(showLoading) {
           showLoading();
        }
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
                            if(mfaChallengeResponse.getFactorResult().equalsIgnoreCase(FactorResultType.WAITING.toString())) {
                                if (sendPushBtn.isEnabled()) {
                                    setPushSentState();
                                    hideLoading();
                                    showMessage(String.format(getString(R.string.mfa_push_sent), deviceName));
                                }
                                runPushStatusChecking(factorId, mfaChallengeResponse.getStateToken());
                            } else if(mfaChallengeResponse.getFactorResult().equalsIgnoreCase(FactorResultType.REJECTED.toString())) {
                                setRejectState();
                                hideLoading();
                            }
                        });
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse successResponse) {
                        runOnUIThread(() -> {
                            hideLoading();
                            sendSessionToken(successResponse.getSessionToken());
                        });
                    }
                }
                );
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