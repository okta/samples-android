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
package com.okta.android.samples.browser_sign_in.user_dashboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okta.android.samples.browser_sign_in.BuildConfig
import com.okta.authfoundation.claims.email
import com.okta.authfoundation.claims.name
import com.okta.authfoundation.client.OidcClientResult
import com.okta.authfoundationbootstrap.CredentialBootstrap
import com.okta.webauthenticationui.WebAuthenticationClient.Companion.createWebAuthenticationClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UserDashboardViewModel @Inject constructor() : ViewModel() {
    private val _loginState = MutableLiveData<UserDashboardLoginState>(UserDashboardLoginState.Loading)
    private val _userGreetingState = MutableLiveData<UserGreetingState>(UserGreetingState.Loading)
    private val _userInfoState = MutableLiveData<UserInfoState>(UserInfoState.Empty)

    val loginState: LiveData<UserDashboardLoginState> = _loginState
    val userGreetingState: LiveData<UserGreetingState> = _userGreetingState
    val userInfoState: LiveData<UserInfoState> = _userInfoState

    init {
        viewModelScope.launch {
            refreshCurrentLoginState()
        }
    }

    fun logoutOfBrowser(context: Context) {
        viewModelScope.launch {
            _loginState.value = UserDashboardLoginState.Loading

            val result =
                CredentialBootstrap.oidcClient.createWebAuthenticationClient().logoutOfBrowser(
                    context = context,
                    redirectUrl = BuildConfig.SIGN_OUT_REDIRECT_URI,
                    CredentialBootstrap.defaultCredential().token?.idToken ?: ""
                )
            when (result) {
                is OidcClientResult.Error -> {
                    Timber.e(result.exception, "Failed to logout.")
                    _loginState.value = UserDashboardLoginState.LogoutFailed(
                        errorMessage = "Failed to logout."
                    )
                }
                is OidcClientResult.Success -> {
                    CredentialBootstrap.defaultCredential().delete()
                    _loginState.value = UserDashboardLoginState.LoggedOut
                }
            }
        }
    }

    fun fetchUserInfo() {
        viewModelScope.launch {
            _userInfoState.value = UserInfoState.Loading
            val credential = CredentialBootstrap.defaultCredential()

            when (val result = credential.getUserInfo()) {
                is OidcClientResult.Error -> {
                    Timber.e(result.exception, "Failed to fetch userinfo.")
                    _userInfoState.value = UserInfoState.Loaded(
                        userInfoText = "Failed to fetch userinfo."
                    )
                }
                is OidcClientResult.Success -> {
                    val userClaimsList =
                        result.result.deserializeClaims(JsonObject.serializer()).asList()
                    _userInfoState.value = UserInfoState.Loaded(
                        userInfoText = userClaimsList.joinToString("\n")
                    )
                }
            }
        }
    }

    private suspend fun refreshCurrentLoginState() {
        val idToken = CredentialBootstrap.defaultCredential().idToken()
        if (idToken == null) {
            _userGreetingState.value = UserGreetingState.Loading
            _loginState.value = UserDashboardLoginState.LoggedOut
        } else {
            val userName = idToken.name ?: ""
            val userEmail = idToken.email ?: ""
            _userGreetingState.value = UserGreetingState.Loaded(
                name = userName,
                email = userEmail
            )
            _loginState.value = UserDashboardLoginState.LoggedIn
        }
    }
}

/**
 * Represents login state of the user in the current state of the [UserDashboardViewModel]
 */
sealed class UserDashboardLoginState {
    object Loading : UserDashboardLoginState()
    object LoggedOut : UserDashboardLoginState()
    object LoggedIn : UserDashboardLoginState()
    data class LogoutFailed(
        val errorMessage: String
    ) : UserDashboardLoginState()
}

/**
 * Represents the user greeting in the current state of the [UserDashboardViewModel]
 */
sealed class UserGreetingState {
    object Loading : UserGreetingState()
    data class Loaded(
        val name: String,
        val email: String
    ) : UserGreetingState()
}

/**
 * Represents the user info in the current state of the [UserDashboardViewModel]
 */
sealed class UserInfoState {
    object Loading : UserInfoState()
    object Empty : UserInfoState()
    data class Loaded(
        val userInfoText: String
    ) : UserInfoState()
}

private fun JsonObject.asList(): List<String> {
    val list = mutableListOf<String>()
    for (entry in this) {
        val value = entry.value
        if (value is JsonPrimitive) {
            list.add("${entry.key}: ${value.content}")
        } else {
            list.add("${entry.key}: $value")
        }
    }
    return list
}
