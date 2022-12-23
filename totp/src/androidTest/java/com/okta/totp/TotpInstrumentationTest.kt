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
package com.okta.totp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.okta.totp.barcode_scan.BarcodeScanPageFactory
import com.okta.totp.coroutine.ticker.TestTickerFlowFactory
import com.okta.totp.coroutine.ticker.TickerFlowFactory
import com.okta.totp.otp_display.OtpDisplayPageFactory
import com.okta.totp.otp_display.OtpEntry
import com.okta.totp.otp_repository.OtpUriSharedPreferences
import com.okta.totp.parsing.OtpUriParser
import com.okta.totp.parsing.OtpUriParsingResults
import com.okta.totp.password_generator.TestPasswordGenerator
import com.okta.totp.util.ResourceManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class TotpInstrumentationTest {
    @get:Rule(order = 1)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    var composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var otpUriSharedPreferences: OtpUriSharedPreferences

    @Inject
    lateinit var _tickerFlowFactory: TickerFlowFactory
    private val testTickerFlowFactory: TestTickerFlowFactory
        get() = _tickerFlowFactory as TestTickerFlowFactory

    @Inject
    lateinit var otpUriParser: OtpUriParser

    @Inject
    lateinit var resourceManager: ResourceManager

    @Inject
    lateinit var otpDisplayPageFactory: OtpDisplayPageFactory

    @Inject
    lateinit var barcodeScanPageFactory: BarcodeScanPageFactory

    private val otpSecret = "secret"
    private val otpAccount = "account"
    private val otpIssuer = "issuer"
    private val otpUriString = getOtpUriString(
        otpSecret = otpSecret,
        otpAccount = otpAccount,
        otpIssuer = otpIssuer
    )

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun launchEmptyOtpCodeScreen() {
        val otpDisplayPage = otpDisplayPageFactory.create(composeRule)
        otpDisplayPage.assertOtpListEmpty()
    }

    @Test
    fun addNewOtpUriCodeAndDisplayItOnOtpScreen() {
        val otpParams =
            otpUriParser.parseOtpUriString(otpUriString) as OtpUriParsingResults.OtpData

        val expectedOtpEntry = OtpEntry(
            otpCode = TestPasswordGenerator.getExpectedOtpCodeAtTimestep(0, otpParams),
            account = otpAccount,
            issuer = otpIssuer,
            onDeleteOtpEntry = {}
        )

        val otpDisplayPage = otpDisplayPageFactory.create(composeRule)

        otpDisplayPage
            .assertOtpListEmpty()
            .goToBarcodeScanPage()
            .scanOtpUriString(otpUriString)
            .goToOtpDisplayPage()
            .assertOtpListContentEquals(listOf(expectedOtpEntry))
    }

    @Test
    fun deleteOtpCodeEntryFromList() {
        val otpDisplayPage = otpDisplayPageFactory.create(composeRule)

        otpDisplayPage
            .assertOtpListEmpty()
            .goToBarcodeScanPage()
            .scanOtpUriString(otpUriString)
            .goToOtpDisplayPage()
            .assertOtpListNotEmpty()
            .deleteOtpCodeAt(index = 0)
            .assertOtpListEmpty()

        assertThat(otpUriSharedPreferences.getOtpUriStrings(), equalTo(emptyList()))
    }

    @Test
    fun updateOtpCodeAfterEachTimestep() {
        val otpParams =
            otpUriParser.parseOtpUriString(otpUriString) as OtpUriParsingResults.OtpData

        val numUpdates = 10

        val otpDisplayPage = otpDisplayPageFactory.create(composeRule)

        otpDisplayPage
            .assertOtpListEmpty()
            .goToBarcodeScanPage()
            .scanOtpUriString(otpUriString)
            .goToOtpDisplayPage()

        assertThat(testTickerFlowFactory.tickerEmitters.size, equalTo(1))

        for (timeStep in 0L until numUpdates) {
            val expectedOtpEntry = OtpEntry(
                otpCode = TestPasswordGenerator.getExpectedOtpCodeAtTimestep(timeStep, otpParams),
                account = otpAccount,
                issuer = otpIssuer,
                onDeleteOtpEntry = {}
            )
            otpDisplayPage.assertOtpListContentEquals(listOf(expectedOtpEntry))
            runBlocking {
                testTickerFlowFactory.tickerEmitters[0].emit(Unit)
            }
        }
    }

    @Test
    fun updateMultipleOtpCodesAfterEachStep() {
        val otpStringList = (0 until 3).map { i ->
            getOtpUriString(
                otpSecret = "secret$i",
                otpAccount = "account$i",
                otpIssuer = "issuer$i"
            )
        }

        val otpParamsList = otpStringList.map {
            otpUriParser.parseOtpUriString(it) as OtpUriParsingResults.OtpData
        }

        val numUpdates = 10

        val otpDisplayPage = otpDisplayPageFactory.create(composeRule)

        otpDisplayPage
            .assertOtpListEmpty()
            .goToBarcodeScanPage()
            .scanOtpUriStringList(otpStringList)
            .goToOtpDisplayPage()

        assertThat(testTickerFlowFactory.tickerEmitters.size, equalTo(1))

        var timeStep = 0L
        for (i in 0L until numUpdates) {
            val expectedOtpEntries = otpParamsList.map {
                OtpEntry(
                    otpCode = TestPasswordGenerator.getExpectedOtpCodeAtTimestep(timeStep++, it),
                    account = it.name,
                    issuer = it.issuer,
                    onDeleteOtpEntry = {}
                )
            }
            otpDisplayPage.assertOtpListContentEquals(expectedOtpEntries)
            runBlocking {
                testTickerFlowFactory.tickerEmitters[0].emit(Unit)
            }
        }
    }

    private fun getOtpUriString(
        otpSecret: String,
        otpAccount: String,
        otpIssuer: String
    ): String {
        val otpUriStringTemplate = "otpauth://totp/%s:%s?secret=%s&issuer=%s"
        return otpUriStringTemplate.format(otpIssuer, otpAccount, otpSecret, otpIssuer)
    }
}
