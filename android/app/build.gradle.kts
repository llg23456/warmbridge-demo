plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/** 在 android/local.properties 增加一行：warmbridge.api.baseUrl=http://你的电脑IP:8000/真机必配；模拟器可省略（默认 10.0.2.2） */
val warmbridgeApiBaseUrl: String =
    run {
        val fallback = "http://10.0.2.2:8000/"
        val f = rootProject.file("local.properties")
        if (!f.exists()) return@run fallback
        val key = "warmbridge.api.baseUrl"
        val line = f.readLines()
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?: return@run fallback
        val v = line.removePrefix("$key=").trim()
        when {
            v.isEmpty() -> fallback
            v.endsWith("/") -> v
            else -> "$v/"
        }
    }

android {
    namespace = "com.warmbridge.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.warmbridge.demo"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-demo"

        buildConfigField("String", "API_BASE_URL", "\"$warmbridgeApiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
