package com.okta.android.samples.browser_sign_in;

import android.content.Context;

import com.okta.android.samples.browser_sign_in.rxClient.RxOkta;
import com.okta.android.samples.browser_sign_in.rxClient.RxWebAuthClient;
import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.android.samples.browser_sign_in.util.PreferenceRepository;
import com.okta.android.samples.browser_sign_in.util.SmartLockHelper;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.storage.SharedPreferenceStorage;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.storage.security.SmartLockBaseEncryptionManager;

public class ServiceLocator {
    private final static String FIRE_FOX = "org.mozilla.firefox";
    private final static String ANDROID_BROWSER = "com.android.browser";
    private static volatile WebAuthClient mWebAuth;
    private static volatile EncryptionManager mEncryptionManager;
    private static volatile RxWebAuthClient  mRxWebAuth;
    private static volatile PreferenceRepository mPreferenceRepository;

    public static WebAuthClient provideWebAuthClient(Context context) {
        WebAuthClient localWebAuth = mWebAuth;
        if(localWebAuth == null) {
            synchronized (ServiceLocator.class) {
                localWebAuth = mWebAuth;
                if(localWebAuth == null) {
                    OIDCConfig mOidcConfig = new OIDCConfig.Builder()
                            .withJsonFile(context, R.raw.okta_oidc_config)
                            .create();

                    boolean isSmartLockEncryptionManager = providePreferenceRepository(context).isEnabledSmartLock();

                    mEncryptionManager = (isSmartLockEncryptionManager) ?
                            createSmartLockEncryptionManager(context) : createSimpleEncryptionManager(context);

                    Okta.WebAuthBuilder builder = new Okta.WebAuthBuilder()
                            .withConfig(mOidcConfig)
                            .withContext(context.getApplicationContext())
                            .withStorage(new SharedPreferenceStorage(context))
                            .withTabColor(context.getResources().getColor(R.color.colorPrimary))
                            .supportedBrowsers(ANDROID_BROWSER, FIRE_FOX)
                            .setCacheMode(false)
                            .setRequireHardwareBackedKeyStore(false)
                            .withEncryptionManager(mEncryptionManager);

                    mWebAuth = localWebAuth = builder.create();
                }
            }
        }

        return localWebAuth;
    }

    public static PreferenceRepository providePreferenceRepository(Context context) {
        if(mPreferenceRepository == null) {
            mPreferenceRepository = new PreferenceRepository(context);
        }
        return mPreferenceRepository;
    }

    public static SmartLockBaseEncryptionManager createSmartLockEncryptionManager(Context context) {
        return new SmartLockBaseEncryptionManager(context, 10);
    }

    public static DefaultEncryptionManager createSimpleEncryptionManager(Context context) {
        return new DefaultEncryptionManager(context);
    }

    public static EncryptionManager provideEncryptionManager(Context context) {
        return mEncryptionManager;
    }

    public static void setEncryptionManager(EncryptionManager encryptionManager) {
        mEncryptionManager = encryptionManager;
    }

    public static SmartLockHelper provideSmartLockHelper() {
        return new SmartLockHelper();
    }

    public static OktaProgressDialog provideOktaProgressDialog(Context context) {
        return new OktaProgressDialog(context);
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
                            .withStorage(new SharedPreferenceStorage(context))
                            .withTabColor(context.getResources().getColor(R.color.colorPrimary))
                            .supportedBrowsers(ANDROID_BROWSER, FIRE_FOX)
                            .create();
                }
            }
        }

        return localWebAuth;
    }
}
