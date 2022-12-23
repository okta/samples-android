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
package com.okta.totp.parsing

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.okta.totp.R
import com.okta.totp.util.ResourceManager
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import javax.inject.Inject

class OtpUriParser @Inject constructor(
    private val resourceManager: ResourceManager
) {
    fun parseOtpUriString(otpUriString: String): OtpUriParsingResults {
        val uri = Uri.parse(otpUriString)
        when {
            uri.scheme == null -> {
                return OtpUriParsingResults.Error(
                    resourceManager.getString(R.string.otp_parsing_missing_scheme)
                )
            }
            uri.scheme != OTP_SCHEME -> {
                return OtpUriParsingResults.Error(
                    resourceManager.getString(R.string.otp_parsing_invalid_scheme, uri.scheme!!)
                )
            }
            uri.host == null -> {
                return OtpUriParsingResults.Error(
                    resourceManager.getString(R.string.otp_parsing_missing_type)
                )
            }
            uri.host != TOTP -> {
                return OtpUriParsingResults.Error(
                    resourceManager.getString(R.string.otp_parsing_invalid_type, uri.host!!)
                )
            }
        }

        val label = uri.path?.trim('/')?.split(":")
            ?: return OtpUriParsingResults.Error(resourceManager.getString(R.string.otp_parsing_missing_label))

        val name: String
        var issuer: String?
        when (label.size) {
            1 -> {
                issuer = null
                name = label[0]
            }
            2 -> {
                issuer = label[0]
                name = label[1]
            }
            else -> return OtpUriParsingResults.Error(
                resourceManager.getString(R.string.otp_parsing_invalid_label)
            )
        }

        val paramNames = uri.queryParameterNames
        if (!paramNames.contains(SECRET) || uri.getQueryParameter(SECRET).isNullOrEmpty()) {
            return OtpUriParsingResults.Error(
                resourceManager.getString(R.string.otp_parsing_uri_missing_required_param)
            )
        }

        if (paramNames.contains(ISSUER)) {
            val issuerParam = uri.getQueryParameter(ISSUER)
            if (issuerParam.isNullOrEmpty()) {
                return OtpUriParsingResults.Error(
                    resourceManager.getString(R.string.otp_parsing_issuer_arg_missing)
                )
            } else {
                if (issuer != null && issuer != issuerParam) {
                    return OtpUriParsingResults.Error(
                        resourceManager.getString(R.string.otp_parsing_issuer_mismatch)
                    )
                } else {
                    issuer = issuerParam
                }
            }
        }

        val secretKey = uri.getQueryParameter(SECRET)!!
        val period = uri.getQueryParameter(PERIOD)?.toLongOrNull() ?: DEFAULT_PERIOD
        val digits = uri.getQueryParameter(DIGITS)?.toIntOrNull() ?: DEFAULT_DIGITS
        val algorithm = when (uri.getQueryParameter(ALGORITHM)) {
            "SHA1" -> HmacAlgorithm.SHA1
            "SHA256" -> HmacAlgorithm.SHA256
            "SHA512" -> HmacAlgorithm.SHA512
            null -> DEFAULT_ALGORITHM
            else -> return OtpUriParsingResults.Error(
                resourceManager.getString(R.string.otp_parsing_invalid_algorithm)
            )
        }

        return OtpUriParsingResults.OtpData(
            name = name,
            issuer = issuer,
            base32Secret = secretKey,
            period = period,
            digits = digits,
            algorithm = algorithm
        )
    }

    companion object {
        private const val OTP_SCHEME = "otpauth"
        private const val TOTP = "totp"
        private const val SECRET = "secret"
        private const val PERIOD = "period"

        @VisibleForTesting
        const val DEFAULT_PERIOD = 30L
        private const val DIGITS = "digits"

        @VisibleForTesting
        const val DEFAULT_DIGITS = 6
        private const val ALGORITHM = "algorithm"

        @VisibleForTesting
        val DEFAULT_ALGORITHM = HmacAlgorithm.SHA1
        private const val ISSUER = "issuer"
    }
}
