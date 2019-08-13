/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.okta.oidc

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.snackbar.Snackbar
import com.okta.authn.sdk.client.AuthenticationClient
import com.okta.authn.sdk.client.AuthenticationClients
import com.okta.oidc.fragments.AuthorizedFragment
import com.okta.oidc.fragments.SettingsFragment
import com.okta.oidc.fragments.SharedViewModel
import com.okta.oidc.fragments.SignInFragment
import com.okta.oidc.AuthorizationStatus.AUTHORIZED
import com.okta.oidc.AuthorizationStatus.SIGNED_OUT
import com.okta.oidc.clients.AuthClient
import com.okta.oidc.clients.sessions.SessionClient
import com.okta.oidc.clients.web.WebAuthClient
import com.okta.oidc.results.Result
import com.okta.oidc.storage.SharedPreferenceStorage
import com.okta.oidc.util.AuthorizationException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates.notNull


const val PREF_HARDWARE: String = "hardware_keystore"
const val PREF_CUSTOM: String = "custom_sign_in"
const val PREF_STORAGE_WEB: String = "web_client"
const val PREF_STORAGE_AUTH: String = "auth_client"

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val tag = MainActivity::class.simpleName

    private lateinit var webAuthClient: WebAuthClient
    private lateinit var authClient: AuthClient
    private lateinit var authenticationClient: AuthenticationClient

    private var config: OIDCConfig by notNull()

    private var hardwareKeystore: Boolean = false
    private var customSignIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        ViewModelProvider(this).get(SharedViewModel::class.java).run {
            hint.observe(this@MainActivity, Observer { signIn(false, it, "") })
            userAndPassword.observe(this@MainActivity, Observer { signIn(true, it.first, it.second) })
        }

        config = OIDCConfig.Builder()
            .withJsonFile(this, R.raw.config)
            .create()

        hardwareKeystore = PreferenceManager.getDefaultSharedPreferences(baseContext).getBoolean(PREF_HARDWARE, false)
        customSignIn = PreferenceManager.getDefaultSharedPreferences(baseContext).getBoolean(PREF_CUSTOM, false)

        createAuthClient()
        createWebClient()

        val authenticated = if (customSignIn) {
            authClient.sessionClient.isAuthenticated
        } else {
            webAuthClient.sessionClient.isAuthenticated
        }

        if (authenticated) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, AuthorizedFragment.newInstance(customSignIn)).commit()
        } else {
            supportFragmentManager.beginTransaction().replace(R.id.fragment, SignInFragment.newInstance(customSignIn))
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        getDefaultSharedPreferences(baseContext).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        getDefaultSharedPreferences(baseContext).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment, SettingsFragment.newInstance())
                .addToBackStack(null).commit()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {
        showMessage(getString(R.string.restart_app))
    }

    private fun createAuthClient() {
        authClient = Okta.AuthBuilder()
            .withConfig(config)
            .withContext(this)
            .withStorage(SharedPreferenceStorage(this, PREF_STORAGE_AUTH))
            .setRequireHardwareBackedKeyStore(hardwareKeystore)
            .create()

        authenticationClient = AuthenticationClients.builder()
            .setOrgUrl(BuildConfig.ORG_URL)
            .build()
    }

    private fun createWebClient() {
        webAuthClient = Okta.WebAuthBuilder()
            .withConfig(config)
            .withContext(this)
            .withStorage(SharedPreferenceStorage(this, PREF_STORAGE_WEB))
            .setRequireHardwareBackedKeyStore(hardwareKeystore)
            .create()

        webAuthClient.registerCallback(object : ResultCallback<AuthorizationStatus, AuthorizationException> {
            override fun onCancel() {
                network_progress.hide()
                showMessage(getString(R.string.operation_cancelled))
            }

            override fun onError(msg: String?, exception: AuthorizationException?) {
                signInError(msg, exception)
            }

            override fun onSuccess(result: AuthorizationStatus) {
                network_progress.hide()
                when (result) {
                    AUTHORIZED -> signInSuccess()
                    SIGNED_OUT -> showMessage(getString(R.string.sign_out_success))
                }
            }
        }, this)
    }

    fun signOut() {
        networkCallInProgress()
        webAuthClient.signOutOfOkta(this)
    }

    private fun signInSuccess() {
        showMessage(getString(R.string.authorized))
        supportFragmentManager.beginTransaction().replace(
            R.id.fragment,
            AuthorizedFragment.newInstance(customSignIn)
        ).commit()
    }

    private fun signInError(msg: String?, exception: AuthorizationException?) {
        network_progress.hide()
        showMessage(msg ?: getString(R.string.unknown))
        Log.d(tag, "onError: ", exception)
    }

    private fun signIn(
        customSignIn: Boolean,
        hintOrUsername: String,
        password: String
    ) {
        if (customSignIn) {
            hintOrUsername.isEmpty().or(password.isEmpty())
                .run {
                    if (this) {
                        showMessage(getString(R.string.invalid_input))
                    } else {
                        networkCallInProgress()
                        authenticateUser(hintOrUsername, password)
                    }
                }
        } else {
            networkCallInProgress()
            webAuthClient.signIn(this, AuthenticationPayload.Builder().setLoginHint(hintOrUsername).build())
        }
    }

    private fun authenticateUser(username: String, password: String) {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                authenticationClient.authenticate(
                    username, password.toCharArray(),
                    null, null
                )
            }?.run {
                authClient.signIn(sessionToken, null, object : RequestCallback<Result, AuthorizationException> {
                    override fun onSuccess(result: Result) {
                        signInSuccess()
                    }

                    override fun onError(error: String?, exception: AuthorizationException?) {
                        signInError(error, exception)
                    }
                })
            }
        }
    }

    fun getSession(): SessionClient? {
        return if (customSignIn) {
            authClient.sessionClient
        } else {
            webAuthClient.sessionClient
        }
    }

    private fun networkCallInProgress() {
        network_progress?.show()
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.loading), Snackbar.LENGTH_INDEFINITE)
            .let { bar ->
                bar.setAction(getString(R.string.cancel)) {
                    if (customSignIn) {
                        authClient.cancel()
                    } else {
                        webAuthClient.cancel()
                    }
                    bar.dismiss()
                }
            }.show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .let { bar ->
                bar.setAction(getString(R.string.dismiss)) {
                    bar.dismiss()
                }
            }.show()
    }
}
