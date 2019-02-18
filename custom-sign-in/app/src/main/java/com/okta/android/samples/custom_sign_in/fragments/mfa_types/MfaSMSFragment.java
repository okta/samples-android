package com.okta.android.samples.custom_sign_in.fragments.mfa_types;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.VerifyFactorRequest;
import com.okta.authn.sdk.resource.VerifyPassCodeFactorRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MfaSMSFragment extends BaseFragment {
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final String FACTOR_ID_KEY = "FACTOR_ID_KEY";
    public static final String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    public static final String PHONE_NUMBER_KEY = "PHONE_NUMBER_KEY";
    private TextView phoneNumberTextView;
    private EditText codeEditText;
    private Button sendBtn;
    private Button verifyBtn;

    public static Fragment createFragment(String factorId, String stateToken, String phoneNumber) {
        MfaSMSFragment fragment = new MfaSMSFragment();

        Bundle arguments = new Bundle();
        arguments.putString(FACTOR_ID_KEY, factorId);
        arguments.putString(STATE_TOKEN_KEY, stateToken);
        arguments.putString(PHONE_NUMBER_KEY, phoneNumber);

        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.mfa_sms_code_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.phoneNumberTextView = view.findViewById(R.id.phonenumber_textview);
        this.codeEditText = view.findViewById(R.id.code_edittext);
        this.sendBtn = view.findViewById(R.id.resend_btn);
        this.verifyBtn = view.findViewById(R.id.verify_btn);

        String factorId = getArguments().getString(FACTOR_ID_KEY);
        String stateToken = getArguments().getString(STATE_TOKEN_KEY);
        String phoneNumber = getArguments().getString(PHONE_NUMBER_KEY);

        initView(factorId, stateToken, phoneNumber);
    }

    public void initView(String factorId, String stateToken, String phoneNumber) {
        phoneNumberTextView.setText(phoneNumber);

        sendBtn.setOnClickListener((v) -> {
            sendCode(factorId, stateToken);
        });
        verifyBtn.setOnClickListener((v) -> {
            verifyCode(factorId, stateToken);
        });
    }

    private void sendCode(String factorId, String stateToken) {
        VerifyFactorRequest smsVerifyRequest = authenticationClient.instantiate(VerifyPassCodeFactorRequest.class)
                .setStateToken(stateToken);

        verifyFactor(factorId, smsVerifyRequest);
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
        loadingView.show();
        executor.submit(() -> {
            try {
                authenticationClient.verifyFactor(factorId, request, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        phoneNumberTextView.post(() -> {
                            loadingView.hide();
                            messageView.showMessage(authenticationResponse.toString());
                        });
                    }

                    @Override
                    public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {
                        phoneNumberTextView.post(() -> {
                            loadingView.hide();
                            messageView.showMessage(getString(R.string.mfa_sms_sent_code));
                        });
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse successResponse) {
                        phoneNumberTextView.post(() -> {
                            loadingView.hide();
                            sendSessionToken(successResponse.getSessionToken());
                        });
                    }
                });
            } catch (Exception e) {
                phoneNumberTextView.post(() -> {
                    loadingView.hide();
                    messageView.showMessage(e.getMessage());
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
