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
package com.okta.totp;

import android.content.Context;

import com.okta.totp.storage.TokensRepository;
import com.okta.totp.storage.security.DefaultEncryptionManager;
import com.okta.totp.util.TokensFactory;

public class ServiceLocator {
    private static TokensRepository mTokensRepository;
    private static TokensFactory mTokensFactory;
    private static DefaultEncryptionManager mDefaultEncryptionManager;

    public static TokensRepository provideTokensRepository(Context context) {
        if(mTokensRepository == null) {
            mTokensRepository = new TokensRepository(context, provideTokensFactory(context));
        }
        return mTokensRepository;
    }

    public static TokensFactory provideTokensFactory(Context context) {
        if(mTokensFactory == null) {
            mTokensFactory = new TokensFactory(provideDefaultEncryptionManager(context));
        }
        return mTokensFactory;
    }

    private static DefaultEncryptionManager provideDefaultEncryptionManager(Context context) {
        if(mDefaultEncryptionManager == null) {
            mDefaultEncryptionManager = new DefaultEncryptionManager(context);
        }
        return mDefaultEncryptionManager;
    }
}
