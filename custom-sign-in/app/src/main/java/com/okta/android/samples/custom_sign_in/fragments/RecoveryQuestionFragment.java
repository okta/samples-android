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

import com.okta.android.samples.custom_sign_in.StartActivity;
import com.okta.android.samples.custom_sign_in.base.BaseFragment;
import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.util.KeyboardUtil;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecoveryQuestionFragment extends BaseFragment {
    private String TAG = "RecoveryQuestion";
    private static String QUESTION_KEY = "QUESTION_KEY";
    private static String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";

    private String question = null;
    private String token = null;

    Button answerQuestionBtn = null;
    EditText answerQuestionEditText = null;
    TextView questionTextView = null;

    public static RecoveryQuestionFragment createFragment(String token, String question) {
        RecoveryQuestionFragment fragment = new RecoveryQuestionFragment();

        Bundle arguments = new Bundle();
        arguments.putString(QUESTION_KEY, question);
        arguments.putString(STATE_TOKEN_KEY, token);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.answer_question_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        question = getArguments().getString(QUESTION_KEY);
        token = getArguments().getString(STATE_TOKEN_KEY);
        if(TextUtils.isEmpty(question) || TextUtils.isEmpty(token)) {
            return;
        }

        answerQuestionEditText = view.findViewById(R.id.answer_question_edittext);
        answerQuestionBtn = view.findViewById(R.id.answer_question_btn);
        questionTextView = view.findViewById(R.id.question_textview);

        answerQuestionBtn.setOnClickListener(v -> answerQuestion());
        questionTextView.setText(question);
    }

    private void answerQuestion() {
        if(TextUtils.isEmpty(answerQuestionEditText.getText())) {
            answerQuestionEditText.setError(getString(R.string.empty_field_error));
        } else {
            answerQuestionEditText.setError(null);
        }

        KeyboardUtil.hideSoftKeyboard(getActivity());
        String answer = answerQuestionEditText.getText().toString();
        showLoading();
        submit(() -> {
            try {
                AuthenticationResponse response = authenticationClient.answerRecoveryQuestion(answer, token, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
                        runOnUIThread(() -> {
                            hideLoading();
                            showMessage(String.format(getString(R.string.not_handle_message), authenticationResponse.getStatus().name()));
                            navigation.close();
                        });
                    }

                    public void handlePasswordReset(AuthenticationResponse passwordReset) {
                        // Get next action
                        String stateToken = passwordReset.getStateToken();
                        if (stateToken == null)
                            throw new IllegalArgumentException("Missed stateToken");

                        runOnUIThread(() ->  {
                            hideLoading();
                            navigation.present(PasswordResetFragment.createFragment(stateToken, passwordReset));
                        });
                    }

                    @Override
                    public void handleSuccess(AuthenticationResponse successResponse) {
                        runOnUIThread(() ->  {
                            hideLoading();
                            showMessage(successResponse.getRecoveryType() + " " + successResponse.getStatusString());
                            navigation.close();
                            startActivity(StartActivity.createIntent(getContext()));
                        });
                    }
                });


            } catch (AuthenticationException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                runOnUIThread(() -> {
                    showMessage(e.getLocalizedMessage());
                    hideLoading();
                });
            }
        });
    }
}