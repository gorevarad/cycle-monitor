import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.room)
}

// Google Maps Platform requires an API key. It is never hardcoded here: it is read from
// local.properties (gitignored) so it never ends up in version control. Screens that need the
// map gracefully fall back to a "map unavailable" state when this is blank -- see
// map/MapAvailability.kt. Obtain a key at https://developers.google.com/maps/documentation/android-sdk/get-api-key
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.cyclemonitor.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cyclemonitor.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["mapsApiKey"] = mapsApiKey
        buildConfigField("boolean", "MAPS_CONFIGURED", (mapsApiKey.isNotBlank()).toString())
        // Also exposed as a plain string so app code (Directions API calls) can use the same key
        // the Maps SDK meta-data uses, without re-reading local.properties at runtime.
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildTypes {
        debug {
            // Debug builds may wire the MockLocationProvider behind a Settings toggle for
            // development/demo purposes; see di/AppContainer.kt. This flag lets that code be
            // compiled out of release builds entirely rather than merely hidden in the UI.
            buildConfigField("boolean", "ALLOW_MOCK_LOCATION", "true")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "ALLOW_MOCK_LOCATION", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Using kapt (tied to the Kotlin Gradle plugin version already pinned above) rather than KSP
    // here, since KSP's own version has to be matched to the exact Kotlin version separately and
    // could not be verified against current releases in this environment. KSP is the currently
    // recommended, faster option for Room -- switch to it (`ksp(libs.androidx.room.compiler)`
    // + the KSP plugin) once you've confirmed the KSP release matching your Kotlin version at
    // https://github.com/google/ksp/releases.
    kapt(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
