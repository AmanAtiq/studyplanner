plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
<<<<<<< HEAD
    alias(libs.plugins.kotlin.compose)          // ← ADD THIS (replaces composeOptions block)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.studyassistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.studyassistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = com.android.build.gradle.internal.cxx.configure.gradleLocalProperties(rootDir, providers)
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"${localProps.getProperty("ANTHROPIC_API_KEY", "")}\"")
=======
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.studyassistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.studyassistant"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
>>>>>>> eb7d1561246a8482446ed58b38fa850a9a0307c9
    }

    buildTypes {
        release {
            isMinifyEnabled = false
<<<<<<< HEAD
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }

    // ← DELETE the entire composeOptions { } block — not needed with Kotlin 2.0 plugin
}

dependencies {
=======
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
>>>>>>> eb7d1561246a8482446ed58b38fa850a9a0307c9
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

<<<<<<< HEAD
=======
    // Compose BOM
>>>>>>> eb7d1561246a8482446ed58b38fa850a9a0307c9
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
<<<<<<< HEAD
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.android.material:material:1.12.0")
=======

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt DI
>>>>>>> eb7d1561246a8482446ed58b38fa850a9a0307c9
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

<<<<<<< HEAD
=======
    // Firebase
>>>>>>> eb7d1561246a8482446ed58b38fa850a9a0307c9
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

<<<<<<< HEAD
=======
    // Room
>>>>>>> eb7d1561246a8482446ed58b38fa850a9a0307c9
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

<<<<<<< HEAD
=======
    // Networking (for AI API)
>>>>>>> eb7d1561246a8482446ed58b38fa850a9a0307c9
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

<<<<<<< HEAD
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
}

kapt {
    correctErrorTypes = true
=======
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // PDF / File
    implementation("com.itextpdf:itextpdf:5.5.13.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
>>>>>>> eb7d1561246a8482446ed58b38fa850a9a0307c9
}