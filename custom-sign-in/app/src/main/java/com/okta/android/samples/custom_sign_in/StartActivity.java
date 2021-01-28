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
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    public static Intent createIntent(Context context) {
        return new Intent(context, StartActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ((Button)findViewById(R.id.native_sign_in)).setOnClickListener(v ->
                startActivity(NativeSignInActivity.createNativeSignIn(this)));

        ((Button)findViewById(R.id.native_sign_in_mfa)).setOnClickListener(v ->
                startActivity(NativeSignInActivity.createNativeSignInWithMFA(this)));

        ((Button)findViewById(R.id.password_reset)).setOnClickListener(v ->
                startActivity(RecoveryActivity.createPasswordRecovery(this)));
    }
}

