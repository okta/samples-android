package com.okta.android.samples.custom_sign_in;

import android.content.Context;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

final class Utils {
    private static final int BUFFER_SIZE = 1024;

    static String getAsset(Context context, String filename) {
        try {
            StringBuilder builder = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(
                    context.getResources().getAssets().open(filename), "UTF-8");

            char[] buffer = new char[BUFFER_SIZE];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, length);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getJwt(String issuer, String nonce, Date expiredDate, Date issuedAt,
                                String... audience) {
        JwtBuilder builder = Jwts.builder();
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        Map<String, Object> map = new HashMap<>();
        map.put(Claims.AUDIENCE, Arrays.asList(audience));

        return builder
                .addClaims(map)
                .claim("nonce", nonce)
                .setIssuer(issuer)
                .setSubject("sub")
                .setExpiration(expiredDate)
                .setIssuedAt(issuedAt)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    public static Date getNow() {
        long nowMillis = System.currentTimeMillis();
        return new Date(nowMillis);
    }

    public static Date getTomorrow() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, 1);
        return c.getTime();
    }

    public static Date getYesterday() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, -1);
        return c.getTime();
    }

    public static Date getExpiredFromTomorrow() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, 2);
        return c.getTime();
    }

    public static class Ui {

        public static void waitFor(ViewInteraction viewInteraction, ViewAssertion viewAssertion, long timeout) {

            PollingTimeoutIdler idler = new PollingTimeoutIdler(viewInteraction, viewAssertion, timeout);
            IdlingRegistry.getInstance().register(idler);

            viewInteraction.check(viewAssertion);

            IdlingRegistry.getInstance().unregister(idler);
        }

    }
}