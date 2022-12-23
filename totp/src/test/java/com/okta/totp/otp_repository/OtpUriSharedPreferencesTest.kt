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
package com.okta.totp.otp_repository

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class OtpUriSharedPreferencesTest {

    lateinit var otpUriSharedPreferences: OtpUriSharedPreferences

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.getApplication()
        otpUriSharedPreferences = OtpUriSharedPreferences(appContext)
    }

    @Test
    fun `getOtpUriStrings returns empty list without adding any entries`() {
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEmpty()
    }

    @Test
    fun `add otp uri string`() {
        otpUriSharedPreferences.addOtpUriString("abcd")
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEqualTo(listOf("abcd"))
    }

    @Test
    fun `add two distinct otp uri strings`() {
        otpUriSharedPreferences.addOtpUriString("abcd1")
        otpUriSharedPreferences.addOtpUriString("abcd2")
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEqualTo(
            listOf("abcd1", "abcd2")
        )
    }

    @Test
    fun `adding two same otp uri strings only keeps one entry`() {
        otpUriSharedPreferences.addOtpUriString("abcd")
        otpUriSharedPreferences.addOtpUriString("abcd")
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEqualTo(
            listOf("abcd")
        )
    }

    @Test
    fun `adding otp uri strings preserves insertion order for items with ascending names`() {
        val ascendingOrderItems = (0..9).map { it.toString() }
        ascendingOrderItems.map { otpUriSharedPreferences.addOtpUriString(it) }
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEqualTo(ascendingOrderItems)
    }

    @Test
    fun `adding otp uri strings preserves insertion order for items with descending names`() {
        val ascendingOrderItems = (9 downTo 0).map { it.toString() }
        ascendingOrderItems.map { otpUriSharedPreferences.addOtpUriString(it) }
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEqualTo(ascendingOrderItems)
    }

    @Test
    fun `adding otp uri strings preserves insertion order for items in random order`() {
        val ascendingOrderItems = (0..9).map { it.toString() }.shuffled()
        ascendingOrderItems.map { otpUriSharedPreferences.addOtpUriString(it) }
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEqualTo(ascendingOrderItems)
    }

    @Test
    fun `adding a duplicate entry moves that entry to the end of the list`() {
        val ascendingOrderItems = (0..9).map { it.toString() }
        ascendingOrderItems.map { otpUriSharedPreferences.addOtpUriString(it) }
        otpUriSharedPreferences.addOtpUriString("5")
        val expectedList = ascendingOrderItems.toMutableList().apply {
            remove("5")
            add(this.size, "5")
        }
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEqualTo(expectedList.toList())
    }

    @Test
    fun `remove otp uri string`() {
        otpUriSharedPreferences.addOtpUriString("abcd")
        otpUriSharedPreferences.removeOtpUriString("abcd")
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEmpty()
    }

    @Test
    fun `removing non-existent key does nothing`() {
        otpUriSharedPreferences.addOtpUriString("abcd")
        otpUriSharedPreferences.removeOtpUriString("abcdefg")
        assertThat(otpUriSharedPreferences.getOtpUriStrings()).isEqualTo(
            listOf("abcd")
        )
    }
}
