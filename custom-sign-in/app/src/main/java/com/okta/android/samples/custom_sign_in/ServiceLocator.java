package com.okta.android.samples.custom_sign_in;

import android.content.Context;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.storage.SimpleOktaStorage;

import com.okta.android.samples.custom_sign_in.rx_client.RxOkta;
import com.okta.android.samples.custom_sign_in.rx_client.RxAuthClient;

public class ServiceLocator {
    private static volatile AuthClient mAuth;
    private static volatile RxAuthClient mRxAuth;

    public static AuthClient provideWebAuthClient(Context context) {
        AuthClient localAuth = mAuth;
        if(localAuth == null) {
            synchronized (ServiceLocator.class) {
                localAuth = mAuth;
                if(localAuth == null) {
                    OIDCConfig mOidcConfig = new OIDCConfig.Builder()
                            .withJsonFile(context, R.raw.okta_oidc_config)
                            .create();

                    mAuth = localAuth = new Okta.AuthBuilder()
                            .withConfig(mOidcConfig)
                            .withContext(context.getApplicationContext())
                            .withStorage(new SimpleOktaStorage(context))
                            .create();
                }
            }
        }

        return localAuth;
    }

    public static RxAuthClient provideRxWebAuthClient(Context context) {
        RxAuthClient localAuth = mRxAuth;
        if(localAuth == null) {
            synchronized (ServiceLocator.class) {
                localAuth = mRxAuth;
                if (localAuth == null) {

                    OIDCConfig mOidcConfig = new OIDCConfig.Builder()
                            .withJsonFile(context, R.raw.okta_oidc_config)
                            .create();

                    mRxAuth = localAuth = new RxOkta.AuthBuilder()
                            .withConfig(mOidcConfig)
                            .withContext(context)
                            .withStorage(new SimpleOktaStorage(context))
                            .create();
                }
            }
        }

        return localAuth;
    }
}
