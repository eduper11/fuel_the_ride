import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use { load(it) }
    }
}

fun releaseCredential(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(name).orEmpty()

val releaseKeystoreFile = releaseCredential("KEYSTORE_FILE")
val releaseKeystorePassword = releaseCredential("KEYSTORE_PASSWORD")
val releaseKeyAlias = releaseCredential("KEY_ALIAS")
val releaseKeyPassword = releaseCredential("KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isNotBlank() }

val fuelTheRideVersionName = providers.gradleProperty("fueltheride.versionName").orNull
    ?: error("Set fueltheride.versionName in gradle.properties, for example 1.0.0")
val fuelTheRideVersionCode = providers.gradleProperty("fueltheride.versionCode").orNull?.toIntOrNull()
    ?: error("Set fueltheride.versionCode in gradle.properties to a positive integer")

require(fuelTheRideVersionName.matches(Regex("""\d+\.\d+\.\d+"""))) {
    "fueltheride.versionName must be major.minor.patch, for example 1.0.1"
}
require(fuelTheRideVersionCode > 0) {
    "fueltheride.versionCode must be greater than 0"
}

android {
    namespace = "com.fueltheride"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fueltheride"
        minSdk = 23
        targetSdk = 34
        versionCode = fuelTheRideVersionCode
        versionName = fuelTheRideVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreFile)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.karoo.ext)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.service)
}
