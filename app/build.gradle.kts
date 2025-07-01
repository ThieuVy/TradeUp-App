plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.testapptradeup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.testapptradeup"
        minSdk = 24
        targetSdk = 35
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.play.services.location)
    implementation(libs.firebase.firestore)
    implementation(libs.swiperefreshlayout)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.firebase.auth.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.auth)
    implementation (libs.gson)
    implementation (libs.glide)
    annotationProcessor (libs.compiler)
    implementation (libs.recyclerview)
    implementation(libs.firebase.analytics)

    // Cloudinary SDK for image uploads and management
    implementation (libs.cloudinary.android.v302)
    // Stripe SDK for payment processing
    implementation (libs.stripe.android)
    // Firebase Cloud Functions SDK (for calling backend functions securely)
    implementation (libs.firebase.functions)
    implementation (libs.play.services.auth)
//    implementation (libs.identity.credential.binding) // Sử dụng phiên bản mới nhất
//    implementation (libs.googleid.v120) // Sử dụng phiên bản mới nhất
}