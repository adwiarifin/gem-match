plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// The one place the version is written. release-please rewrites the string on the marked line.
val appVersion = "0.3.2" // x-release-please-version

// 1.4.2 becomes 10402, so versionCode only ever grows with the version and never needs a manual bump.
// Every 0.x.y stays below 1.0.0 (10000). Minor and patch must stay below 100 or two versions would share a code.
val appVersionCode =
    appVersion.split(".").map { it.toInt() }.let { parts ->
        require(parts.size == 3) { "appVersion must be plain major.minor.patch, got $appVersion" }
        val (major, minor, patch) = parts
        require(minor < 100 && patch < 100) { "versionCode needs minor and patch below 100, got $appVersion" }
        major * 10_000 + minor * 100 + patch
    }

android {
    namespace = "dev.kesatria.gemmatch"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.kesatria.gemmatch"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersion
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
    testImplementation(libs.kotlin.test.junit5)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

detekt {
    buildUponDefaultConfig = true
}
