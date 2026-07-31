package dev.helm.camera

import android.content.Context
import android.content.Intent
import dev.helm.sdk.McuService
import dev.helm.sdk.McuServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Task 6.1/6.2 — observes MCU gear and stream signals, launches/dismisses ReverseCameraActivity.
class ReverseCameraManager(
    private val context: Context,
    private val service: McuService,
    private val scope: CoroutineScope,
) {
    fun start() {
        service.reverseGear
            .onEach { engaged ->
                if (engaged) {
                    context.startActivity(
                        Intent(context, ReverseCameraActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                } else {
                    ReverseCameraActivity.requestDismiss()
                }
            }
            .launchIn(scope)
    }

    companion object {
        fun startIfReady(context: Context, scope: CoroutineScope) {
            if (!McuServiceLocator.isInitialized) return
            ReverseCameraManager(context, McuServiceLocator.service, scope).start()
        }
    }
}
