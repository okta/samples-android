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
package com.okta.android.samples.custom_sign_in;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;

import com.okta.android.samples.custom_sign_in.base.ContainerActivity;
import com.okta.android.samples.custom_sign_in.fragments.PasswordRecoveryFragment;
import com.okta.android.samples.custom_sign_in.fragments.RecoveryQuestionFragment;

public class RecoveryActivity extends ContainerActivity {
    private static String MODE_KEY = "MODE_KEY";
    private static String STATE_TOKEN_KEY = "STATE_TOKEN_KEY";
    private static String QUESTION_KEY = "QUESTION_KEY";

    enum MODE {
        PASSWORD_RECOVERY,
        PASSWORD_RECOVERY_QUESTION,
        UNKNOWN
    }

    public static Intent createPasswordRecovery(Context context) {
        Intent intent = new Intent(context, RecoveryActivity.class);
        intent.putExtra(MODE_KEY, MODE.PASSWORD_RECOVERY.ordinal());
        return intent;

    }

    public static Intent createRecoveryQuestion(Context context, String question, String stateToken) {
        Intent intent = new Intent(context, RecoveryActivity.class);
        intent.putExtra(MODE_KEY, MODE.PASSWORD_RECOVERY_QUESTION.ordinal());
        intent.putExtra(QUESTION_KEY, question);
        intent.putExtra(STATE_TOKEN_KEY, stateToken);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private Fragment getFragmentByModeId(int id, Intent intent) {
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
        } else {
            finish();
        }
    }
}
