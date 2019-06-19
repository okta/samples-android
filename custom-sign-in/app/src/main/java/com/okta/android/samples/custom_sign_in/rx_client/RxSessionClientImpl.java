/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.okta.android.samples.custom_sign_in.rx_client;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.okta.oidc.Tokens;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;

import org.json.JSONObject;

import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;

class RxSessionClientImpl implements RxSessionClient {
    private SyncSessionClient mSyncSessionClientImpl;

    RxSessionClientImpl(SyncSessionClient syncSessionClient) {
        mSyncSessionClientImpl = syncSessionClient;
    }
    @Override
    public Single<JSONObject> authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties, @Nullable Map<String, String> postParameters, @NonNull ConnectionParameters.RequestMethod method) {
        return null;
    }

    @Override
    public Single<UserInfo> getUserProfile() {
        return Single.create(emitter -> emitter.onSuccess(mSyncSessionClientImpl.getUserProfile()));
    }

    @Override
    public Single<IntrospectInfo> introspectToken(String token, String tokenType) {
        return Single.create(emitter -> emitter.onSuccess(mSyncSessionClientImpl.introspectToken(token, tokenType)));
    }

    @Override
    public Single<Boolean> revokeToken(String token) {
        return Single.create(emitter -> emitter.onSuccess(mSyncSessionClientImpl.revokeToken(token)));
    }

    @Override
    public Single<Tokens> refreshToken() {
        return Single.create(emitter -> emitter.onSuccess(mSyncSessionClientImpl.refreshToken()));
    }

    @Override
    public Single<Tokens> getTokens() {
        return Single.create(emitter -> emitter.onSuccess(mSyncSessionClientImpl.getTokens()));
    }

    @Override
    public Single<Boolean> isLoggedIn() {
        return Single.create(emitter -> emitter.onSuccess(mSyncSessionClientImpl.isAuthenticated()));
    }

    @Override
    public Completable clear() {
        return Completable.create(emitter -> {
            mSyncSessionClientImpl.clear();
            emitter.onComplete();
        });
    }
}
