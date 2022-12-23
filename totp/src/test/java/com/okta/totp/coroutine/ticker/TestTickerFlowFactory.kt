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
package com.okta.totp.coroutine.ticker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlin.time.Duration

internal class TestTickerFlowFactory(
    private val maxUpdates: Int
) : TickerFlowFactory {

    override fun getTickerFlow(period: Duration, initialDelay: Duration): Flow<Unit> {
        if (maxUpdates == 0) return emptyFlow()
        return TickerFlowFactoryImpl().getTickerFlow(period, initialDelay).take(maxUpdates)
    }
}
