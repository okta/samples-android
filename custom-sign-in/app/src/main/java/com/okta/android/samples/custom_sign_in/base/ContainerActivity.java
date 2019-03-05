package com.okta.android.samples.custom_sign_in.base;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.okta.android.samples.custom_sign_in.BuildConfig;
import com.okta.android.samples.custom_sign_in.R;
import com.okta.android.samples.custom_sign_in.util.INavigation;
import com.okta.android.samples.custom_sign_in.util.NavigationHelper;
import com.okta.android.samples.custom_sign_in.util.OktaProgressDialog;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;


public class ContainerActivity extends AppCompatActivity implements
        ILoadingView,
        IMessageView,
        IOktaAuthenticationClientProvider,
        INavigationProvider {
    AuthenticationClient authenticationClient = null;
    OktaProgressDialog oktaProgressDialog = null;
    protected INavigation navigation = null;
    private FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        this.oktaProgressDialog = new OktaProgressDialog(this);
        this.navigation = new NavigationHelper(this, fragmentManager, R.id.container);

        init();

    }

    private void init() {
        authenticationClient = AuthenticationClients.builder()
                .setOrgUrl(BuildConfig.BASE_URL)
                .build();
    }

    @Override
    public void onBackPressed() {
        int fragmentsCount = fragmentManager.getBackStackEntryCount();
        if (fragmentsCount == 1) {
            finish();
        } else if(fragmentsCount > 1) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }

    @Override
    public void show() {
        oktaProgressDialog.show();
    }

    @Override
    public void show(String message) {
        oktaProgressDialog.show(message);
    }

    @Override
    public void hide() {
        oktaProgressDialog.hide();
    }

    @Override
    public AuthenticationClient provideAuthenticationClient() {
        return authenticationClient;
    }

    @Override
    public INavigation provideNavigation() {
        return navigation;
    }
}