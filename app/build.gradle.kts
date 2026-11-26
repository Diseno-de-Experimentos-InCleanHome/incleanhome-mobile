plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.incleanhome.mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.incleanhome.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Radius limits of US32 until EXQ04 fixes them (assumption documented in the TB2 evidence).
        buildConfigField("int", "SERVICE_RADIUS_MIN_KM", "1")
        buildConfigField("int", "SERVICE_RADIUS_MAX_KM", "15")
    }

    // Production and testing environments (TB2). No secrets are stored here: URLs are public and
    // the testing API URL is supplied by the backend team through a Gradle property.
    flavorDimensions += "environment"
    productFlavors {
        create("production") {
            dimension = "environment"
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
            buildConfigField("String", "BASE_URL", "\"https://incleanhome-api-e4tv.onrender.com/api/\"")
            // Experiments stay off for real users until the configuration endpoint enables them.
            buildConfigField(
                "String", "EXPERIMENTS",
                "\"EC-BLQ01=off:50;EC-BLQ02=off:50;EC-BLQ03=off:50;EC-BLQ06=off:50;EC-BLQ04=off:100;EC-BLQ01-SA=off:100\""
            )
            buildConfigField("int", "REVIEW_REMINDER_DELAY_HOURS", "24")
            buildConfigField("boolean", "DIAGNOSTICS_ENABLED", "false")
            buildConfigField("boolean", "TRACKING_LOGCAT", "false")
        }
        create("staging") {
            dimension = "environment"
            // Default variant in the IDE, so a debug run never writes test data to production.
            isDefault = true
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "ENVIRONMENT", "\"staging\"")
            val stagingApiUrl = providers.gradleProperty("incleanhome.stagingApiUrl")
                .orElse("http://10.0.2.2:5000/api/")
                .get()
            buildConfigField("String", "BASE_URL", "\"$stagingApiUrl\"")
            val stagingExperiments = providers.gradleProperty("incleanhome.stagingExperiments")
                .orElse("EC-BLQ01=on:50;EC-BLQ02=on:50;EC-BLQ03=off:50;EC-BLQ06=on:50;EC-BLQ04=on:100;EC-BLQ01-SA=on:100")
                .get()
            buildConfigField("String", "EXPERIMENTS", "\"$stagingExperiments\"")
            buildConfigField("int", "REVIEW_REMINDER_DELAY_HOURS", "0")
            buildConfigField("boolean", "DIAGNOSTICS_ENABLED", "true")
            buildConfigField("boolean", "TRACKING_LOGCAT", "true")
        }
    }

    buildTypes {
        all {
            // Endpoints still missing in incleanhome-backend (docs/backend-endpoints.md); enable once deployed.
            buildConfigField(
                "boolean", "REMOTE_EXPERIMENTS_ENABLED",
                providers.gradleProperty("incleanhome.remoteExperiments").orElse("false").get()
            )
            buildConfigField(
                "boolean", "TRACKING_UPLOAD_ENABLED",
                providers.gradleProperty("incleanhome.trackingUpload").orElse("false").get()
            )
            buildConfigField(
                "boolean", "SERVICE_AREA_API_ENABLED",
                providers.gradleProperty("incleanhome.serviceAreaApi").orElse("false").get()
            )
        }
        release {
            optimization {
                enable = false
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
    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
