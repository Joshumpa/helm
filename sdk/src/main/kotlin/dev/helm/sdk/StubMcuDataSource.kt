package dev.helm.sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StubMcuDataSource : McuDataSource {
    override fun events(): Flow<McuEvent> = flow {
        emit(McuEvent.DayNightChanged(isNight = false))
        emit(McuEvent.AudioSourceChanged(sourceId = AudioSourceId.IDLE))
    }

    override suspend fun send(code: Int, arg1: Int, arg2: Int, data: ByteArray): Result<Unit> =
        Result.success(Unit)
}
