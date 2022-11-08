package com.okta.totp.coroutine.ticker

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.time.Duration

class TickerFlowFactoryImpl @Inject constructor() : TickerFlowFactory {
    override fun getTickerFlow(period: Duration, initialDelay: Duration): Flow<Unit> = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }
}
