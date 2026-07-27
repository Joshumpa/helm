plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    source.setFrom(
        fileTree(rootDir) {
            include("**/src/main/kotlin/**/*.kt", "**/src/test/kotlin/**/*.kt")
            exclude("**/build/**")
        }
    )
}
