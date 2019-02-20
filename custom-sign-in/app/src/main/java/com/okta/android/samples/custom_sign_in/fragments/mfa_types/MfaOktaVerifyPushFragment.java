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
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import com.okta.authn.sdk.resource.VerifyFactorRequest;
import com.okta.authn.sdk.resource.VerifyPushFactorRequest;
import com.okta.sdk.resource.user.factor.FactorResultType;

import java.util.concurrent.TimeUnit;

public class MfaOktaVerifyPushFragment extends BaseFragment {

    public static final String FACTOR_ID_KEY = "FACTOR_ID_KEY";
    public static final String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    public static final String DEVICE_NAME_KEY = "DEVICE_NAME_KEY";
    private TextView deviceTextView;
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
                            showMessage(authenticationResponse.toString());
                        });
                    }

                    @Override
                    public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {
                        runOnUIThread(() -> {
                            if(mfaChallengeResponse.getFactorResult().equalsIgnoreCase(FactorResultType.WAITING.toString())) {
                                if (sendPushBtn.isEnabled()) {
                                    sendPushBtn.setEnabled(false);
                                    hideLoading();
                                    showMessage(String.format(getString(R.string.mfa_push_sent), deviceName));
                                }
                                runPushStatusChecking(factorId, mfaChallengeResponse.getStateToken());
                            } else if(mfaChallengeResponse.getFactorResult().equalsIgnoreCase(FactorResultType.REJECTED.toString())) {
                                sendPushBtn.setEnabled(true);
                                hideLoading();
                                showMessage(mfaChallengeResponse.toString());
                                navigation.close();
                            }
                        });
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse successResponse) {
                        if(Thread.interrupted()) {
                            return;
                        }
                        runOnUIThread(() -> {
                            hideLoading();
                            sendSessionToken(successResponse.getSessionToken());
                        });
                    }
                }
                );
            } catch (Exception e) {
                runOnUIThread(() -> {
                    hideLoading();
                    showMessage(e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    private void sendSessionToken(String sessionToken) {
        if(getTargetFragment() != null && (getTargetFragment() instanceof IMFAResult)) {
            ((IMFAResult)getTargetFragment()).onSuccess(sessionToken);
        }
    }
}