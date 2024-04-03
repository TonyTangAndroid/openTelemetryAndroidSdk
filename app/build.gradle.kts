plugins {
    id("otel.android-app-conventions")
}

android {
    namespace = "com.example.hello_otel"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

}

dependencies {
    api(libs.timber)
    api(libs.opentelemetry.sdk.testing)
    api(libs.opentelemetry.exporter.otlp)
    api(libs.opentelemetry.exporter.logging)
    api(libs.opentelemetry.extension.trace.propagators)
    api(libs.okhttp.mockwebserver)
    api(project(":android-agent"))
    api(libs.annotationx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.awaitility)
    testImplementation(libs.truth)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.truth)


}
