package com.okta.browser.fragments

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.okta.browser.MainActivity

import com.okta.browser.R
import com.okta.oidc.RequestCallback
import com.okta.oidc.clients.sessions.SessionClient
import com.okta.oidc.net.response.UserInfo
import com.okta.oidc.util.AuthorizationException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.authorized_fragment.*

class AuthorizedFragment : Fragment(), View.OnClickListener {
    private val LOG_TAG: String = "AuthorizedFragment"
    private var sessionClient: SessionClient? = null
    private var customSignIn: Boolean = false

    companion object {
        private const val CUSTOM_SIGN_IN = "custom_sign_in"
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
        savedInstanceState?.run { customSignIn = getBoolean(CUSTOM_SIGN_IN, false) }

        activity?.let {
            sessionClient = (it as MainActivity).getSession()
            sessionClient?.let {
                get_profile.setOnClickListener(this)
                check_expired.setOnClickListener(this)
                sign_out_okta.setOnClickListener(this)
                clear_data.setOnClickListener(this)
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
        }
    }

    private fun clearData() {
        sessionClient?.run {
            clear()
            Snackbar.make(info_view, getString(R.string.data_cleared), Snackbar.LENGTH_SHORT).show()
            requireFragmentManager().beginTransaction().replace(R.id.fragment, SignInFragment.newInstance(customSignIn))
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
                    info_view.text = user.toString()
                    Snackbar.make(info_view, getString(R.string.success), Snackbar.LENGTH_SHORT).show()
                }

                override fun onError(error: String, exception: AuthorizationException) {
                    Log.d(LOG_TAG, error, exception)
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
        network_progress?.show()
        Snackbar.make(info_view, getString(R.string.loading), Snackbar.LENGTH_INDEFINITE)
            .let { bar ->
                bar.setAction(getString(R.string.cancel)) {
                    sessionClient?.cancel()
                    bar.dismiss()
                }
            }.show()
    }
}
