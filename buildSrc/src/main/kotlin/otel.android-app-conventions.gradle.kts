import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.application")
    id("otel.errorprone-conventions")
}

android {
    namespace = "io.opentelemetry.android"
    compileSdk = (property("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("android.minSdk") as String).toInt()
        targetSdk = (property("android.targetSdk") as String).toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        val javaVersion = rootProject.extra["java_version"] as JavaVersion
        sourceCompatibility(javaVersion)
        targetCompatibility(javaVersion)
    }
}

val otelVersion = rootProject.property("otel.sdk.version")
dependencies {
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("io.opentelemetry:opentelemetry-sdk-testing:$otelVersion")
}