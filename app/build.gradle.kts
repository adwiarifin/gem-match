plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.kesatria.gemmatch"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.kesatria.gemmatch"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":core"))
}
