package com.okta.android.samples.custom_sign_in.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
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
import com.okta.android.samples.custom_sign_in.fragments.mfa_types.MfaSMSFragment;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.appauth.android.AuthenticationError;
import com.okta.appauth.android.OktaAppAuth;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeSignInWithMFAFragment extends BaseFragment implements IMFAResult {
    private String TAG = "NativeSignInWithMFA";
    private static final int SESSION_TOKEN_RES_CODE = 1;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

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
        loadingView.show();
        executor.submit(() -> {
            try {
                authenticationClient.authenticate(login, password.toCharArray(), null, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        loginEditText.post(() -> {
                            loadingView.hide();
                            messageView.showMessage(authenticationResponse.toString());
                        });
                    }

                    @Override
                    public void handleMfaRequired(AuthenticationResponse mfaRequiredResponse) {
                        loginEditText.post(() -> {
                            loadingView.hide();
                            showDialogWithPromptFactors(mfaRequiredResponse.getStateToken(), mfaRequiredResponse.getFactors());
                        });
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse successResponse) {
                        String sessionToken = successResponse.getSessionToken();
                        authenticateViaOktaAndroidSDK(sessionToken);
                    }
                });
            } catch (Exception e) {
                loginEditText.post(() -> {
                    loadingView.hide();
                    messageView.showMessage(e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onSuccess(String sessionToken) {
        loadingView.show();
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
                loginEditText.post(() -> {
                    loadingView.hide();
                    showUserInfo();
                });
            }

            @Override
            public void onTokenFailure(@NonNull AuthenticationError authenticationError) {
                loginEditText.post(() -> {
                    loadingView.hide();
                    messageView.showMessage(authenticationError.getLocalizedMessage());
                    navigation.close();
                });
            }
        });
    }

    @MainThread
    private void showDialogWithPromptFactors(String stateToken, List<Factor> factors) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        alertBuilder.setTitle(getString(R.string.select_mfa));

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.select_dialog_item);
        for(Factor factor : factors) {
            arrayAdapter.add(factor.getType().toString().toUpperCase());
        }

        alertBuilder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        alertBuilder.setAdapter(arrayAdapter, (dialog, which) -> {
            String factorType = arrayAdapter.getItem(which);
            Factor selectedFactor = null;
            for(Factor factor:factors) {
                if(factor.getType().toString().equalsIgnoreCase(factorType)) {
                    selectedFactor = factor;
                    break;
                }
            }
            dialog.dismiss();
            showFactor(stateToken, selectedFactor);
        });
        alertBuilder.show();
    }

    private void showFactor(String stateToken, Factor factor) {
        switch (factor.getType()) {
            case SMS:
                String factorId = factor.getId();
                String phoneNumber = (String) factor.getProfile().get("phoneNumber");

                Fragment fragment = MfaSMSFragment.createFragment(factorId, stateToken, phoneNumber);
                fragment.setTargetFragment(this, SESSION_TOKEN_RES_CODE);

                this.navigation.push(fragment);
                break;
        }
    }
}
