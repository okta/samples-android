package com.okta.oidc.fragments

import androidx.biometric.BiometricPrompt
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.okta.oidc.MainActivity

import com.okta.oidc.R
import kotlinx.android.synthetic.main.fragment_biometric.*
import kotlinx.android.synthetic.main.fragment_biometric.clear_data
import java.util.concurrent.Executors
import kotlin.properties.Delegates

class BiometricFragment : Fragment(), View.OnClickListener {
    private var viewModel: SharedViewModel by Delegates.notNull()

    companion object {
        @JvmStatic
        fun newInstance() = BiometricFragment()
    }
    
    override fun onResume() {
        super.onResume()
        activity?.let {
            (it as MainActivity).supportActionBar?.hide()
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            (it as MainActivity).supportActionBar?.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_biometric, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.run {
            viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        }
        retry.setOnClickListener(this)
        clear_data.setOnClickListener(this)
        exit.setOnClickListener(this)
        promptCredentials()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            retry.id -> promptCredentials()
            clear_data.id -> activity?.let {
                (it as MainActivity).getSession()?.clear()
                Snackbar.make(clear_data, getString(R.string.data_cleared), Snackbar.LENGTH_SHORT)
                    .show()
            }
            exit.id -> activity?.finish()
        }
    }

    private fun promptCredentials() {
        BiometricPrompt(this, Executors.newSingleThreadExecutor(), object :
            AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                activity?.runOnUiThread {
                    status.text = getString(R.string.error_status, errorCode, errString)
                    buttons?.visibility = View.VISIBLE
                    viewModel.deviceAuthenticated.postValue(false)
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.deviceAuthenticated.postValue(true)
            }

            override fun onAuthenticationFailed() {
                activity?.runOnUiThread {
                    buttons?.visibility = View.VISIBLE
                    viewModel.deviceAuthenticated.postValue(false)
                }
            }
        }).authenticate(
            PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setDeviceCredentialAllowed(true)
                .setConfirmationRequired(true)
                .build()
        )
    }
}
