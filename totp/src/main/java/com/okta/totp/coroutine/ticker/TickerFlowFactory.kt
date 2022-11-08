package com.okta.totp.coroutine.ticker

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

fun interface TickerFlowFactory {
    fun getTickerFlow(period: Duration, initialDelay: Duration): Flow<Unit>
}
