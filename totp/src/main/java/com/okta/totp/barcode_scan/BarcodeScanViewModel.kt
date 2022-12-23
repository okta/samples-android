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

import androidx.lifecycle.ViewModel
import com.okta.totp.R
import com.okta.totp.otp_repository.OtpUriSharedPreferences
import com.okta.totp.parsing.OtpUriParser
import com.okta.totp.parsing.OtpUriParsingResults
import com.okta.totp.util.ResourceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BarcodeScanViewModel @Inject constructor(
    private val otpUriParser: OtpUriParser,
    private val otpUriSharedPreferences: OtpUriSharedPreferences,
    private val resourceManager: ResourceManager
) : ViewModel() {

    fun addOtpUriString(otpUriString: String): AddOtpResult {
        val parsingResult = otpUriParser.parseOtpUriString(otpUriString)
        when (parsingResult) {
            is OtpUriParsingResults.Error -> return AddOtpResult.Error(parsingResult.errorMessage)
            is OtpUriParsingResults.OtpData -> {
                otpUriSharedPreferences.addOtpUriString(otpUriString)
                return AddOtpResult.Success(
                    resourceManager.getString(
                        R.string.otp_scan_success,
                        parsingResult.name
                    )
                )
            }
        }
    }
}

sealed interface AddOtpResult {
    data class Success(
        val message: String
    ) : AddOtpResult

    data class Error(
        val errorMessage: String
    ) : AddOtpResult
}
