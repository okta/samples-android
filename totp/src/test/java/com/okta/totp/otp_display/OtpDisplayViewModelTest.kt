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

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.okta.totp.coroutine.CoroutineDispatcherRule
import com.okta.totp.coroutine.ticker.TestTickerFlowFactory
import com.okta.totp.otp_repository.OtpUriSharedPreferences
import com.okta.totp.parsing.OtpUriParser
import com.okta.totp.parsing.OtpUriParsingResults
import com.okta.totp.password_generator.PasswordGeneratorFactory
import com.okta.totp.password_generator.TestPasswordGenerator
import com.okta.totp.password_generator.TestPasswordGeneratorFactory
import com.okta.totp.time.TestTimeProvider
import com.okta.totp.util.TestResourceManager
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class OtpDisplayViewModelTest {
    @get:Rule
    val mockRule = MockKRule(this)

    @get:Rule
    val coroutineDispatcherRule = CoroutineDispatcherRule()

    @MockK
    private lateinit var otpUriSharedPreferences: OtpUriSharedPreferences

    private lateinit var otpUriParser: OtpUriParser
    private lateinit var passwordGeneratorFactory: PasswordGeneratorFactory

    private val otpUriStringList = (0..9).map {
        "otpauth://totp/issuer$it:name$it?secret=secret$it"
    }

    @Before
    fun setUp() {
        val timeProvider = TestTimeProvider()
        passwordGeneratorFactory = TestPasswordGeneratorFactory(timeProvider)
        otpUriParser = OtpUriParser(TestResourceManager())
    }

    @Test
    fun `emit empty list if no otp uri strings are stored`() = runTest {
        every { otpUriSharedPreferences.getOtpUriStrings() } returns emptyList()
        val otpDisplayViewModel = getOtpDisplayViewModel(maxUpdates = 0)
        otpDisplayViewModel.otpScreenUiStateFlow.test {
            assertThat(awaitItem()).isEmpty()
            expectNoEvents()
        }
    }

    @Test
    fun `emit initial totp codes for single entry`() = runTest {
        every { otpUriSharedPreferences.getOtpUriStrings() } returns listOf(
            "otpauth://totp/issuer:name?secret=secret"
        )
        val otpDisplayViewModel = getOtpDisplayViewModel(maxUpdates = 0)

        val expectedTimeStep = 0L
        val expectedName = "name"
        val expectedIssuer = "issuer"
        val expectedSecret = "secret"
        otpDisplayViewModel.otpScreenUiStateFlow.test {
            assertThat(awaitItem()).isEqualTo(
                listOf(
                    OtpEntry(
                        otpCode = getExpectedOtpCodeAtTimestepWithInfo(
                            timeStep = expectedTimeStep,
                            name = expectedName,
                            issuer = expectedIssuer,
                            secret = expectedSecret
                        ),
                        account = expectedName,
                        issuer = expectedIssuer,
                        onDeleteOtpEntry = {} // Not checked in equals
                    )
                )
            )
            expectNoEvents()
        }
    }

    @Test
    fun `emit initial totp codes for all entries in list`() = runTest {
        every { otpUriSharedPreferences.getOtpUriStrings() } returns otpUriStringList
        val otpDisplayViewModel = getOtpDisplayViewModel(maxUpdates = 0)

        val expectedResult = (0..9).map {
            val expectedTimeStep = it.toLong()
            val expectedName = "name$it"
            val expectedIssuer = "issuer$it"
            val expectedSecret = "secret$it"
            OtpEntry(
                otpCode = getExpectedOtpCodeAtTimestepWithInfo(
                    timeStep = expectedTimeStep,
                    name = expectedName,
                    issuer = expectedIssuer,
                    secret = expectedSecret
                ),
                account = expectedName,
                issuer = expectedIssuer,
                onDeleteOtpEntry = {} // Not checked in equals
            )
        }

        otpDisplayViewModel.otpScreenUiStateFlow.test {
            assertThat(awaitItem()).isEqualTo(expectedResult)
            expectNoEvents()
        }
    }

    @Test
    fun `updating otp entries once should emit entry with otp code regenerated at new timestep`() = runTest {
        every { otpUriSharedPreferences.getOtpUriStrings() } returns otpUriStringList
        val numUpdates = 1
        val otpDisplayViewModel = getOtpDisplayViewModel(maxUpdates = numUpdates)

        val expectedEmission1 = (0..9).map {
            val expectedTimeStep = it.toLong()
            val expectedName = "name$it"
            val expectedIssuer = "issuer$it"
            val expectedSecret = "secret$it"
            OtpEntry(
                otpCode = getExpectedOtpCodeAtTimestepWithInfo(
                    timeStep = expectedTimeStep,
                    name = expectedName,
                    issuer = expectedIssuer,
                    secret = expectedSecret
                ),
                account = expectedName,
                issuer = expectedIssuer,
                onDeleteOtpEntry = {} // Not checked in equals
            )
        }

        val expectedEmission2 = expectedEmission1.mapIndexed { firstEmissionTimestep, otpEntry ->
            val expectedTimestep = firstEmissionTimestep.toLong() + expectedEmission1.size
            val expectedName = "name$firstEmissionTimestep"
            val expectedIssuer = "issuer$firstEmissionTimestep"
            val expectedSecret = "secret$firstEmissionTimestep"
            OtpEntry(
                otpCode = getExpectedOtpCodeAtTimestepWithInfo(
                    timeStep = expectedTimestep,
                    name = expectedName,
                    issuer = expectedIssuer,
                    secret = expectedSecret
                ),
                account = expectedName,
                issuer = expectedIssuer,
                onDeleteOtpEntry = {} // Not checked in equals
            )
        }

        otpDisplayViewModel.otpScreenUiStateFlow.test {
            assertThat(awaitItem()).isEqualTo(expectedEmission1)
            assertThat(awaitItem()).isEqualTo(expectedEmission2)
            expectNoEvents()
        }
    }

    @Test
    fun `updating otp entries over multiple timesteps regenerates otp codes at each new timestep`() = runTest {
        every { otpUriSharedPreferences.getOtpUriStrings() } returns otpUriStringList
        val numUpdates = 10
        val otpDisplayViewModel = getOtpDisplayViewModel(maxUpdates = numUpdates)

        val emissions = (0..numUpdates).map { numOfPreviousEmissions ->
            (0..9).map {
                val expectedTimeStep = (numOfPreviousEmissions * otpUriStringList.size) + it.toLong()
                val expectedName = "name$it"
                val expectedIssuer = "issuer$it"
                val expectedSecret = "secret$it"
                OtpEntry(
                    otpCode = getExpectedOtpCodeAtTimestepWithInfo(
                        timeStep = expectedTimeStep,
                        name = expectedName,
                        issuer = expectedIssuer,
                        secret = expectedSecret
                    ),
                    account = expectedName,
                    issuer = expectedIssuer,
                    onDeleteOtpEntry = {} // Not checked in equals
                )
            }
        }

        otpDisplayViewModel.otpScreenUiStateFlow.test {
            emissions.take(emissions.size).mapIndexed { index, item ->
                assertThat(awaitItem()).isEqualTo(item)
            }
            expectNoEvents()
        }
    }

    @Test
    fun `deleting otp code emits a new list with that otp code removed`() = runTest {
        every { otpUriSharedPreferences.getOtpUriStrings() } returns otpUriStringList
        every { otpUriSharedPreferences.removeOtpUriString(any()) } just runs
        val otpDisplayViewModel = getOtpDisplayViewModel(maxUpdates = 0)

        val expectedEmission1 = (0..9).map {
            val expectedTimeStep = it.toLong()
            val expectedName = "name$it"
            val expectedIssuer = "issuer$it"
            val expectedSecret = "secret$it"
            OtpEntry(
                otpCode = getExpectedOtpCodeAtTimestepWithInfo(
                    timeStep = expectedTimeStep,
                    name = expectedName,
                    issuer = expectedIssuer,
                    secret = expectedSecret
                ),
                account = expectedName,
                issuer = expectedIssuer,
                onDeleteOtpEntry = {} // Not checked in equals
            )
        }

        val expectedEmission2 = expectedEmission1.filter { it.account != "name5" }

        otpDisplayViewModel.otpScreenUiStateFlow.test {
            val actualEmission1 = awaitItem()
            assertThat(actualEmission1).isEqualTo(expectedEmission1)
            actualEmission1.first { it.account == "name5" }.delete()
            assertThat(awaitItem()).isEqualTo(expectedEmission2)
            expectNoEvents()
        }
    }

    private fun getOtpDisplayViewModel(maxUpdates: Int): OtpDisplayViewModel {
        val testTickerFlowFactory = TestTickerFlowFactory(maxUpdates)
        return OtpDisplayViewModel(
            otpUriSharedPreferences = otpUriSharedPreferences,
            otpUriParser = otpUriParser,
            passwordGeneratorFactory = passwordGeneratorFactory,
            ioDispatcher = StandardTestDispatcher(),
            tickerFlowFactory = testTickerFlowFactory
        )
    }

    private fun getExpectedOtpCodeAtTimestepWithInfo(
        timeStep: Long,
        name: String,
        issuer: String,
        secret: String
    ): String {
        val otpParams = OtpUriParsingResults.OtpData(
            name = name,
            issuer = issuer,
            base32Secret = secret,
            period = OtpUriParser.DEFAULT_PERIOD,
            digits = OtpUriParser.DEFAULT_DIGITS,
            algorithm = OtpUriParser.DEFAULT_ALGORITHM
        )
        return TestPasswordGenerator.getExpectedOtpCodeAtTimestep(timeStep, otpParams)
    }
}
