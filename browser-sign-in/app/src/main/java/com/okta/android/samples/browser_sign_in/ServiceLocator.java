package com.okta.android.samples.browser_sign_in;

import android.content.Context;

import com.okta.android.samples.browser_sign_in.rxClient.RxOkta;
import com.okta.android.samples.browser_sign_in.rxClient.RxWebAuthClient;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.storage.SimpleOktaStorage;

public class ServiceLocator {
    private final static String FIRE_FOX = "org.mozilla.firefox";
    private static volatile WebAuthClient mWebAuth;
    private static volatile RxWebAuthClient  mRxWebAuth;

    public static WebAuthClient provideWebAuthClient(Context context) {
        WebAuthClient localWebAuth = mWebAuth;
        if(localWebAuth == null) {
            synchronized (ServiceLocator.class) {
                localWebAuth = mWebAuth;
                if(localWebAuth == null) {
                    OIDCConfig mOidcConfig = new OIDCConfig.Builder()
                            .withJsonFile(context, R.raw.okta_oidc_config)
                            .create();

                    mWebAuth = localWebAuth = new Okta.WebAuthBuilder()
                            .withConfig(mOidcConfig)
                            .withContext(context.getApplicationContext())
                            .withStorage(new SimpleOktaStorage(context))
                            .withTabColor(context.getResources().getColor(R.color.colorPrimary))
                            .supportedBrowsers(FIRE_FOX)
                            .create();
                }
            }
        }

        return localWebAuth;
    }

    public static RxWebAuthClient provideRxWebAuthClient(Context context) {
        RxWebAuthClient localWebAuth = mRxWebAuth;
        if(localWebAuth == null) {
            synchronized (ServiceLocator.class) {
                localWebAuth = mRxWebAuth;
                if (localWebAuth == null) {

                    OIDCConfig mOidcConfig = new OIDCConfig.Builder()
                            .withJsonFile(context, R.raw.okta_oidc_config)
                            .create();

                    mRxWebAuth = localWebAuth = new RxOkta.WebAuthBuilder()
                            .withConfig(mOidcConfig)
                            .withContext(context.getApplicationContext())
                            .withStorage(new SimpleOktaStorage(context))
                            .withTabColor(context.getResources().getColor(R.color.colorPrimary))
                            .supportedBrowsers(FIRE_FOX)
                            .create();
                }
            }
        }

        return localWebAuth;
    }
}
