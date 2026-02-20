import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include

plugins {
    alias(libs.plugins.generic.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.generic.hilt)
}

android {
    namespace = "org.withus.app.smartlotomobile.data.repository"
}

dependencies {
    implementation(project(":common:utils"))
    implementation(project(":domain:model"))
    implementation(project(":domain:interfaces"))
    implementation(project(":data:datasource:local"))
    implementation(project(":data:datasource:remote"))
    implementation(project(":data:datasource:remote"))
    implementation(project(":data:mapper"))

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
}