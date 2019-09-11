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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.lifecycle.*
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.snackbar.Snackbar
import com.okta.authn.sdk.client.AuthenticationClient
import com.okta.authn.sdk.client.AuthenticationClients
import com.okta.oidc.AuthorizationStatus.AUTHORIZED
import com.okta.oidc.AuthorizationStatus.SIGNED_OUT
import com.okta.oidc.clients.AuthClient
import com.okta.oidc.clients.sessions.SessionClient
import com.okta.oidc.clients.web.WebAuthClient
import com.okta.oidc.fragments.*
import com.okta.oidc.results.Result
import com.okta.oidc.storage.SharedPreferenceStorage
import com.okta.oidc.storage.security.DefaultEncryptionManager
import com.okta.oidc.storage.security.EncryptionManager
import com.okta.oidc.storage.security.GuardedEncryptionManager
import com.okta.oidc.util.AuthorizationException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates.notNull


const val PREF_HARDWARE: String = "hardware_keystore"
const val PREF_CUSTOM: String = "custom_sign_in"
const val PREF_BIOMETRIC: String = "use_biometric"
const val PREF_STORAGE_WEB: String = "web_client"
const val PREF_STORAGE_AUTH: String = "auth_client"

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val logTag = MainActivity::class.simpleName
    private val settingsTag = "SETTINGS_FRAGMENT"

    private var hardwareKeystore: Boolean = false
    private var customSignIn: Boolean = false
    private var useBiometric: Boolean = false
    private var preferencesChange: Boolean = false

    private lateinit var webAuthClient: WebAuthClient
    private lateinit var authClient: AuthClient
    private lateinit var authenticationClient: AuthenticationClient
    private lateinit var keyGuardEncryptionManager: GuardedEncryptionManager
    private lateinit var defaultEncryptionManager: DefaultEncryptionManager
    private lateinit var currentEncryptionManager: EncryptionManager

    private var config: OIDCConfig by notNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        ViewModelProvider(this).get(SharedViewModel::class.java).run {
            hint.observe(this@MainActivity, Observer { signIn(false, it, "") })
            userAndPassword.observe(
                this@MainActivity,
                Observer { signIn(true, it.first, it.second) })
            deviceAuthenticated.observe(this@MainActivity, Observer {
                if (it) {
                    currentEncryptionManager.cipher ?: currentEncryptionManager.recreateCipher()
                    startFragmentTransaction()
                } else {
                    showMessage(getString(R.string.unauthenticated))
                }
            })
        }

        config = OIDCConfig.Builder()
            .withJsonFile(this, R.raw.config)
            .create()

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentLifecycleCallbacks() {
            override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
                if (f.tag?.equals(settingsTag) == true && preferencesChange) {
                    updateClients()
                }
            }
        }, false)

        keyGuardEncryptionManager = GuardedEncryptionManager(this)
        defaultEncryptionManager = DefaultEncryptionManager(this)

        hardwareKeystore = getDefaultSharedPreferences(baseContext)
            .getBoolean(PREF_HARDWARE, false)
        customSignIn = getDefaultSharedPreferences(baseContext)
            .getBoolean(PREF_CUSTOM, false)
        useBiometric = getDefaultSharedPreferences(baseContext)
            .getBoolean(PREF_BIOMETRIC, false)

        currentEncryptionManager =
            if (useBiometric) keyGuardEncryptionManager else defaultEncryptionManager

        createAuthClient()
        createWebClient()
        startFragmentTransaction()
    }

    override fun onResume() {
        super.onResume()
        getDefaultSharedPreferences(baseContext).registerOnSharedPreferenceChangeListener(this)
        if (isAuthenticated() && !currentEncryptionManager.isUserAuthenticatedOnDevice) {
            biometricPrompt()
        }
    }

    override fun onPause() {
        super.onPause()
        getDefaultSharedPreferences(baseContext).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings
            && supportFragmentManager.findFragmentByTag(settingsTag) == null
        ) {
            preferencesChange = false
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, SettingsFragment.newInstance(), settingsTag)
                .addToBackStack(null).commit()
            return false
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {
        preferencesChange = true
    }

    private fun updateClients() {
        val recreateClient = onBiometricChanged(
            getDefaultSharedPreferences(baseContext).getBoolean(PREF_BIOMETRIC, useBiometric)
        ).or(
            onHardwareRequirementChanged(
                getDefaultSharedPreferences(baseContext).getBoolean(
                    PREF_HARDWARE, hardwareKeystore
                )
            )
        )
        if (recreateClient) {
            createAuthClient()
            createWebClient()
        }

        getDefaultSharedPreferences(baseContext)
            .getBoolean(PREF_CUSTOM, customSignIn).takeIf { it != customSignIn }?.apply {
                customSignIn = this
                startFragmentTransaction()
            }
    }

    private fun isAuthenticated(): Boolean {
        return if (customSignIn) {
            authClient.sessionClient.isAuthenticated
        } else {
            webAuthClient.sessionClient.isAuthenticated
        }
    }

    private fun startFragmentTransaction() {
        if (isAuthenticated() && !currentEncryptionManager.isUserAuthenticatedOnDevice) {
            return
        }
        if (isAuthenticated()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, AuthorizedFragment.newInstance(customSignIn)).commit()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, SignInFragment.newInstance(customSignIn))
                .commit()
        }
    }

    private fun biometricPrompt() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment, BiometricFragment.newInstance())
            .commit()
    }

    private fun onHardwareRequirementChanged(on: Boolean): Boolean {
        if (on != hardwareKeystore) {
            hardwareKeystore = on
            return true
        }
        return false
    }

    private fun onBiometricChanged(on: Boolean): Boolean {
        if (on != useBiometric) {
            try {
                useBiometric = on
                if (!keyGuardEncryptionManager.isValidKeys) {
                    keyGuardEncryptionManager.recreateKeys(this)
                }
                keyGuardEncryptionManager.recreateCipher()
                currentEncryptionManager = if (useBiometric) {
                    keyGuardEncryptionManager
                } else {
                    defaultEncryptionManager
                }
                getSession()?.migrateTo(currentEncryptionManager)
            } catch (e: AuthorizationException) {
                showMessage(getString(R.string.migrate_error))
                Log.d(logTag, "Error migrateTo", e)
            }
            return true
        }
        return false
    }

    private fun createAuthClient() {
        authClient = Okta.AuthBuilder()
            .withConfig(config)
            .withContext(this)
            .withStorage(SharedPreferenceStorage(this, PREF_STORAGE_AUTH))
            .withEncryptionManager(currentEncryptionManager)
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
            .withEncryptionManager(currentEncryptionManager)
            .setRequireHardwareBackedKeyStore(hardwareKeystore)
            .create()

        webAuthClient.registerCallback(object :
            ResultCallback<AuthorizationStatus, AuthorizationException> {
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
        network_progress.hide()
        supportFragmentManager.beginTransaction().replace(
            R.id.fragment,
            AuthorizedFragment.newInstance(customSignIn)
        ).commit()
    }

    private fun signInError(msg: String?, exception: AuthorizationException?) {
        network_progress.hide()
        showMessage(msg ?: getString(R.string.unknown))
        Log.d(logTag, "onError: ", exception)
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
            webAuthClient.signIn(
                this,
                AuthenticationPayload.Builder().setLoginHint(hintOrUsername).build()
            )
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
                authClient.signIn(
                    sessionToken,
                    null,
                    object : RequestCallback<Result, AuthorizationException> {
                        override fun onSuccess(result: Result) {
                            signInSuccess()
                        }

                        override fun onError(
                            error: String?,
                            exception: AuthorizationException?
                        ) {
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
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.loading),
            Snackbar.LENGTH_INDEFINITE
        )
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
