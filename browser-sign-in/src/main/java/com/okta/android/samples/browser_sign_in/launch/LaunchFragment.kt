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
package com.okta.android.samples.browser_sign_in.launch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.okta.android.samples.browser_sign_in.R
import com.okta.android.samples.browser_sign_in.biometric.BiometricCredentialsManager
import com.okta.android.samples.browser_sign_in.databinding.FragmentLaunchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LaunchFragment : Fragment() {
    @Inject
    lateinit var biometricCredentialsManager: BiometricCredentialsManager

    private var _binding: FragmentLaunchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaunchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.authenticateButton.setOnClickListener {
            binding.launchLoadingProgressBar.visibility = View.VISIBLE
            binding.retryBiometricsContainer.visibility = View.GONE
            requestBiometricAuthentication()
        }
        if (biometricCredentialsManager.biometricEnabled) {
            requestBiometricAuthentication()
        } else {
            lifecycleScope.launch {
                biometricCredentialsManager.useDefaultCredentialStorage()
                navigateToLoginScreen()
            }
        }
    }

    private fun requestBiometricAuthentication() {
        biometricCredentialsManager.requestBiometricAuthentication(
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lifecycleScope.launch {
                        biometricCredentialsManager.useBiometricCredentialStorage()
                        navigateToLoginScreen()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Timber.e(
                        "Failed biometric authentication with errorCode: $errorCode" +
                            " and errorString: $errString"
                    )
                    displayBiometricAuthFailure()
                }

                override fun onAuthenticationFailed() {
                    displayBiometricAuthFailure()
                }
            }
        )
    }

    private fun displayBiometricAuthFailure() {
        Toast.makeText(context, R.string.biometric_authentication_error, Toast.LENGTH_LONG).show()
        binding.launchLoadingProgressBar.visibility = View.GONE
        binding.retryBiometricsContainer.visibility = View.VISIBLE
    }

    private fun navigateToLoginScreen() {
        val navigationAction = LaunchFragmentDirections.actionLaunchFragmentToLoginFragment()
        findNavController().navigate(navigationAction)
    }
}
