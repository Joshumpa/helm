package dev.helm.launcher.media

import android.app.Notification
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HelmNotificationListener : NotificationListenerService() {

    companion object {
        private val _mediaToken = MutableStateFlow<MediaSession.Token?>(null)
        val mediaToken: StateFlow<MediaSession.Token?> = _mediaToken.asStateFlow()
    }

    override fun onListenerConnected() {
        activeNotifications?.forEach { sbn -> extractToken(sbn)?.let { _mediaToken.value = it } }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        extractToken(sbn)?.let { _mediaToken.value = it }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (extractToken(sbn) != null) _mediaToken.value = null
    }

    private fun extractToken(sbn: StatusBarNotification): MediaSession.Token? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sbn.notification.extras.getParcelable(
                Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            sbn.notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        }
}
