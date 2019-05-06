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
package com.okta.android.samples.browser_sign_in;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.util.AuthorizationException;


public class BrowserSignInActivity extends AppCompatActivity {
    private String TAG = "BrowserSignIn";
    private static final String EXTRA_FAILED = "failed";
    private OktaProgressDialog oktaProgressDialog;

    private WebAuthClient mWebAuth;
    private SessionClient mSessionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_sign_in);

        oktaProgressDialog = new OktaProgressDialog(this);

        ((Button) findViewById(R.id.browser_sign_in)).setOnClickListener(v -> signIn());

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebAuth.isInProgress()) {
            oktaProgressDialog.show();
        }
    }

    private void init() {

        mWebAuth = ServiceLocator.provideWebAuthClient(this);
        mSessionClient = mWebAuth.getSessionClient();

        if (mSessionClient.isAuthenticated()) {
            showUserInfo();
        }

        setupCallback();
    }

    void setupCallback() {
        ResultCallback<AuthorizationStatus, AuthorizationException> callback =
                new ResultCallback<AuthorizationStatus, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull AuthorizationStatus status) {
                        Log.d("SampleActivity", "AUTHORIZED");

                        if (status == AuthorizationStatus.AUTHORIZED) {
                            oktaProgressDialog.hide();

                            Log.i(TAG, "User is already authenticated, proceeding " +
                                    "to token activity");
                            showUserInfo();
                        } else if (status == AuthorizationStatus.SIGNED_OUT) {
                            displayAuth();
                        }
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "CANCELED!");
                        oktaProgressDialog.hide();
                        showMessage(getString(R.string.auth_canceled));
                    }

                    @Override
                    public void onError(@Nullable String msg, AuthorizationException error) {
                        oktaProgressDialog.hide();
                        showMessage(getString(R.string.init_error)
                                + ":"
                                + error.errorDescription);
                    }
                };

        mWebAuth.registerCallback(callback, this);
    }


    private void signIn() {
        mWebAuth.signIn(this, null);
    }

    private void displayAuth() {
        findViewById(R.id.auth_container).setVisibility(View.VISIBLE);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showUserInfo() {
        startActivity(new Intent(BrowserSignInActivity.this, UserInfoActivity.class));
        finish();
    }
}
