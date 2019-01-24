package com.okta.android.samples.custom_sign_in;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ((Button)findViewById(R.id.native_sing_in)).setOnClickListener(v ->
                startActivity(new Intent(StartActivity.this, NativeSignInActivity.class)));

        ((Button)findViewById(R.id.password_reset)).setOnClickListener(v ->
                startActivity(new Intent(StartActivity.this, PasswordRecoverActivity.class)));

        ((Button)findViewById(R.id.native_sing_in_mfa)).setOnClickListener(v ->
                startActivity(new Intent(StartActivity.this, NativeSignInWithMFAActivity.class)));
    }
}

