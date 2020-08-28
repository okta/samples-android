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

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.okta.sample.MainActivity

import com.okta.oidc.RequestCallback
import com.okta.oidc.clients.sessions.SessionClient
import com.okta.oidc.net.response.UserInfo
import com.okta.oidc.util.AuthorizationException
import com.okta.sample.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.authorized_fragment.*

class AuthorizedFragment : Fragment(), View.OnClickListener {
    private val TAG: String = "AuthorizedFragment"
    private var sessionClient: SessionClient? = null
    private var customSignIn: Boolean = false

    companion object {
        private const val CUSTOM_SIGN_IN = "custom_sign_in"
        @JvmStatic
        fun newInstance(customSignIn: Boolean) = AuthorizedFragment().apply {
            arguments = Bundle().apply {
                putBoolean(CUSTOM_SIGN_IN, customSignIn)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.authorized_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.run { customSignIn = getBoolean(CUSTOM_SIGN_IN, false) }

        activity?.let {
            sessionClient = (it as MainActivity).getSession()
            sessionClient?.let {
                get_profile.setOnClickListener(this)
                check_expired.setOnClickListener(this)
                sign_out_okta.setOnClickListener(this)
                clear_data.setOnClickListener(this)
                manage_tokens.setOnClickListener(this)
                if (customSignIn) {
                    sign_out_okta.visibility = View.GONE
                }
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
            get_profile.id -> getProfile()
            check_expired.id -> checkExpired()
            sign_out_okta.id -> signOutOfOkta()
            clear_data.id -> clearData()
            manage_tokens.id -> manageTokens()
        }
    }

    private fun manageTokens() {
        requireFragmentManager().beginTransaction().addToBackStack(null)
            .replace(R.id.fragment, ManageTokensFragment.newInstance())
            .commit()
    }

    private fun clearData() {
        sessionClient?.run {
            clear()
            Snackbar.make(info_view, getString(R.string.data_cleared), Snackbar.LENGTH_SHORT).show()
            requireFragmentManager().beginTransaction().replace(R.id.fragment,
                SignInFragment.newInstance(customSignIn)
            )
                .commit()
        }
    }

    private fun signOutOfOkta() {
        activity?.let {
            (it as MainActivity).signOut()
        }
    }

    private fun getProfile() {
        sessionClient?.run {
            networkCallInProgress()
            getUserProfile(object : RequestCallback<UserInfo, AuthorizationException> {
                override fun onSuccess(user: UserInfo) {
                    activity?.network_progress?.hide()
                    info_view.text = user.toString()
                    Snackbar.make(info_view, getString(R.string.success), Snackbar.LENGTH_SHORT).show()
                }

                override fun onError(error: String?, exception: AuthorizationException) {
                    activity?.network_progress?.hide()
                    Log.d(TAG, error, exception)
                    Snackbar.make(info_view, getString(R.string.error), Snackbar.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun checkExpired() {
        sessionClient?.run {
            Snackbar.make(
                info_view,
                getString(R.string.token_expired) + tokens.isAccessTokenExpired,
                Snackbar.LENGTH_SHORT
            ).show()
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
