package com.okta.android.samples.custom_sign_in;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.okta.android.samples.custom_sign_in.base.ContainerActivity;
import com.okta.android.samples.custom_sign_in.fragments.PasswordRecoveryFragment;
import com.okta.android.samples.custom_sign_in.fragments.RecoveryQuestionFragment;

public class PasswordRecoveryActivity extends ContainerActivity {
    private static String MODE_KEY = "MODE_KEY";
    private static String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    private static String QUESTION_KEY = "QUESTION_KEY";

    enum MODE {
        PASSWORD_RECOVERY,
        PASSWORD_RECOVERY_QUESTION,
        UNKNOWN
    }


    public static Intent createPasswordRecovery(Context context) {
        Intent intent = new Intent(context, PasswordRecoveryActivity.class);
        intent.putExtra(MODE_KEY, MODE.PASSWORD_RECOVERY.ordinal());
        return intent;
    }

    public static Intent createPasswordRecoveryQuestion(Context context, String question, String stateToken) {
        Intent intent = new Intent(context, PasswordRecoveryActivity.class);
        intent.putExtra(MODE_KEY, MODE.PASSWORD_RECOVERY_QUESTION.ordinal());
        intent.putExtra(QUESTION_KEY, question);
        intent.putExtra(STATE_TOKEN_KEY, stateToken);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private Fragment getFragmentByModeId(int id, Intent intent){
        if(id == MODE.PASSWORD_RECOVERY.ordinal()) {
            return new PasswordRecoveryFragment();
        } else if(id == MODE.PASSWORD_RECOVERY_QUESTION.ordinal()) {
            String question = intent.getStringExtra(QUESTION_KEY);
            String token = intent.getStringExtra(STATE_TOKEN_KEY);
            if(TextUtils.isEmpty(question) || TextUtils.isEmpty(token)) {
                return null;
            }

            return RecoveryQuestionFragment.createFragment(token, question);
        } else {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment fragment = getFragmentByModeId(getIntent().getIntExtra(MODE_KEY, -1), getIntent());
        if(fragment != null) {
            this.navigation.present(fragment);
        }
    }
}
