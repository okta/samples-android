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
package com.okta.android.samples.browser_sign_in.biometric

import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.okta.android.samples.browser_sign_in.storage.BiometricCredentialSharedPrefs
import com.okta.android.samples.browser_sign_in.storage.CredentialTokenStorage
import com.okta.android.samples.browser_sign_in.storage.DefaultCredentialSharedPrefs
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject
import javax.inject.Provider

/**
 * Helper class for toggling between biometric and non-biometric storage, and for requesting
 * biometric prompt
 */
@FragmentScoped
class BiometricCredentialsManager @Inject constructor(
    private val fragment: Fragment,
    private val biometricToggleStorage: BiometricToggleStorage,
    private val credentialTokenStorage: CredentialTokenStorage,
    @BiometricCredentialSharedPrefs
    private val biometricSharedPrefsProvider: Provider<SharedPreferences>, // Using Provider for lazy instantiation
    @DefaultCredentialSharedPrefs
    private val defaultSharedPrefsProvider: Provider<SharedPreferences> // Using Provider for lazy instantiation
) {
    val biometricEnabled get() = biometricToggleStorage.biometricEnabled
    val biometricAuthenticated get() = biometricToggleStorage.biometricAuthenticated

    fun requestBiometricAuthentication(
        biometricPromptAuthenticationCallback: BiometricPrompt.AuthenticationCallback
    ) {
        BiometricPrompt(
            fragment,
            ContextCompat.getMainExecutor(fragment.requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricToggleStorage.biometricAuthenticated = true
                    biometricPromptAuthenticationCallback.onAuthenticationSucceeded(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricPromptAuthenticationCallback.onAuthenticationError(
                        errorCode,
                        errString
                    )
                }

                override fun onAuthenticationFailed() {
                    biometricPromptAuthenticationCallback.onAuthenticationFailed()
                }
            }
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
        )
    }

    suspend fun useBiometricCredentialStorage() {
        credentialTokenStorage.setSharedPreferences(biometricSharedPrefsProvider.get())
        biometricToggleStorage.biometricEnabled = true
    }

    suspend fun useDefaultCredentialStorage() {
        credentialTokenStorage.setSharedPreferences(defaultSharedPrefsProvider.get())
        biometricToggleStorage.biometricEnabled = false
    }
}
