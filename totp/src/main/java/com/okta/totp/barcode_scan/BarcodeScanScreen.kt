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

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.okta.totp.R

@Composable
fun BarcodeScanScreen(
    barcodeScanViewModel: BarcodeScanViewModel,
    onNavigateToOtpScreen: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(modifier = Modifier.background(color = Color.LightGray)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.go_back),
                    modifier = Modifier
                        .clickable(onClick = onNavigateToOtpScreen)
                        .padding(16.dp)
                        .testTag(BarcodeScanScreenTestTags.BACK_BUTTON)
                )
                Text(
                    text = stringResource(id = R.string.barcode_screen_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(color = Color.LightGray)
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .testTag(BarcodeScanScreenTestTags.TITLE)
                )
            }
        }
    ) { paddingValues ->
        CameraPreviewWithPermissions(
            modifier = Modifier.padding(paddingValues),
            onAddOtpUriString = { otpUriString -> barcodeScanViewModel.addOtpUriString(otpUriString) },
            onNavigateToOtpScreen = onNavigateToOtpScreen
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreviewWithPermissions(
    modifier: Modifier = Modifier,
    onAddOtpUriString: (String) -> AddOtpResult,
    onNavigateToOtpScreen: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

    when (cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            CameraPreview(modifier, onAddOtpUriString, onNavigateToOtpScreen)
        }
        is PermissionStatus.Denied -> {
            Column(modifier.padding(8.dp)) {
                Text(
                    stringResource(id = R.string.camera_permission_request),
                    modifier = Modifier.testTag(BarcodeScanScreenTestTags.REQUEST_PERMISSION_TEXT)
                )
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    modifier = Modifier.testTag(BarcodeScanScreenTestTags.REQUEST_PERMISSION_BUTTON)
                ) {
                    Text(stringResource(id = R.string.request_permission))
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onAddOtpUriString: (String) -> AddOtpResult,
    onNavigateToOtpScreen: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }
    var barcodeScanner: BarcodeScanner? by remember { mutableStateOf(null) }

    var dialogMessage by remember { mutableStateOf("") }
    var openDialog by remember { mutableStateOf(false) }
    var dialogDismissAction by remember { mutableStateOf({}) }

    Column(modifier = modifier) {
        if (openDialog) {
            AlertDialog(
                onDismissRequest = {
                    openDialog = false
                    dialogDismissAction()
                },
                text = {
                    Text(dialogMessage)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openDialog = false
                            dialogDismissAction()
                        }
                    ) {
                        Text(stringResource(id = R.string.ok))
                    }
                }
            )
        }
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                cameraController.bindToLifecycle(lifecycleOwner)
                previewView.controller = cameraController

                barcodeScanner?.close()
                barcodeScanner = BarcodeScanning.getClient()
                val executor = ContextCompat.getMainExecutor(ctx)

                cameraController.clearImageAnalysisAnalyzer()
                cameraController.setImageAnalysisAnalyzer(
                    executor,
                    MlKitAnalyzer(
                        listOf(barcodeScanner),
                        CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        executor
                    ) { result ->
                        val barcode = result.getValue(barcodeScanner!!)?.firstOrNull()
                        barcode?.displayValue?.let { otpUriString ->
                            val addOtpResult = onAddOtpUriString(otpUriString)
                            when (addOtpResult) {
                                is AddOtpResult.Success -> {
                                    dialogDismissAction = onNavigateToOtpScreen
                                    dialogMessage = addOtpResult.message
                                    openDialog = true
                                    cameraController.clearImageAnalysisAnalyzer()
                                }
                                is AddOtpResult.Error -> {
                                    dialogMessage = addOtpResult.errorMessage
                                    openDialog = true
                                }
                            }
                        }
                    }
                )
                cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
