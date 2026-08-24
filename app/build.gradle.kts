import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from local.properties (gitignored, per-machine) so the
// keystore/passwords never touch source control. Absent on machines that
// haven't set it up (e.g. CI) -- release build type just stays unsigned then.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE")

// versionCode is derived from the commit count so releases need no manual edit.
// - Play only requires the number to increase; gaps between releases are fine.
// - Shallow clones under-count, so CI checkouts must use fetch-depth: 0.
// - Fails the build rather than guessing: a published code can never be reused.
val gitVersionCode: Int = run {
    val git = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }
    check(git.result.get().exitValue == 0) {
        "Could not read the git commit count for versionCode -- build from a full git clone."
    }
    git.standardOutput.asText.get().trim().toInt()
}

android {
    namespace = "com.davidp.simpleweeklyreminders"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.davidp.simpleweeklyreminders"
        minSdk = 26
        targetSdk = 36
        versionCode = gitVersionCode
        versionName = "1.11"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Bundle native symbols so Play can symbolicate crashes in the .so
            // files our AndroidX deps ship (graphics-path, datastore). Stored in
            // AAB metadata -- stripped before delivery, so no user download cost.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Enable Room schema export for migrations
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Compose-aware lifecycle helpers (LocalLifecycleOwner moved here from compose-ui)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Settings storage
    implementation(libs.androidx.datastore.preferences)

    // Drag to reorder
    implementation(libs.reorderable)
}

// Reports the version Gradle resolved, without building: ./gradlew -q :app:printVersion
tasks.register("printVersion") {
    val code = android.defaultConfig.versionCode
    val name = android.defaultConfig.versionName
    doLast { println("versionCode=$code versionName=$name") }
}
