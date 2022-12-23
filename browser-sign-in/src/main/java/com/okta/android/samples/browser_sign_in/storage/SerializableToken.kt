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

import com.okta.authfoundation.credential.Token
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO(OKTA-557900): Remove this class when switching to new token model in okta-mobile-kotlin
@Serializable
class SerializableToken internal constructor(
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("access_token") val accessToken: String,
    @SerialName("scope") val scope: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("device_secret") val deviceSecret: String? = null,
    @SerialName("issued_token_type") val issuedTokenType: String? = null
) {
    fun asToken(): Token {
        return Token(
            tokenType = tokenType,
            expiresIn = expiresIn,
            accessToken = accessToken,
            scope = scope,
            refreshToken = refreshToken,
            idToken = idToken,
            deviceSecret = deviceSecret,
            issuedTokenType = issuedTokenType
        )
    }
}

fun Token.asSerializableToken(): SerializableToken {
    return SerializableToken(
        tokenType = tokenType,
        expiresIn = expiresIn,
        accessToken = accessToken,
        scope = scope,
        refreshToken = refreshToken,
        idToken = idToken,
        deviceSecret = deviceSecret,
        issuedTokenType = issuedTokenType
    )
}
