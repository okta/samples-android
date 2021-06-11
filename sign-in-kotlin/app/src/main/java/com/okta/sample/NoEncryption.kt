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

import android.content.Context
import com.okta.oidc.storage.security.EncryptionManager
import java.io.UnsupportedEncodingException
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher

/**
 * A sample on how to disable encryption. Set this implementation in
 */
class NoEncryption : EncryptionManager {
    @Throws(GeneralSecurityException::class)
    override fun encrypt(value: String?): String? {
        return value
    }

    @Throws(GeneralSecurityException::class)
    override fun decrypt(value: String?): String? {
        return value
    }

    @Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
    override fun getHashed(value: String?): String? {
        return value
    }

    override fun isHardwareBackedKeyStore(): Boolean {
        return true
    }

    override fun recreateCipher() {
        //NO-OP
    }

    override fun setCipher(cipher: Cipher) {
        //NO-OP
    }

    override fun getCipher(): Cipher? {
        //NO-OP
        return null
    }

    override fun removeKeys() {
        //NO-OP
    }

    override fun recreateKeys(context: Context) {
        //NO-OP
    }

    override fun isUserAuthenticatedOnDevice(): Boolean {
        return true
    }

    override fun isValidKeys(): Boolean {
        return true
    }
}