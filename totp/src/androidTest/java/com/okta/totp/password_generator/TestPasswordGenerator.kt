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
package com.okta.totp.password_generator

import com.okta.totp.parsing.OtpUriParsingResults
import com.okta.totp.time.TimeProvider

class TestPasswordGenerator(
    private val otpParams: OtpUriParsingResults.OtpData,
    private val timeProvider: TimeProvider
) : PasswordGenerator {
    override fun generate(): String {
        return getExpectedOtpCodeAtTimestep(
            timeStep = timeProvider.getCurrentTimeMillis(),
            otpParams = otpParams
        )
    }

    companion object {
        fun getExpectedOtpCodeAtTimestep(
            timeStep: Long,
            otpParams: OtpUriParsingResults.OtpData
        ): String {
            return "$timeStep: $otpParams"
        }
    }
}
