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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.okta.totp.barcode_scan.BarcodeScanScreen
import com.okta.totp.navigation.NavigateTo
import com.okta.totp.otp_display.OtpScreen
import com.okta.totp.ui.theme.SamplesAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SamplesAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = NavigateTo.OtpScreen.route) {
                        composable(route = NavigateTo.OtpScreen.route) {
                            OtpScreen(
                                otpDisplayViewModel = hiltViewModel(),
                                onNavigateToBarcodeScreen = {
                                    navController.navigate(NavigateTo.BarcodeScan.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(route = NavigateTo.BarcodeScan.route) {
                            BarcodeScanScreen(
                                barcodeScanViewModel = hiltViewModel(),
                                onNavigateToOtpScreen = {
                                    navController.navigate(NavigateTo.OtpScreen.route) {
                                        popUpTo(NavigateTo.OtpScreen.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
