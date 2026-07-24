package dev.helm.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object CarSystem {

    // Package candidates ordered by preference (from manifest_raw.txt analysis)
    fun openRadio(context: Context) = launchFirstAvailable(
        context,
        "com.tw.radio",
        "android.car.app.radio",
        "com.syu.radio",
    )

    fun openBluetooth(context: Context) = launchFirstAvailable(
        context,
        "com.tw.bt",
        "com.autochips.bluetooth",
        "com.aotochips.bluetooth",
    )

    fun openCarPlay(context: Context) = launchFirstAvailable(
        context,
        "com.tima.carnet.vt",
        "com.zjinnova.zlink",
        "net.easyconn",
    )

    fun openNavigation(context: Context) = launchFirstAvailable(
        context,
        "android.car.app.gps",
        "com.syu.onekeynavi",
    )

    fun openReverseCamera(context: Context) = launchFirstAvailable(
        context,
        "com.autochips.android.backcar",
    )

    fun openMusic(context: Context) = launchFirstAvailable(
        context,
        "com.tw.music",
        "android.car.app.media",
        "com.syu.music",
    )

    fun openVideo(context: Context) = launchFirstAvailable(
        context,
        "com.tw.video",
        "android.car.app.mp4",
        "com.syu.video",
    )

    // URI scheme confirmed from manifest — autoVerify=true on EnterSettingActivity
    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("launcher://variety/setting"))
        )
    }

    fun triggerScreenSaver(context: Context) {
        context.sendBroadcast(Intent("cn.cardoor.intent.action.DAY_DREAM"))
    }

    fun getSystemInfo(): HelmDeviceInfo = HelmDeviceInfo(
        soc = Build.HARDWARE,
        mcuVersion = "",        // TODO: read from MCU bridge once identified
        systemVersion = Build.VERSION.RELEASE,
    )

    private fun launchFirstAvailable(context: Context, vararg packages: String) {
        val pm = context.packageManager
        for (pkg in packages) {
            val intent = pm.getLaunchIntentForPackage(pkg) ?: continue
            context.startActivity(intent)
            return
        }
    }
}
