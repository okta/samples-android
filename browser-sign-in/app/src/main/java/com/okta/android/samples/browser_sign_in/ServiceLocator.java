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
package com.okta.android.samples.browser_sign_in;

import android.content.Context;

import com.okta.android.samples.browser_sign_in.util.OktaProgressDialog;
import com.okta.android.samples.browser_sign_in.util.PreferenceRepository;
import com.okta.android.samples.browser_sign_in.util.SmartLockHelper;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.storage.SharedPreferenceStorage;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.storage.security.GuardedEncryptionManager;

public class ServiceLocator {
    private final static String FIRE_FOX = "org.mozilla.firefox";
    private final static String ANDROID_BROWSER = "com.android.browser";
    private static volatile WebAuthClient mWebAuth;
    private static volatile EncryptionManager mEncryptionManager;
    private static volatile PreferenceRepository mPreferenceRepository;

    public static WebAuthClient provideWebAuthClient(Context context) {
        WebAuthClient localWebAuth = mWebAuth;
        if(localWebAuth == null) {
            synchronized (ServiceLocator.class) {
                localWebAuth = mWebAuth;
                if(localWebAuth == null) {
                    OIDCConfig mOidcConfig = new OIDCConfig.Builder()
                            .withJsonFile(context, R.raw.okta_oidc_config)
                            .create();

                    boolean isSmartLockEncryptionManager = providePreferenceRepository(context).isEnabledSmartLock();

                    mEncryptionManager = (isSmartLockEncryptionManager) ?
                            createGuardedEncryptionManager(context) : createSimpleEncryptionManager(context);

                    Okta.WebAuthBuilder builder = new Okta.WebAuthBuilder()
                            .withConfig(mOidcConfig)
                            .withContext(context.getApplicationContext())
                            .withStorage(new SharedPreferenceStorage(context))
                            .withTabColor(context.getResources().getColor(R.color.colorPrimary))
                            .supportedBrowsers(ANDROID_BROWSER, FIRE_FOX)
                            .setCacheMode(false)
                            .setRequireHardwareBackedKeyStore(false)
                            .withEncryptionManager(mEncryptionManager);

                    localWebAuth = mWebAuth = builder.create();
                }
            }
        }

        return localWebAuth;
    }

    public static PreferenceRepository providePreferenceRepository(Context context) {
        if(mPreferenceRepository == null) {
            mPreferenceRepository = new PreferenceRepository(context);
        }
        return mPreferenceRepository;
    }

    public static GuardedEncryptionManager createGuardedEncryptionManager(Context context) {
        return new GuardedEncryptionManager(context, 10);
    }

    public static DefaultEncryptionManager createSimpleEncryptionManager(Context context) {
        return new DefaultEncryptionManager(context);
    }

    public static EncryptionManager provideEncryptionManager(Context context) {
        return mEncryptionManager;
    }

    public static void setEncryptionManager(EncryptionManager encryptionManager) {
        mEncryptionManager = encryptionManager;
    }

    public static SmartLockHelper provideSmartLockHelper() {
        return new SmartLockHelper();
    }

    public static OktaProgressDialog provideOktaProgressDialog(Context context) {
        return new OktaProgressDialog(context);
    }
}
