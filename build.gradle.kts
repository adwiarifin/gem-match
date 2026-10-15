plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// Lockfiles feed osv-scanner (gradle.lockfile). Only what ships in the APK is locked: build tooling
// (AGP, detekt, ktlint) never reaches a player. Refresh with: ./gradlew dependencies --write-locks
allprojects {
    configurations
        .matching { it.name == "runtimeClasspath" || it.name == "releaseRuntimeClasspath" }
        .configureEach { resolutionStrategy.activateDependencyLocking() }
}
