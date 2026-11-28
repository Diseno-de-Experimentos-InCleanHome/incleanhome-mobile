plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/** Boolean BuildConfig value from a -P Gradle property; anything other than "true" counts as false. */
fun gradleFlag(name: String, default: Boolean): String =
    providers.gradleProperty(name).orElse(default.toString()).get().trim().equals("true", ignoreCase = true).toString()

/**
 * Release signing data, read only from ~/.gradle/gradle.properties, -P properties or environment
 * variables. Nothing is stored in the repository; without all four values the release APK is unsigned.
 */
fun signingValue(property: String, environment: String): String? =
    providers.gradleProperty(property).orElse(providers.environmentVariable(environment)).orNull?.takeIf(String::isNotBlank)

val releaseKeystore = signingValue("incleanhome.signing.storeFile", "INCLEANHOME_SIGNING_STORE_FILE")
val releaseStorePassword = signingValue("incleanhome.signing.storePassword", "INCLEANHOME_SIGNING_STORE_PASSWORD")
val releaseKeyAlias = signingValue("incleanhome.signing.keyAlias", "INCLEANHOME_SIGNING_KEY_ALIAS")
val releaseKeyPassword = signingValue("incleanhome.signing.keyPassword", "INCLEANHOME_SIGNING_KEY_PASSWORD")
val releaseSigningConfigured =
    listOf(releaseKeystore, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }

android {
    namespace = "com.incleanhome.mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.incleanhome.mobile"
        minSdk = 26
        targetSdk = 37
        // v1.0.0 was published with versionCode 1; 1.1.0 is the To-Be release (US32–US43).
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Radius limits of US32 until EXQ04 fixes them; they match ServiceArea:Min/MaxRadiusKm of the API.
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
            // Public API deployed by the backend team (the same one the web uses); HTTPS only.
            val productionApiUrl = providers.gradleProperty("incleanhome.productionApiUrl")
                .orElse("https://incleanhomebackend-q8za.onrender.com/api/")
                .get()
            require(productionApiUrl.startsWith("https://")) { "The production API must use HTTPS" }
            buildConfigField("String", "BASE_URL", "\"$productionApiUrl\"")
            // Experiments stay off for real users until the configuration endpoint enables them.
            buildConfigField(
                "String", "EXPERIMENTS",
                "\"EC-BLQ01=off:50;EC-BLQ02=off:50;EC-BLQ03=off:50;EC-BLQ06=off:50;EC-BLQ04=off:100;EC-BLQ01-SA=off:100\""
            )
            buildConfigField("int", "REVIEW_REMINDER_DELAY_HOURS", "24")
            buildConfigField("boolean", "DIAGNOSTICS_ENABLED", "false")
            buildConfigField("boolean", "TRACKING_LOGCAT", "false")
            // US43/US42 endpoints exist since incleanhome-backend v1.1.0, but production keeps them
            // off until the team authorises it (-Pincleanhome.remoteExperiments / trackingUpload).
            buildConfigField("boolean", "REMOTE_EXPERIMENTS_ENABLED", gradleFlag("incleanhome.remoteExperiments", false))
            buildConfigField("boolean", "TRACKING_UPLOAD_ENABLED", gradleFlag("incleanhome.trackingUpload", false))
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
            // The testing API publishes GET experiments/config and POST tracking/events (backend v1.1.0).
            buildConfigField("boolean", "REMOTE_EXPERIMENTS_ENABLED", gradleFlag("incleanhome.remoteExperiments", true))
            buildConfigField("boolean", "TRACKING_UPLOAD_ENABLED", gradleFlag("incleanhome.trackingUpload", true))
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeystore!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
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
