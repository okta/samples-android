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
package com.okta.android.samples.browser_sign_in.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.okta.android.samples.browser_sign_in.R
import com.okta.android.samples.browser_sign_in.biometric.BiometricCredentialsManager
import com.okta.android.samples.browser_sign_in.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {
    @Inject
    lateinit var biometricCredentialsManager: BiometricCredentialsManager

    private val viewModel: LoginViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        binding.biometricCheckbox.isChecked = biometricCredentialsManager.biometricEnabled
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginButton.setOnClickListener {
            viewModel.login(requireContext())
        }
        binding.biometricCheckbox.setOnCheckedChangeListener { checkBoxView, isChecked ->
            lifecycleScope.launch {
                if (isChecked) {
                    if (biometricCredentialsManager.biometricAuthenticated) {
                        biometricCredentialsManager.useBiometricCredentialStorage()
                    } else {
                        requestBiometricAuthentication(checkBoxView)
                    }
                } else {
                    biometricCredentialsManager.useDefaultCredentialStorage()
                }
            }
        }

        /**
         * Update the user interface for changes in the sign-in flow.
         *
         * Use an observer to react to updates in [LoginScreenLoginState]. Updates are asynchronous
         * and are triggered both by user actions, such as button clicks, and completing the flow.
         */
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginScreenLoginState.Loading -> {
                    with(binding) {
                        loginLoadingProgressBar.visibility = View.VISIBLE
                        loginContainer.visibility = View.GONE
                    }
                }
                is LoginScreenLoginState.LoggedOut -> {
                    with(binding) {
                        loginLoadingProgressBar.visibility = View.GONE
                        loginContainer.visibility = View.VISIBLE
                    }
                }
                is LoginScreenLoginState.LoggedIn -> {
                    val navigationAction =
                        LoginFragmentDirections.actionLoginFragmentToUserDashboardFragment()
                    findNavController().navigate(navigationAction)
                }
                is LoginScreenLoginState.LoginFailed -> {
                    with(binding) {
                        loginLoadingProgressBar.visibility = View.GONE
                        loginContainer.visibility = View.VISIBLE
                        statusTextView.text = state.errorMessage
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun requestBiometricAuthentication(checkBoxView: CompoundButton) {
        biometricCredentialsManager.requestBiometricAuthentication(
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lifecycleScope.launch {
                        biometricCredentialsManager.useBiometricCredentialStorage()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Timber.e(
                        "Failed biometric authentication with errorCode: $errorCode" +
                            " and errorString: $errString"
                    )
                    displayBiometricAuthFailure()
                    checkBoxView.isChecked = false
                }

                override fun onAuthenticationFailed() {
                    displayBiometricAuthFailure()
                    checkBoxView.isChecked = false
                }
            }
        )
    }

    private fun displayBiometricAuthFailure() {
        Toast.makeText(context, R.string.biometric_authentication_error, Toast.LENGTH_LONG).show()
    }
}
