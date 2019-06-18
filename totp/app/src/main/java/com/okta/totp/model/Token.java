package com.okta.totp.model;

import com.marcelkliemannel.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

public class Token implements Serializable {
    private PersistableToken persistableToken;
    private TimeBasedOneTimePasswordGenerator timeBasedOneTimePasswordGenerator;

    public Token(PersistableToken persistableToken, TimeBasedOneTimePasswordGenerator timeBasedOneTimePasswordGenerator) {
        this.persistableToken = persistableToken;
        this.timeBasedOneTimePasswordGenerator = timeBasedOneTimePasswordGenerator;
    }

    public String getName() {
        return persistableToken.getName();
    }

    public String getIssuer() {
        return persistableToken.getIssuer();
    }

    public String getCurrentPassword() {
        return timeBasedOneTimePasswordGenerator.generate(new Date(System.currentTimeMillis()));
    }

    public JSONObject toJSON() throws JSONException {
        return persistableToken.toJSON();
    }
}
