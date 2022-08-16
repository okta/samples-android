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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.okta.android.samples.browser_sign_in.R
import com.okta.android.samples.browser_sign_in.test.waitForView
import com.okta.android.samples.browser_sign_in.web.WebPage

internal class LoginPage {
    init {
        waitForView(withId(R.id.login_container), 10_000L)
    }

    fun login(): WebPage<LoginPage> {
        onView(withId(R.id.login_button)).perform(click())
        return WebPage(this)
    }

    fun assertNoErrors(): LoginPage {
        waitForView(withId(R.id.status_text_view), 10_000L)
        onView(withId(R.id.status_text_view)).check(matches(withText("Have an account?")))
        return this
    }

    fun assertHasError(error: String): LoginPage {
        waitForView(withId(R.id.status_text_view), 10_000L)
        onView(withId(R.id.status_text_view)).check(matches(withText(error)))
        return this
    }
}
