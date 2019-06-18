package com.okta.totp.util;

import android.net.Uri;

import com.marcelkliemannel.kotlinonetimepassword.HmacAlgorithm;
import com.marcelkliemannel.kotlinonetimepassword.TimeBasedOneTimePasswordConfig;
import com.marcelkliemannel.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator;
import com.okta.totp.model.PersistableToken;
import com.okta.totp.model.Token;
import com.okta.totp.storage.security.DefaultEncryptionManager;

import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TokensFactory {
    private final int DEFAULT_PERIOD = 30;
    private final int DEFAULT_DIGITS = 8;
    private final String DEFAULT_ALGORITHM = "SHA1";

    private DefaultEncryptionManager defaultEncryptionManager;

    public TokensFactory(DefaultEncryptionManager defaultEncryptionManager) {
        this.defaultEncryptionManager = defaultEncryptionManager;
    }

    public Token createToken(Uri uri) throws IllegalArgumentException, GeneralSecurityException {
        String name = "";
        String issuer = "Not Set";
        if (uri.getPath() == null) {
            throw new IllegalArgumentException("Missed name or issuer");
        }
        String[] nameAndIssuer = uri.getPath().split(":");
        if (nameAndIssuer.length == 2) {
            issuer = nameAndIssuer[0].replaceAll("/","");
            name = nameAndIssuer[1].replaceAll("/","");;
        } else {
            name = nameAndIssuer[0].replaceAll("/","");;
        }

        Set<String> params = uri.getQueryParameterNames();
        if (!params.contains("secret")
                || !params.contains("period")
                || !params.contains("digits")
                || !params.contains("algorithm")) {
            throw new IllegalArgumentException("Missed one of the following parameters secret, period, digits, algorithm");
        }
        String secretKey = uri.getQueryParameter("secret");
        int period = Integer.parseInt(uri.getQueryParameter("period"));
        int digits = Integer.parseInt(uri.getQueryParameter("digits"));
        String algorithm = uri.getQueryParameter("algorithm");

        String encryptedSecretKey = this.defaultEncryptionManager.encrypt(secretKey);
        PersistableToken persistableToken = new PersistableToken(name, issuer, encryptedSecretKey, period, digits, algorithm);
        return new Token(persistableToken, createTotpGenerator(period, digits, algorithm, secretKey));
    }

    public Token createToken(String name, String issuer, String rawKey) throws GeneralSecurityException {
        String secretKey = Base32String.encode(rawKey.getBytes());
        String encryptedSecretKey = this.defaultEncryptionManager.encrypt(secretKey);
        PersistableToken persistableToken = new PersistableToken(name, issuer, encryptedSecretKey, DEFAULT_PERIOD, DEFAULT_DIGITS, DEFAULT_ALGORITHM);
        return new Token(persistableToken, createTotpGenerator(persistableToken));
    }

    public Token createToken(PersistableToken persistableToken) throws GeneralSecurityException {
        return new Token(persistableToken, createTotpGenerator(persistableToken));
    }

    private TimeBasedOneTimePasswordGenerator createTotpGenerator(int period, int digits, String algorithm, String secretKey) {
        HmacAlgorithm hmacAlgorithm = HmacAlgorithm.SHA1;
        if ("SHA1".equalsIgnoreCase(algorithm)) {
            hmacAlgorithm = HmacAlgorithm.SHA1;
        } else if ("SHA256".equalsIgnoreCase(algorithm)) {
            hmacAlgorithm = HmacAlgorithm.SHA256;
        } else if ("SHA512".equalsIgnoreCase(algorithm)) {
            hmacAlgorithm = HmacAlgorithm.SHA512;
        }
        return createGeneratorForSecretKey(period, TimeUnit.SECONDS, digits, hmacAlgorithm, secretKey);
    }

    private TimeBasedOneTimePasswordGenerator createTotpGenerator(PersistableToken persistableToken) throws GeneralSecurityException {
        String decryptedKey = this.defaultEncryptionManager.decrypt(persistableToken.getKey());
        return createTotpGenerator(persistableToken.getPeriod(),persistableToken.getDigits(),persistableToken.getAlgorithm(), decryptedKey);
    }

    private TimeBasedOneTimePasswordGenerator createGeneratorForSecretKey(int timeStep, TimeUnit timeUnit, int codeDigits, HmacAlgorithm hmacAlgorithm, String key) {
        TimeBasedOneTimePasswordConfig config = new TimeBasedOneTimePasswordConfig(timeStep, timeUnit, codeDigits, hmacAlgorithm);
        return new TimeBasedOneTimePasswordGenerator(key.getBytes(), config);
    }

}
