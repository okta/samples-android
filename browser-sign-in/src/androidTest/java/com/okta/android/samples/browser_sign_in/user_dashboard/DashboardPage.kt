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
package com.okta.android.samples.browser_sign_in.user_dashboard

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.okta.android.samples.browser_sign_in.R
import com.okta.android.samples.browser_sign_in.login.LoginPage
import com.okta.android.samples.browser_sign_in.test.waitForResourceId
import com.okta.android.samples.browser_sign_in.test.waitForView
import org.hamcrest.CoreMatchers.containsString

internal class DashboardPage {
    init {
        waitForResourceId(".*user_dashboard")
    }

    fun logout(): LoginPage {
        onView(withId(R.id.logout_button)).perform(click())
        return LoginPage()
    }

    fun fetchUserInfo(): DashboardPage {
        onView(withId(R.id.fetch_userinfo_button)).perform(click())
        return this
    }

    fun assertUserGreetingWithNameAndEmail(name: String, email: String): DashboardPage {
        val greetingText = "Welcome ${name}\n" + "Email: $email"
        onView(withId(R.id.user_greeting_text_view)).check(matches(withText(greetingText)))
        return this
    }

    fun assertEmptyUserInfo(): DashboardPage {
        onView(withId(R.id.user_info_text_view)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        return this
    }

    fun assertUserInfoContainsNameAndEmail(name: String, email: String): DashboardPage {
        waitForView(withId(R.id.user_info_text_view), 10_000L)
        with(onView(withId(R.id.user_info_text_view))) {
            check(matches(withText(containsString("name: $name"))))
            check(matches(withText(containsString("email: $email"))))
        }
        return this
    }

    fun assertUserInfoContainsExpectedClaims(): DashboardPage {
        waitForView(withId(R.id.user_info_text_view), 10_000L)
        CLAIMS.map { claim ->
            onView(withId(R.id.user_info_text_view)).check(
                matches(withText(containsString("$claim:")))
            )
        }
        return this
    }

    companion object {
        private val CLAIMS = listOf(
            "sub",
            "name",
            "locale",
            "email",
            "preferred_username",
            "given_name",
            "family_name",
            "zoneinfo",
            "updated_at",
            "email_verified"
        )
    }
}
