package com.okta.android.samples.custom_sign_in;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    public static Intent createIntent(Context context) {
        return new Intent(context, StartActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ((Button)findViewById(R.id.native_sing_in)).setOnClickListener(v ->
                startActivity(NativeSignInActivity.createNativeSignIn(this)));

        ((Button)findViewById(R.id.native_sing_in_mfa)).setOnClickListener(v ->
                startActivity(NativeSignInActivity.createNativeSingInWithMFA(this)));

        ((Button)findViewById(R.id.password_reset)).setOnClickListener(v ->
                startActivity(RecoveryActivity.createPasswordRecovery(this)));
    }
}

