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
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.helm.core.Spacing
import dev.helm.core.neumorphicClickable
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

    private val _bondingAddress = MutableStateFlow<String?>(null)
    val bondingAddress: StateFlow<String?> = _bondingAddress.asStateFlow()

    private var a2dpProxy: BluetoothProfile? = null

    private val receiver = object : BroadcastReceiver() {
        @Suppress("DEPRECATION") // getParcelableExtra(String) deprecated at API 33; minSdk is 29
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
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return
                    when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)) {
                        BluetoothDevice.BOND_BONDING -> _bondingAddress.value = device.address
                        BluetoothDevice.BOND_BONDED -> { _bondingAddress.value = null; loadPaired() }
                        BluetoothDevice.BOND_NONE -> _bondingAddress.value = null
                    }
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
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
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
        if (!hasBluetoothScanPermission()) {
            Log.w("Bluetooth", "startScan: BLUETOOTH_SCAN permission not granted")
            return
        }
        _discovered.value = emptyList()
        adapter?.startDiscovery()
    }

    private fun hasBluetoothScanPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            android.Manifest.permission.BLUETOOTH_SCAN
        else
            android.Manifest.permission.BLUETOOTH
        return getApplication<Application>()
            .checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
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
        } catch (e: ReflectiveOperationException) {
            android.util.Log.e("Bluetooth", "A2DP connect failed: ${e::class.simpleName}")
        }
    }

    fun disconnect(device: BtDevice) {
        val bd = adapter?.getRemoteDevice(device.address) ?: return
        try {
            a2dpProxy?.javaClass?.getMethod("disconnect", BluetoothDevice::class.java)?.invoke(a2dpProxy, bd)
        } catch (e: ReflectiveOperationException) {
            android.util.Log.e("Bluetooth", "A2DP disconnect failed: ${e::class.simpleName}")
        }
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
    val bondingAddress by vm.bondingAddress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 72.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .neumorphicClickable(onClick = onBack, cornerRadius = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xxl),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Filled.BluetoothDisabled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "Bluetooth desactivado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Column
        }

        if (paired.isNotEmpty()) {
            SectionHeader("Dispositivos emparejados")
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                paired.forEach { device ->
                    DeviceRow(
                        device = device,
                        isConnected = device.address == connectedAddress,
                        isBonding = device.address == bondingAddress,
                        onConnect = { vm.connect(device) },
                        onDisconnect = { vm.disconnect(device) },
                    )
                }
            }
        }

        ScanButton(
            scanning = scanning,
            onToggle = { if (scanning) vm.stopScan() else vm.startScan() },
        )

        if (discovered.isNotEmpty()) {
            SectionHeader("Disponibles")
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                discovered.forEach { device ->
                    DeviceRow(
                        device = device,
                        isConnected = false,
                        isBonding = device.address == bondingAddress,
                        onConnect = { vm.connect(device) },
                        onDisconnect = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Spacing.xs),
    )
}

@Composable
private fun ScanButton(scanning: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .neumorphicClickable(
                onClick = onToggle,
                cornerRadius = 20.dp,
                showAccentBorder = scanning,
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (scanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = if (scanning) "Buscando…" else "Buscar dispositivos",
            style = MaterialTheme.typography.titleMedium,
            color = if (scanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun DeviceRow(
    device: BtDevice,
    isConnected: Boolean,
    isBonding: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .neumorphicClickable(
                onClick = {
                    when {
                        isBonding -> Unit
                        isConnected -> onDisconnect()
                        else -> onConnect()
                    }
                },
                cornerRadius = 16.dp,
                showAccentBorder = isConnected,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isConnected) primary.copy(alpha = 0.15f) else surfaceVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isBonding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) primary else onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                color = onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                isConnected -> Text(
                    text = "Conectado",
                    style = MaterialTheme.typography.labelLarge,
                    color = primary,
                )
                isBonding -> Text(
                    text = "Emparejando…",
                    style = MaterialTheme.typography.labelLarge,
                    color = onSurfaceVariant,
                )
                device.bonded -> Text(
                    text = "Emparejado",
                    style = MaterialTheme.typography.labelLarge,
                    color = onSurfaceVariant,
                )
            }
        }

        if (isConnected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
