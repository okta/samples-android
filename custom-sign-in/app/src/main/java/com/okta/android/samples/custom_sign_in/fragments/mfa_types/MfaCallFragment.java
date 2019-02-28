package com.okta.android.samples.custom_sign_in.fragments.mfa_types;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import com.okta.authn.sdk.resource.VerifyFactorRequest;
import com.okta.authn.sdk.resource.VerifyPassCodeFactorRequest;
import com.okta.authn.sdk.resource.VerifyPushFactorRequest;

public class MfaCallFragment extends BaseFragment {
    private String TAG = "MfaCall";
    public static final String FACTOR_ID_KEY = "FACTOR_ID_KEY";
    public static final String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    public static final String PHONE_NAME_KEY = "PHONE_NAME_KEY";
    private TextView phoneNumberTextView;
    private Button callBtn;
    private Button verifyBtn;
    private EditText codeEditText;

    private String phoneNumber;

    public static Fragment createFragment(String stateToken, Factor factor) {
        MfaCallFragment fragment = new MfaCallFragment();

        Bundle arguments = new Bundle();
        arguments.putString(FACTOR_ID_KEY, factor.getId());
        arguments.putString(STATE_TOKEN_KEY, stateToken);
        arguments.putString(PHONE_NAME_KEY, (String) factor.getProfile().get("phoneNumber"));

        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.mfa_call_code_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.phoneNumberTextView = view.findViewById(R.id.phonenumber_textview);
        this.codeEditText = view.findViewById(R.id.code_edittext);
        this.callBtn = view.findViewById(R.id.call_btn);
        this.verifyBtn = view.findViewById(R.id.verify_btn);

        String factorId = getArguments().getString(FACTOR_ID_KEY);
        String stateToken = getArguments().getString(STATE_TOKEN_KEY);
        this.phoneNumber = getArguments().getString(PHONE_NAME_KEY);

        initView(factorId, stateToken, phoneNumber);
    }

    public void initView(String factorId, String stateToken, String phoneNumber) {
        this.phoneNumberTextView.setText(phoneNumber);

        callBtn.setOnClickListener((v) -> {
            sendCall(factorId, stateToken);
        });
        verifyBtn.setOnClickListener((v) -> {
            verifyCode(factorId, stateToken);
        });
    }

    private void sendCall(String factorId, String stateToken) {
        VerifyFactorRequest pushVerifyRequest = authenticationClient.instantiate(VerifyPassCodeFactorRequest.class)
                .setStateToken(stateToken);

        verifyFactor(factorId, pushVerifyRequest);
    }

    private void verifyCode(String factorId, String stateToken) {
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
    }

    private void verifyFactor(String factorId, VerifyFactorRequest request) {
        KeyboardUtil.hideSoftKeyboard(getActivity());
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
                            showMessage(String.format(getString(R.string.mfa_call_sent), phoneNumberTextView));
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