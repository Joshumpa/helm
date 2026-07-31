plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.helm.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.helm.launcher"
        minSdk = 29
        targetSdk = 35
        versionCode = (findProperty("versionCode") as? String)?.toInt() ?: 1
        versionName = (findProperty("versionName") as? String) ?: "0.1.0"
    }

    val keystorePath = System.getenv("KEYSTORE_PATH")
    if (!keystorePath.isNullOrBlank()) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // No abiFilter — all ABIs supported so the emulator (x86_64) works
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
            ndk {
                abiFilters += "armeabi-v7a"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(project(":sdk"))
    implementation(project(":core"))
    implementation(project(":themes"))
    implementation(project(":widgets"))
    implementation(project(":audio"))
    implementation(project(":bluetooth"))
    implementation(project(":radio"))
    implementation(project(":carplay"))
    implementation(project(":settings"))
    implementation(project(":ota"))
    implementation(project(":camera"))
    implementation(libs.media3.session)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
}
