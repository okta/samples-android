package com.okta.totp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.okta.totp.barcode.BarcodeCaptureActivity;
import com.okta.totp.model.Token;
import com.okta.totp.storage.TokensRepository;
import com.okta.totp.util.TokensFactory;
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionButton;
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionHelper;
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionLayout;
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RFACLabelItem;
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RapidFloatingActionContentLabelList;

import org.json.JSONException;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RapidFloatingActionContentLabelList.OnRapidFloatingActionContentLabelListListener, TokenAdapter.ItemRemoveListener {
    private final int RESULT_CODE = 1000;
    private final int BARCODE_RESULT_CODE = 2000;
    private final String RESULT_KEY = "RESULT_KEY";
    private RapidFloatingActionHelper fabHelper;
    private RecyclerView recyclerView;
    private TokenAdapter tokenAdapter;
    private TokensRepository tokensRepository;
    private TokensFactory tokensFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tokensRepository = ServiceLocator.provideTokensRepository(this);
        tokensFactory = ServiceLocator.provideTokensFactory(this);

        initRecyclerView();
        initFabBtn();
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Token> tokens = new ArrayList<>();
        try {
            tokens = tokensRepository.getTokens();
        } catch (JSONException e) {
            showMessage(e.getMessage());
        }
        tokenAdapter = new TokenAdapter(tokens, this);
        recyclerView.setAdapter(tokenAdapter);
    }

    private void initFabBtn() {
        RapidFloatingActionButton fab = findViewById(R.id.fab);
        RapidFloatingActionLayout fabLayout = findViewById(R.id.fab_layout);

        RapidFloatingActionContentLabelList rfaContent = new RapidFloatingActionContentLabelList(this);
        rfaContent.setOnRapidFloatingActionContentLabelListListener(this);
        List<RFACLabelItem> items = new ArrayList<>();
        items.add(new RFACLabelItem<Integer>()
                .setLabel(getString(R.string.scan_qr_code))
        );
        items.add(new RFACLabelItem<Integer>()
                .setLabel(getString(R.string.input_manually))
        );

        rfaContent.setItems(items);
        fabHelper = new RapidFloatingActionHelper(
                this,
                fabLayout,
                fab,
                rfaContent
        ).build();
    }

    private void navigateToScanQrCode() {
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        startActivityForResult(intent, BARCODE_RESULT_CODE);

    }

    private void navigateToEnterManually() {
        startActivityForResult(ManualCodeEntryActivity.createIntent(this, RESULT_CODE, RESULT_KEY), RESULT_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CODE) {
            if(data.getBooleanExtra(RESULT_KEY, false)) {
                updateTokens();
            }
        } else if(requestCode == BARCODE_RESULT_CODE) {
            if(resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    addTokenFormBarcodeScanner(barcode);
                }
            }
        }
    }

    private void addTokenFormBarcodeScanner(Barcode barcode) {
        try {
            Uri uri = Uri.parse(barcode.displayValue);
            tokensRepository.addToken(tokensFactory.createToken(uri));
            updateTokens();
        } catch (JSONException e) {
            showMessage(e.getMessage());
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage());
        } catch (GeneralSecurityException e) {
            showMessage(getString(R.string.encryption_error)+":"+e.getLocalizedMessage());
        }
    }

    private void updateTokens() {
        try {
            tokenAdapter.updateTokens(tokensRepository.getTokens());
            tokenAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            showMessage(e.getMessage());
        }
    }

    private void removeToken(Token token) {
        try {
            tokensRepository.removeToken(token);
            tokenAdapter.remove(token);
            tokenAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            showMessage(e.getMessage());
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemove(Token token) {
        removeToken(token);
    }

    @Override
    public void onRFACItemLabelClick(int position, RFACLabelItem item) {
        handleFabItemClick(item);
        fabHelper.toggleContent();
    }

    @Override
    public void onRFACItemIconClick(int position, RFACLabelItem item) {
        handleFabItemClick(item);
        fabHelper.toggleContent();
    }

    private void handleFabItemClick(RFACLabelItem item) {
        if (item.getLabel().equalsIgnoreCase(getString(R.string.scan_qr_code))) {
            navigateToScanQrCode();
        } else if (item.getLabel().equalsIgnoreCase(getString(R.string.input_manually))) {
            navigateToEnterManually();
        }
    }
}
