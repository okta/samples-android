package com.okta.android.samples.custom_sign_in;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.okta.android.samples.custom_sign_in.base.ContainerActivity;
import com.okta.android.samples.custom_sign_in.fragments.PasswordRecoveryFragment;
import com.okta.android.samples.custom_sign_in.fragments.RecoveryQuestionFragment;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class PasswordRecoveryActivity extends ContainerActivity {
    private static String RESET_PASSWORD_PATH = "/signin/reset-password/";
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


    public static Intent createPasswordRecovery(Context context) {
        return new Intent(context, PasswordRecoveryActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getDeepLinkPath(getIntent());
        if(path != null) {
            if (path.startsWith(RESET_PASSWORD_PATH)) {
                handleResetPassword(path);
            }
        } else {
            this.navigation.present(new PasswordRecoveryFragment());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String path = getDeepLinkPath(getIntent());
        if(path != null) {
            if (path.startsWith(RESET_PASSWORD_PATH)) {
                handleResetPassword(path);
            }
        }

    }

    private String getDeepLinkPath(Intent intent) {
        if (intent.getData() != null) {
            String path = intent.getData().getPath();
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    private void handleResetPassword(String path) {
        String[] elements = path.split("/");
        if(elements.length == 0) {
            finish();
        }

        String token = elements[elements.length-1];

        show();
        executor.submit(() -> {
            try {
                AuthenticationResponse response = provideAuthenticationClient().verifyRecoveryToken(token, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        runOnUiThread(() -> {
                            showMessage(authenticationResponse.toString());
                            finish();
                        });
                    }

                    @Override
                    public void handleRecovery(AuthenticationResponse recovery) {
                        String stateToken = recovery.getStateToken();
                        if(stateToken == null)
                            throw new IllegalArgumentException("Missed stateToken");

                        Map<String, String> recovery_question = recovery.getUser().getRecoveryQuestion();
                        String question = recovery_question.get("question");
                        if(question == null)
                            throw new IllegalArgumentException("Missed question");


                        runOnUiThread(() -> {
                            hide();
                            navigation.present(RecoveryQuestionFragment.createFragment(stateToken, question));
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hide();
                    showMessage(e.getLocalizedMessage());
                    finish();
                });
            }
        });
    }
}
