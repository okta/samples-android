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

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.okta.android.samples.browser_sign_in.databinding.ActivityBrowserSignInBinding

class BrowserSignInActivity : AppCompatActivity() {
    private val viewModel: BrowserSignInViewModel by viewModels()
    private lateinit var binding: ActivityBrowserSignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /**
         * Update the user interface for changes in the sign-in flow.
         *
         * Use an observer to react to updates in [UserSignInState]. Updates are asynchronous and are triggered both by user actions,
         * such as button clicks, and completing the flow.
         */
        viewModel.userGreetingState.observe(this) { state ->
            when (state) {
                is UserSignInState.Loaded -> {
                    with(binding) {
                        userGreetingProgressBar.visibility = View.GONE
                        userGreetingTextView.visibility = View.VISIBLE

                        primaryButton.isEnabled = true
                        primaryButton.setBackgroundColor(getColor(PRIMARY_BUTTON_COLOR))
                        userGreetingTextView.text = state.greetingText
                        setButtonModel(primaryButton, state.buttonModel)
                    }
                }
                is UserSignInState.Loading -> {
                    with(binding) {
                        userGreetingProgressBar.visibility = View.VISIBLE
                        primaryButton.isEnabled = false
                        primaryButton.setBackgroundColor(getColor(DISABLED_BUTTON_COLOR))
                        userGreetingTextView.visibility = View.GONE
                    }
                }
            }
        }

        /**
         * Update the user interface for changes in the sign-in flow.
         *
         * Use an observer to react to updates in [UserInfoState]. Updates are asynchronous and are triggered both by user actions,
         * such as button clicks, and completing the flow.
         */
        viewModel.userInfoState.observe(this) { state ->
            when (state) {
                is UserInfoState.Empty -> {
                    with(binding) {
                        userInfoProgressBar.visibility = View.GONE
                        userInfoTextView.visibility = View.GONE
                        secondaryButton.isVisible = state.buttonModel != null

                        secondaryButton.setBackgroundColor(getColor(SECONDARY_BUTTON_COLOR))
                        state.buttonModel?.let { setButtonModel(secondaryButton, state.buttonModel) }
                    }
                }
                is UserInfoState.Loaded -> {
                    with(binding) {
                        userInfoProgressBar.visibility = View.GONE
                        userInfoTextView.visibility = View.VISIBLE
                        secondaryButton.visibility = View.VISIBLE

                        userInfoTextView.text = state.userInfoText
                        setButtonModel(secondaryButton, state.buttonModel)
                        secondaryButton.setBackgroundColor(getColor(SECONDARY_BUTTON_COLOR))
                    }
                }
                is UserInfoState.Loading -> {
                    with(binding) {
                        userInfoTextView.visibility = View.GONE
                        userInfoProgressBar.visibility = View.VISIBLE

                        secondaryButton.visibility = View.VISIBLE
                        secondaryButton.isEnabled = false
                        secondaryButton.setBackgroundColor(getColor(DISABLED_BUTTON_COLOR))
                    }
                }
            }
        }
    }

    private fun setButtonModel(button: Button, buttonModel: ButtonModel) {
        button.isEnabled = true
        button.text = buttonModel.actionText
        button.setOnClickListener {
            buttonModel.buttonAction(this)
        }
    }

    companion object {
        private const val DISABLED_BUTTON_COLOR = R.color.gray
        private const val PRIMARY_BUTTON_COLOR = R.color.purple_700
        private const val SECONDARY_BUTTON_COLOR = R.color.purple_500
    }
}
