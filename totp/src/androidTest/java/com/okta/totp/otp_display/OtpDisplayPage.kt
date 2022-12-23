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
package com.okta.totp.otp_display

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.okta.totp.R
import com.okta.totp.barcode_scan.BarcodeScanPage
import com.okta.totp.barcode_scan.BarcodeScanPageFactory
import com.okta.totp.util.ResourceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat

class OtpDisplayPage @AssistedInject constructor(
    private val barcodeScanPageFactory: BarcodeScanPageFactory,
    private val resourceManager: ResourceManager,
    @Assisted private val composeRule: ComposeContentTestRule
) {
    init {
        composeRule.apply {
            waitForIdle()
            onNodeWithTag(OtpScreenTestTags.TITLE)
                .assertExists()
                .assertTextEquals(resourceManager.getString(R.string.otp_screen_title))
            onNodeWithTag(OtpScreenTestTags.OTP_SCREEN_LIST).assertExists()
        }
    }

    fun assertOtpListEmpty(): OtpDisplayPage {
        composeRule.onNodeWithTag(OtpScreenTestTags.OTP_CODE).assertDoesNotExist()
        return this
    }

    fun assertOtpListNotEmpty(): OtpDisplayPage {
        assertThat(
            composeRule.onAllNodesWithTag(OtpScreenTestTags.OTP_CODE).fetchSemanticsNodes().size,
            not(0)
        )
        return this
    }

    fun assertOtpListContentEquals(expectedContent: List<OtpEntry>): OtpDisplayPage {
        composeRule.apply {
            val otpCodeNodes = onAllNodesWithTag(OtpScreenTestTags.OTP_CODE)
            assertThat(otpCodeNodes.fetchSemanticsNodes().size, equalTo(expectedContent.size))
            for (i in 0 until expectedContent.size) {
                otpCodeNodes[i].onChildAt(0).assertTextEquals(expectedContent[i].otpCode)
                otpCodeNodes[i].onChildAt(1).assertTextEquals(
                    resourceManager.getString(R.string.account_label, expectedContent[i].account)
                )
                expectedContent[i].issuer?.let {
                    otpCodeNodes[i].onChildAt(2).assertTextEquals(
                        resourceManager.getString(R.string.issuer_label, it)
                    )
                }
            }
        }
        return this
    }

    fun deleteOtpCodeAt(index: Int): OtpDisplayPage {
        composeRule.apply {
            onAllNodesWithTag(OtpScreenTestTags.OTP_CODE_DELETE_BUTTON)[index].performClick()
        }
        return this
    }

    fun goToBarcodeScanPage(): BarcodeScanPage {
        composeRule.onNodeWithTag(OtpScreenTestTags.ADD_BUTTON).performClick()
        return barcodeScanPageFactory.create(composeRule)
    }
}
