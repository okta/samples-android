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

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okta.android.samples.browser_sign_in.BuildConfig
import com.okta.authfoundation.client.OidcClientResult
import com.okta.authfoundationbootstrap.CredentialBootstrap
import com.okta.webauthenticationui.WebAuthenticationClient.Companion.createWebAuthenticationClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {
    private val _loginState = MutableLiveData<LoginScreenLoginState>(LoginScreenLoginState.Loading)

    val loginState: LiveData<LoginScreenLoginState> = _loginState

    init {
        viewModelScope.launch {
            refreshCurrentLoginState()
        }
    }

    fun login(context: Context) {
        viewModelScope.launch {
            _loginState.value = LoginScreenLoginState.Loading

            val result = CredentialBootstrap.oidcClient.createWebAuthenticationClient().login(
                context = context,
                redirectUrl = BuildConfig.SIGN_IN_REDIRECT_URI
            )

            when (result) {
                is OidcClientResult.Error -> {
                    Timber.e(result.exception, "Failed to login.")
                    _loginState.value =
                        LoginScreenLoginState.LoginFailed(errorMessage = "Failed to login.")
                }
                is OidcClientResult.Success -> {
                    val credential = CredentialBootstrap.defaultCredential()
                    credential.storeToken(token = result.result)
                    _loginState.value = LoginScreenLoginState.LoggedIn
                }
            }
        }
    }

    private suspend fun refreshCurrentLoginState() {
        val idToken = CredentialBootstrap.defaultCredential().idToken()
        if (idToken == null) {
            _loginState.value = LoginScreenLoginState.LoggedOut
        } else {
            _loginState.value = LoginScreenLoginState.LoggedIn
        }
    }
}

/**
 * Represents login state of the user in the current state of the [LoginViewModel]
 */
sealed class LoginScreenLoginState {
    object Loading : LoginScreenLoginState()
    object LoggedOut : LoginScreenLoginState()
    object LoggedIn : LoginScreenLoginState()
    data class LoginFailed(
        val errorMessage: String
    ) : LoginScreenLoginState()
}
