plugins {
    alias(libs.plugins.generic.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.generic.hilt)
}

android {
    namespace = "org.withus.app.smartlotomobile.data.mapper"
}

dependencies {
    implementation(project(":domain:model"))
    implementation(project(":common:utils"))
    implementation(project(":data:datasource:local"))
    implementation(project(":data:datasource:remote"))
}