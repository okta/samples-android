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

import com.okta.sdk.client.Client
import com.okta.sdk.client.Clients
import com.okta.sdk.resource.user.User
import com.okta.sdk.resource.user.UserBuilder
import org.junit.Assert
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.*

internal class UserRule : TestRule {
    private val client: Client = Clients.builder()
        .setClientId(EndToEndCredentials["/managementSdk/clientId"])
        .setOrgUrl(EndToEndCredentials["/managementSdk/orgUrl"])
        .setClientCredentials { EndToEndCredentials["/managementSdk/token"] }
        .build()

    lateinit var email: String
    lateinit var password: String
    val firstName = "Test"
    val lastName = "User"

    override fun apply(base: Statement, description: Description): Statement {
        return UserStatement(this, base, client)
    }
}

private class UserStatement(
    private val rule: UserRule,
    private val base: Statement,
    private val client: Client,
) : Statement() {
    override fun evaluate() {
        val user = createUser()
        try {
            base.evaluate()
        } finally {
            deleteUser(user)
        }
    }

    private fun createUser(): User {
        rule.email = "${UUID.randomUUID()}@oktatest.com"
        rule.password = UUID.randomUUID().toString()
        val user: User = UserBuilder.instance()
            .setEmail(rule.email)
            .setFirstName(rule.firstName)
            .setLastName(rule.lastName)
            .setPassword(rule.password.toCharArray())
            .setActive(true)
            .buildAndCreate(client)
        Assert.assertNotNull(user.id)
        return user
    }

    private fun deleteUser(user: User) {
        user.deactivate()
        user.delete()
    }
}
