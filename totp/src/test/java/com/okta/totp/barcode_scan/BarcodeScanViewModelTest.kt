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
package com.okta.totp.barcode_scan

import com.google.common.truth.Truth.assertThat
import com.okta.totp.R
import com.okta.totp.otp_repository.OtpUriSharedPreferences
import com.okta.totp.parsing.OtpUriParser
import com.okta.totp.parsing.OtpUriParsingResults
import com.okta.totp.util.TestResourceManager
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class BarcodeScanViewModelTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var otpUriParser: OtpUriParser

    @MockK
    private lateinit var otpUriSharedPreferences: OtpUriSharedPreferences

    private lateinit var barcodeScanViewModel: BarcodeScanViewModel

    private val otpParsingErrorResult = OtpUriParsingResults.Error("error")
    private val otpParsingNonErrorResult = OtpUriParsingResults.OtpData(
        name = "name",
        issuer = "issuer",
        base32Secret = "",
        period = 1,
        digits = 1,
        algorithm = HmacAlgorithm.SHA1
    )

    @Before
    fun setUp() {
        barcodeScanViewModel = BarcodeScanViewModel(
            otpUriParser,
            otpUriSharedPreferences,
            TestResourceManager()
        )
    }

    @Test
    fun `adding invalid otpUriString returns error`() {
        every { otpUriParser.parseOtpUriString("invalidUriString") } returns
            otpParsingErrorResult

        val result = barcodeScanViewModel.addOtpUriString("invalidUriString")

        assertThat(result).isEqualTo(AddOtpResult.Error("error"))
        verify {
            otpUriParser.parseOtpUriString("invalidUriString")
            otpUriSharedPreferences wasNot Called
        }
    }

    @Test
    fun `adding valid otpUriString adds it to OtpUriSharedPreferences`() {
        every { otpUriParser.parseOtpUriString("validUriString") } returns
            otpParsingNonErrorResult
        every { otpUriSharedPreferences.addOtpUriString("validUriString") } just Runs

        val result = barcodeScanViewModel.addOtpUriString("validUriString").let {
            assertThat(it).isInstanceOf(AddOtpResult.Success::class.java)
            it as AddOtpResult.Success
        }

        assertThat(result.message).isEqualTo(
            TestResourceManager.getString(R.string.otp_scan_success, "name")
        )

        verify {
            otpUriParser.parseOtpUriString("validUriString")
            otpUriSharedPreferences.addOtpUriString("validUriString")
        }
    }
}
