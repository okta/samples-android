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
package com.okta.android.samples.browser_sign_in.test

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException

fun waitForView(matcher: Matcher<View>, timeout: Long) {
    val viewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "Timeout waiting for view."
        }

        override fun perform(uiController: UiController, rootView: View) {
            uiController.loopMainThreadUntilIdle()
            val endTime = System.currentTimeMillis() + timeout

            val matcherWithVisibility =
                allOf(matcher, withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
            do {
                for (child in TreeIterables.breadthFirstViewTraversal(rootView)) {
                    if (matcherWithVisibility.matches(child)) {
                        return
                    }
                }
                uiController.loopMainThreadForAtLeast(100)
            } while (System.currentTimeMillis() < endTime) // in case of a timeout we throw an exception -&gt; test fails
            throw PerformException.Builder()
                .withCause(TimeoutException())
                .withActionDescription(description)
                .withViewDescription(HumanReadables.describe(rootView))
                .build()
        }
    }

    onView(isRoot()).perform(viewAction)
}
