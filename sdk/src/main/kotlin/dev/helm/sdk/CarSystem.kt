package dev.helm.sdk

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object CarSystem {

    // All packages confirmed on-device via pm list packages -f (TermOne Plus, no root)
    fun openRadio(context: Context) = launch(context, "com.tw.radio")

    fun openBluetooth(context: Context) = launch(context, "com.tw.bt")

    // com.zjinnova.zlink is in /data/app — user-installed, not pre-flashed
    fun openCarPlay(context: Context) = launch(context, "com.zjinnova.zlink")

    fun openReverseCamera(context: Context) = launch(context, "com.tw.reverse")

    fun openRightCamera(context: Context) = launch(context, "com.tw.rightview")

    fun openCamera360(context: Context) = launch(context, "cn.cardoor.zt360")

    fun openMusic(context: Context) = launch(context, "com.tw.music")

    fun openVideo(context: Context) = launch(context, "com.tw.video")

    fun openDvr(context: Context) = launch(context, "com.tw.dvr")

    fun openEq(context: Context) = launch(context, "com.tw.eq")

    fun openAux(context: Context) = launch(context, "com.tw.auxin")

    fun openCarSettings(context: Context) = launch(context, "com.dofun.carsetting")

    // URI scheme confirmed from manifest — autoVerify=true on EnterSettingActivity
    fun openSettings(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("launcher://variety/setting")))
        } catch (_: ActivityNotFoundException) {}
    }

    fun triggerScreenSaver(context: Context) {
        context.sendBroadcast(Intent("cn.cardoor.intent.action.DAY_DREAM"))
    }

    fun getSystemInfo(): HelmDeviceInfo = HelmDeviceInfo(
        soc = Build.HARDWARE,
        mcuVersion = "",        // TODO: read from com.tw.uart once IPC mechanism confirmed
        systemVersion = Build.VERSION.RELEASE,
    )

    private fun launch(context: Context, pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        context.startActivity(intent)
    }
}
