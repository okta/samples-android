package com.okta.totp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.okta.totp.storage.TokensRepository;
import com.okta.totp.util.TokensFactory;

import org.json.JSONException;

import java.security.GeneralSecurityException;

public class ManualCodeEntryActivity extends AppCompatActivity {
    public static final String RESULT_CODE_KEY = "RESULT_CODE_KEY";
    public static final String RESULT_KEY = "RESULT_KEY";
    TextInputEditText nameEditText;
    TextInputEditText issuerEditText;
    TextInputEditText keyEditText;
    TokensRepository tokensRepository;
    TokensFactory tokensFactory;

    public static Intent createIntent(Context context, int resultCode, String resultKey) {
        Intent intent = new Intent(context, ManualCodeEntryActivity.class);
        intent.putExtra(RESULT_CODE_KEY, resultCode);
        intent.putExtra(RESULT_KEY, resultKey);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_code_entry_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        nameEditText = findViewById(R.id.name_edit_text);
        issuerEditText = findViewById(R.id.issuer_edit_text);
        keyEditText = findViewById(R.id.key_edit_text);

        tokensRepository = ServiceLocator.provideTokensRepository(this);
        tokensFactory = ServiceLocator.provideTokensFactory(this);

        findViewById(R.id.done_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void addItem() {
        String name = nameEditText.getText().toString();
        String issuer = issuerEditText.getText().toString();
        String key = keyEditText.getText().toString();
        if (name.isEmpty() || issuer.isEmpty() || key.isEmpty()) {
            showMessage("All fields are required!");
            return;
        }
        try {
            tokensRepository.addToken(tokensFactory.createToken(name, issuer, key));
        } catch (JSONException e) {
            showMessage(e.getMessage());
        } catch (GeneralSecurityException e) {
            showMessage(getString(R.string.encryption_error)+":"+e.getLocalizedMessage());
        }

        Intent intent = new Intent();
        intent.putExtra(getIntent().getStringExtra(RESULT_KEY), true);
        setResult(getIntent().getIntExtra(RESULT_CODE_KEY, -1), intent);
        finish();
    }
}
