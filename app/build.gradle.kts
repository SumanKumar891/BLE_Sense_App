plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.blegame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.blegame"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

//    buildFeatures{
//        compose = true
//    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    bundle{
        language{
            enableSplit = false
        }
    }

    packaging {
        resources.excludes += listOf(
            "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt"
        )
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "src/main/assets")
        }
    }
}

dependencies {

//    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
//    implementation(composeBom)
//    androidTestImplementation(composeBom)
//
//    // Choose one of the following:
//    // Material Design 3
//    implementation("androidx.compose.material3:material3")
//    // or Material Design 2
//    implementation("androidx.compose.material:material")
//    // or skip Material Design and build directly on top of foundational components
//    implementation("androidx.compose.foundation:foundation")
//    // or only import the main APIs for the underlying toolkit systems,
//    // such as input and measurement/layout
//    implementation("androidx.compose.ui:ui")
//
//    // Android Studio Preview support
//    implementation("androidx.compose.ui:ui-tooling-preview")
//    debugImplementation("androidx.compose.ui:ui-tooling")
//
//    // UI Tests
//    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
//    debugImplementation("androidx.compose.ui:ui-test-manifest")
//
//    // Optional - Included automatically by material, only add when you need
//    // the icons but not the material library (e.g. when using Material3 or a
//    // custom design system based on Foundation)
//    implementation("androidx.compose.material:material-icons-core")
//    // Optional - Add full set of material icons
//    implementation("androidx.compose.material:material-icons-extended")
//    // Optional - Add window size utils
//    implementation("androidx.compose.material3.adaptive:adaptive")
//
//    // Optional - Integration with activities
//    implementation("androidx.activity:activity-compose:1.9.2")
//    // Optional - Integration with ViewModels
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
//    // Optional - Integration with LiveData
//    implementation("androidx.compose.runtime:runtime-livedata")
//    // Optional - Integration with RxJava
//    implementation("androidx.compose.runtime:runtime-rxjava2")
//
//    implementation ("io.coil-kt:coil-compose:2.2.2")



























    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")

     // or the latest version


    // RecyclerView and Bluetooth dependencies
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.polidea.rxandroidble2:rxandroidble:1.11.1") {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // GIF handling and animations
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.25")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.androidx.constraintlayout)
    kapt("com.github.bumptech.glide:compiler:4.15.1")
    implementation("com.airbnb.android:lottie:6.1.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
