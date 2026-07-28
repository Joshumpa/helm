package dev.helm.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.helm.core.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BtDevice(val name: String, val address: String, val bonded: Boolean)

@SuppressLint("MissingPermission")
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val adapter: BluetoothAdapter? =
        (application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _enabled = MutableStateFlow(adapter?.isEnabled == true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _paired = MutableStateFlow<List<BtDevice>>(emptyList())
    val paired: StateFlow<List<BtDevice>> = _paired.asStateFlow()

    private val _discovered = MutableStateFlow<List<BtDevice>>(emptyList())
    val discovered: StateFlow<List<BtDevice>> = _discovered.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _connectedAddress = MutableStateFlow<String?>(null)
    val connectedAddress: StateFlow<String?> = _connectedAddress.asStateFlow()

    private var a2dpProxy: BluetoothProfile? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return
                    val item = BtDevice(device.name ?: device.address, device.address, bonded = false)
                    if (_discovered.value.none { it.address == device.address }) {
                        _discovered.value = _discovered.value + item
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> _scanning.value = true
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _scanning.value = false
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _enabled.value = state == BluetoothAdapter.STATE_ON
                    if (_enabled.value) loadPaired()
                }
            }
        }
    }

    init {
        loadPaired()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        application.registerReceiver(receiver, filter)
        adapter?.getProfileProxy(application, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                a2dpProxy = proxy
                _connectedAddress.value = proxy.connectedDevices.firstOrNull()?.address
            }
            override fun onServiceDisconnected(profile: Int) {
                a2dpProxy = null
                _connectedAddress.value = null
            }
        }, BluetoothProfile.A2DP)
    }

    fun loadPaired() {
        _paired.value = adapter?.bondedDevices.orEmpty()
            .map { BtDevice(it.name ?: it.address, it.address, bonded = true) }
    }

    fun startScan() {
        _discovered.value = emptyList()
        adapter?.startDiscovery()
    }

    fun stopScan() {
        adapter?.cancelDiscovery()
    }

    fun connect(device: BtDevice) {
        val bd = adapter?.getRemoteDevice(device.address) ?: return
        if (bd.bondState != BluetoothDevice.BOND_BONDED) {
            bd.createBond()
            return
        }
        try {
            // BluetoothA2dp.connect() is @hide — succeeds post-root with BLUETOOTH_PRIVILEGED
            a2dpProxy?.javaClass?.getMethod("connect", BluetoothDevice::class.java)?.invoke(a2dpProxy, bd)
        } catch (_: Exception) { }
    }

    fun disconnect(device: BtDevice) {
        val bd = adapter?.getRemoteDevice(device.address) ?: return
        try {
            a2dpProxy?.javaClass?.getMethod("disconnect", BluetoothDevice::class.java)?.invoke(a2dpProxy, bd)
        } catch (_: Exception) { }
    }

    override fun onCleared() {
        adapter?.cancelDiscovery()
        adapter?.closeProfileProxy(BluetoothProfile.A2DP, a2dpProxy)
        runCatching { getApplication<Application>().unregisterReceiver(receiver) }
    }
}

@Composable
fun BluetoothScreen(onBack: () -> Unit) {
    val vm: BluetoothViewModel = viewModel()
    val enabled by vm.enabled.collectAsState()
    val paired by vm.paired.collectAsState()
    val discovered by vm.discovered.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val connectedAddress by vm.connectedAddress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(Spacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "Bluetooth",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (enabled) Icons.Filled.Bluetooth else Icons.Filled.BluetoothDisabled,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }

        if (!enabled) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Bluetooth desactivado",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        if (paired.isNotEmpty()) {
            Text(
                text = "Dispositivos emparejados",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                paired.forEach { device ->
                    DeviceRow(
                        device = device,
                        isConnected = device.address == connectedAddress,
                        onConnect = { vm.connect(device) },
                        onDisconnect = { vm.disconnect(device) },
                    )
                }
            }
        }

        Button(
            onClick = { if (scanning) vm.stopScan() else vm.startScan() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (scanning) Icons.Filled.BluetoothSearching else Icons.Filled.Bluetooth,
                contentDescription = null,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(if (scanning) "Detener búsqueda" else "Buscar dispositivos")
        }

        if (discovered.isNotEmpty()) {
            Text(
                text = "Disponibles",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                discovered.forEach { device ->
                    DeviceRow(
                        device = device,
                        isConnected = false,
                        onConnect = { vm.connect(device) },
                        onDisconnect = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: BtDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (isConnected) {
                Text(
                    text = "Conectado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (isConnected) {
            TextButton(onClick = onDisconnect) { Text("Desconectar") }
        } else {
            TextButton(onClick = onConnect) { Text("Conectar") }
        }
    }
}
