
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.raphou.bubbly.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {

    implementation(project(":core:domain"))
    implementation(libs.androidx.lifecycle.runtime.ktx)


    //DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    //Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //Datastore
    implementation(libs.androidx.core.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.datastore)

    //Retrofit
    implementation(libs.squareup.retrofit.runtime)
    implementation(libs.squareup.retrofit.gson)

}
