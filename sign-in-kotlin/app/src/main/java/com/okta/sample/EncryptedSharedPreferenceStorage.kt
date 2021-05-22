/*
 * Copyright (c) 2021, Okta, Inc. and/or its affiliates. All rights reserved.
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
package com.okta.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.*
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKey.KeyScheme
import androidx.security.crypto.MasterKeys
import com.okta.oidc.storage.OktaStorage
import com.okta.sample.EncryptedSharedPreferenceStorage

class EncryptedSharedPreferenceStorage constructor(
    context: Context,
    masterKeyAlias: String,
    prefsName: String,
) : OktaStorage {
    private val prefs: SharedPreferences

    init {
        val masterKey =
            MasterKey.Builder(context, masterKeyAlias).setKeyScheme(KeyScheme.AES256_GCM).build()

        prefs = EncryptedSharedPreferences.create(context,
            prefsName,
            masterKey,
            PrefKeyEncryptionScheme.AES256_SIV,
            AES256_GCM)
    }

    @SuppressLint("ApplySharedPref")
    override fun save(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }

    override fun get(key: String): String? {
        return prefs.getString(key, null)
    }

    @SuppressLint("ApplySharedPref")
    override fun delete(key: String) {
        prefs.edit().remove(key).commit()
    }
}