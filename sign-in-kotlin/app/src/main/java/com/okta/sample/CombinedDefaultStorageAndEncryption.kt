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

import android.text.TextUtils
import com.okta.oidc.storage.OktaStorage
import com.okta.oidc.storage.SharedPreferenceStorage
import com.okta.oidc.storage.security.DefaultEncryptionManager

/**
 * Combines DefaultEncryptionManager and SharedPreferenceStorage. This class is a combination of the
 * SDK provided shared preference and encryption manager.
 */
class CombinedDefaultStorageAndEncryption(
    private val sharedPreferenceStorage: SharedPreferenceStorage,
    private val defaultEncryption: DefaultEncryptionManager,
) : OktaStorage {

    override fun save(key: String, value: String) {
        // NO-OP do not use this to save.
    }

    override fun get(key: String): String? {
        return sharedPreferenceStorage.get(defaultEncryption.getHashed(key))?.run {
            defaultEncryption.decrypt(this)
        }
    }

    override fun delete(key: String) {
        sharedPreferenceStorage.delete(defaultEncryption.getHashed(key))
    }
}
