import com.google.protobuf.gradle.id

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.hilt)
  id("kotlin-kapt")
  alias(libs.plugins.protobuf)
}

android {
  namespace = "dev.fanfly.apps.vellum"
  compileSdk = 36

  defaultConfig {
    applicationId = "dev.fanfly.apps.vellum"
    minSdk = 31
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(libs.flogger)
  implementation(libs.flogger.system.backend)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.hilt.android)
  implementation(libs.protobuf.javalite)
  implementation(libs.protobuf.kotlin.lite)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  kapt(libs.hilt.compiler)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}

// Add this new block to configure the Kotlin Protobuf generator
protobuf {
  protoc {
    // The version of protoc should match the version of your protobuf-kotlin-lite library.
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }

  // This block configures the protoc task to generate Kotlin code.
  generateProtoTasks {
    all().forEach { task ->
      task.builtins {
        // Enable the built-in Kotlin generator.
        id("kotlin") {
          // Use the "lite" option to generate code for the lite runtime,
          // which corresponds to your `protobuf-kotlin-lite` dependency.
          option("lite")
        }
        // Enable the built-in Java generator.
        id("java") {
          // Use the "lite" option to generate code for the lite runtime,
          // which corresponds to your `protobuf-javalite` dependency.
          option("lite")
        }
      }
    }
  }
}