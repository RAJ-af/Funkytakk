plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.funkytalk.app"
    minSdk = 28
    targetSdk = 36
    versionCode = project.findProperty("APP_VERSION_CODE")?.toString()?.toIntOrNull() ?: 1
    versionName = project.findProperty("APP_VERSION_NAME")?.toString() ?: "0.9.0-beta1"

    buildConfigField("String", "GROUP_INVITE_HOST", "\"funkytalk-f1aae.web.app\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("arm64-v8a", "armeabi-v7a")
      isUniversalApk = true
    }
  }

  val envReleaseStoreFile = System.getenv("RELEASE_STORE_FILE")
  val envReleaseStorePassword = System.getenv("RELEASE_STORE_PASSWORD")
  val envReleaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
  val envReleaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")

  val hasReleaseSigningCredentials = !envReleaseStoreFile.isNullOrBlank() &&
      !envReleaseStorePassword.isNullOrBlank() &&
      !envReleaseKeyAlias.isNullOrBlank() &&
      !envReleaseKeyPassword.isNullOrBlank() &&
      file(envReleaseStoreFile).exists()

  val projectDebugKeystore = rootProject.file("debug.keystore")

  signingConfigs {
    getByName("debug") {
      if (projectDebugKeystore.exists()) {
        storeFile = projectDebugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    if (hasReleaseSigningCredentials) {
      create("release") {
        storeFile = file(envReleaseStoreFile!!)
        storePassword = envReleaseStorePassword
        keyAlias = envReleaseKeyAlias
        keyPassword = envReleaseKeyPassword
      }
    }
  }

  val qaTestVaultPassword = run {
    val envFile = rootProject.file(".env")
    var found = ""
    if (envFile.exists()) {
      envFile.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("QA_TEST_VAULT_PASSWORD=")) {
          found = trimmed.substringAfter("QA_TEST_VAULT_PASSWORD=")
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        }
      }
    }
    found
  }

  buildTypes {
    release {
      buildConfigField("String", "QA_TEST_VAULT_PASSWORD", "\"\"")
      isDebuggable = false
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      manifestPlaceholders["firebase_crashlytics_collection_enabled"] = "true"
      configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
        mappingFileUploadEnabled = true
      }
      if (hasReleaseSigningCredentials) {
        signingConfig = signingConfigs.getByName("release")
      } else {
        signingConfig = null
        logger.warn("SECURITY WARNING: Release signing credentials (RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD) are not configured or keystore file does not exist. Release build will NOT be signed with debug keys.")
      }
    }
    debug {
      buildConfigField("String", "QA_TEST_VAULT_PASSWORD", "\"$qaTestVaultPassword\"")
      signingConfig = signingConfigs.getByName("debug")
      manifestPlaceholders["firebase_crashlytics_collection_enabled"] = "false"
      // Testing/Debug optimization: support 64-bit and 32-bit ARM physical test devices.
      ndk {
        abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  // Keep server-side secrets out of the client Android APK
  ignoreList.add("GEMINI_API_KEY")
  ignoreList.add("BREVO_API_KEY")
  ignoreList.add("BREVO_TEMPLATE_ID")
  ignoreList.add("QA_TEST_VAULT_PASSWORD")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.browser)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.messaging)
  implementation(libs.firebase.crashlytics)
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.appcheck.playintegrity)
  implementation(libs.play.services.auth)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.tink.android)
  implementation(libs.sqlcipher.android)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.ui)
  implementation(libs.telephoto.zoomable.image.coil)
  implementation(libs.lottie.compose)
  implementation(libs.coil.compose)
  implementation(libs.coil.svg)
  implementation(libs.coil.gif)
  implementation(libs.converter.moshi)
  implementation(libs.agora.voice)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation("org.json:json:20231013")
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.firebase.appcheck.debug)
  // "ksp"(libs.moshi.kotlin.codegen)
}
