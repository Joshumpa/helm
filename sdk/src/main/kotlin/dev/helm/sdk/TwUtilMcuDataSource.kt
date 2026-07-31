package dev.helm.sdk

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

// Track B only — requires android.uid.system. Uses TwUtilAdapter with exponential back-off
// reconnection: 500 ms, 1 s, 2 s, 4 s, 8 s, 16 s, 30 s (max).
internal class TwUtilMcuDataSource : McuDataSource {

    private val adapter = TwUtilAdapter()

    override fun events(): Flow<McuEvent> = adapter.events()
        .retryWhen { _, attempt ->
            delay(minOf(500L shl attempt.toInt().coerceAtMost(6), 30_000L))
            true
        }

    override suspend fun send(code: Int, arg1: Int, arg2: Int, data: ByteArray): Result<Unit> =
        adapter.send(code, arg1, arg2, data)
}
