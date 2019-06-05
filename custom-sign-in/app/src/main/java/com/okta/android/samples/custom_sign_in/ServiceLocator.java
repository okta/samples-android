package com.okta.android.samples.custom_sign_in;

import android.content.Context;

import com.okta.android.samples.custom_sign_in.util.OktaProgressDialog;
import com.okta.android.samples.custom_sign_in.util.PreferenceRepository;
import com.okta.android.samples.custom_sign_in.util.SmartLockHelper;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.storage.SharedPreferenceStorage;

import com.okta.android.samples.custom_sign_in.rx_client.RxOkta;
import com.okta.android.samples.custom_sign_in.rx_client.RxAuthClient;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.storage.security.SmartLockBaseEncryptionManager;

public class ServiceLocator {
    private static volatile AuthClient mAuth;
    private static volatile RxAuthClient mRxAuth;
    private static volatile EncryptionManager mEncryptionManager;
    private static volatile PreferenceRepository mPreferenceRepository;

    public static AuthClient provideAuthClient(Context context) {
        AuthClient localAuth = mAuth;
        if(localAuth == null) {
            synchronized (ServiceLocator.class) {
                localAuth = mAuth;
                if(localAuth == null) {
                    OIDCConfig mOidcConfig = new OIDCConfig.Builder()
                            .withJsonFile(context, R.raw.okta_oidc_config)
                            .create();

                    boolean isSmartLockEncryptionManager = providePreferenceRepository(context).isEnabledSmartLock();

                    mEncryptionManager = (isSmartLockEncryptionManager) ?
                            createSmartLockEncryptionManager(context) : createSimpleEncryptionManager(context);

                    mAuth = localAuth = new Okta.AuthBuilder()
                            .withConfig(mOidcConfig)
                            .withContext(context.getApplicationContext())
                            .withStorage(new SharedPreferenceStorage(context))
                            .setCacheMode(false)
                            .setRequireHardwareBackedKeyStore(false)
                            .withEncryptionManager(mEncryptionManager)
                            .create();
                }
            }
        }

        return localAuth;
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

    public static RxAuthClient provideRxAuthClient(Context context) {
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
                            .withStorage(new SharedPreferenceStorage(context))
                            .create();
                }
            }
        }

        return localAuth;
    }
}
