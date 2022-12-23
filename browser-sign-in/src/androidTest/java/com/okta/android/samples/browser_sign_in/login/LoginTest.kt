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
package com.okta.android.samples.browser_sign_in.login

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.okta.android.samples.browser_sign_in.BrowserSignInActivity
import com.okta.android.samples.browser_sign_in.test.UserRule
import com.okta.android.samples.browser_sign_in.web.WebPage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LoginTest {
    @get:Rule
    val activityRule = activityScenarioRule<BrowserSignInActivity>()

    @get:Rule
    val userRule = UserRule()

    @Before
    fun clearWebData() {
        WebPage.clearData()
    }

    @Test
    fun testLogin() {
        LoginPage().login()
            .username(userRule.email)
            .password(userRule.password)
            .login()
            .assertUserGreetingWithNameAndEmail(
                name = "${userRule.firstName} ${userRule.lastName}",
                email = userRule.email
            )
    }

    @Test
    fun testLoginError() {
        LoginPage().login()
            .username(userRule.email)
            .password("Invalid")
            .loginExpectingError()
            .assertHasError("Unable to sign in")
            .cancel()
            .assertHasError("Failed to login.")
    }

    @Test
    fun testLoginCancellation() {
        LoginPage().login()
            .cancel()
            .assertHasError("Failed to login.")
    }
}
