plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace = "com.example.testapptradeup"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.testapptradeup"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures.apply {
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
    //noinspection UseTomlInstead
    implementation(libs.firebase.storage)
    implementation (libs.lifecycle.viewmodel)
    implementation (libs.lifecycle.livedata)
    implementation (libs.lifecycle.common.java8)
    implementation (libs.navigation.safe.args.gradle.plugin)
    implementation (libs.circleimageview)

    implementation (libs.lifecycle.process)

    implementation(libs.google.firebase.messaging)
    implementation (libs.geofire.android)

    implementation(libs.firebase.appcheck.playintegrity)

    implementation(libs.flexbox)
    implementation (libs.geofire.android.common)
    implementation(libs.emoji.google)
    implementation(libs.emoji.android)
    implementation(libs.firebase.database.v2031)
    implementation(libs.circleindicator)

    debugImplementation(libs.firebase.appcheck.debug)
}
configurations.all {
    exclude(group = "xpp3", module = "xpp3")
}