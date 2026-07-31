package dev.helm.sdk

import kotlinx.coroutines.flow.Flow

interface McuDataSource {
    fun events(): Flow<McuEvent>
    suspend fun send(code: Int, arg1: Int, arg2: Int = 0, data: ByteArray = ByteArray(0)): Result<Unit>
}
