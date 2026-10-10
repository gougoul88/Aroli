plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.aroli.storybox"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aroli.storybox"
        minSdk = 29
        targetSdk = 34
        versionCode = 5
        versionName = "1.4"
    }

    // Signing configuration for release builds
    signingConfigs {
        create("release") {
            // Read password from .keystore.pass file (generated at project init)
            val passwordFile = rootProject.file(".keystore.pass")
            val password = if (passwordFile.exists()) {
                passwordFile.readText().trim()
            } else {
                project.findProperty("storePassword") as? String ?: ""
            }
            
            keyAlias = project.findProperty("alias") as? String ?: "aroli_release"
            keyPassword = password
            storeFile = rootProject.file("Aroli.keystore")
            storePassword = password
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            // For production, you may want to enable minification:
            // isMinifyEnabled = true
            // proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Playback
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")

    // Networking (GitHub-hosted manifest/audio fetch, Phase 3)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Cover art loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Per-profile playback position persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Manifest JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // SAF folder listing (LocalFolderRepository)
    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Task to generate keystore if it doesn't exist
tasks.register("generateKeystore") {
    doLast {
        val keystorePath = rootProject.file("Aroli.keystore")
        val passwordPath = rootProject.file(".keystore.pass")
        
        if (!passwordPath.exists()) {
            throw GradleException("Password file not found: ${passwordPath.absolutePath}")
        }
        
        val password = passwordPath.readText().trim()
        
        // Always delete and regenerate to ensure correct password
        if (keystorePath.exists()) {
            keystorePath.delete()
        }
        
        // Use Java's built-in KeyStore to create a PKCS12 keystore
        val keytoolPath = "${System.getProperty("java.home")}/bin/keytool"
        val pb = ProcessBuilder(
            keytoolPath,
            "-genkey", "-v", "-keystore", keystorePath.absolutePath,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-alias", "aroli_release",
            "-dname", "CN=Aroli Story Box,O=Aroli,L=France,ST=France,C=FR",
            "-storepass", password,
            "-keypass", password,
            "-storetype", "PKCS12"
        )
        
        val process = pb.start()
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        
        if (exitCode == 0) {
            println("✓ Keystore generated: ${keystorePath.absolutePath}")
        } else {
            throw GradleException("Failed to generate keystore (exit code $exitCode): $stderr")
        }
    }
}

// Make release build depend on keystore generation
tasks.configureEach {
    if (name == "assembleRelease" || name.contains("Release")) {
        dependsOn("generateKeystore")
    }
}
