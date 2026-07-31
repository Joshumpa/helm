package dev.helm.launcher

import android.app.Application
import dev.helm.camera.ReverseCameraManager
import dev.helm.launcher.mcu.McuDayNightObserver
import dev.helm.sdk.McuAudioRouter
import dev.helm.sdk.McuServiceLocator
import kotlinx.coroutines.MainScope

class HelmApplication : Application() {

    private val appScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        McuServiceLocator.init(this)
        McuDayNightObserver.start(this, appScope)
        McuAudioRouter(this, McuServiceLocator.service, appScope).start()
        ReverseCameraManager.startIfReady(this, appScope)
    }
}
