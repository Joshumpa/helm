package dev.helm.sdk

import android.content.Context
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

// Singleton holder for McuService. Call init() once from Application.onCreate().
// Chooses TwUtilMcuDataSource when running as system UID; StubMcuDataSource otherwise.
object McuServiceLocator {

    @Volatile private var _service: McuService? = null

    val service: McuService
        get() = checkNotNull(_service) { "McuServiceLocator.init() not called" }

    val isInitialized: Boolean get() = _service != null

    @Synchronized
    fun init(context: Context) {
        if (_service != null) return
        val scope = CoroutineScope(SupervisorJob())
        val source: McuDataSource = if (Process.myUid() == Process.SYSTEM_UID) {
            TwUtilMcuDataSource()
        } else {
            StubMcuDataSource()
        }
        _service = McuService(source, context.applicationContext, scope)
    }
}
