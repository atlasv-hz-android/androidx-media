plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.atlasv.android.plugin.publishlib")
}

android {
    namespace = "com.atlasv.android.mediax.downloader"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"${project.property("LIB_VERSION")}\"")
    }

    buildTypes {
        release {
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    api(project(":lib-datasource"))
    // Release
    releaseApi(libs.androidx.media3.datasource.okhttp)
    releaseApi(libs.androidx.media3.database)
    releaseApi(libs.androidx.media3.common)
    releaseApi(libs.androidx.media3.exoplayer.hls)
    // debug
    debugApi(project(":lib-datasource-okhttp"))
    debugApi(project(":lib-database"))
    debugApi(project(":lib-common"))
    debugApi(project(":lib-exoplayer-hls"))

    api(libs.atlasv.loader)
}