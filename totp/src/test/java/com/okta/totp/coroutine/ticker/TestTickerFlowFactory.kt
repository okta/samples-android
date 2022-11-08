package com.okta.totp.coroutine.ticker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlin.time.Duration

internal class TestTickerFlowFactory(
    private val maxUpdates: Int,
) : TickerFlowFactory {

    override fun getTickerFlow(period: Duration, initialDelay: Duration): Flow<Unit> {
        if (maxUpdates == 0) return emptyFlow()
        return TickerFlowFactoryImpl().getTickerFlow(period, initialDelay).take(maxUpdates)
    }
}
