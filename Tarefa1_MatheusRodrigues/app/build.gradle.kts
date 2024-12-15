plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.tarefa1_matheusrodrigues"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tarefa1_matheusrodrigues"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":RaceLibrary")) // Referência correta ao módulo RaceLibrary
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-common-ktx:20.2.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.1.1")
    implementation("androidx.test.ext:junit:1.1.3")
    implementation("androidx.core:core-ktx:1.6.0")

    //implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Dependências de Teste
    testImplementation("junit:junit:4.13.2")
}
