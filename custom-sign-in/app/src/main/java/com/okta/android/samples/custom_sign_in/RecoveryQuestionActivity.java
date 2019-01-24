package com.okta.android.samples.custom_sign_in;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.android.samples.custom_sign_in.util.OktaProgressDialog;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Link;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecoveryQuestionActivity extends AppCompatActivity {
    private String TAG = "QuestionActivity";
    private static String QUESTION_KEY = "QUESTION_KEY";
    private static String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    AuthenticationClient authenticationClient = null;
    private String question = null;
    private String token = null;

    Button answerQuestionBtn = null;
    EditText answerQuestionEditText = null;
    TextView questionTextView = null;
    OktaProgressDialog oktaProgressDialog = null;

    public static Intent createIntent(Context context, String question, String stateToken) {
        Intent intent = new Intent(context, RecoveryQuestionActivity.class);
        intent.putExtra(QUESTION_KEY,question);
        intent.putExtra(STATE_TOKEN_KEY,stateToken);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_question);

        oktaProgressDialog = new OktaProgressDialog(this);

        question = getIntent().getStringExtra(QUESTION_KEY);
        token = getIntent().getStringExtra(STATE_TOKEN_KEY);
        if(TextUtils.isEmpty(question) || TextUtils.isEmpty(token)) {
            return;
        }

        answerQuestionEditText = findViewById(R.id.answer_question_edittext);
        answerQuestionBtn = findViewById(R.id.answer_question_btn);
        questionTextView = findViewById(R.id.question_textview);

        answerQuestionBtn.setOnClickListener(v -> answerQuestion());
        questionTextView.setText(question);

        init();
    }

    private void init() {
        authenticationClient = AuthenticationClients.builder()
                .setOrgUrl(BuildConfig.BASE_URL)
                .build();
    }

    private void answerQuestion() {
        if(TextUtils.isEmpty(answerQuestionEditText.getText())) {
            answerQuestionEditText.setError(getString(R.string.empty_field_error));
        } else {
            answerQuestionEditText.setError(null);
        }

        KeyboardUtil.hideSoftKeyboard(this);
        String answer = answerQuestionEditText.getText().toString();
        oktaProgressDialog.show();
        executor.submit(() -> {
            try {
                AuthenticationResponse response = authenticationClient.answerRecoveryQuestion(answer, token, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) { }
                });

                // Get next action
                String stateToken = response.getStateToken();
                if(stateToken == null)
                    throw new IllegalArgumentException("Missed stateToken");
                Link next = response.getLinks().get("next");
                String name = next.getName();
                if(!"resetPassword".equalsIgnoreCase(name))
                    throw new IllegalArgumentException("Missed resetPassword");

                startActivity(PasswordResetActivity.createIntent(this, stateToken, getPolicy(response)));
                runOnUiThread(() -> oktaProgressDialog.hide());
                finish();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showMessage(e.getLocalizedMessage());
                    oktaProgressDialog.hide();
                });
                Log.e(TAG, Log.getStackTraceString(e));
            }
        });
    }

    private Map<String, Integer> getPolicy(AuthenticationResponse response) {
        Map<String, Object> embedded = response.getEmbedded();
        if(embedded == null)
            return null;
        Map<String, Object> policy = (Map<String, Object>) embedded.get("policy");
        if(policy == null)
            return null;
        Map<String, Integer> complexity = (Map<String, Integer>) policy.get("complexity");
        if(complexity == null)
            return null;

        return complexity;
    }

    private void showMessage(String message) {
        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }
}
