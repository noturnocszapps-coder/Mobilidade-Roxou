plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// No GitHub Actions, cada execução ganha um número maior (GITHUB_RUN_NUMBER),
// para o Android aceitar o APK novo como atualização do anterior.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "br.com.roxou.mobilidade"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.roxou.mobilidade"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.2.$buildNumber"
    }

    signingConfigs {
        // Chave de debug fixa, guardada no repositório. Sem ela, cada build do
        // GitHub Actions teria uma assinatura diferente e o celular recusaria a
        // atualização ("app não instalado"). Serve só para debug, nunca para a Play Store.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-data"))
    implementation(project(":core-ui"))
    implementation(project(":feature-trips"))
    implementation(project(":feature-goals"))
    implementation(project(":feature-rental"))
    implementation(project(":feature-finance"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material.icons)
    testImplementation(libs.junit)
}
