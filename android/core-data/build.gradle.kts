plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.com.roxou.mobilidade.data"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        // Vêm das variáveis do GitHub (Settings > Secrets and variables > Actions > Variables).
        buildConfigField("String", "API_URL", "\"${System.getenv("ROXOU_API_URL").orEmpty()}\"")
        buildConfigField("String", "CHAVE_PUBLICA", "\"${System.getenv("ROXOU_LICENCA_CHAVE_PUBLICA").orEmpty()}\"")
        buildConfigField("String", "CERT_SHA256", "\"${System.getenv("ROXOU_CERT_SHA256").orEmpty().replace(":", "").lowercase()}\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core-domain"))
    api(project(":core-licenca"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    api(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
