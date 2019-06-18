package com.okta.totp;

import android.content.Context;

import com.okta.totp.storage.TokensRepository;
import com.okta.totp.storage.security.DefaultEncryptionManager;
import com.okta.totp.util.TokensFactory;

public class ServiceLocator {
    private static TokensRepository mTokensRepository;
    private static TokensFactory mTokensFactory;
    private static DefaultEncryptionManager mDefaultEncryptionManager;

    public static TokensRepository provideTokensRepository(Context context) {
        if(mTokensRepository == null) {
            mTokensRepository = new TokensRepository(context, provideTokensFactory(context));
        }
        return mTokensRepository;
    }

    public static TokensFactory provideTokensFactory(Context context) {
        if(mTokensFactory == null) {
            mTokensFactory = new TokensFactory(provideDefaultEncryptionManager(context));
        }
        return mTokensFactory;
    }

    private static DefaultEncryptionManager provideDefaultEncryptionManager(Context context) {
        if(mDefaultEncryptionManager == null) {
            mDefaultEncryptionManager = new DefaultEncryptionManager(context);
        }
        return mDefaultEncryptionManager;
    }
}
