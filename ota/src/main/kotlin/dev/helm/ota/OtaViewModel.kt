package dev.helm.ota

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class OtaViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = OtaRepository()
    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    val state: StateFlow<OtaState> = _state.asStateFlow()

    fun checkForUpdate() {
        if (_state.value is OtaState.Checking) return
        _state.value = OtaState.Checking
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val latest = repo.fetchLatest()
                if (latest == null) {
                    _state.value = OtaState.Error("Sin respuesta del servidor")
                    return@launch
                }
                val currentName = getApplication<Application>().packageManager
                    .getPackageInfo(getApplication<Application>().packageName, 0)
                    .versionName.orEmpty()
                if (isNewer(latest.versionName, currentName)) {
                    _state.value = OtaState.UpdateAvailable(latest)
                } else {
                    _state.value = OtaState.UpToDate
                }
            }.onFailure { e ->
                _state.value = OtaState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun downloadUpdate(info: OtaInfo) {
        _state.value = OtaState.Downloading(0f)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val dir = getApplication<Application>().filesDir.resolve("ota").also { it.mkdirs() }
                val dest = File(dir, "helm-update.apk")
                repo.download(info.apkUrl, dest) { progress ->
                    _state.value = OtaState.Downloading(progress)
                }
                _state.value = OtaState.ReadyToInstall(dest.absolutePath)
            }.onFailure { e ->
                _state.value = OtaState.Error(e.message ?: "Error al descargar")
            }
        }
    }

    fun install(context: Context, apkPath: String) {
        repo.install(context, File(apkPath))
    }

    fun reset() {
        _state.value = OtaState.Idle
    }

    private fun isNewer(server: String, installed: String): Boolean {
        val s = server.split('.').map { it.toIntOrNull() ?: 0 }
        val c = installed.split('.').map { it.toIntOrNull() ?: 0 }
        val len = maxOf(s.size, c.size)
        for (i in 0 until len) {
            val sv = s.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (sv > cv) return true
            if (sv < cv) return false
        }
        return false
    }
}
