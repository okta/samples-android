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
import android.content.SharedPreferences
import com.okta.android.samples.browser_sign_in.coroutine.qualifiers.IoDispatcher
import com.okta.authfoundation.credential.TokenStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of okta-mobile-kotlin's TokenStorage interface that allows moving credentials
 * between different SharedPreferences. EncryptedSharedPreferences with different encryption models
 * are used in this sample to toggle between biometric and non-biometric EncryptedSharedPreferences.
 */
@Singleton
class CredentialTokenStorage @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TokenStorage {
    companion object {
        private const val PREFERENCE_KEY = "com.okta.sample.storage_entries"
    }

    private var credentialSharedPrefs: SharedPreferences? = null
    private val sharedPrefsMutex = Mutex()

    override suspend fun entries(): List<TokenStorage.Entry> {
        var entries: List<TokenStorage.Entry> = emptyList()
        accessStorage { existingEntries ->
            entries = existingEntries
            existingEntries
        }
        return entries
    }

    override suspend fun add(id: String) {
        accessStorage { existingEntries ->
            existingEntries += TokenStorage.Entry(id, null, emptyMap())
            existingEntries
        }
    }

    override suspend fun remove(id: String) {
        accessStorage { existingEntries ->
            val index = existingEntries.indexOfFirst { it.identifier == id }
            existingEntries.removeAt(index)
            existingEntries
        }
    }

    override suspend fun replace(updatedEntry: TokenStorage.Entry) {
        accessStorage { existingEntries ->
            val index = existingEntries.indexOfFirst { it.identifier == updatedEntry.identifier }
            existingEntries[index] = updatedEntry
            existingEntries
        }
    }

    @SuppressLint("ApplySharedPref")
    suspend fun setSharedPreferences(sharedPreferences: SharedPreferences) {
        withContext(ioDispatcher) {
            sharedPrefsMutex.withLock {
                credentialSharedPrefs?.let { oldSharedPreferences ->
                    val oldPrefsContents = oldSharedPreferences.getString(PREFERENCE_KEY, null)

                    oldSharedPreferences.edit()
                        .clear()
                        .commit()

                    sharedPreferences.edit()
                        .putString(PREFERENCE_KEY, oldPrefsContents)
                        .commit()
                }

                credentialSharedPrefs = sharedPreferences
            }
        }
    }

    private suspend fun accessStorage(
        block: (MutableList<TokenStorage.Entry>) -> List<TokenStorage.Entry>
    ) {
        withContext(ioDispatcher) {
            sharedPrefsMutex.withLock {
                if (credentialSharedPrefs == null) {
                    throw IllegalStateException(
                        "Attempted accessing credential storage before calling setSharedPreferences"
                    )
                }
                val existingJson = credentialSharedPrefs!!.getString(PREFERENCE_KEY, null)
                val existingEntries: MutableList<TokenStorage.Entry> = if (existingJson == null) {
                    mutableListOf()
                } else {
                    Json.decodeFromString(StoredTokens.serializer(), existingJson)
                        .toTokenStorageEntries()
                }
                val updatedEntries = block(existingEntries)
                val updatedJson = Json.encodeToString(
                    StoredTokens.serializer(),
                    StoredTokens.from(updatedEntries)
                )
                val editor = credentialSharedPrefs!!.edit()
                editor.putString(PREFERENCE_KEY, updatedJson)
                editor.commit()
            }
        }
    }
}

@Serializable
private class StoredTokens(
    @SerialName("entries") val entries: List<Entry>
) {
    @Serializable
    class Entry(
        @SerialName("identifier") val identifier: String,
        @SerialName("token") val token: SerializableToken?,
        @SerialName("tags") val tags: Map<String, String>
    )

    companion object {
        fun from(entries: List<TokenStorage.Entry>): StoredTokens {
            return StoredTokens(
                entries.map {
                    Entry(
                        it.identifier,
                        it.token?.asSerializableToken(),
                        it.tags
                    )
                }
            )
        }
    }

    fun toTokenStorageEntries(): MutableList<TokenStorage.Entry> {
        return entries.map { TokenStorage.Entry(it.identifier, it.token?.asToken(), it.tags) }
            .toMutableList()
    }
}
