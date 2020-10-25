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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.okta.sample.R
import kotlinx.android.synthetic.main.sign_in_fragment.*
import kotlin.properties.Delegates.notNull

class SignInFragment : Fragment() {

    private var viewModel: SharedViewModel by notNull()

    companion object {
        private const val CUSTOM_SIGN_IN = "custom_sign_in"
        @JvmStatic
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
            viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        }

        arguments?.run {
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
            viewModel.hint.postValue(username.text?.toString() ?: "")
        }
        username.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sign_in_button.callOnClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun initializeCustomAction() {
        username.hint = activity?.getString(R.string.prompt_username)
        pass_layout.visibility = View.VISIBLE
        sign_in_button.setOnClickListener {
            viewModel.userAndPassword.postValue(Pair(username.text?.toString() ?: "", password.text?.toString() ?: ""))
        }
        username.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                password.requestFocus()
                return@OnEditorActionListener true
            }
            false
        })
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sign_in_button.callOnClick()
                return@OnEditorActionListener true
            }
            false
        })
    }
}
