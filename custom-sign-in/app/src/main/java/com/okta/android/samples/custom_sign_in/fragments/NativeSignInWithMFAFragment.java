package com.okta.android.samples.custom_sign_in.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.UserInfoActivity;
import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.android.samples.custom_sign_in.base.IOktaAppAuthClientProvider;
import com.okta.android.samples.custom_sign_in.fragments.mfa_types.IMFAResult;
import com.okta.android.samples.custom_sign_in.fragments.mfa_types.MfaCallFragment;
import com.okta.android.samples.custom_sign_in.fragments.mfa_types.MfaGoogleVerifyCodeFragment;
import com.okta.android.samples.custom_sign_in.fragments.mfa_types.MfaOktaVerifyCodeFragment;
import com.okta.android.samples.custom_sign_in.fragments.mfa_types.MfaOktaVerifyPushFragment;
import com.okta.android.samples.custom_sign_in.fragments.mfa_types.MfaSMSFragment;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.appauth.android.AuthenticationError;
import com.okta.appauth.android.OktaAppAuth;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;

import java.util.List;

public class NativeSignInWithMFAFragment extends BaseFragment implements IMFAResult {
    private String TAG = "NativeSignInWithMFA";
    private static final int SESSION_TOKEN_RES_CODE = 1;


    private EditText loginEditText;
    private EditText passwordEditText;
    private Button signInBtn;

    private OktaAppAuth oktaAppAuth;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IOktaAppAuthClientProvider){
            oktaAppAuth = ((IOktaAppAuthClientProvider)context).provideOktaAppAuthClient();
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
        signInBtn = view.findViewById(R.id.sign_in_btn);
        signInBtn.setOnClickListener(v -> {
            signIn();
        });

    }

    private void signIn() {
        String login = loginEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(login)) {
            loginEditText.setError(getString(R.string.empty_field_error));
            return;
        } else if(TextUtils.isEmpty(password)) {
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
                    public void handleMfaRequired(AuthenticationResponse mfaRequiredResponse) {
                        runOnUIThread(() -> {
                            hideLoading();
                            showDialogWithPromptFactors(mfaRequiredResponse.getStateToken(), mfaRequiredResponse.getFactors());
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

    @Override
    public void onSuccess(String sessionToken) {
        showLoading();
        authenticateViaOktaAndroidSDK(sessionToken);
    }

    @MainThread
    private void showUserInfo() {
        startActivity(UserInfoActivity.createIntent(getContext()));
        navigation.close();
    }


    private void authenticateViaOktaAndroidSDK(String sessionToken) {
        this.oktaAppAuth.authenticate(sessionToken, new OktaAppAuth.OktaNativeAuthListener() {
            @Override
            public void onSuccess() {
                runOnUIThread(() -> {
                    hideLoading();
                    showUserInfo();
                });
            }

            @Override
            public void onTokenFailure(@NonNull AuthenticationError authenticationError) {
                runOnUIThread(() -> {
                    hideLoading();
                    showMessage(authenticationError.getLocalizedMessage());
                    navigation.close();
                });
            }
        });
    }

    @MainThread
    private void showDialogWithPromptFactors(String stateToken, List<Factor> factors) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        alertBuilder.setTitle(getString(R.string.select_mfa));

        final ArrayAdapter<FactorItemModel> arrayAdapter = new ArrayAdapter<FactorItemModel>(getContext(), android.R.layout.select_dialog_item);
        for(Factor factor : factors) {
            arrayAdapter.add(new FactorItemModel(factor));
        }

        alertBuilder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        alertBuilder.setAdapter(arrayAdapter, (dialog, which) -> {
            FactorItemModel factorItemModel = arrayAdapter.getItem(which);
            Factor selectedFactor = null;
            if(factorItemModel != null)
                selectedFactor = factorItemModel.getFactor();

            dialog.dismiss();
            if(selectedFactor != null)
                showFactor(stateToken, selectedFactor);
        });
        alertBuilder.show();
    }

    private void showFactor(String stateToken, Factor factor) {
        Fragment fragment;
        switch (factor.getType()) {
            case SMS:
                fragment = MfaSMSFragment.createFragment(stateToken, factor);
                fragment.setTargetFragment(this, SESSION_TOKEN_RES_CODE);

                this.navigation.push(fragment);
                break;
            case PUSH:
                fragment = MfaOktaVerifyPushFragment.createFragment(stateToken, factor);
                fragment.setTargetFragment(this, SESSION_TOKEN_RES_CODE);

                this.navigation.push(fragment);
                break;
            case TOKEN_SOFTWARE_TOTP:
                if("GOOGLE".equalsIgnoreCase(factor.getVendorName())) {
                    fragment = MfaGoogleVerifyCodeFragment.createFragment(stateToken, factor);
                } else {
                    fragment = MfaOktaVerifyCodeFragment.createFragment(stateToken, factor);
                }
                fragment.setTargetFragment(this, SESSION_TOKEN_RES_CODE);

                this.navigation.push(fragment);
                break;
            case CALL:
                fragment = MfaCallFragment.createFragment(stateToken, factor);
                fragment.setTargetFragment(this, SESSION_TOKEN_RES_CODE);

                this.navigation.push(fragment);
                break;
        }
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

    private static class FactorItemModel {
        private Factor factor;

        public FactorItemModel(Factor factor) {
            this.factor = factor;
        }

        public Factor getFactor() {
            return factor;
        }

        @Override
        public String toString() {
            return factor.getType().toString().toUpperCase() + " [" + factor.getVendorName() + "]";
        }
    }
}
