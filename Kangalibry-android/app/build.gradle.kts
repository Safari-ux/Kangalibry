import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Credenciales de firma: se leen de keystore.properties (en la raíz, fuera de Git) o de variables de entorno.
// Nunca se escriben contraseñas en este archivo.
val keystoreProperties = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

fun credencialFirma(propiedad: String, variableEntorno: String): String? =
    (keystoreProperties.getProperty(propiedad) ?: System.getenv(variableEntorno))
        ?.takeIf { it.isNotBlank() }

val rutaKeystore = credencialFirma("storeFile", "TURKIDS_STORE_FILE")
val claveKeystore = credencialFirma("storePassword", "TURKIDS_STORE_PASSWORD")
val aliasLlave = credencialFirma("keyAlias", "TURKIDS_KEY_ALIAS")
val claveLlave = credencialFirma("keyPassword", "TURKIDS_KEY_PASSWORD")
val firmaConfigurada = listOf(rutaKeystore, claveKeystore, aliasLlave, claveLlave).all { it != null }

android {
    namespace = "com.example.turkids"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.turkids"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (firmaConfigurada) {
                storeFile = rootProject.file(rutaKeystore!!)
                storePassword = claveKeystore
                keyAlias = aliasLlave
                keyPassword = claveLlave
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // Sin credenciales (por ejemplo en otro equipo) el release se genera sin firmar en vez de fallar
            if (firmaConfigurada) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size.class1)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
