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

package com.okta.android.samples.browser_sign_in.rxClient;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;

import org.json.JSONObject;

import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;


/**
 * This is the client for Okta OpenID Connect & OAuth 2.0 APIs. You can get the client when
 * user is authorized.
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/">Okta API docs</a>
 */
public interface RxSessionClient {

    Single<JSONObject> authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                                         @Nullable Map<String, String> postParameters,
                                         @NonNull HttpConnection.RequestMethod method);

    Single<UserInfo> getUserProfile();

    Single<IntrospectInfo> introspectToken(String token, String tokenType);

    Single<Boolean> revokeToken(String token);

    Single<Tokens> refreshToken();

    Single<Tokens> getTokens();

    Single<Boolean> isLoggedIn();

    Completable clear();
}
