package dev.helm.sdk

import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Track B only. Reflective wrapper over android.tw.john.TWUtil (platform @hide class,
// requires android.uid.system). Assumes TWUtil(Handler) constructor — verified post-FEL.
internal class TwUtilAdapter {

    companion object {
        private const val TWUTIL_CLASS = "android.tw.john.TWUtil"

        // Full subscription list from docs/mcu-protocol.md
        private val CODES = shortArrayOf(
            258, 260, 516, 517, 528, 262, 263, 266, 274,
            513, 514, 515, 523, 524,
            769, 770, 772, 1028,
            1285, 1286, 1287, 1288, 1289, 1304,
            (-25088).toShort(), (-25071).toShort(), (-24816).toShort(),
            (-24805).toShort(), (-24804).toShort(),
        )
    }

    fun events(): Flow<McuEvent> = callbackFlow {
        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                trySend(parseMessage(msg))
            }
        }

        val clazz = Class.forName(TWUTIL_CLASS)
        val ctor = clazz.getConstructor(Handler::class.java)
        val twUtil = ctor.newInstance(handler)
        val openMethod = clazz.getMethod("open", ShortArray::class.java)
        val startMethod = clazz.getMethod("start")
        val stopMethod = runCatching { clazz.getMethod("stop") }.getOrNull()

        openMethod.invoke(twUtil, CODES)
        startMethod.invoke(twUtil)

        awaitClose {
            runCatching { stopMethod?.invoke(twUtil) }
        }
    }

    suspend fun send(code: Int, arg1: Int, arg2: Int, data: ByteArray): Result<Unit> = runCatching {
        val clazz = Class.forName(TWUTIL_CLASS)
        // TWUtil.write(int code, int arg1, int arg2, byte[] data) — confirmed from decompile
        val writeMethod = clazz.getMethod(
            "write", Int::class.java, Int::class.java, Int::class.java, ByteArray::class.java
        )
        writeMethod.invoke(null, code, arg1, arg2, data)
        Unit
    }

    private fun parseMessage(msg: Message): McuEvent = when (msg.what) {
        0x0203 -> parseSteeringVolume(msg)
        0x0106 -> parseMcuVolumeLevels(msg.obj)
        0x0505 -> parseDoorState(msg.obj)
        0x0506 -> parseCameraResolution(msg.obj)
        0x0507 -> parseParkingRadar(msg.obj)
        0x0508 -> parseAmbientTemp(msg.obj)
        0x0518 -> parseRtcSync(msg.obj)
        0x010A -> parseMcuVersion(msg.obj)
        else   -> parseSimple(msg)
    }

    @Suppress("MagicNumber")
    private fun parseSimple(msg: Message): McuEvent = when (msg.what) {
        0x0204  -> McuEvent.DayNightChanged(msg.arg1 == 1)
        0x0205  -> McuEvent.CarPlaySessionChanged(msg.arg1 != 0)
        0x0301  -> McuEvent.AudioSourceChanged(msg.arg2)
        0x0302  -> McuEvent.PhoneCallChanged(msg.arg1 != 0)
        0x020B  -> McuEvent.ReverseGearChanged(msg.arg1 == 1)
        -24804  -> McuEvent.CameraStreamChanged(msg.arg1 == 1)   // 0x9F1C
        0x0202  -> McuEvent.Heartbeat
        -24816  -> McuEvent.PowerOffPending(msg.arg2)             // 0x9F10
        -24805  -> McuEvent.HardwareMuteChanged(msg.arg1 != 0)    // 0x9F1B
        0x0210  -> McuEvent.ScreenRotationChanged(msg.arg1)
        else    -> McuEvent.Raw(msg.what, msg.arg1, msg.arg2)
    }

    @Suppress("MagicNumber")
    private fun parseSteeringVolume(msg: Message): McuEvent {
        val level = msg.arg1 and 0x7FFFFFFF
        val muted = (msg.arg1 ushr 31) != 0
        val maxLevel = msg.arg2 and 0x7FFFFFFF
        return McuEvent.SteeringVolumeChanged(level, maxLevel, muted)
    }

    private fun parseMcuVolumeLevels(obj: Any?): McuEvent {
        val a = objAsIntArray(obj)
        return McuEvent.McuVolumeLevels(a.getOrElse(0) { 0 }, a.getOrElse(1) { 0 })
    }

    @Suppress("MagicNumber")
    private fun parseDoorState(obj: Any?): McuEvent {
        val bits = objAsIntArray(obj).getOrElse(0) { 0 } and 0xFF
        return McuEvent.DoorStateChanged(
            DoorState(
                driverOpen    = (bits and 0x40) != 0,
                passengerOpen = (bits and 0x80) != 0,
                rearLeftOpen  = (bits and 0x20) != 0,
                rearRightOpen = (bits and 0x10) != 0,
                trunkOpen     = (bits and 0x08) != 0,
            )
        )
    }

    @Suppress("MagicNumber")
    private fun parseCameraResolution(obj: Any?): McuEvent {
        val a = objAsIntArray(obj)
        val w = (a.getOrElse(0) { 0 } and 0xFF) or ((a.getOrElse(1) { 0 } and 0xFF) shl 8)
        val h = (a.getOrElse(2) { 0 } and 0xFF) or ((a.getOrElse(3) { 0 } and 0xFF) shl 8)
        return McuEvent.CameraResolutionChanged(w, h)
    }

    @Suppress("MagicNumber")
    private fun parseParkingRadar(obj: Any?): McuEvent {
        val a = objAsIntArray(obj)
        return McuEvent.ParkingRadarChanged(
            front    = (0..3).map { a.getOrElse(it) { 0 } and 0xFF },
            frontMax = a.getOrElse(4) { 0 } and 0xFF,
            rear     = (5..8).map { a.getOrElse(it) { 0 } and 0xFF },
            rearMax  = a.getOrElse(9) { 0 } and 0xFF,
        )
    }

    @Suppress("MagicNumber")
    private fun parseAmbientTemp(obj: Any?): McuEvent {
        val raw = objAsIntArray(obj).getOrElse(1) { 127 } and 0xFF
        return McuEvent.AmbientTempChanged(if (raw == 127) null else raw)
    }

    @Suppress("MagicNumber")
    private fun parseRtcSync(obj: Any?): McuEvent {
        val a = objAsIntArray(obj)
        return McuEvent.RtcTimeSyncReceived(
            hour   = a.getOrElse(0) { 0 },
            minute = a.getOrElse(1) { 0 },
            second = a.getOrElse(2) { 0 },
            year   = a.getOrElse(3) { 0 } + 2000,
            month  = a.getOrElse(4) { 0 },
            day    = a.getOrElse(5) { 0 },
        )
    }

    private fun parseMcuVersion(obj: Any?): McuEvent {
        val version = when (obj) {
            is ByteArray -> String(obj, Charsets.US_ASCII).trimEnd(' ')
            is String    -> obj
            else         -> obj?.toString().orEmpty()
        }
        return McuEvent.McuVersionReceived(version)
    }

    private fun objAsIntArray(obj: Any?): IntArray = when (obj) {
        is IntArray  -> obj
        is ByteArray -> IntArray(obj.size) { obj[it].toInt() and 0xFF }
        else         -> IntArray(0)
    }
}
