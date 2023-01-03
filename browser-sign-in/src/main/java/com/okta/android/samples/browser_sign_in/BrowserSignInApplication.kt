/*
 * Copyright 2022-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.android.samples.browser_sign_in

import android.app.Application
import androidx.browser.customtabs.CustomTabColorSchemeParams
import com.okta.android.samples.browser_sign_in.storage.CredentialTokenStorage
import com.okta.authfoundation.AuthFoundationDefaults
import com.okta.authfoundation.client.OidcClient
import com.okta.authfoundation.client.OidcConfiguration
import com.okta.authfoundation.client.SharedPreferencesCache
import com.okta.authfoundation.credential.CredentialDataSource.Companion.createCredentialDataSource
import com.okta.authfoundation.events.EventCoordinator
import com.okta.authfoundation.events.EventHandler
import com.okta.authfoundationbootstrap.CredentialBootstrap
import com.okta.webauthenticationui.events.CustomizeCustomTabsEvent
import dagger.hilt.android.HiltAndroidApp
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class BrowserSignInApplication : Application() {
    @Inject
    lateinit var credentialTokenStorage: CredentialTokenStorage

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initializes Auth Foundation and Credential Bootstrap classes for use in the Activity.
        AuthFoundationDefaults.cache = SharedPreferencesCache.create(this)

        // Optionally customize Chrome custom tab colors
        AuthFoundationDefaults.eventCoordinator = EventCoordinator(
            object : EventHandler {
                override fun onEvent(event: Any) {
                    when (event) {
                        is CustomizeCustomTabsEvent -> {
                            val toolbarColor = applicationContext.getColor(R.color.purple_700)
                            val navbarColor = applicationContext.getColor(R.color.teal_700)
                            val colorScheme = CustomTabColorSchemeParams.Builder()
                                .setNavigationBarColor(navbarColor)
                                .setToolbarColor(toolbarColor)
                                .build()
                            event.intentBuilder
                                .setDefaultColorSchemeParams(colorScheme)
                                .build()
                            event.intentBuilder
                        }
                    }
                }
            }
        )

        val oidcConfiguration = OidcConfiguration(
            clientId = BuildConfig.CLIENT_ID,
            defaultScope = "openid email profile offline_access"
        )
        val client = OidcClient.createFromDiscoveryUrl(
            oidcConfiguration,
            "${BuildConfig.ISSUER}/.well-known/openid-configuration".toHttpUrl()
        )

        // CredentialTokenStorage is a custom TokenStorage that allows switching between different
        // SharedPreferences. This sample toggles between biometric EncryptedSharedPreferences, and
        // non-biometric EncryptedSharedPreferences
        val credentialDataSource = client.createCredentialDataSource(credentialTokenStorage)
        // If switching between different credential encryption is not needed, create a
        // CredentialDataSource by passing in the encryption specs as follows:
        // val credentialDataSource = client.createCredentialDataSource(
        //     this,
        //     keyGenParameterSpec = <KeygenParameterSpec>
        // )
        // See SharedPreferencesModule.kt for example of KeyGenParameterSpec used by this sample
        CredentialBootstrap.initialize(credentialDataSource)
    }
}
