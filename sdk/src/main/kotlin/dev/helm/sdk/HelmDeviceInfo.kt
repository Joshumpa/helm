package dev.helm.sdk

data class HelmDeviceInfo(
    val soc: String,
    val mcuVersion: String,
    val systemVersion: String,
)
