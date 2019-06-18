package com.okta.totp.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.okta.totp.model.PersistableToken;
import com.okta.totp.model.Token;
import com.okta.totp.util.TokensFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class TokensRepository {
    private final String FILENAME = "PREFERENCES_FILE";
    private final String TOKENS_KEY = "TOKENS";
    SharedPreferences sharedPreferences;
    TokensFactory tokensFactory;

    public TokensRepository(Context context, TokensFactory tokensFactory) {
        this.sharedPreferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
        this.tokensFactory = tokensFactory;
    }

    private void save(List<Token> tokens) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Token token : tokens) {
            jsonArray.put(token.toJSON());
        }

        this.sharedPreferences.edit().putString(TOKENS_KEY, jsonArray.toString()).apply();
    }

    public void addToken(Token token) throws JSONException {
        List<Token> tokens = getTokens();
        tokens.add(token);
        save(tokens);
    }

    public void removeToken(Token token) throws JSONException {
        List<Token> tokens = getTokens();
        tokens.remove(token);
        save(tokens);
    }

    public List<Token> getTokens() throws JSONException {
        List<Token> tokens = new ArrayList<>();
        String tokenSerializable = this.sharedPreferences.getString(TOKENS_KEY, null);
        if (tokenSerializable != null) {
            JSONArray jsonArray = new JSONArray(tokenSerializable);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                PersistableToken persistableToken = PersistableToken.fromJSON(jsonObject);
                try {
                    Token token = tokensFactory.createToken(persistableToken);
                    tokens.add(token);
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }
            }
        }
        return tokens;
    }
}
