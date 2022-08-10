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

package com.okta.android.samples.browser_sign_in

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.okta.authfoundation.claims.email
import com.okta.authfoundation.claims.name
import com.okta.authfoundation.client.OidcClientResult
import com.okta.authfoundationbootstrap.CredentialBootstrap
import com.okta.webauthenticationui.WebAuthenticationClient.Companion.createWebAuthenticationClient
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import kotlin.collections.iterator

class BrowserSignInViewModel(application: Application) : AndroidViewModel(application) {
    private val _userGreetingState = MutableLiveData<UserSignInState>(UserSignInState.Loading)
    private val _userInfoState = MutableLiveData<UserInfoState>(UserInfoState.Empty(buttonModel = null))

    val userGreetingState: LiveData<UserSignInState> = _userGreetingState
    val userInfoState: LiveData<UserInfoState> = _userInfoState

    init {
        viewModelScope.launch {
            _userGreetingState.value = getCurrentCredentialState()
        }
    }

    private fun login(context: Context) {
        viewModelScope.launch {
            _userGreetingState.value = UserSignInState.Loading

            val result = CredentialBootstrap.oidcClient.createWebAuthenticationClient().login(
                context = context,
                redirectUrl = BuildConfig.SIGN_IN_REDIRECT_URI,
            )
            when (result) {
                is OidcClientResult.Error -> {
                    Timber.e(result.exception, "Failed to login.")
                    _userGreetingState.value = UserSignInState.Loaded(
                        greetingText = "Failed to login.",
                        buttonModel = ButtonModel(
                            actionText = getApplication<Application>().getString(R.string.login_button),
                            buttonAction = ::login,
                        ),
                    )
                }
                is OidcClientResult.Success -> {
                    val credential = CredentialBootstrap.defaultCredential()
                    credential.storeToken(token = result.result)
                    _userGreetingState.value = getCurrentCredentialState()
                    _userInfoState.value = UserInfoState.Empty(
                        buttonModel = ButtonModel(
                            actionText = getApplication<Application>().getString(R.string.fetch_userinfo_button),
                            buttonAction = { fetchUserInfo() }
                        )
                    )
                }
            }
        }
    }

    private fun logoutOfBrowser(context: Context) {
        viewModelScope.launch {
            _userGreetingState.value = UserSignInState.Loading

            val result =
                CredentialBootstrap.oidcClient.createWebAuthenticationClient().logoutOfBrowser(
                    context = context,
                    redirectUrl = BuildConfig.SIGN_OUT_REDIRECT_URI,
                    CredentialBootstrap.defaultCredential().token?.idToken ?: "",
                )
            when (result) {
                is OidcClientResult.Error -> {
                    Timber.e(result.exception, "Failed to logout.")
                    _userGreetingState.value = UserSignInState.Loaded(
                        greetingText = "Failed to logout.",
                        buttonModel = ButtonModel(
                            actionText = getApplication<Application>().getString(R.string.logout_of_browser_button),
                            buttonAction = ::logoutOfBrowser,
                        ),
                    )
                }
                is OidcClientResult.Success -> {
                    CredentialBootstrap.defaultCredential().delete()
                    _userGreetingState.value = getCurrentCredentialState()
                    _userInfoState.value = UserInfoState.Empty(buttonModel = null)
                }
            }
        }
    }

    private fun fetchUserInfo() {
        viewModelScope.launch {
            _userInfoState.value = UserInfoState.Loading
            val credential = CredentialBootstrap.defaultCredential()

            when (val result = credential.getUserInfo()) {
                is OidcClientResult.Error -> {
                    Timber.e(result.exception, "Failed to fetch userinfo.")
                    _userInfoState.value = UserInfoState.Loaded(
                        userInfoText = "Failed to fetch userinfo.",
                        buttonModel = ButtonModel(
                            actionText = getApplication<Application>().getString(R.string.fetch_userinfo_button),
                            buttonAction = { fetchUserInfo() },
                        ),
                    )
                }
                is OidcClientResult.Success -> {
                    val userClaimsList = result.result.deserializeClaims(JsonObject.serializer()).asList()
                    _userInfoState.value = UserInfoState.Loaded(
                        userInfoText = userClaimsList.joinToString("\n"),
                        buttonModel = ButtonModel(
                            actionText = getApplication<Application>().getString(R.string.fetch_userinfo_button),
                            buttonAction = { fetchUserInfo() },
                        ),
                    )
                }
            }
        }
    }

    private suspend fun getCurrentCredentialState(): UserSignInState {
        val credential = CredentialBootstrap.defaultCredential()
        return if (credential.token == null) {
            UserSignInState.Loaded(
                greetingText = getApplication<Application>().getString(R.string.have_account),
                buttonModel = ButtonModel(
                    actionText = getApplication<Application>().getString(R.string.login_button),
                    buttonAction = ::login,
                ),
            )
        } else {
            val name = credential.idToken()?.name ?: ""
            val email = credential.idToken()?.email ?: ""

            UserSignInState.Loaded(
                greetingText = getApplication<Application>().getString(
                    R.string.welcome_user,
                    name,
                    email
                ),
                buttonModel = ButtonModel(
                    actionText = getApplication<Application>().getString(R.string.logout_of_browser_button),
                    buttonAction = ::logoutOfBrowser,
                ),
            )
        }
    }
}

/**
 * Represents the greeting in the current state of the [BrowserSignInViewModel].
 */
sealed class UserSignInState {
    object Loading : UserSignInState()
    data class Loaded(
        val greetingText: String,
        val buttonModel: ButtonModel,
    ) : UserSignInState()
}

/**
 * Represents the user info in the current state of the [BrowserSignInViewModel]
 */
sealed class UserInfoState {
    object Loading : UserInfoState()
    data class Empty(
        val buttonModel: ButtonModel?,
    ) : UserInfoState()
    data class Loaded(
        val userInfoText: String,
        val buttonModel: ButtonModel,
    ) : UserInfoState()
}

data class ButtonModel(
    val actionText: String,
    val buttonAction: (Context) -> Unit,
)

private fun JsonObject.asList(): List<String> {
    val list = mutableListOf<String>()
    for (entry in this) {
        val value = entry.value
        if (value is JsonPrimitive) {
            list.add("${entry.key}: ${value.content}")
        } else {
            list.add("${entry.key}: ${value}")
        }
    }
    return list
}
