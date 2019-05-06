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

import androidx.annotation.ColorInt;

import com.okta.oidc.OktaBuilder;

/**
 * A collection of builders for creating different type of authentication clients.
 * {@link RxWebAuthClient}
 */
public class RxOkta {

    /**
     * The RX web authentication client builder.
     */
    public static class WebAuthBuilder extends OktaBuilder<RxWebAuthClient, WebAuthBuilder> {
        private int mCustomTabColor;
        private String[] mSupportedBrowsers;

        public WebAuthBuilder withTabColor(@ColorInt int customTabColor) {
            mCustomTabColor = customTabColor;
            return toThis();
        }

        public WebAuthBuilder supportedBrowsers(String... browsers) {
            mSupportedBrowsers = browsers;
            return toThis();
        }

        @Override
        protected WebAuthBuilder toThis() {
            return this;
        }

        /**
         * Create AuthClient.
         *
         * @return the authenticate client {@link RxWebAuthClient}
         */
        @Override
        public RxWebAuthClient create() {
            super.withAuthenticationClientFactory((oidcConfig, oktaState, connectionFactory) -> new RxWebAuthClientImpl(oidcConfig, oktaState, connectionFactory, mCustomTabColor, mSupportedBrowsers));
            return createAuthClient();
        }
    }
}
