package com.okta.totp.model;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class PersistableToken {
    private static final String NAME_KEY = "NAME_KEY";
    private static final String ISSUER_KEY = "ISSUER_KEY";
    private static final String KEY_KEY = "KEY_KEY";
    private static final String PERIOD_KEY = "PERIOD_KEY";
    private static final String DIGITS_KEY = "DIGITS_KEY";
    private static final String ALGORITHM_KEY = "ALGORITHM_KEY";
    private String name;
    private String issuer;
    private String key;
    private int period;
    private int digits;
    private String algorithm;

    public PersistableToken(String name, String issuer, String key, int period, int digits, String algorithm) {
        this.name = name;
        this.issuer = issuer;
        this.key = key;
        this.period = period;
        this.digits = digits;
        this.algorithm = algorithm;
    }

    public String getName() {
        return name;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getKey() {
        return key;
    }

    public int getPeriod() {
        return period;
    }

    public int getDigits() {
        return digits;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistableToken that = (PersistableToken) o;
        return period == that.period &&
                digits == that.digits &&
                Objects.equals(name, that.name) &&
                Objects.equals(issuer, that.issuer) &&
                Objects.equals(key, that.key) &&
                Objects.equals(algorithm, that.algorithm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, issuer, key, period, digits, algorithm);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();

        object.put(NAME_KEY, name);
        object.put(ISSUER_KEY, issuer);
        object.put(KEY_KEY, key);
        object.put(PERIOD_KEY, period);
        object.put(DIGITS_KEY, digits);
        object.put(ALGORITHM_KEY, algorithm);

        return object;
    }

    public static PersistableToken fromJSON(JSONObject object) throws JSONException {
        String name = object.getString(NAME_KEY);
        String issuer = object.getString(ISSUER_KEY);
        String key = object.getString(KEY_KEY);
        int period = object.getInt(PERIOD_KEY);
        int digits = object.getInt(DIGITS_KEY);
        String algorithm = object.getString(ALGORITHM_KEY);

        return new PersistableToken(name, issuer, key, period, digits, algorithm);
    }
}
