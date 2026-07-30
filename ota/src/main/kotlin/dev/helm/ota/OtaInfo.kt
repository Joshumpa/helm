package dev.helm.ota

data class OtaInfo(
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
)
