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

import android.content.Context;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.SyncAuthClient;
import com.okta.oidc.clients.SyncAuthClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.security.EncryptionManager;

import io.reactivex.Single;

class RxAuthClientImpl implements RxAuthClient {
    private SyncAuthClient mSyncAuthClient;
    private RxSessionClientImpl rxSessionClientImpl;

    RxAuthClientImpl(OIDCConfig oidcConfig, Context context, OktaStorage oktaStorage, EncryptionManager encryptionManager, HttpConnectionFactory connectionFactory, boolean isRequireHardwareBackedKeyStore, boolean isCacheMode) {
        mSyncAuthClient = new SyncAuthClientFactory().createClient(oidcConfig, context, oktaStorage, encryptionManager, connectionFactory, isRequireHardwareBackedKeyStore, isCacheMode);
        rxSessionClientImpl = new RxSessionClientImpl(mSyncAuthClient.getSessionClient());
    }

    @Override
    public Single<Result> logIn(String sessionToken, AuthenticationPayload payload) {
        return Single.create(emitter -> emitter.onSuccess(mSyncAuthClient.signIn(sessionToken, payload)));
    }

    @Override
    public RxSessionClient getSessionClient() {
        return rxSessionClientImpl;
    }
}
