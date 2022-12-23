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

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.okta.totp.R

@Composable
fun OtpScreen(otpDisplayViewModel: OtpDisplayViewModel, onNavigateToBarcodeScreen: () -> Unit) {
    val otpEntryList by otpDisplayViewModel.otpScreenUiStateFlow.collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            Text(
                text = stringResource(id = R.string.otp_screen_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(color = Color.LightGray)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag(OtpScreenTestTags.TITLE)
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToBarcodeScreen,
                modifier = Modifier.testTag(OtpScreenTestTags.ADD_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.otp_screen_add_button_description)
                )
            }
        }
    ) { padding ->
        OtpScreenList(
            otpEntryList = otpEntryList,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun OtpScreenList(otpEntryList: List<OtpEntry>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .padding(16.dp)
            .testTag(OtpScreenTestTags.OTP_SCREEN_LIST),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(otpEntryList) { otpData ->
            OtpCode(otpEntry = otpData)
        }
    }
}

@Composable
fun OtpCode(otpEntry: OtpEntry, modifier: Modifier = Modifier) {
    Row(modifier = modifier.testTag(OtpScreenTestTags.OTP_CODE)) {
        Column(modifier = Modifier.weight(5f)) {
            Text(
                otpEntry.otpCode,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                fontSize = 16.sp,
                modifier = Modifier.testTag(OtpScreenTestTags.OTP_CODE_TEXT)
            )
            Text(
                stringResource(id = R.string.account_label, otpEntry.account),
                modifier = Modifier.testTag(OtpScreenTestTags.OTP_CODE_ACCOUNT)
            )
            otpEntry.issuer?.let {
                Text(
                    stringResource(id = R.string.issuer_label, otpEntry.issuer),
                    modifier = Modifier.testTag(OtpScreenTestTags.OTP_CODE_ISSUER)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(16.dp)
                .testTag(OtpScreenTestTags.OTP_CODE_DELETE_BUTTON),
            onClick = { otpEntry.delete() }
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(id = R.string.delete_button_description)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewOtpScreenList() {
    val context = LocalContext.current
    OtpScreenList(
        listOf(
            OtpEntry(
                "123456789",
                "accoun1t@gmail.com",
                "issuer1",
                onDeleteOtpEntry = {
                    Toast.makeText(
                        context,
                        "Deleting entry 1",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ),
            OtpEntry(
                "321412341",
                "account2@gmail.com",
                "issuer2",
                onDeleteOtpEntry = {
                    Toast.makeText(
                        context,
                        "Deleting entry 2",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ),
            OtpEntry(
                "653156123",
                "account3@gmail.com",
                "issuer3",
                onDeleteOtpEntry = {
                    Toast.makeText(
                        context,
                        "Deleting entry 3",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        )
    )
}

class OtpEntry(
    val otpCode: String,
    val account: String,
    val issuer: String?,
    private val onDeleteOtpEntry: () -> Unit
) {
    fun delete() {
        onDeleteOtpEntry()
    }

    override fun equals(other: Any?): Boolean {
        if (other is OtpEntry) {
            return other.otpCode == otpCode && other.account == account && other.issuer == issuer
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = otpCode.hashCode()
        result = 31 * result + account.hashCode()
        result = 31 * result + (issuer?.hashCode() ?: 0)
        return result
    }
}
