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

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.okta.totp.coroutine.ticker.TestTickerFlowFactory
import com.okta.totp.coroutine.ticker.TickerFlowFactory
import com.okta.totp.otp_display.OtpDisplayPage
import com.okta.totp.otp_display.OtpDisplayPageFactory
import com.okta.totp.otp_repository.OtpUriSharedPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class BarcodeScanPage @AssistedInject constructor(
    private val otpUriSharedPreferences: OtpUriSharedPreferences,
    private val otpDisplayPageFactory: OtpDisplayPageFactory,
    private val tickerFlowFactory: TickerFlowFactory,
    @Assisted private val composeRule: ComposeContentTestRule
) {
    init {
        composeRule.apply {
            waitForIdle()
            onNodeWithTag(BarcodeScanScreenTestTags.TITLE).assertExists()
        }
    }

    fun scanOtpUriString(otpUriString: String): BarcodeScanPage {
        otpUriSharedPreferences.addOtpUriString(otpUriString)
        return this
    }

    fun scanOtpUriStringList(otpUriStringList: List<String>): BarcodeScanPage {
        otpUriStringList.forEach {
            scanOtpUriString(it)
        }
        return this
    }

    fun goToOtpDisplayPage(): OtpDisplayPage {
        composeRule.onNodeWithTag(BarcodeScanScreenTestTags.BACK_BUTTON).performClick()
        (tickerFlowFactory as TestTickerFlowFactory).clearTickers()
        return otpDisplayPageFactory.create(composeRule)
    }
}
