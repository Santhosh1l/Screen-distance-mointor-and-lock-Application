plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.myapplication" // Specify the namespace here
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.0.0-alpha29")
    implementation("androidx.camera:camera-extensions:1.0.0-alpha29")
    testImplementation ("junit:junit:4.13.2")

    // Mockito for mocking dependencies
    testImplementation ("org.mockito:mockito-core:4.8.0")

    // AndroidX Test - Core Testing libraries
    androidTestImplementation ("androidx.test:core:1.4.0")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")

    // Optional: Espresso for UI testing
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
    // ML Kit face detection
    implementation("com.google.mlkit:face-detection:16.0.0")
    implementation(libs.core)
    implementation(libs.ext.junit)
    testImplementation(libs.junit.junit)
}
