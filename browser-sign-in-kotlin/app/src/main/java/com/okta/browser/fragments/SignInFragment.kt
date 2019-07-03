package com.okta.browser.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders

import com.okta.browser.R
import kotlinx.android.synthetic.main.sign_in_fragment.*
import kotlin.properties.Delegates.notNull

class SignInFragment : Fragment() {

    private var viewModel: SharedViewModel by notNull()

    companion object {
        private const val CUSTOM_SIGN_IN = "custom_sign_in"
        fun newInstance(customSignIn: Boolean) = SignInFragment().apply {
            arguments = Bundle().apply {
                putBoolean(CUSTOM_SIGN_IN, customSignIn)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sign_in_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.run {
            viewModel = ViewModelProviders.of(this).get(SharedViewModel::class.java)
        }

        savedInstanceState?.run {
            if (getBoolean(CUSTOM_SIGN_IN, false)) {
                initializeCustomAction()
            } else {
                initializeBrowserAction()
            }
        } ?: initializeBrowserAction()
    }

    private fun initializeBrowserAction() {
        username.hint = activity?.getString(R.string.prompt_hint)
        pass_layout.visibility = View.GONE
        sign_in_button.setOnClickListener {
            viewModel.hint.postValue(username.text?.toString())
        }
    }

    private fun initializeCustomAction() {
        username.hint = activity?.getString(R.string.prompt_username)
        pass_layout.visibility = View.VISIBLE
        sign_in_button.setOnClickListener {
            viewModel.userAndPassword.postValue(Pair(username.text.toString(), password.text.toString()))
        }
    }
}
