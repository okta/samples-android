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
package com.okta.totp.otp_repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtpUriSharedPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey
        .Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        SHARED_PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val sharedPrefsEditor = sharedPrefs.edit()

    private var nextIndex = sharedPrefs.all.maxOfOrNull { a -> a.value as Int } ?: 0

    fun addOtpUriString(otpUriString: String) {
        sharedPrefsEditor.putInt(otpUriString, nextIndex)
        nextIndex++
        sharedPrefsEditor.apply()
    }

    fun getOtpUriStrings(): List<String> {
        return sharedPrefs.all.entries.sortedBy { entry -> entry.value as Int }.map { it.key }
    }

    fun removeOtpUriString(otpUriString: String) {
        sharedPrefsEditor.remove(otpUriString)
        sharedPrefsEditor.apply()
    }

    companion object {
        private const val SHARED_PREFS_FILE_NAME = "otp_storage"
    }
}
