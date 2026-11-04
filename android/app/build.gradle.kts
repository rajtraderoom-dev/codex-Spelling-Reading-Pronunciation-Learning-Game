plugins { id("com.android.application") }
android {
    namespace = "com.wordgarden.app"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig { applicationId = "com.wordgarden.app"; minSdk = 24; targetSdk = 37; versionCode = 1; versionName = "1.0.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation("androidx.emoji2:emoji2-bundled:1.7.0")
    implementation("androidx.emoji2:emoji2-views:1.7.0")
    testImplementation("junit:junit:4.13.2")
}
