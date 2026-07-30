# Bluetooth A2DP reflection — connect/disconnect are @hide, invoked by name at runtime
-keepclassmembers class android.bluetooth.BluetoothA2dp {
    public *** connect(android.bluetooth.BluetoothDevice);
    public *** disconnect(android.bluetooth.BluetoothDevice);
}

# OTA — PackageManager.getPackageArchiveInfo reads class metadata from the downloaded APK
-keep class dev.helm.ota.** { *; }

# SDK public API — feature modules reference these by reflection or dynamic dispatch
-keep class dev.helm.sdk.** { *; }

# Kotlin coroutines internals required for structured concurrency
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
