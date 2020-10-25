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

package com.okta.sample.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.okta.sample.MainActivity
import com.okta.oidc.RequestCallback
import com.okta.oidc.Tokens
import com.okta.oidc.clients.sessions.SessionClient
import com.okta.oidc.net.params.TokenTypeHint
import com.okta.oidc.net.response.IntrospectInfo
import com.okta.oidc.util.AuthorizationException
import com.okta.sample.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_manage_tokens.*

class ManageTokensFragment : Fragment(), View.OnClickListener {
    private val TAG: String = "ManageTokensFragment"
    private var sessionClient: SessionClient? = null

    companion object {
        @JvmStatic
        fun newInstance() = ManageTokensFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_tokens, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            sessionClient = (it as MainActivity).getSession()
            sessionClient?.let {
                refresh_token.setOnClickListener(this)
                revoke_token.setOnClickListener(this)
                introspect_token.setOnClickListener(this)
                display_token.setOnClickListener(this)
            } ?: Snackbar.make(
                view,
                getString(R.string.invalid_session),
                Snackbar.LENGTH_INDEFINITE
            ).run {
                setAction(getString(R.string.dismiss)) {
                    activity?.onBackPressed()
                    dismiss()
                }
            }.show()
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            refresh_token.id -> refreshToken()
            revoke_token.id -> selectToken(resources.getStringArray(R.array.tokens).dropLast(1).toTypedArray()) {
                when (it) {
                    0 -> revokeToken(sessionClient?.tokens?.accessToken, TokenTypeHint.ACCESS_TOKEN)
                    1 -> revokeToken(sessionClient?.tokens?.refreshToken, TokenTypeHint.REFRESH_TOKEN)
                }
            }
            introspect_token.id -> selectToken(resources.getStringArray(R.array.tokens)) {
                when (it) {
                    0 -> introspectToken(sessionClient?.tokens?.accessToken, TokenTypeHint.ACCESS_TOKEN)
                    1 -> introspectToken(sessionClient?.tokens?.refreshToken, TokenTypeHint.REFRESH_TOKEN)
                    2 -> introspectToken(sessionClient?.tokens?.idToken, TokenTypeHint.ID_TOKEN)
                }
            }
            display_token.id -> {
                selectToken(resources.getStringArray(R.array.tokens)) {
                    when (it) {
                        0 -> info_view.text = sessionClient?.tokens?.accessToken
                        1 -> info_view.text = sessionClient?.tokens?.refreshToken
                        2 -> info_view.text = sessionClient?.tokens?.idToken
                    }
                }
            }
        }
    }

    private fun selectToken(choices: Array<String>, action: (Int) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.select_token))
            .setSingleChoiceItems(choices, -1) { dialogInterface, which ->
                action(which)
                dialogInterface.dismiss()
            }.create().show()
    }

    private fun introspectToken(token: String?, hint: String) {
        sessionClient?.run {
            networkCallInProgress()
            introspectToken(
                token,
                hint,
                object : RequestCallback<IntrospectInfo, AuthorizationException> {
                    override fun onSuccess(result: IntrospectInfo) {
                        activity?.network_progress?.hide()
                        info_view.text = getString(R.string.active, hint, result.isActive)
                        Snackbar.make(info_view, getString(R.string.success), Snackbar.LENGTH_SHORT).show()
                    }

                    override fun onError(error: String?, exception: AuthorizationException?) {
                        activity?.network_progress?.hide()
                        Log.d(TAG, "Error: ", exception)
                        Snackbar.make(info_view, getString(R.string.error), Snackbar.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun revokeToken(token: String?, hint: String) {
        sessionClient?.run {
            networkCallInProgress()
            revokeToken(
                token,
                object : RequestCallback<Boolean, AuthorizationException> {
                    override fun onSuccess(result: Boolean) {
                        activity?.network_progress?.hide()
                        info_view.text = getString(R.string.revoked, hint, result)
                        Snackbar.make(info_view, getString(R.string.success), Snackbar.LENGTH_SHORT).show()
                    }

                    override fun onError(error: String?, exception: AuthorizationException?) {
                        activity?.network_progress?.hide()
                        Log.d(TAG, "Error: ", exception)
                        Snackbar.make(info_view, getString(R.string.error), Snackbar.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun refreshToken() {
        sessionClient?.run {
            networkCallInProgress()
            refreshToken(object : RequestCallback<Tokens, AuthorizationException> {
                override fun onSuccess(result: Tokens) {
                    activity?.network_progress?.hide()
                    Snackbar.make(info_view, getString(R.string.success), Snackbar.LENGTH_SHORT).show()
                }

                override fun onError(error: String?, exception: AuthorizationException?) {
                    activity?.network_progress?.hide()
                    Log.d(TAG, "Error: ", exception)
                    Snackbar.make(info_view, getString(R.string.error), Snackbar.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun networkCallInProgress() {
        activity?.network_progress?.show()
        Snackbar.make(info_view, getString(R.string.loading), Snackbar.LENGTH_INDEFINITE)
            .let { bar ->
                bar.setAction(getString(R.string.cancel)) {
                    sessionClient?.cancel()
                    bar.dismiss()
                }
            }.show()
    }
}