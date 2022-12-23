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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricToggleStorage @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val FILE_NAME = "com.okta.biometric.toggle"
        private const val PREFERENCE_KEY = "com.okta.biometric.biometric_enabled"
    }

    private val biometricSharedPreferences =
        appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private var _biometricEnabled = biometricSharedPreferences.getBoolean(PREFERENCE_KEY, false)
    private var _biometricAuthenticated = false

    var biometricEnabled: Boolean
        get() = _biometricEnabled
        set(value) {
            _biometricEnabled = value
            biometricSharedPreferences.edit()
                .putBoolean(PREFERENCE_KEY, _biometricEnabled)
                .apply()
        }

    var biometricAuthenticated: Boolean
        get() = _biometricAuthenticated
        set(value) {
            _biometricAuthenticated = _biometricAuthenticated or value
        }
}
