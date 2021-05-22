package com.okta.sample

import com.okta.oidc.storage.OktaStorage
import java.lang.Exception

class EncryptedStorage(
    private val combinedDefaultStorageAndEncryption: CombinedDefaultStorageAndEncryption,
    private val encryptedSharedPreferenceStorage: EncryptedSharedPreferenceStorage,
) : OktaStorage by encryptedSharedPreferenceStorage {

    override fun get(key: String): String? {
        return encryptedSharedPreferenceStorage.get(key) ?: run {
            combinedDefaultStorageAndEncryption.get(key)
        }
    }

    override fun delete(key: String) {
        combinedDefaultStorageAndEncryption.delete(key)
        encryptedSharedPreferenceStorage.delete(key)
    }
}
