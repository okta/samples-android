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

import com.google.common.truth.Truth.assertThat
import com.okta.totp.R
import com.okta.totp.util.TestResourceManager
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for Android Uri class
@RunWith(RobolectricTestRunner::class)
internal class OtpUriParserTest {
    private lateinit var otpUriParser: OtpUriParser

    private val name = "name"
    private val issuer = "issuer"
    private val secret = "secret"
    private val period = 45L
    private val digits = 8
    private val algorithmString = "SHA512"
    private val algorithm = HmacAlgorithm.SHA512

    @Before
    fun setUp() {
        otpUriParser = OtpUriParser(TestResourceManager())
    }

    @Test
    fun `parse minimal valid uri code`() {
        val validUriString = "otpauth://totp/$name?secret=$secret"
        val result = otpUriParser.parseOtpUriString(validUriString)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.OtpData(
                name = name,
                issuer = null,
                base32Secret = secret,
                period = OtpUriParser.DEFAULT_PERIOD,
                digits = OtpUriParser.DEFAULT_DIGITS,
                algorithm = OtpUriParser.DEFAULT_ALGORITHM
            )
        )
    }

    @Test
    fun `parse valid uri code with optional issuer in uri label`() {
        val validUriString = "otpauth://totp/$issuer:$name?secret=$secret"
        val result = otpUriParser.parseOtpUriString(validUriString)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.OtpData(
                name = name,
                issuer = issuer,
                base32Secret = secret,
                period = OtpUriParser.DEFAULT_PERIOD,
                digits = OtpUriParser.DEFAULT_DIGITS,
                algorithm = OtpUriParser.DEFAULT_ALGORITHM
            )
        )
    }

    @Test
    fun `parse valid uri code with optional issuer in uri label and query params`() {
        val validUriString = "otpauth://totp/$issuer:$name?secret=$secret&issuer=$issuer"
        val result = otpUriParser.parseOtpUriString(validUriString)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.OtpData(
                name = name,
                issuer = issuer,
                base32Secret = secret,
                period = OtpUriParser.DEFAULT_PERIOD,
                digits = OtpUriParser.DEFAULT_DIGITS,
                algorithm = OtpUriParser.DEFAULT_ALGORITHM
            )
        )
    }

    @Test
    fun `parse valid uri code with all optional params`() {
        val validUriString = "otpauth://totp/$issuer:$name?secret=$secret&issuer=$issuer&" +
            "algorithm=$algorithmString&digits=$digits&period=$period"
        val result = otpUriParser.parseOtpUriString(validUriString)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.OtpData(
                name = name,
                issuer = issuer,
                base32Secret = secret,
                period = period,
                digits = digits,
                algorithm = algorithm
            )
        )
    }

    @Test
    fun `parse uri code with invalid scheme`() {
        val invalidScheme = "invalidscheme"
        val uriStringWithInvalidScheme = "$invalidScheme://totp/$name?secret=$secret"
        val result = otpUriParser.parseOtpUriString(uriStringWithInvalidScheme)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.Error(
                errorMessage = TestResourceManager.getString(R.string.otp_parsing_invalid_scheme, invalidScheme)
            )
        )
    }

    @Test
    fun `parse uri code with invalid type`() {
        val invalidType = "hotp"
        val uriStringWithInvalidType = "otpauth://$invalidType/$name?secret=$secret"
        val result = otpUriParser.parseOtpUriString(uriStringWithInvalidType)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.Error(
                errorMessage = TestResourceManager.getString(R.string.otp_parsing_invalid_type, invalidType)
            )
        )
    }

    @Test
    fun `parse uri code with invalid label`() {
        val uriStringWithInvalidLabel = "otpauth://totp/$issuer:$name:extraInvalidField?secret=$secret"
        val result = otpUriParser.parseOtpUriString(uriStringWithInvalidLabel)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.Error(
                errorMessage = TestResourceManager.getString(R.string.otp_parsing_invalid_label)
            )
        )
    }

    @Test
    fun `parse uri code with missing secret parameter`() {
        val uriStringWithMissingSecret = "otpauth://totp/$issuer:$name?&issuer=$issuer&" +
            "algorithm=$algorithmString&digits=$digits&period=$period"
        val result = otpUriParser.parseOtpUriString(uriStringWithMissingSecret)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.Error(
                errorMessage = TestResourceManager.getString(R.string.otp_parsing_uri_missing_required_param)
            )
        )
    }

    @Test
    fun `parse uri code with secret parameter with missing argument`() {
        val uriStringWithMissingSecretArgument = "otpauth://totp/$issuer:$name?&secret&issuer=$issuer&" +
            "algorithm=$algorithmString&digits=$digits&period=$period"
        val result = otpUriParser.parseOtpUriString(uriStringWithMissingSecretArgument)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.Error(
                errorMessage = TestResourceManager.getString(R.string.otp_parsing_uri_missing_required_param)
            )
        )
    }

    @Test
    fun `parse uri code with mismatched issuer label and query parameter`() {
        val differentIssuer = "differentIssuer"
        val uriStringWithMismatchedIssuer = "otpauth://totp/$issuer:$name?&secret=$secret&issuer=$differentIssuer"
        val result = otpUriParser.parseOtpUriString(uriStringWithMismatchedIssuer)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.Error(
                errorMessage = TestResourceManager.getString(R.string.otp_parsing_issuer_mismatch)
            )
        )
    }

    @Test
    fun `parse uri code with issuer query parameter missing argument`() {
        val uriStringWithMismatchedIssuer = "otpauth://totp/$issuer:$name?&secret=$secret&issuer"
        val result = otpUriParser.parseOtpUriString(uriStringWithMismatchedIssuer)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.Error(
                errorMessage = TestResourceManager.getString(R.string.otp_parsing_issuer_arg_missing)
            )
        )
    }

    @Test
    fun `parse uri code with invalid algorithm query parameter`() {
        val invalidAlgorithm = "SHA65535"
        val uriStringWithInvalidAlgorithm = "otpauth://totp/$issuer:$name?&secret=$secret&algorithm=$invalidAlgorithm"
        val result = otpUriParser.parseOtpUriString(uriStringWithInvalidAlgorithm)
        assertThat(result).isEqualTo(
            OtpUriParsingResults.Error(
                errorMessage = TestResourceManager.getString(R.string.otp_parsing_invalid_algorithm)
            )
        )
    }
}
