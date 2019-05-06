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

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.clients.BaseAuth;
import com.okta.oidc.results.Result;

import io.reactivex.Single;

/**
 * The Authentication client for logging in using a sessionToken.
 *
 * For login using web browser
 * {@link RxWebAuthClient}
 */
public interface RxAuthClient extends BaseAuth<RxSessionClient> {
    Single<Result> logIn(String sessionToken, AuthenticationPayload payload);
}
