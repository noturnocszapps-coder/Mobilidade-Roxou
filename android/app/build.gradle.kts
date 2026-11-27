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
        versionName = "0.3.$buildNumber"
    }

    signingConfigs {
        // Chave de debug fixa, guardada no repositório. Sem ela, cada build do
        // GitHub Actions teria uma assinatura diferente e o celular recusaria a
        // atualização ("app não instalado"). Serve só para debug, nunca para a Play Store.
        // Versão de venda: chave própria, guardada só nos Secrets do GitHub (nunca no repositório).
        create("venda") {
            System.getenv("ROXOU_KEYSTORE_ARQUIVO")?.let { storeFile = file(it) }
            storePassword = System.getenv("ROXOU_KEYSTORE_SENHA")
            keyAlias = System.getenv("ROXOU_KEY_ALIAS")
            keyPassword = System.getenv("ROXOU_KEY_SENHA")
        }
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
            signingConfig = signingConfigs.getByName("venda")
            // Código encolhido e embaralhado (R8): bem mais difícil de ler e alterar.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // APK de testador: igual ao de venda (R8), assinado com a chave de teste e ativado por código.
        create("avaliacao") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
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
    implementation(project(":overlay"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material.icons)
    testImplementation(libs.junit)
}

// A versão de venda nunca sai sem licença de verdade: sem a chave pública, o build falha
// (assim não existe "tudo liberado" no APK de venda).
gradle.taskGraph.whenReady {
    val venda = allTasks.any { it.project == project && it.name.contains("Release") && it.name.startsWith("assemble") }
    if (venda) {
        val faltando = listOf("ROXOU_LICENCA_CHAVE_PUBLICA", "ROXOU_API_URL", "ROXOU_CERT_SHA256", "ROXOU_KEYSTORE_ARQUIVO")
            .filter { System.getenv(it).isNullOrBlank() }
        if (faltando.isNotEmpty()) throw GradleException("Versão de venda sem: ${faltando.joinToString()}")
    }
}
