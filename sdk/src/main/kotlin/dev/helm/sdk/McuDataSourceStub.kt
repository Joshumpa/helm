package dev.helm.sdk

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// No-op implementation used during development and on non-system builds
class McuDataSourceStub : McuDataSource {
    override val speed: StateFlow<Int> = MutableStateFlow(0)
    override val adasEvents: SharedFlow<AdasEvent> = MutableSharedFlow()
}
