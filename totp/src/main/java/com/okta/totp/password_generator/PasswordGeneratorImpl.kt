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
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

class PasswordGeneratorImpl(
    otpParams: OtpUriParsingResults.OtpData,
    private val timeProvider: TimeProvider
) : PasswordGenerator {
    private val timeBasedOneTimePasswordGenerator = TimeBasedOneTimePasswordGenerator(
        secret = Base32().decode(otpParams.base32Secret),
        config = TimeBasedOneTimePasswordConfig(
            codeDigits = otpParams.digits,
            hmacAlgorithm = otpParams.algorithm,
            timeStep = otpParams.period,
            timeStepUnit = TimeUnit.SECONDS
        )
    )

    override fun generate(): String {
        return timeBasedOneTimePasswordGenerator.generate(timestamp = timeProvider.getCurrentTimeMillis())
    }
}
