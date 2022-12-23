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
package com.okta.android.samples.browser_sign_in.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.okta.android.samples.browser_sign_in.biometric.BiometricToggleStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import java.security.KeyStoreException
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object SharedPreferencesModule {
    private const val BIOMETRIC_FILE_NAME = "com.okta.sample.biometric.storage"
    private const val NON_BIOMETRIC_FILE_NAME = "com.okta.sample.non_biometric.storage"
    private const val USER_AUTHENTICATION_TIMEOUT_SECS = 10
    private const val BIOMETRIC_KEY_SIZE = 256

    @SuppressLint("ApplySharedPref")
    private fun createSharedPreferences(
        applicationContext: Context,
        fileName: String,
        keyGenParameterSpec: KeyGenParameterSpec
    ): SharedPreferences {
        val getSharedPreferencesWithMasterKey = { masterKeyAlias: String ->
            EncryptedSharedPreferences.create(
                fileName,
                masterKeyAlias,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
            getSharedPreferencesWithMasterKey(masterKeyAlias)
        } catch (e: KeyStoreException) {
            // Key invalidated, so delete it and create it again. This can happen in case of
            // biometric encryption when the user adds or removes fingerprints
            KeyStore.getInstance("AndroidKeystore").apply {
                load(null)
                deleteEntry(keyGenParameterSpec.keystoreAlias)
            }
            // Clear shared preference and try again
            val sharedPreferences = applicationContext.getSharedPreferences(
                fileName,
                Context.MODE_PRIVATE
            )
            sharedPreferences.edit().clear().commit()
            val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
            getSharedPreferencesWithMasterKey(masterKeyAlias)
        } catch (e: Exception) {
            // Clear shared preference and try again
            val sharedPreferences = applicationContext.getSharedPreferences(
                fileName,
                Context.MODE_PRIVATE
            )
            sharedPreferences.edit().clear().commit()
            val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
            getSharedPreferencesWithMasterKey(masterKeyAlias)
        }
    }

    @DefaultCredentialSharedPrefs
    @Provides
    @Singleton
    fun providesDefaultCredentialSharedPrefs(@ApplicationContext context: Context): SharedPreferences {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        return createSharedPreferences(
            context,
            NON_BIOMETRIC_FILE_NAME,
            keyGenParameterSpec
        )
    }

    @Suppress("DEPRECATION")
    @BiometricCredentialSharedPrefs
    @Provides
    @Singleton
    fun providesBiometricCredentialSharedPrefs(
        @ApplicationContext context: Context,
        biometricToggleStorage: BiometricToggleStorage
    ): SharedPreferences {
        if (!biometricToggleStorage.biometricAuthenticated) {
            throw IllegalStateException(
                "Attempted accessing biometric shared preferences without biometric authentication"
            )
        }

        val biometricKeyGenParameterSpecBuilder = KeyGenParameterSpec.Builder(
            "com_okta_sample_storage",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)

        val keyGenParameterSpec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricKeyGenParameterSpecBuilder
                .setUserAuthenticationParameters(USER_AUTHENTICATION_TIMEOUT_SECS, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .setKeySize(BIOMETRIC_KEY_SIZE)
        } else {
            // Pre-Android R does not have a non-deprecated API for this
            biometricKeyGenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(
                USER_AUTHENTICATION_TIMEOUT_SECS
            )
        }
            .build()

        return createSharedPreferences(
            context,
            BIOMETRIC_FILE_NAME,
            keyGenParameterSpec
        )
    }
}
